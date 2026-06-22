package io.github.kardeiro.gallery.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.kardeiro.gallery.R
import io.github.kardeiro.gallery.data.MediaRepository
import io.github.kardeiro.gallery.data.model.MediaItem
import io.github.kardeiro.gallery.data.model.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialIndex: Int,
    bucketId: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showBars by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val all = repository.loadMedia()
        mediaItems = if (bucketId != null) all.filter { it.bucketId == bucketId } else all
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaItems.size }
    )

    Scaffold(
        topBar = {
            if (showBars && mediaItems.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text("${pagerState.currentPage + 1} / ${mediaItems.size}")
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val item = mediaItems.getOrNull(pagerState.currentPage)
                            item?.let {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = it.mimeType
                                    putExtra(Intent.EXTRA_STREAM, it.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { /* Info dialog */ }) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.info))
                        }
                        IconButton(onClick = {
                            val item = mediaItems.getOrNull(pagerState.currentPage)
                            item?.let {
                                repository.deleteMedia(it.uri)
                                mediaItems = repository.loadMedia()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (mediaItems.isEmpty()) return@Scaffold

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val item = mediaItems[page]
            if (item.mediaType == MediaType.VIDEO) {
                VideoPlayer(item = item, isVisible = page == pagerState.currentPage)
            } else {
                ZoomableImage(item = item)
            }
        }
    }
}

@Composable
private fun ZoomableImage(item: MediaItem) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
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
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(2048)
                .crossfade(true)
                .build(),
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
    isVisible: Boolean,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = isVisible
        }
    }

    DisposableEffect(isVisible) {
        player.playWhenReady = isVisible
        onDispose {
            player.playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

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
