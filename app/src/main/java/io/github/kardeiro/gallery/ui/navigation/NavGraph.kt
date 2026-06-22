package io.github.kardeiro.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.kardeiro.gallery.data.MediaRepository
import io.github.kardeiro.gallery.ui.screen.AlbumScreen
import io.github.kardeiro.gallery.ui.screen.GalleryScreen
import io.github.kardeiro.gallery.ui.screen.AlbumDetailScreen
import io.github.kardeiro.gallery.ui.screen.ViewerScreen

object Routes {
    const val GALLERY = "gallery"
    const val ALBUMS = "albums"
    const val VIEWER = "viewer/{index}?bucketId={bucketId}"
    const val ALBUM_VIEW = "album/{bucketId}/{bucketName}"

    fun viewerRoute(index: Int, bucketId: String? = null) =
        if (bucketId != null) "viewer/$index?bucketId=$bucketId" else "viewer/$index"
    fun albumRoute(bucketId: String, bucketName: String) = "album/$bucketId/$bucketName"
}

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val repository = remember { MediaRepository(context) }

    NavHost(
        navController = navController,
        startDestination = Routes.GALLERY
    ) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                repository = repository,
                onNavigateToAlbums = {
                    navController.navigate(Routes.ALBUMS)
                },
                onNavigateToViewer = { index ->
                    navController.navigate(Routes.viewerRoute(index))
                }
            )
        }

        composable(Routes.ALBUMS) {
            AlbumScreen(
                repository = repository,
                onNavigateToAlbum = { bucketId, bucketName ->
                    navController.navigate(Routes.albumRoute(bucketId, bucketName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ALBUM_VIEW,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("bucketName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: return@composable
            val bucketName = backStackEntry.arguments?.getString("bucketName") ?: return@composable
            AlbumDetailScreen(
                repository = repository,
                bucketId = bucketId,
                bucketDisplayName = bucketName,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { index ->
                    navController.navigate(Routes.viewerRoute(index, bucketId))
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("index") { type = NavType.IntType },
            )
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            val bucketId = backStackEntry.arguments?.getString("bucketId")
            ViewerScreen(
                repository = repository,
                initialIndex = index,
                bucketId = bucketId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
