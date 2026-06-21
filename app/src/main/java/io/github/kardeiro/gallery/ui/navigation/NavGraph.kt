package io.github.kardeiro.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.kardeiro.gallery.ui.screen.AlbumScreen
import io.github.kardeiro.gallery.ui.screen.GalleryScreen
import io.github.kardeiro.gallery.ui.screen.ViewerScreen

object Routes {
    const val GALLERY = "gallery"
    const val ALBUMS = "albums"
    const val VIEWER = "viewer/{index}"
    const val ALBUM_VIEW = "album/{bucketId}/{bucketName}"

    fun viewerRoute(index: Int) = "viewer/$index"
    fun albumRoute(bucketId: String, bucketName: String) = "album/$bucketId/$bucketName"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.GALLERY
    ) {
        composable(Routes.GALLERY) {
            GalleryScreen(
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
                onNavigateToAlbum = { bucketId, bucketName ->
                    navController.navigate(Routes.albumRoute(bucketId, bucketName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            ViewerScreen(
                initialIndex = index,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
