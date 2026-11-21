/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.networkstreaming

import android.content.Context
import app.video.player.ui.utils.NavigationBackHandler as BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import app.video.player.presentation.components.AppTextField as OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.video.player.R
import app.video.player.database.entities.NetworkStreamEntryEntity
import app.video.player.database.repository.NetworkStreamEntryRepository
import app.video.player.domain.network.ConnectionStatus
import app.video.player.domain.network.NetworkConnection
import app.video.player.preferences.YtdlPreferences
import app.video.player.preferences.NetworkBookmarkPreferences
import app.video.player.preferences.preference.collectAsState
import app.video.player.presentation.Screen
import app.video.player.repository.wyzie.WyzieSearchRepository
import app.video.player.utils.media.MediaInfoParser
import app.video.player.ui.browser.cards.NetworkConnectionCard
import app.video.player.ui.browser.components.BrowserTopBar
import app.video.player.ui.browser.dialogs.AddConnectionSheet
import app.video.player.ui.browser.dialogs.EditConnectionSheet
import app.video.player.ui.browser.dialogs.RenameDialog
import app.video.player.ui.components.InlineSearchBar
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.player.ytdlp.YtdlpInstallPromptDialog
import app.video.player.ui.player.ytdlp.YtdlpInstallProgressDialog
import app.video.player.ui.player.ytdlp.YtdlpManager
import app.video.player.ui.preferences.PreferencesScreen
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.navigateTo
import app.video.player.ui.utils.popSafely
import app.video.player.utils.media.SharedUrlExtractor
import app.video.player.utils.media.MediaUtils
import app.video.player.utils.media.HttpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject


@Serializable
object NetworkStreamingScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val viewModel: NetworkStreamingViewModel =
      viewModel(factory = NetworkStreamingViewModel.factory(context.applicationContext as android.app.Application))
    val ytdlPreferences = koinInject<YtdlPreferences>()
    val bookmarkPreferences = koinInject<NetworkBookmarkPreferences>()
    val linkDownloadCoordinator = koinInject<app.video.player.domain.download.LinkDownloadCoordinator>()
    val connections by viewModel.connections.collectAsState()
    val folderBookmarks by bookmarkPreferences.bookmarks.collectAsState()
    val connectionStatuses by viewModel.connectionStatuses.collectAsState()
    val recentLinks by viewModel.recentLinks.collectAsState()
    var renamingEntry by remember { mutableStateOf<NetworkStreamEntryEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<NetworkConnection?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val navigationHaptics = app.video.player.ui.utils.rememberAppHaptics()

    // yt-dlp install gate for the "paste link -> Play" flow: instead of silently installing
    // yt-dlp in the background while the player buffers on first use, we ask up front.
    var pendingYtdlpUrl by remember { mutableStateOf<String?>(null) }
    var showYtdlpInstallPrompt by remember { mutableStateOf(false) }
    var showYtdlpInstallProgress by remember { mutableStateOf(false) }
    var ytdlpInstallLastLog by remember { mutableStateOf("") }
    var ytdlpInstallError by remember { mutableStateOf<String?>(null) }
    var ytdlpInstallJob by remember { mutableStateOf<Job?>(null) }
    var linkPlaybackJob by remember { mutableStateOf<Job?>(null) }

    fun proceedToPlay(url: String) {
      if (linkPlaybackJob?.isActive == true) return
      linkPlaybackJob =
        coroutineScope.launch {
          try {
            val resolved = HttpUtils.resolveDirectStreamUrl(url)
            val playableSource = resolved.streamUrl
            val resolvedTitle = resolved.title

            val extractedPlaylist =
              if (YtdlpManager.isPotentialPlaylistUrl(playableSource)) {
                YtdlpManager.extractPlaylist(context, playableSource, ytdlPreferences).getOrNull()
              } else {
                null
              }
            viewModel.recordSubmittedLink(playableSource, resolvedTitle)
            if (extractedPlaylist != null) {
              YtdlpManager.playPlaylist(context, extractedPlaylist, "network_stream")
            } else {
              MediaUtils.playFile(playableSource, context, "network_stream", title = resolvedTitle)
            }
          } finally {
            linkPlaybackJob = null
          }
        }
    }

    fun playLinkGatingYtdlp(url: String) {
      if (YtdlpManager.requiresYtdlp(url) && !YtdlpManager.isInstalled(context)) {
        pendingYtdlpUrl = url
        showYtdlpInstallPrompt = true
      } else {
        proceedToPlay(url)
      }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
      if (isSearching) focusRequester.requestFocus()
    }

    val filteredConnections =
      remember(connections, searchQuery) {
        if (searchQuery.isBlank()) {
          connections
        } else {
          connections.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
              it.host.contains(searchQuery, ignoreCase = true) ||
              it.protocol.displayName.contains(searchQuery, ignoreCase = true)
          }
        }
      }

    val filteredRecentLinks =
      remember(recentLinks, searchQuery) {
        recentLinks.filter { entry ->
          searchQuery.isBlank() ||
            entry.fileName.contains(searchQuery, ignoreCase = true) ||
            entry.canonicalSourceUri.contains(searchQuery, ignoreCase = true)
        }
      }

    val resolvedFolderBookmarks =
      remember(folderBookmarks, connections) {
        resolveNetworkFolderBookmarks(folderBookmarks, connections)
      }

    val filteredFolderBookmarks =
      remember(resolvedFolderBookmarks, searchQuery) {
        if (searchQuery.isBlank()) {
          resolvedFolderBookmarks
        } else {
          resolvedFolderBookmarks.filter { item ->
            item.bookmark.folderName.contains(searchQuery, ignoreCase = true) ||
              item.connection.name.contains(searchQuery, ignoreCase = true) ||
              item.bookmark.path.contains(searchQuery, ignoreCase = true)
          }
        }
      }

    BackHandler(enabled = isSearching) {
      isSearching = false
      searchQuery = ""
    }

    val headerContainerColor =
      if (app.video.player.ui.theme.LocalAppWallpaperActive.current) {
        Color.Transparent
      } else if (MaterialTheme.colorScheme.background == Color.Black) {
        Color.Black
      } else {
        MaterialTheme.colorScheme.surfaceContainer
      }

    Scaffold(
      containerColor = app.video.player.ui.theme.wallpaperAwareBackgroundColor(),
      modifier = Modifier.fillMaxSize(),
      topBar = {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(headerContainerColor),
        ) {
          if (isSearching) {
            InlineSearchBar(
              query = searchQuery,
              onQueryChange = { searchQuery = it },
              onSearch = { },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              inputFieldModifier = Modifier.focusRequester(focusRequester),
              placeholder = {
                Text(stringResource(R.string.settings_search_title))
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.RoundedFilled.Search,
                  contentDescription = stringResource(R.string.settings_search_title),
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = {
                    isSearching = false
                    searchQuery = ""
                  },
                ) {
                  Icon(
                    imageVector = Icons.RoundedFilled.Close,
                    contentDescription = stringResource(R.string.generic_cancel),
                  )
                }
              },
              shape = MaterialTheme.shapes.extraLarge,
              tonalElevation = 6.dp,
            )
          } else {
            Box {
              BrowserTopBar(
                title = stringResource(R.string.ui_online_streaming),
                isInSelectionMode = false,
                selectedCount = 0,
                totalCount = connections.size + recentLinks.size + resolvedFolderBookmarks.size,
                onBackClick =
                  if (app.video.player.ui.browser.LocalIsMainTabPage.current) {
                    null
                  } else {
                    { backstack.popSafely() }
                  },
                onCancelSelection = { },
                onSortClick = null,
                onSearchClick = null,
                onSettingsClick = {
                  backstack.navigateTo(app.video.player.ui.preferences.PreferencesScreen)
                },
                onDeleteClick = null,
                onRenameClick = null,
                isSingleSelection = false,
                onInfoClick = null,
                onShareClick = null,
                onPlayClick = null,
                onSelectAll = null,
                onInvertSelection = null,
                onDeselectAll = null,
                additionalActions = {},
              )
            }
          }

        }
      },
      floatingActionButton = {},
    ) { padding ->
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(padding),
      ) {
        LocalNetworkContent(
          connections = filteredConnections,
          bookmarks = filteredFolderBookmarks,
          connectionStatuses = connectionStatuses,
          recentLinks = filteredRecentLinks,
          isPreparingLink =
            showYtdlpInstallPrompt ||
              showYtdlpInstallProgress ||
              linkPlaybackJob?.isActive == true,
          onPlayLink = { url ->
            val playableSource = url.trim()
            playLinkGatingYtdlp(playableSource)
          },
          onPlayRecent = { entry ->
            viewModel.recordExistingLinkPlayed(entry.stableKey)
            MediaUtils.playFile(
              source = entry.canonicalSourceUri,
              context = context,
              launchSource = "network_recent",
              title = entry.fileName,
            )
          },
          onRenameRecent = { entry -> renamingEntry = entry },
          onDeleteRecent = viewModel::deleteStreamEntry,
          onConnect = { viewModel.connect(it) },
          onDisconnect = { viewModel.disconnect(it) },
          onEdit = { editingConnection = it },
          onDelete = { viewModel.deleteConnection(it) },
          onBrowse = { conn, status ->
            if (status?.isConnected == true) {
              backstack.navigateTo(
                NetworkBrowserScreen(
                  connectionId = conn.id,
                  connectionName = conn.name,
                  currentPath = "/",
                ),
              )
            }
          },
          onAutoConnectChange = { conn, autoConnect ->
            viewModel.updateConnection(conn.copy(autoConnect = autoConnect))
          },
          onOpenBookmark = { item ->
            backstack.navigateTo(
              NetworkBrowserScreen(
                connectionId = item.connection.id,
                connectionName = item.connection.name,
                currentPath = item.bookmark.path,
              ),
            )
          },
          onManageBookmarks = { backstack.navigateTo(NetworkBookmarksScreen) },
        )
      }

      AddConnectionSheet(
        isOpen = showAddSheet,
        savedConnections = connections,
        onDismiss = { showAddSheet = false },
        onSave = { connection ->
          viewModel.addConnection(connection)
          showAddSheet = false
        },
      )

      YtdlpInstallPromptDialog(
        isOpen = showYtdlpInstallPrompt,
        onInstall = {
          showYtdlpInstallPrompt = false
          ytdlpInstallError = null
          ytdlpInstallLastLog = ""
          showYtdlpInstallProgress = true
          ytdlpInstallJob =
            coroutineScope.launch {
              val success =
                YtdlpManager.runInstall(context) { log ->
                  // runInstall logs from an IO dispatcher; hop back to Main before touching state.
                  coroutineScope.launch(Dispatchers.Main) {
                    ytdlpInstallLastLog = log
                  }
                }
              if (success) {
                showYtdlpInstallProgress = false
                pendingYtdlpUrl?.let { proceedToPlay(it) }
                pendingYtdlpUrl = null
              } else {
                // Leave the progress dialog open so the error is visible; Cancel dismisses it.
                ytdlpInstallError = context.getString(R.string.ytdlp_install_failed)
              }
            }
        },
        onConfigure = {
          showYtdlpInstallPrompt = false
          pendingYtdlpUrl = null
          backstack.navigateTo(PreferencesScreen)
        },
        onDismiss = {
          showYtdlpInstallPrompt = false
          pendingYtdlpUrl = null
        },
      )

      YtdlpInstallProgressDialog(
        isOpen = showYtdlpInstallProgress,
        lastLogLine = ytdlpInstallLastLog,
        error = ytdlpInstallError,
        onCancel = {
          ytdlpInstallJob?.cancel()
          ytdlpInstallJob = null
          showYtdlpInstallProgress = false
          pendingYtdlpUrl = null
        },
      )

      editingConnection?.let { connection ->
        EditConnectionSheet(
          connection = connection,
          savedConnections = connections,
          isOpen = true,
          onDismiss = { editingConnection = null },
          onSave = { updatedConnection, clearPassword ->
            viewModel.updateConnection(updatedConnection, clearPassword)
            editingConnection = null
          },
        )
      }

      renamingEntry?.let { entry ->
        RenameDialog(
          isOpen = true,
          onDismiss = { renamingEntry = null },
          onConfirm = { newName ->
            viewModel.renameStreamEntry(entry.stableKey, newName)
            renamingEntry = null
          },
          currentName = entry.fileName,
          itemType = "stream",
        )
      }
    }
  }
}

@Composable
private fun LocalNetworkContent(
  connections: List<NetworkConnection>,
  bookmarks: List<ResolvedNetworkFolderBookmark>,
  connectionStatuses: Map<Long, ConnectionStatus>,
  recentLinks: List<NetworkStreamEntryEntity>,
  isPreparingLink: Boolean,
  onPlayLink: (String) -> Unit,
  onPlayRecent: (NetworkStreamEntryEntity) -> Unit,
  onRenameRecent: (NetworkStreamEntryEntity) -> Unit,
  onDeleteRecent: (String) -> Unit,
  onConnect: (NetworkConnection) -> Unit,
  onDisconnect: (NetworkConnection) -> Unit,
  onEdit: (NetworkConnection) -> Unit,
  onDelete: (NetworkConnection) -> Unit,
  onBrowse: (NetworkConnection, ConnectionStatus?) -> Unit,
  onAutoConnectChange: (NetworkConnection, Boolean) -> Unit,
  onOpenBookmark: (ResolvedNetworkFolderBookmark) -> Unit,
  onManageBookmarks: () -> Unit,
) {
  val navBarHeight = app.video.player.ui.browser.LocalNavigationBarHeight.current
  val bottomOverlayClearance = maxOf(navBarHeight, app.video.player.ui.browser.NavigationBarState.miniPlayerClearance)
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomOverlayClearance + 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Stream Link Section (URL input & recent streams)
    item {
      StreamLinkSection(
        recentLinks = recentLinks,
        isPreparing = isPreparingLink,
        onPlayLink = onPlayLink,
        onPlayRecent = onPlayRecent,
        onRenameRecent = onRenameRecent,
        onDeleteRecent = onDeleteRecent,
      )
    }

    if (bookmarks.isNotEmpty()) {
      item {
        NetworkBookmarkSection(
          bookmarks = bookmarks,
          onOpen = onOpenBookmark,
          onManage = onManageBookmarks,
        )
      }
    }

    // 2. Saved Network Connections Section
    if (connections.isNotEmpty()) {
      items(connections, key = { it.id }) { connection ->
        val status = connectionStatuses[connection.id]
        NetworkConnectionCard(
          connection = connection,
          onConnect = { onConnect(it) },
          onDisconnect = { onDisconnect(it) },
          onEdit = { onEdit(it) },
          onDelete = { onDelete(it) },
          onBrowse = { onBrowse(it, status) },
          onAutoConnectChange = { conn, autoConnect -> onAutoConnectChange(conn, autoConnect) },
          isConnected = status?.isConnected ?: false,
          isConnecting = status?.isConnecting ?: false,
          error = status?.error,
          modifier = Modifier.padding(bottom = 0.dp),
        )
      }
    }
  }
}

@Composable
private fun EmptyStateCard(
  icon: AppIcon,
  title: String,
  subtitle: String,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun StreamLinkSection(
  recentLinks: List<NetworkStreamEntryEntity>,
  isPreparing: Boolean,
  onPlayLink: (String) -> Unit,
  onPlayRecent: (NetworkStreamEntryEntity) -> Unit,
  onRenameRecent: (NetworkStreamEntryEntity) -> Unit,
  onDeleteRecent: (String) -> Unit,
) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val playStreamContentDescription = stringResource(R.string.ui_play_stream)
  var linkUrl by rememberSaveable { mutableStateOf("") }

  fun pasteFromClipboard() {
    val clipboardManager =
      context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    val clipData = clipboardManager?.primaryClip
    if (clipData != null && clipData.itemCount > 0) {
      val text =
        clipData
          .getItemAt(0)
          .text
          ?.toString()
          ?.trim()
          .orEmpty()
      if (text.isNotBlank()) {
        linkUrl = SharedUrlExtractor.normalizeInput(text)
      }
    }
  }

  fun playCurrentLink() {
    val sanitizedUrl = SharedUrlExtractor.normalizeInput(linkUrl)
    if (sanitizedUrl.isBlank()) return

    keyboardController?.hide()
    onPlayLink(sanitizedUrl)
    linkUrl = ""
  }

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // 1. Stream URL Input Box
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      shape = RoundedCornerShape(16.dp),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = linkUrl,
          onValueChange = { linkUrl = it },
          enabled = !isPreparing,
          modifier = Modifier.weight(1f),
          placeholder = {
            Text(
              text = stringResource(R.string.ui_enter_stream_url),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.RoundedFilled.Link,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(20.dp),
            )
          },
          trailingIcon = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(onClick = { pasteFromClipboard() }, enabled = !isPreparing) {
                Icon(
                  imageVector = Icons.RoundedFilled.ContentPaste,
                  contentDescription = stringResource(R.string.ui_paste_stream_url),
                  modifier = Modifier.size(18.dp),
                )
              }
              if (linkUrl.isNotBlank()) {
                IconButton(onClick = { linkUrl = "" }, enabled = !isPreparing) {
                  Icon(
                    imageVector = Icons.RoundedFilled.Close,
                    contentDescription = stringResource(R.string.ui_clear_stream_url),
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions =
            KeyboardActions(
              onGo = { playCurrentLink() },
            ),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Button(
          onClick = { playCurrentLink() },
          enabled = linkUrl.isNotBlank() && !isPreparing,
          contentPadding = PaddingValues(12.dp),
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
          modifier =
            Modifier.semantics {
              contentDescription = playStreamContentDescription
            },
        ) {
          if (isPreparing) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.RoundedFilled.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }

    // 2. Recent Stream Links (Top 10) with Quick Autofill & Media Save
    val topRecent = remember(recentLinks) { recentLinks.take(10) }
    if (topRecent.isNotEmpty()) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.RoundedFilled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = stringResource(R.string.ui_recent_streams),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        topRecent.forEach { entry ->
          val posterOrBackdrop = entry.posterUrl ?: entry.backdropUrl
          Surface(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  linkUrl = entry.canonicalSourceUri
                },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
          ) {
            Row(
              modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (!posterOrBackdrop.isNullOrBlank()) {
                Box(
                  modifier =
                    Modifier
                      .size(width = 56.dp, height = 36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                  contentAlignment = Alignment.Center,
                ) {
                  app.video.player.presentation.components.RemoteImage(
                    url = posterOrBackdrop,
                    contentDescription = entry.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                  )
                }
              } else {
                Box(
                  modifier =
                    Modifier
                      .size(width = 36.dp, height = 36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.RoundedFilled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                val displayTitle =
                  remember(entry.fileName, entry.canonicalSourceUri) {
                    MediaInfoParser.parseStreamTitle(entry.canonicalSourceUri, entry.fileName)
                  }
                Text(
                  text = displayTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = entry.canonicalSourceUri,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              IconButton(
                onClick = { onRenameRecent(entry) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Edit,
                  contentDescription = stringResource(R.string.rename),
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = { onDeleteRecent(entry.stableKey) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Delete,
                  contentDescription = stringResource(R.string.delete),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier.size(16.dp),
                )
              }
              IconButton(
                onClick = { onPlayRecent(entry) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.PlayArrow,
                  contentDescription = stringResource(R.string.ui_play),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
