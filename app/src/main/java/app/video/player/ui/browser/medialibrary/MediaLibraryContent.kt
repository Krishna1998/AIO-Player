/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.medialibrary

import android.content.Intent
import android.os.Environment
import app.video.player.ui.components.AppToast as Toast
import app.video.player.ui.utils.NavigationBackHandler as BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import app.video.player.ui.theme.AppMotion
import app.video.player.ui.browser.NavigationBarState
import app.video.player.ui.browser.fab.FabPlacement
import app.video.player.ui.browser.fab.FabScrollHelper
import app.video.player.ui.components.InlineSearchBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.snapshotFlow
import app.video.player.ui.utils.NavigationPager
import app.video.player.ui.utils.rememberTabNavigation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import app.video.player.R
import app.video.player.utils.storage.FileTypeUtils
import app.video.player.database.repository.SecureFolderRepository
import app.video.player.BuildConfig
import app.video.player.domain.media.model.Video
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.FoldersPreferences
import app.video.player.preferences.MediaLibraryType
import app.video.player.preferences.PlayerPreferences
import app.video.player.preferences.SecureFolderPreferences
import app.video.player.preferences.preference.collectAsState
import app.video.player.ui.browser.states.PermissionDeniedState
import app.video.player.ui.browser.states.StoragePermissionPrompt
import app.video.player.utils.permission.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import app.video.player.ui.browser.MainScreen
import app.video.player.ui.browser.LocalNavigationBarHeight
import app.video.player.presentation.components.captureLiquidGlassBackdrop
import app.video.player.ui.browser.components.BrowserBottomBar
import app.video.player.ui.browser.components.rememberBrowserBottomBarBackdrop
import app.video.player.ui.browser.components.BrowserTopBar
import app.video.player.ui.browser.dialogs.AddToPlaylistDialog
import app.video.player.ui.browser.dialogs.toPlaylistCandidates
import app.video.player.ui.browser.dialogs.DeleteConfirmationDialog
import app.video.player.ui.browser.dialogs.FileOperationProgressDialog
import app.video.player.ui.browser.dialogs.FolderPickerDialog
import app.video.player.ui.browser.dialogs.FolderSortDialog
import app.video.player.ui.browser.dialogs.RenameDialog
import app.video.player.ui.browser.dialogs.rememberSelectionInfoAction
import app.video.player.ui.browser.dialogs.VideoSortDialog
import app.video.player.ui.browser.dialogs.rememberBrowserSortDialogOpen
import app.video.player.ui.browser.playlist.ALL_VIDEOS_PLAYLIST_ID
import app.video.player.ui.browser.selection.rememberSelectionManager
import app.video.player.ui.browser.states.EmptyState
import app.video.player.ui.browser.videolist.VideoListContent
import app.video.player.ui.browser.videolist.VideoWithPlaybackInfo
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.player.ContainerGeometryProbe
import app.video.player.ui.player.PlaybackIdentity
import app.video.player.ui.player.PlaybackItem
import app.video.player.ui.player.PreparedPlaybackLaunchStore
import app.video.player.ui.player.PlayerActivity
import app.video.player.ui.securefolder.SecureFolderGateScreen
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.navigateTo
import app.video.player.ui.tv.focusableWhen
import app.video.player.utils.history.RecentlyPlayedOps
import app.video.player.utils.media.CopyPasteOps
import app.video.player.utils.media.MediaUtils
import app.video.player.utils.media.OpenDocumentTreeContract
import app.video.player.utils.sort.SortUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MediaLibraryContent(forceAudio: Boolean = false) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val backstack = LocalBackStack.current
  val browserPreferences = koinInject<BrowserPreferences>()
  val foldersPreferences = koinInject<FoldersPreferences>()
  val appearancePreferences = koinInject<app.video.player.preferences.AppearancePreferences>()
  val showQuickPlayFab by appearancePreferences.showQuickPlayFab.collectAsState()
  val playerPreferences = koinInject<PlayerPreferences>()
  val navigationBarHeight = LocalNavigationBarHeight.current

  val viewModel: MediaLibraryViewModel =
    viewModel(
      factory = MediaLibraryViewModel.factory(context.applicationContext as android.app.Application),
    )
  val videos by viewModel.videos.collectAsState()
  val videosWithPlaybackInfo by viewModel.videosWithPlaybackInfo.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()

  // Permissions
  val permissionState =
    PermissionUtils.handleStoragePermission(
      audioOnly = forceAudio,
      onPermissionGranted = { viewModel.refresh() },
    )

  val selectedTreeUris by foldersPreferences.scopedHiddenFolderUris.collectAsState()
  val includeSelectedFolders by foldersPreferences.includeNoMediaFolders.collectAsState()
  val canBrowseSelectedFolders = BuildConfig.SCOPED_STORAGE_ONLY && includeSelectedFolders &&
    selectedTreeUris.isNotEmpty()

  var isPermissionSetupCompleted by rememberSaveable {
    mutableStateOf(
      permissionState.status == PermissionStatus.Granted || browserPreferences.onboardingCompleted.get(),
    )
  }

  LaunchedEffect(permissionState.status, isPermissionSetupCompleted) {
    if (!forceAudio) {
      MainScreen.updatePermissionState(
        isDenied = !isPermissionSetupCompleted,
      )
    }
  }

  val hasCompletedInitialLoad = !isLoading || videos.isNotEmpty()
  androidx.activity.compose.ReportDrawnWhen {
    !isPermissionSetupCompleted ||
      (permissionState.status != PermissionStatus.Granted && !canBrowseSelectedFolders) ||
      hasCompletedInitialLoad
  }

  val videoSortType by browserPreferences.videoSortType.collectAsState()
  val videoSortOrder by browserPreferences.videoSortOrder.collectAsState()
  val folderSortType by browserPreferences.folderSortType.collectAsState()
  val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()
  val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
  val includeAudioBrowser by browserPreferences.includeAudioBrowser.collectAsState()
  val savedMediaType by browserPreferences.mediaLibraryType.collectAsState()
  val mediaType = if (forceAudio) MediaLibraryType.Audio else if (includeAudioBrowser) savedMediaType else MediaLibraryType.Video
  val musicCoverArtSize by browserPreferences.musicCoverArtSize.collectAsState()
  var videoSubTab by rememberSaveable { mutableIntStateOf(0) }
  val sortedVideos =
    remember(videos, videoSortType, videoSortOrder) {
      SortUtils.sortVideos(videos, videoSortType, videoSortOrder)
    }
  val sortedVideosWithInfo =
    remember(sortedVideos, videosWithPlaybackInfo) {
      val infoById = videosWithPlaybackInfo.associateBy { it.video.path }
      sortedVideos.map { video ->
        infoById[video.path] ?: VideoWithPlaybackInfo(video)
      }
    }
  val mediaLibraryTabs = remember(forceAudio) {
    if (forceAudio) listOf(MediaLibraryType.Audio) else MediaLibraryType.entries.toList()
  }
  val initialPageIndex = remember(mediaLibraryTabs) {
    mediaLibraryTabs.indexOf(mediaType).coerceAtLeast(0)
  }
  val pagerState = rememberPagerState(initialPage = initialPageIndex) {
    mediaLibraryTabs.size
  }
  val navigateTab = rememberTabNavigation(pagerState)

  LaunchedEffect(NavigationBarState.allMediaTabRequestId) {
    if (NavigationBarState.allMediaTabRequestId != 0L && !forceAudio) {
      if (!includeAudioBrowser) {
        browserPreferences.includeAudioBrowser.set(true)
      }
      browserPreferences.mediaLibraryType.set(MediaLibraryType.All)
      val targetIndex = mediaLibraryTabs.indexOf(MediaLibraryType.All)
      if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
        navigateTab(targetIndex)
      }
    }
  }

  LaunchedEffect(pagerState, mediaLibraryTabs) {
    snapshotFlow { pagerState.settledPage to pagerState.isScrollInProgress }
      .collect { (page, isScrolling) ->
        if (!isScrolling && !forceAudio) {
          mediaLibraryTabs.getOrNull(page)?.let { targetType ->
            if (mediaType != targetType) {
              if (!includeAudioBrowser && (targetType == MediaLibraryType.Audio || targetType == MediaLibraryType.All)) {
                browserPreferences.includeAudioBrowser.set(true)
                app.video.player.utils.media.MediaLibraryEvents.notifyChanged()
              }
              browserPreferences.mediaLibraryType.set(targetType)
            }
          }
        }
      }
  }

  LaunchedEffect(mediaType, mediaLibraryTabs) {
    if (!forceAudio) {
      val targetIndex = mediaLibraryTabs.indexOf(mediaType).coerceAtLeast(0)
      if (pagerState.currentPage != targetIndex && !pagerState.isScrollInProgress) {
        navigateTab(targetIndex)
      }
    }
  }

  val currentTab = if (forceAudio) {
    MediaLibraryType.Audio
  } else {
    mediaLibraryTabs.getOrNull(pagerState.currentPage) ?: mediaType
  }

  DisposableEffect(currentTab) {
    NavigationBarState.isAllMediaTabActive = (currentTab == MediaLibraryType.All)
    onDispose {
      NavigationBarState.isAllMediaTabActive = false
    }
  }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var isSearching by rememberSaveable { mutableStateOf(false) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  val currentVideosWithInfo =
    remember(sortedVideosWithInfo, currentTab, isSearching, searchQuery, videoSortType, videoSortOrder) {
      val base = if (isSearching) sortedVideosWithInfo else when (currentTab) {
        MediaLibraryType.All -> sortedVideosWithInfo
        MediaLibraryType.Video -> sortedVideosWithInfo.filter { !it.video.isAudio }
        MediaLibraryType.Audio -> {
          when (videoSortType) {
            app.video.player.preferences.VideoSortType.Title -> {
              sortedVideosWithInfo.filter { it.video.isAudio }
                .sortedWith { a, b ->
                  val titleA = if (a.video.title.isNotBlank()) a.video.title else FileTypeUtils.stripExtension(a.video.displayName)
                  val titleB = if (b.video.title.isNotBlank()) b.video.title else FileTypeUtils.stripExtension(b.video.displayName)
                  val res = SortUtils.NaturalOrderComparator.DEFAULT.compare(titleA, titleB)
                  if (videoSortOrder.isAscending) res else -res
                }
            }
            app.video.player.preferences.VideoSortType.Duration -> {
              val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.duration }
              if (videoSortOrder.isAscending) list else list.reversed()
            }
            app.video.player.preferences.VideoSortType.Date -> {
              val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.dateModified }
              if (videoSortOrder.isAscending) list else list.reversed()
            }
            app.video.player.preferences.VideoSortType.Size -> {
              val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.size }
              if (videoSortOrder.isAscending) list else list.reversed()
            }
          }
        }
      }
      if (isSearching && searchQuery.isNotBlank()) {
        base.filter { item ->
          item.video.displayName.contains(searchQuery, ignoreCase = true) ||
            item.video.path.contains(searchQuery, ignoreCase = true) ||
            item.video.title.contains(searchQuery, ignoreCase = true) ||
            item.video.artist.contains(searchQuery, ignoreCase = true)
        }
      } else {
        base
      }
    }

  val selectionManager =
    rememberSelectionManager(
      items = currentVideosWithInfo.map { it.video },
      getId = { it.path.hashCode().toLong() },
      onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
      onRenameItem = { video, newName ->
        coroutineScope.launch { viewModel.renameVideo(video, newName) }
        Result.success(Unit)
      },
      onOperationComplete = { viewModel.refresh() },
    )
  val selectedVideos = selectionManager.getSelectedItems()
  val showSelectionInfo = rememberSelectionInfoAction(
    videos = { selectionManager.getSelectedItems() },
    onSingleOpened = { selectionManager.clear() },
  )
  val watchedVideoIds = remember(currentVideosWithInfo) {
    currentVideosWithInfo.filter(VideoWithPlaybackInfo::isWatched).mapTo(hashSetOf()) { it.video.id }
  }

  val isRefreshing = remember { mutableStateOf(false) }
  val sortDialogOpen = rememberBrowserSortDialogOpen(useHomeState = !forceAudio)
  val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
  val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
  val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }
  val isFabVisible = remember { mutableStateOf(true) }
  val isFabExpanded = remember { mutableStateOf(false) }
  val quickPlayFabDirect by appearancePreferences.quickPlayFabDirect.collectAsState()
  var showFloatingBottomBar by remember { mutableStateOf(false) }
  val animationDuration = 300
  val lastPlayRequestIndex = remember { mutableIntStateOf(-1) }

  val filePicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      uri?.let {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        MediaUtils.playFile(it.toString(), context, "open_file")
      }
    }

  val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
  val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
  val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
  val operationProgress by CopyPasteOps.operationProgress.collectAsState()

  // Move-to-Secure-Folder state
  val secureFolderRepository = koinInject<SecureFolderRepository>()
  val secureFolderPreferences = koinInject<SecureFolderPreferences>()
  val moveToSecureConfirmOpen = rememberSaveable { mutableStateOf(false) }
  val moveToSecureProgressOpen = rememberSaveable { mutableStateOf(false) }
  val secureFolderProgress by secureFolderRepository.progress.collectAsState()

  fun moveSelectedToSecureFolder() {
    val selectedVideos = selectionManager.getSelectedItems()
    if (selectedVideos.isEmpty()) return
    moveToSecureProgressOpen.value = true
    coroutineScope.launch {
      val result = secureFolderRepository.moveIn(context, selectedVideos)
      moveToSecureProgressOpen.value = false
      if (result.isSuccess) selectionManager.clear()
      viewModel.refresh()
      result
        .onSuccess { batch ->
          val message =
            if (batch.failedIds.isEmpty()) {
              context.getString(R.string.secure_folder_moved_success, batch.succeededIds.size)
            } else {
              context.getString(R.string.secure_folder_moved_partial, batch.succeededIds.size, batch.failedIds.size)
            }
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
          Toast.error(context, context.getString(
            R.string.secure_folder_move_failed_details,
            error.localizedMessage ?: context.getString(R.string.generic_unknown_error),
          ))
        }
    }
  }
  val treePickerLauncher =
    rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
      if (uri == null) {
        return@rememberLauncherForActivityResult
      }
      val selectedVideos = selectionManager.getSelectedItems()
      if (selectedVideos.isEmpty()) {
        return@rememberLauncherForActivityResult
      }

      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      }

      when {
        operationType.value != null -> {
          progressDialogOpen.value = true
          coroutineScope.launch {
            when (operationType.value) {
              is CopyPasteOps.OperationType.Copy -> {
                CopyPasteOps.copyFilesToTreeUri(context, selectedVideos, uri)
              }

              is CopyPasteOps.OperationType.Move -> {
                CopyPasteOps.moveFilesToTreeUri(context, selectedVideos, uri)
              }

              else -> {}
            }
          }
        }
      }
    }

  LaunchedEffect(isSearching) {
    if (isSearching) {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  var lastHandledSearchTrigger by rememberSaveable { mutableStateOf(NavigationBarState.homeSearchTriggerRequestId) }

  LaunchedEffect(NavigationBarState.homeSearchTriggerRequestId) {
    val currentId = NavigationBarState.homeSearchTriggerRequestId
    if (currentId > 0L && currentId != lastHandledSearchTrigger) {
      lastHandledSearchTrigger = currentId
      NavigationBarState.clearHomeSearchTrigger()
      isSearching = true
      if (!forceAudio) {
        val allMediaIndex = mediaLibraryTabs.indexOf(MediaLibraryType.All)
        if (allMediaIndex >= 0 && pagerState.currentPage != allMediaIndex) {
          pagerState.scrollToPage(allMediaIndex)
        }
      }
    }
  }

  LaunchedEffect(NavigationBarState.dismissSearchRequestId) {
    if (NavigationBarState.dismissSearchRequestId > 0L) {
      isSearching = false
      searchQuery = ""
      keyboardController?.hide()
    }
  }

  LaunchedEffect(includeAudioBrowser, savedMediaType, forceAudio) {
    if (!forceAudio && !includeAudioBrowser && savedMediaType != MediaLibraryType.Video) {
      browserPreferences.mediaLibraryType.set(MediaLibraryType.Video)
    }
  }

  LaunchedEffect(selectionManager.isInSelectionMode, currentTab) {
    showFloatingBottomBar = selectionManager.isInSelectionMode
  }
  app.video.player.ui.browser.NavigationBarSelectionEffect(
    inSelectionMode = selectionManager.isInSelectionMode,
    onlyVideos = currentTab == MediaLibraryType.Video,
  )

  LaunchedEffect(pagerState.currentPage) {
    if (selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }
  }

  fun playFromMediaLibrary(video: Video, startPositionSeconds: Double? = null) {
    val playlistVideos = currentVideosWithInfo.map { it.video }
    if (!playerPreferences.shouldBuildAutomaticQueue(video.isAudio) || playlistVideos.size <= 1) {
      MediaUtils.playFile(video, context, "media_library", startPositionSeconds = startPositionSeconds)
      return
    }

    val index = playlistVideos.indexOfFirst { it.path == video.path }.coerceAtLeast(0)
    lastPlayRequestIndex.intValue = index

    val queueItems = playlistVideos.map { item ->
      PlaybackItem.fromUri(
        uri = item.uri.toString(),
        stableId = if (app.video.player.BuildConfig.SCOPED_STORAGE_ONLY && item.uri.scheme == "content") {
          app.video.player.ui.browser.videolist.videoPlaybackIdentifiers(item).first()
        } else PlaybackIdentity.forLocalPath(item.path),
        title = item.displayName,
        mimeType = item.mimeType,
        durationSeconds = (item.duration / 1000L).toInt().takeIf { it > 0 },
        videoWidth = item.width,
        videoHeight = item.height,
      )
    }
    val isAudio = currentTab == MediaLibraryType.Audio || video.isAudio
    if (MediaUtils.shouldPlayInMiniPlayerOnly(isAudio)) {
      MediaUtils.playInMiniPlayer(context, queueItems, index, startPositionSeconds = startPositionSeconds)
      return
    }

    val launchToken = PreparedPlaybackLaunchStore.stage(
      items = queueItems,
      currentIndex = index,
      isExplicitQueue = true,
    )

    val intent =
      android.content.Intent(android.content.Intent.ACTION_VIEW, video.uri).apply {
        setClass(context, PlayerActivity::class.java)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("internal_launch", true)
        putExtra(PlayerActivity.EXTRA_PREPARED_PLAYBACK_QUEUE, true)
        putExtra(PlayerActivity.EXTRA_PREPARED_PLAYBACK_TOKEN, launchToken)
        putExtra("playlist_id", ALL_VIDEOS_PLAYLIST_ID)
        putExtra("playlist_index", index)
        putExtra("launch_source", "media_library")
        putExtra("media_library_all", currentTab == MediaLibraryType.All)
        putExtra("media_library_audio", currentTab == MediaLibraryType.Audio)
        putExtra("is_audio", video.isAudio)
        putExtra("title", video.displayName)
        putExtra("local_media_path", video.path)
        putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, video.width)
        putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, video.height)
        putExtra(PlayerActivity.EXTRA_VIDEO_ROTATION, video.rotation)
        startPositionSeconds?.takeIf { it.isFinite() && it > 0 }?.let {
          putExtra(PlayerActivity.EXTRA_START_POSITION_SECONDS, it)
        }
      }
    ContainerGeometryProbe.prefetch(video.path, video.width, video.height, video.rotation, isAudio)
    context.startActivity(intent)
  }

  BackHandler(enabled = selectionManager.isInSelectionMode || isSearching || (isFabExpanded.value && !quickPlayFabDirect)) {
    when {
      isFabExpanded.value && !quickPlayFabDirect -> isFabExpanded.value = false
      selectionManager.isInSelectionMode -> selectionManager.clear()
      isSearching -> {
        isSearching = false
        searchQuery = ""
        NavigationBarState.clearHomeSearchTrigger()
      }
    }
  }

  val bottomBarBackdrop = rememberBrowserBottomBarBackdrop()

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.captureLiquidGlassBackdrop(bottomBarBackdrop),
      containerColor = app.video.player.ui.theme.wallpaperAwareBackgroundColor(),
    topBar = {
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
            Text(androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_search_media))
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.RoundedFilled.Search,
              contentDescription =
                androidx.compose.ui.res.stringResource(
                  app.video.player.R.string.settings_search_title,
                ),
            )
          },
          trailingIcon = {
            IconButton(
              onClick = {
                isSearching = false
                searchQuery = ""
                NavigationBarState.clearHomeSearchTrigger()
              },
            ) {
              Icon(
                imageVector = Icons.RoundedFilled.Close,
                contentDescription =
                  androidx.compose.ui.res.stringResource(
                    app.video.player.R.string.generic_cancel,
                  ),
              )
            }
          },
        )
      } else {
        BrowserTopBar(
          title =
            if (forceAudio) {
              androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_music)
            } else {
              androidx.compose.ui.res.stringResource(app.video.player.R.string.app_name)
            },
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = currentVideosWithInfo.size,
          onBackClick = null,
          onCancelSelection = { selectionManager.clear() },
          onSortClick = { sortDialogOpen.value = true },
          onSearchClick = {
            isSearching = !isSearching
            if (!isSearching) {
              searchQuery = ""
              NavigationBarState.clearHomeSearchTrigger()
            } else if (!forceAudio) {
              coroutineScope.launch {
                val allMediaIndex = mediaLibraryTabs.indexOf(MediaLibraryType.All)
                if (allMediaIndex >= 0 && pagerState.currentPage != allMediaIndex) {
                  pagerState.scrollToPage(allMediaIndex)
                }
              }
            }
          },
          onSettingsClick = {
            backstack.navigateTo(app.video.player.ui.preferences.PreferencesScreen)
          },
          onTitleDoubleTap = { backstack.navigateTo(SecureFolderGateScreen) },
          onTitleLongPress = { backstack.navigateTo(SecureFolderGateScreen) },
          isSingleSelection = selectionManager.isSingleSelection,
          onInfoClick = showSelectionInfo,
          onShareClick = { selectionManager.shareSelected() },
          onPlayClick = { selectionManager.playSelected() },
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
          onMoveToSecureClick = {
            if (!secureFolderPreferences.isPinSet()) {
              backstack.navigateTo(SecureFolderGateScreen)
            } else if (secureFolderPreferences.dontAskBeforeMove.get()) {
              moveSelectedToSecureFolder()
            } else {
              moveToSecureConfirmOpen.value = true
            }
          },
        )
      }
    },
    floatingActionButton = {
      val isFabShouldBeVisible =
        currentVideosWithInfo.isNotEmpty() &&
          showQuickPlayFab &&
          !selectionManager.isInSelectionMode &&
          isFabVisible.value &&
          !MainScreen.getPermissionDeniedState()

      FloatingActionButtonMenu(
        modifier = Modifier.padding(bottom = FabPlacement.scaffoldBottomPadding()),
        expanded = isFabExpanded.value && !quickPlayFabDirect,
        button = {
          TooltipBox(
            positionProvider =
              TooltipDefaults.rememberTooltipPositionProvider(
                if (isFabExpanded.value && !quickPlayFabDirect) {
                  TooltipAnchorPosition.Start
                } else {
                  TooltipAnchorPosition.Above
                },
              ),
            tooltip = {
              PlainTooltip {
                Text(
                  androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_toggle_menu),
                )
              }
            },
            state = rememberTooltipState(),
          ) {
            ToggleFloatingActionButton(
              modifier =
                Modifier.animateFloatingActionButton(
                  visible = isFabShouldBeVisible,
                  alignment = Alignment.BottomEnd,
                ).focusableWhen(isFabShouldBeVisible),
              checked = isFabExpanded.value && !quickPlayFabDirect,
                onCheckedChange = {
                  if (quickPlayFabDirect) {
                    coroutineScope.launch {
                      val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
                      val lastPlayed = recentlyPlayedVideos.firstOrNull()
                      val targetVideo =
                        if (lastPlayed != null) {
                          currentVideosWithInfo.firstOrNull { it.video.path == lastPlayed.filePath }?.video
                        } else {
                          null
                        }

                      playFromMediaLibrary(targetVideo ?: currentVideosWithInfo.first().video)
                    }
                  } else {
                    isFabExpanded.value = !isFabExpanded.value
                  }
                },
              ) {
                val imageVector by remember {
                  derivedStateOf {
                    if (checkedProgress > 0.5f && !quickPlayFabDirect) Icons.RoundedFilled.Close else Icons.RoundedFilled.PlayArrow
                  }
                }
                Icon(
                  imageVector = imageVector,
                  contentDescription = null,
                  modifier = Modifier.animateIcon({ if (quickPlayFabDirect) 0f else checkedProgress }),
                )
              }
            }
          },
        ) {
          if (!quickPlayFabDirect) {
            FloatingActionButtonMenuItem(
              onClick = {
                isFabExpanded.value = false
                filePicker.launch(
                  when (currentTab) {
                    MediaLibraryType.All -> FileTypeUtils.videoPickerMimeTypes() + arrayOf("audio/*")
                    MediaLibraryType.Audio -> arrayOf("audio/*")
                    MediaLibraryType.Video -> FileTypeUtils.videoPickerMimeTypes()
                  },
                )
              },
              icon = { Icon(Icons.RoundedFilled.FileOpen, contentDescription = null) },
              text = {
                Text(
                  text =
                    androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_open_file),
                )
              },
            )

            FloatingActionButtonMenuItem(
              onClick = {
                isFabExpanded.value = false
                coroutineScope.launch {
                  val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
                  val lastPlayed = recentlyPlayedVideos.firstOrNull()
                  val targetVideo =
                    if (lastPlayed != null) {
                      currentVideosWithInfo.firstOrNull { it.video.path == lastPlayed.filePath }?.video
                    } else {
                      null
                    }

                  playFromMediaLibrary(targetVideo ?: currentVideosWithInfo.first().video)
                }
              },
              icon = { Icon(Icons.RoundedFilled.PlayArrow, contentDescription = null) },
              text = {
                Text(
                  text =
                    when (currentTab) {
                      MediaLibraryType.All -> androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_play_recent_or_first_media)
                      MediaLibraryType.Audio -> androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_play_recent_or_first_audio)
                      MediaLibraryType.Video -> androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_play_recent_or_first_video)
                    },
                )
              },
            )
          }
        }
      },
    ) { padding ->
      val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
      val videosWereDeletedOrMoved = false

      Box(modifier = Modifier.fillMaxSize()) {
        if ((isPermissionSetupCompleted && permissionState.status == PermissionStatus.Granted) || canBrowseSelectedFolders) {
          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(padding),
          ) {
          if (!forceAudio) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(3.5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                MediaLibraryType.entries.forEachIndexed { index, type ->
                  val isSelected = currentTab == type
                  val count = when (type) {
                    MediaLibraryType.All -> sortedVideosWithInfo.size
                    MediaLibraryType.Video -> sortedVideosWithInfo.count { !it.video.isAudio }
                    MediaLibraryType.Audio -> sortedVideosWithInfo.count { it.video.isAudio }
                  }
                  val label = when (type) {
                    MediaLibraryType.All -> stringResource(R.string.ui_all_media)
                    MediaLibraryType.Video -> stringResource(R.string.ui_videos)
                    MediaLibraryType.Audio -> stringResource(R.string.ui_music)
                  }
                  val iconVector = when (type) {
                    MediaLibraryType.All -> Icons.RoundedFilled.GridView
                    MediaLibraryType.Video -> Icons.RoundedFilled.VideoLibrary
                    MediaLibraryType.Audio -> Icons.RoundedFilled.Audiotrack
                  }

                  Surface(
                    onClick = {
                      if (currentTab != type) {
                        navigateTab(index)
                      }
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) {
                      MaterialTheme.colorScheme.primaryContainer
                    } else {
                      Color.Transparent
                    },
                    border = if (isSelected) {
                      BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    } else null,
                    modifier = Modifier.weight(1f),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) {
                          MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        },
                      )
                      Spacer(modifier = Modifier.width(5.dp))
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) {
                          MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (count > 0) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                          text = "($count)",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                          ),
                          color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                          } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                          },
                          maxLines = 1,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Box(modifier = Modifier.weight(1f)) {
            NavigationPager(
              state = pagerState,
              key = { page -> mediaLibraryTabs.getOrNull(page)?.name ?: page.toString() },
              modifier = Modifier.fillMaxSize(),
              beyondViewportPageCount = 1,
              allowNestedSwipes = true,
              userScrollEnabled = !selectionManager.isInSelectionMode,
            ) { page ->
              val pageType = mediaLibraryTabs.getOrNull(page) ?: MediaLibraryType.All
              val pageVideosWithInfo = remember(sortedVideosWithInfo, pageType, isSearching, searchQuery, videoSortType, videoSortOrder) {
                val base = if (isSearching) sortedVideosWithInfo else when (pageType) {
                  MediaLibraryType.All -> sortedVideosWithInfo
                  MediaLibraryType.Video -> sortedVideosWithInfo.filter { !it.video.isAudio }
                  MediaLibraryType.Audio -> {
                    when (videoSortType) {
                      app.video.player.preferences.VideoSortType.Title -> {
                        sortedVideosWithInfo.filter { it.video.isAudio }
                          .sortedWith { a, b ->
                            val titleA = if (a.video.title.isNotBlank()) a.video.title else FileTypeUtils.stripExtension(a.video.displayName)
                            val titleB = if (b.video.title.isNotBlank()) b.video.title else FileTypeUtils.stripExtension(b.video.displayName)
                            val res = SortUtils.NaturalOrderComparator.DEFAULT.compare(titleA, titleB)
                            if (videoSortOrder.isAscending) res else -res
                          }
                      }
                      app.video.player.preferences.VideoSortType.Duration -> {
                        val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.duration }
                        if (videoSortOrder.isAscending) list else list.reversed()
                      }
                      app.video.player.preferences.VideoSortType.Date -> {
                        val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.dateModified }
                        if (videoSortOrder.isAscending) list else list.reversed()
                      }
                      app.video.player.preferences.VideoSortType.Size -> {
                        val list = sortedVideosWithInfo.filter { it.video.isAudio }.sortedBy { it.video.size }
                        if (videoSortOrder.isAscending) list else list.reversed()
                      }
                    }
                  }
                }
                if (isSearching && searchQuery.isNotBlank()) {
                  base.filter { item ->
                    item.video.displayName.contains(searchQuery, ignoreCase = true) ||
                      item.video.path.contains(searchQuery, ignoreCase = true) ||
                      item.video.title.contains(searchQuery, ignoreCase = true) ||
                      item.video.artist.contains(searchQuery, ignoreCase = true)
                  }
                } else {
                  base
                }
              }

              if (isSearching && pageVideosWithInfo.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  EmptyState(
                    icon = Icons.RoundedFilled.Search,
                    title = androidx.compose.ui.res.stringResource(app.video.player.R.string.ui_no_media_found),
                    message = "Try a different search term",
                  )
                }
              } else if (pageType == MediaLibraryType.Video && !isSearching) {
                Column(modifier = Modifier.fillMaxSize()) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    androidx.compose.material3.FilterChip(
                      selected = videoSubTab == 0,
                      onClick = { videoSubTab = 0 },
                      label = { Text("Folders") },
                      leadingIcon = {
                        app.video.player.ui.icons.Icon(
                          imageVector = app.video.player.ui.icons.Icons.RoundedFilled.Folder,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      },
                      colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                    )
                    androidx.compose.material3.FilterChip(
                      selected = videoSubTab == 1,
                      onClick = { videoSubTab = 1 },
                      label = { Text("All Videos (${pageVideosWithInfo.size})") },
                      leadingIcon = {
                        app.video.player.ui.icons.Icon(
                          imageVector = app.video.player.ui.icons.Icons.RoundedFilled.VideoLibrary,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      },
                      colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                    )
                  }
                  Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (videoSubTab == 0) {
                      app.video.player.ui.browser.folderlist.FolderListScreen.MediaStoreFolderListContent(
                        audioOnly = false,
                        videoOnly = true,
                        embedded = true,
                        searchQuery = searchQuery,
                      )
                    } else {
                      VideoListContent(
                        folderId = "media_library_video",
                        videosWithInfo = pageVideosWithInfo,
                        isLoading = isLoading && videos.isEmpty(),
                        isRefreshing = isRefreshing,
                        recentlyPlayedFilePath = recentlyPlayedFilePath,
                        videosWereDeletedOrMoved = videosWereDeletedOrMoved,
                        autoScrollToLastPlayed = autoScrollToLastPlayed,
                        onRefresh = { viewModel.refresh() },
                        selectionManager = selectionManager,
                        onVideoClick = { video ->
                          if (selectionManager.isInSelectionMode) {
                            selectionManager.toggleFromUser(video)
                          } else {
                            playFromMediaLibrary(video)
                          }
                        },
                        onVideoLongClick = { video -> selectionManager.handleLongClick(video) },
                        isFabVisible = isFabVisible,
                        modifier = Modifier.fillMaxSize(),
                        showFloatingBottomBar = showFloatingBottomBar,
                        mediaLayoutMode = mediaLayoutMode,
                        musicCoverArtSize = musicCoverArtSize,
                        isFabExpanded = isFabExpanded.value,
                        onFabExpandedChange = { isFabExpanded.value = it },
                        headerContent = null,
                      )
                    }
                  }
                }
              } else {
                VideoListContent(
                  folderId = "media_library_${pageType.name.lowercase()}",
                  videosWithInfo = if (pageType == MediaLibraryType.All && !isSearching) emptyList() else pageVideosWithInfo,
                  isLoading = isLoading && videos.isEmpty(),
                  isRefreshing = isRefreshing,
                  recentlyPlayedFilePath = recentlyPlayedFilePath,
                  videosWereDeletedOrMoved = videosWereDeletedOrMoved,
                  autoScrollToLastPlayed = autoScrollToLastPlayed,
                  onRefresh = { viewModel.refresh() },
                  selectionManager = selectionManager,
                  onVideoClick = { video ->
                    if (selectionManager.isInSelectionMode) {
                      selectionManager.toggleFromUser(video)
                    } else {
                      playFromMediaLibrary(video)
                    }
                  },
                  onVideoLongClick = { video -> selectionManager.handleLongClick(video) },
                  isFabVisible = isFabVisible,
                  modifier = Modifier.fillMaxSize(),
                  showFloatingBottomBar = showFloatingBottomBar,
                  mediaLayoutMode = mediaLayoutMode,
                  musicCoverArtSize = musicCoverArtSize,
                  isFabExpanded = isFabExpanded.value,
                  onFabExpandedChange = { isFabExpanded.value = it },
                  headerContent = if (!isSearching && pageType == MediaLibraryType.All && pageVideosWithInfo.isNotEmpty()) {
                    {
                      app.video.player.ui.browser.bentofusion.BentoFusionHeader(
                        videosWithInfo = pageVideosWithInfo,
                        recentlyPlayedFilePath = recentlyPlayedFilePath,
                        onPlayMedia = { video, startPos -> playFromMediaLibrary(video, startPos) },
                        onFilterSelected = { type ->
                          val targetIdx = mediaLibraryTabs.indexOf(type)
                          if (targetIdx >= 0) navigateTab(targetIdx)
                        },
                      )
                    }
                  } else null,
                )
              }
            }
          }
        }
      } else if (isPermissionSetupCompleted) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          StoragePermissionPrompt(
            onRequestPermission = { permissionState.launchPermissionRequest() },
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          PermissionDeniedState(
            onRequestPermission = { permissionState.launchPermissionRequest() },
            onNext = {
              isPermissionSetupCompleted = true
              browserPreferences.onboardingCompleted.set(true)
              viewModel.refresh()
            },
            modifier = Modifier,
          )
        }
      }

        FabScrollHelper.FabScrim(
          visible = isFabExpanded.value && !quickPlayFabDirect,
          onDismiss = { isFabExpanded.value = false },
        )
      }
    }

    AnimatedVisibility(
      visible = showFloatingBottomBar,
      enter =
        slideInVertically(
          animationSpec =
            spring(
              dampingRatio = AppMotion.Spatial.Expressive.dampingRatio,
              stiffness = AppMotion.Spatial.Expressive.stiffness,
            ),
          initialOffsetY = { fullHeight -> fullHeight },
        ),
      exit =
        slideOutVertically(
          animationSpec =
            spring(
              dampingRatio = AppMotion.Spatial.Standard.dampingRatio,
              stiffness = AppMotion.Spatial.Standard.stiffness,
            ),
          targetOffsetY = { fullHeight -> fullHeight },
        ),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      BrowserBottomBar(
        backdrop = bottomBarBackdrop,
        isSelectionMode = selectionManager.isInSelectionMode,
        onCopyClick = {
          operationType.value = CopyPasteOps.OperationType.Copy
          if (CopyPasteOps.canUseDirectFileOperations()) {
            folderPickerOpen.value = true
          } else {
            treePickerLauncher.launch(null)
          }
        },
        onMoveClick = {
          operationType.value = CopyPasteOps.OperationType.Move
          if (CopyPasteOps.canUseDirectFileOperations()) {
            folderPickerOpen.value = true
          } else {
            treePickerLauncher.launch(null)
          }
        },
        onRenameClick = { renameDialogOpen.value = true },
        onDeleteClick = { deleteDialogOpen.value = true },
        onAddToPlaylistClick = { addToPlaylistDialogOpen.value = true },
        showCopy = true,
        showMove = true,
        showRename = selectionManager.selectedCount > 0,
        modifier = Modifier.padding(bottom = 0.dp),
      )
    }
  }

    if (sortDialogOpen.value) {
      if (currentTab == MediaLibraryType.Video && videoSubTab == 0 && !isSearching) {
        FolderSortDialog(
          isOpen = sortDialogOpen.value,
          onDismiss = { sortDialogOpen.value = false },
          sortType = folderSortType,
          sortOrder = folderSortOrder,
          onSortTypeChange = { browserPreferences.folderSortType.set(it) },
          onSortOrderChange = { browserPreferences.folderSortOrder.set(it) },
          embeddedAlbumView = true,
        )
      } else {
        VideoSortDialog(
          isOpen = sortDialogOpen.value,
          onDismiss = { sortDialogOpen.value = false },
          sortType = videoSortType,
          sortOrder = videoSortOrder,
          onSortTypeChange = { browserPreferences.videoSortType.set(it) },
          onSortOrderChange = { browserPreferences.videoSortOrder.set(it) },
          isFolderView = false,
          enableViewModeOptions = !forceAudio,
          isAudio = currentTab == MediaLibraryType.Audio,
          onMetadataFieldEnabled = viewModel::refresh,
        )
      }
    }

    if (deleteDialogOpen.value) {
      DeleteConfirmationDialog(
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = {
          selectionManager.deleteSelected()
          deleteDialogOpen.value = false
        },
        itemCount = selectionManager.selectedCount,
        isOpen = deleteDialogOpen.value,
        itemType =
          when (mediaType) {
            MediaLibraryType.All -> "media item"
            MediaLibraryType.Audio -> "audio file"
            MediaLibraryType.Video -> "video"
          },
        itemPaths = selectionManager.getSelectedItems().map { it.uri.toString() },
        totalSizeBytes = selectionManager.getSelectedItems().sumOf { it.size.coerceAtLeast(0L) }.takeIf { it > 0L },
      )
    }

    if (renameDialogOpen.value) {
      val video = selectionManager.getSelectedItems().firstOrNull()
      if (video != null) {
        RenameDialog(
          onDismiss = { renameDialogOpen.value = false },
          onConfirm = { newName ->
            selectionManager.renameSelected(newName)
            renameDialogOpen.value = false
          },
          currentName = video.displayName,
          isOpen = renameDialogOpen.value,
          itemType =
            when (mediaType) {
              MediaLibraryType.All -> "media item"
              MediaLibraryType.Audio -> "audio file"
              MediaLibraryType.Video -> "video"
            },
        )
      }
    }

    AddToPlaylistDialog(
      isOpen = addToPlaylistDialogOpen.value,
      candidates = selectionManager.getSelectedItems().toPlaylistCandidates(),
      onDismiss = { addToPlaylistDialogOpen.value = false },
      onSuccess = {
        selectionManager.clear()
        viewModel.refresh()
      },
    )

    if (folderPickerOpen.value) {
      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        currentPath =
          videos.firstOrNull()?.let { File(it.path).parent }
            ?: Environment.getExternalStorageDirectory().absolutePath,
        onDismiss = {
          folderPickerOpen.value = false
        },
        onFolderSelected = { destinationPath ->
          folderPickerOpen.value = false
          val selectedVideos = selectionManager.getSelectedItems()
          if (selectedVideos.isNotEmpty() && operationType.value != null) {
            progressDialogOpen.value = true
            coroutineScope.launch {
              when (operationType.value) {
                is CopyPasteOps.OperationType.Copy -> {
                  CopyPasteOps.copyFiles(context, selectedVideos, destinationPath)
                }

                is CopyPasteOps.OperationType.Move -> {
                  CopyPasteOps.moveFiles(context, selectedVideos, destinationPath)
                }

                else -> {}
              }
            }
          }
        },
      )
    }

    if (operationType.value != null) {
      FileOperationProgressDialog(
        isOpen = progressDialogOpen.value,
        operationType = operationType.value!!,
        progress = operationProgress,
        onCancel = {
          CopyPasteOps.cancelOperation()
        },
        onDismiss = {
          progressDialogOpen.value = false
          operationType.value = null
          selectionManager.clear()
          viewModel.refresh()
        },
      )
    }

    // Move to Secure Folder — confirm (skippable via "don't ask again"), then progress
    app.video.player.ui.securefolder.SecureConfirmDialog(
      isOpen = moveToSecureConfirmOpen.value,
      title = stringResource(R.string.secure_folder_move_items_title, selectionManager.selectedCount),
      subtitle = stringResource(R.string.secure_folder_move_items_subtitle),
      dontAskAgain = secureFolderPreferences.dontAskBeforeMove,
      onConfirm = {
        moveToSecureConfirmOpen.value = false
        moveSelectedToSecureFolder()
      },
      onDismiss = { moveToSecureConfirmOpen.value = false },
    )

    app.video.player.ui.securefolder.SecureFolderProgressDialog(
      isOpen = moveToSecureProgressOpen.value,
      progress = secureFolderProgress,
      label = stringResource(R.string.secure_folder_moving_progress),
      onCancel = { secureFolderRepository.cancelOperation() },
    )
  }
