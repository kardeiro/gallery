package io.github.kardeiro.gallery.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.kardeiro.gallery.data.MediaRepository
import io.github.kardeiro.gallery.data.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    bucketId: String,
    bucketDisplayName: String,
    onBack: () -> Unit,
    onNavigateToViewer: (Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(bucketId) {
        mediaItems = repository.loadMedia().filter { it.bucketId == bucketId }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(bucketDisplayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(
                items = mediaItems,
                key = { it.id }
            ) { item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToViewer(mediaItems.indexOf(item)) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(360)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
