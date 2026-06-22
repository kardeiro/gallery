package io.github.kardeiro.gallery.ui.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.kardeiro.gallery.R
import io.github.kardeiro.gallery.data.MediaRepository
import io.github.kardeiro.gallery.data.model.MediaItem
import io.github.kardeiro.gallery.data.model.MediaType
import io.github.kardeiro.gallery.ui.theme.GalleryMotion
import io.github.kardeiro.gallery.ui.theme.GallerySpacing
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    repository: MediaRepository,
    initialIndex: Int,
    bucketId: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showBars by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var infoItem by remember { mutableStateOf<MediaItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetItem by remember { mutableStateOf<MediaItem?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val itemDeletedMessage = stringResource(R.string.item_deleted)
    val deleteFailedMessage = stringResource(R.string.delete_failed)

    LaunchedEffect(Unit) {
        val all = repository.loadMedia()
        mediaItems = if (bucketId != null) all.filter { it.bucketId == bucketId } else all
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaItems.size }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (showInfoDialog && infoItem != null) {
            MediaInfoDialog(
                item = infoItem!!,
                onDismiss = { showInfoDialog = false }
            )
        }
        if (showDeleteDialog && deleteTargetItem != null) {
            DeleteConfirmDialog(
                onConfirm = {
                    val targetItem = deleteTargetItem ?: return@DeleteConfirmDialog
                    val deleted = repository.deleteMedia(targetItem.uri)
                    showDeleteDialog = false
                    deleteTargetItem = null
                    scope.launch {
                        if (deleted) {
                            mediaItems = repository.loadMedia()
                            snackbarHostState.showSnackbar(itemDeletedMessage)
                        } else {
                            snackbarHostState.showSnackbar(deleteFailedMessage)
                        }
                    }
                },
                onDismiss = { showDeleteDialog = false; deleteTargetItem = null }
            )
        }
        if (mediaItems.isEmpty()) return@Scaffold

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaItems[page]
                if (item.mediaType == MediaType.VIDEO) {
                    if (page == pagerState.currentPage) {
                        VideoPlayer(
                            item = item,
                            onToggleBars = { showBars = !showBars }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    ZoomableImage(
                        item = item,
                        onTap = { showBars = !showBars }
                    )
                }
            }

            ViewerTopOverlay(
                visible = showBars,
                pageText = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(GallerySpacing.Large),
            )

            ViewerActionOverlay(
                visible = showBars,
                onShare = {
                    val item = mediaItems.getOrNull(pagerState.currentPage)
                    item?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = it.mimeType
                            putExtra(Intent.EXTRA_STREAM, it.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                    }
                },
                onInfo = {
                    infoItem = mediaItems.getOrNull(pagerState.currentPage)
                    showInfoDialog = true
                },
                onDelete = {
                    deleteTargetItem = mediaItems.getOrNull(pagerState.currentPage)
                    showDeleteDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(GallerySpacing.Large),
            )
        }
    }
}

@Composable
private fun ViewerTopOverlay(
    visible: Boolean,
    pageText: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(GalleryMotion.Fast)) + slideInVertically(tween(GalleryMotion.Medium)) { -it / 2 },
        exit = fadeOut(tween(GalleryMotion.Fast)) + slideOutVertically(tween(GalleryMotion.Fast)) { -it / 2 },
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = GallerySpacing.Small, vertical = GallerySpacing.Tiny),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GallerySpacing.Small),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = pageText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = GallerySpacing.Medium),
                )
            }
        }
    }
}

@Composable
private fun ViewerActionOverlay(
    visible: Boolean,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(GalleryMotion.Fast)) + slideInVertically(tween(GalleryMotion.Medium)) { it / 2 },
        exit = fadeOut(tween(GalleryMotion.Fast)) + slideOutVertically(tween(GalleryMotion.Fast)) { it / 2 },
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = GallerySpacing.Small, vertical = GallerySpacing.Tiny),
                horizontalArrangement = Arrangement.spacedBy(GallerySpacing.Tiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                }
                IconButton(onClick = onInfo) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.info))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(2048)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun VideoPlayer(
    item: MediaItem,
    onToggleBars: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onToggleBars() }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0) return ""
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
private fun MediaInfoDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val locationText = if (item.latitude != null && item.longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", item.latitude, item.longitude)
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info)) },
        text = {
            Column {
                InfoRow(label = stringResource(R.string.media_type), value = item.mediaType.name)
                InfoRow(label = stringResource(R.string.media_date), value = item.formattedDate)
                InfoRow(label = stringResource(R.string.media_size), value = item.formattedSize)
                InfoRow(label = stringResource(R.string.media_dimensions), value = "${item.width} x ${item.height} px")
                if (item.duration != null) {
                    InfoRow(label = stringResource(R.string.media_duration), value = formatDuration(item.duration))
                }
                if (locationText != null) {
                    HorizontalDivider()
                    InfoRow(label = stringResource(R.string.media_location), value = locationText)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(stringResource(R.string.delete_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
