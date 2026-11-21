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
import app.video.player.domain.network.encodeNetworkPathForUrl
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

class FtpClient(
  private val connection: NetworkConnection,
) : NetworkClient {
  private var client: FTPClient? = null

  override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      disconnect()
      val ftp = FTPClient().apply {
        connectTimeout = 15_000
        defaultTimeout = 15_000
        dataTimeout = 30_000
        controlEncoding = "UTF-8"
        autodetectUTF8 = true
      }

      val port = if (connection.port > 0) connection.port else connection.protocol.defaultPort
      ftp.connect(connection.host, port)

      val reply = ftp.replyCode
      if (!FTPReply.isPositiveCompletion(reply)) {
        ftp.disconnect()
        throw IOException("FTP server refused connection with reply code: $reply")
      }

      val loginSuccess = if (connection.isAnonymous || connection.username.isBlank()) {
        ftp.login("anonymous", "anonymous@")
      } else {
        ftp.login(connection.username, connection.password)
      }

      if (!loginSuccess) {
        val loginReply = ftp.replyCode
        ftp.disconnect()
        throw NetworkAuthenticationException("FTP authentication failed (reply code: $loginReply)")
      }

      ftp.enterLocalPassiveMode()
      ftp.setFileType(FTP.BINARY_FILE_TYPE)
      ftp.bufferSize = 64 * 1024

      client = ftp
    }
  }

  override suspend fun disconnect() = withContext(Dispatchers.IO) {
    runCatching {
      client?.let {
        if (it.isConnected) {
          try {
            it.logout()
          } catch (_: Exception) {}
          it.disconnect()
        }
      }
    }
    client = null
  }

  override fun isConnected(): Boolean = client?.isConnected == true

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> = withContext(Dispatchers.IO) {
    runCatching {
      val ftp = ensureConnected()
      val targetPath = resolvePath(path)
      val files = ftp.listFiles(targetPath) ?: emptyArray()
      
      files.filter { file ->
        val name = file.name
        name != "." && name != ".." && !name.isNullOrBlank()
      }.map { file ->
        val fullPath = if (targetPath.endsWith("/")) {
          "$targetPath${file.name}"
        } else {
          "$targetPath/${file.name}"
        }
        val cleanPath = NetworkPath.from(fullPath).value
        NetworkFile(
          name = file.name,
          path = cleanPath,
          size = file.size,
          isDirectory = file.isDirectory,
          lastModified = file.timestamp?.timeInMillis ?: 0L,
        )
      }
    }
  }

  override suspend fun getFileStream(
    path: String,
    offset: Long,
  ): Result<InputStream> = withContext(Dispatchers.IO) {
    runCatching {
      val ftp = ensureConnected()
      val targetPath = resolvePath(path)
      
      if (offset > 0L) {
        ftp.setRestartOffset(offset)
      }
      val stream = ftp.retrieveFileStream(targetPath)
        ?: throw IOException("Failed to retrieve FTP file stream: ${ftp.replyString}")

      // Wrap InputStream to complete pending FTP command on stream close
      object : InputStream() {
        override fun read(): Int = stream.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)
        override fun available(): Int = stream.available()
        override fun skip(n: Long): Long = stream.skip(n)
        override fun close() {
          try {
            stream.close()
          } finally {
            runCatching { ftp.completePendingCommand() }
          }
        }
      }
    }
  }

  override suspend fun getFileSize(path: String): Result<Long> = withContext(Dispatchers.IO) {
    runCatching {
      val ftp = ensureConnected()
      val targetPath = resolvePath(path)
      val mlist = ftp.mlistFile(targetPath)
      if (mlist != null && mlist.size >= 0L) {
        mlist.size
      } else {
        // Fallback to directory listing
        val parent = targetPath.substringBeforeLast('/', "")
        val fileName = targetPath.substringAfterLast('/')
        val files = ftp.listFiles(if (parent.isEmpty()) "/" else parent) ?: emptyArray()
        files.firstOrNull { it.name == fileName }?.size ?: -1L
      }
    }
  }

  override suspend fun getFileUri(path: String): Result<Uri> = runCatching {
    val cleanPath = resolvePath(path)
    val port = if (connection.port > 0) connection.port else connection.protocol.defaultPort
    val portStr = if (port == 21) "" else ":$port"
    val encodedPath = encodeNetworkPathForUrl(cleanPath)
    Uri.parse("ftp://${connection.host}$portStr$encodedPath")
  }

  private suspend fun ensureConnected(): FTPClient {
    val active = client
    if (active != null && active.isConnected) {
      return active
    }
    connect().getOrThrow()
    return checkNotNull(client)
  }

  private fun resolvePath(path: String): String {
    val configuredRoot = connection.path.trim().ifEmpty { "/" }
    val normalizedPath = path.trim()
    return if (normalizedPath.isEmpty() || normalizedPath == "/") {
      configuredRoot
    } else if (configuredRoot == "/") {
      if (normalizedPath.startsWith("/")) normalizedPath else "/$normalizedPath"
    } else {
      val cleanRoot = configuredRoot.removeSuffix("/")
      val cleanPath = normalizedPath.removePrefix("/")
      "$cleanRoot/$cleanPath"
    }
  }
}
