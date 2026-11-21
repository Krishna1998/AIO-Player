/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.data.network.client

import android.net.Uri
import app.video.player.domain.network.NetworkConnection
import app.video.player.domain.network.NetworkFile
import app.video.player.domain.network.NetworkPath
import app.video.player.domain.network.SmbSharePath
import app.video.player.domain.network.SmbUsername
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.IOException
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmbClient(
  private val connection: NetworkConnection,
) : NetworkClient {
  private var smbClient: SMBClient? = null
  private var smbConnection: Connection? = null
  private var session: Session? = null
  private val openShares = mutableMapOf<String, DiskShare>()
  private val smbSharePath = SmbSharePath(connection.path)

  override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      disconnect()
      val config = SmbConfig.builder()
        .withTimeout(15, TimeUnit.SECONDS)
        .withSoTimeout(25, TimeUnit.SECONDS)
        .build()
      val client = SMBClient(config)
      smbClient = client

      val port = if (connection.port > 0) connection.port else connection.protocol.defaultPort
      val conn = client.connect(connection.host, port)
      smbConnection = conn

      val parsedUser = SmbUsername.parse(connection.username)
      val authContext = if (connection.isAnonymous || connection.username.isBlank()) {
        AuthenticationContext.anonymous()
      } else {
        AuthenticationContext(
          parsedUser.username,
          connection.password.toCharArray(),
          parsedUser.domain.orEmpty(),
        )
      }

      try {
        session = conn.authenticate(authContext)
      } catch (e: Exception) {
        throw NetworkAuthenticationException("SMB authentication failed: ${e.message}", e)
      }
    }
  }

  override suspend fun disconnect() = withContext(Dispatchers.IO) {
    openShares.values.forEach { share ->
      runCatching { share.close() }
    }
    openShares.clear()
    runCatching { session?.close() }
    session = null
    runCatching { smbConnection?.close() }
    smbConnection = null
    runCatching { smbClient?.close() }
    smbClient = null
  }

  override fun isConnected(): Boolean =
    session != null && smbConnection?.isConnected == true

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> = withContext(Dispatchers.IO) {
    runCatching {
      ensureConnected()
      val target = smbSharePath.resolve(NetworkPath.from(path))
      val share = getOrOpenShare(target.shareName)
      val smbSubPath = target.path.relative.replace('/', '\\')

      val fileList = share.list(smbSubPath)
      fileList.filter { fileInfo ->
        val name = fileInfo.fileName
        name != "." && name != ".." && !name.isNullOrBlank()
      }.map { fileInfo ->
        val name = fileInfo.fileName
        val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        val cleanPath = NetworkPath.from(fullPath).value
        val isDir = (fileInfo.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
        
        NetworkFile(
          name = name,
          path = cleanPath,
          size = fileInfo.endOfFile,
          isDirectory = isDir,
          lastModified = fileInfo.changeTime?.toEpochMillis() ?: 0L,
        )
      }
    }
  }

  override suspend fun getFileStream(
    path: String,
    offset: Long,
  ): Result<InputStream> = withContext(Dispatchers.IO) {
    runCatching {
      ensureConnected()
      val target = smbSharePath.resolve(NetworkPath.from(path))
      val share = getOrOpenShare(target.shareName)
      val smbSubPath = target.path.relative.replace('/', '\\')

      val file = share.openFile(
        smbSubPath,
        EnumSet.of(AccessMask.GENERIC_READ),
        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
        EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
        SMB2CreateDisposition.FILE_OPEN,
        null,
      )

      val inputStream = file.inputStream
      if (offset > 0L) {
        var skipped = 0L
        while (skipped < offset) {
          val s = inputStream.skip(offset - skipped)
          if (s <= 0L) {
            if (inputStream.read() == -1) break
            skipped += 1
          } else {
            skipped += s
          }
        }
      }

      // Close both file and its stream when consumer closes
      object : InputStream() {
        override fun read(): Int = inputStream.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = inputStream.read(b, off, len)
        override fun available(): Int = inputStream.available()
        override fun skip(n: Long): Long = inputStream.skip(n)
        override fun close() {
          try {
            inputStream.close()
          } finally {
            runCatching { file.close() }
          }
        }
      }
    }
  }

  override suspend fun getFileSize(path: String): Result<Long> = withContext(Dispatchers.IO) {
    runCatching {
      ensureConnected()
      val target = smbSharePath.resolve(NetworkPath.from(path))
      val share = getOrOpenShare(target.shareName)
      val smbSubPath = target.path.relative.replace('/', '\\')
      val info = share.getFileInformation(smbSubPath)
      info.standardInformation.endOfFile
    }
  }

  override suspend fun getFileUri(path: String): Result<Uri> = runCatching {
    val port = if (connection.port > 0) connection.port else connection.protocol.defaultPort
    val portStr = if (port == 445) "" else ":$port"
    val cleanPath = NetworkPath.from(path).value
    Uri.parse("smb://${connection.host}$portStr$cleanPath")
  }

  private suspend fun ensureConnected(): Session {
    val s = session
    if (s != null && smbConnection?.isConnected == true) {
      return s
    }
    connect().getOrThrow()
    return checkNotNull(session)
  }

  private fun getOrOpenShare(shareName: String): DiskShare {
    openShares[shareName]?.let { return it }
    val s = checkNotNull(session) { "SMB session not connected" }
    val diskShare = s.connectShare(shareName) as? DiskShare
      ?: throw IOException("Share '$shareName' is not a disk share")
    openShares[shareName] = diskShare
    return diskShare
  }
}
