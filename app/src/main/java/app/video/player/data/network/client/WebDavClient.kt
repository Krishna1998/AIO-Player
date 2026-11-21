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
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDavClient(
  private val connection: NetworkConnection,
) : NetworkClient {
  private var sardine: Sardine? = null
  private var connected: Boolean = false

  override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      disconnect()
      val client = OkHttpSardine()
      if (!connection.isAnonymous && connection.username.isNotBlank()) {
        client.setCredentials(connection.username, connection.password)
      }
      sardine = client
      // Test connectivity by querying the root directory
      val rootUrl = buildUrl(connection.path.ifEmpty { "/" })
      try {
        client.exists(rootUrl)
      } catch (e: Exception) {
        val message = e.message.orEmpty()
        if (message.contains("401") || message.contains("403")) {
          throw NetworkAuthenticationException("WebDAV authentication failed: $message", e)
        }
        throw e
      }
      connected = true
    }
  }

  override suspend fun disconnect() = withContext(Dispatchers.IO) {
    connected = false
    sardine = null
  }

  override fun isConnected(): Boolean = connected && sardine != null

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> = withContext(Dispatchers.IO) {
    runCatching {
      val client = ensureConnected()
      val url = buildUrl(path)
      val resources: List<DavResource> = client.list(url)
      
      // Sardine list() includes the queried directory itself as the first resource
      val currentPathSuffix = path.trim().removePrefix("/").removeSuffix("/")
      
      resources.filter { resource ->
        val resPath = resource.path.trim().removePrefix("/").removeSuffix("/")
        // Filter out the requested directory itself
        resPath.isNotEmpty() && resPath != currentPathSuffix
      }.map { resource ->
        val name = resource.name.ifEmpty {
          resource.path.trim().removeSuffix("/").substringAfterLast('/')
        }
        val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        val cleanPath = NetworkPath.from(fullPath).value
        
        NetworkFile(
          name = name,
          path = cleanPath,
          size = resource.contentLength ?: 0L,
          isDirectory = resource.isDirectory,
          lastModified = resource.modified?.time ?: 0L,
          mimeType = resource.contentType,
        )
      }
    }
  }

  override suspend fun getFileStream(
    path: String,
    offset: Long,
  ): Result<InputStream> = withContext(Dispatchers.IO) {
    runCatching {
      val client = ensureConnected()
      val url = buildUrl(path)
      val headers = if (offset > 0L) {
        mapOf("Range" to "bytes=$offset-")
      } else {
        emptyMap()
      }
      client.get(url, headers)
    }
  }

  override suspend fun getFileSize(path: String): Result<Long> = withContext(Dispatchers.IO) {
    runCatching {
      val client = ensureConnected()
      val url = buildUrl(path)
      val resources = client.list(url)
      resources.firstOrNull()?.contentLength ?: -1L
    }
  }

  override suspend fun getFileUri(path: String): Result<Uri> = runCatching {
    val url = buildUrl(path)
    Uri.parse(url)
  }

  private suspend fun ensureConnected(): Sardine {
    val active = sardine
    if (active != null && connected) {
      return active
    }
    connect().getOrThrow()
    return checkNotNull(sardine)
  }

  private fun buildUrl(subPath: String): String {
    val scheme = if (connection.useHttps) "https" else "http"
    val defaultPort = if (connection.useHttps) 443 else 80
    val port = if (connection.port > 0) connection.port else defaultPort
    val portStr = if (port == defaultPort) "" else ":$port"
    
    val configuredRoot = connection.path.trim().ifEmpty { "/" }
    val normalizedSub = subPath.trim()
    
    val combinedPath = if (normalizedSub.isEmpty() || normalizedSub == "/") {
      configuredRoot
    } else if (configuredRoot == "/") {
      if (normalizedSub.startsWith("/")) normalizedSub else "/$normalizedSub"
    } else {
      val cleanRoot = configuredRoot.removeSuffix("/")
      val cleanSub = normalizedSub.removePrefix("/")
      "$cleanRoot/$cleanSub"
    }
    
    val encodedPath = encodeNetworkPathForUrl(combinedPath)
    return "$scheme://${connection.host}$portStr$encodedPath"
  }
}
