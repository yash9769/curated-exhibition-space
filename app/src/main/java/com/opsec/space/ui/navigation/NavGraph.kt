package com.opsec.space.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opsec.space.ui.screens.opsec.MainContainerScreen
import com.opsec.space.ui.screens.viewer.ImageViewerScreen
import com.opsec.space.ui.screens.trash.TrashScreen
import com.opsec.space.ui.screens.vault.VaultScreen
import com.opsec.space.ui.screens.slideshow.SlideshowScreen
import com.opsec.space.ui.screens.edit.EditScreen
import com.opsec.space.ui.screens.startup.StartupLockScreen

sealed class Screen(val route: String) {
    object Opsec : Screen("opsec")
    object ImageViewer : Screen("image_viewer/{imageId}") {
        fun createRoute(imageId: Long) = "image_viewer/$imageId"
    }
    object Trash : Screen("trash")
    object Vault : Screen("vault")
    object Slideshow : Screen("slideshow/{imageId}") {
        fun createRoute(imageId: Long) = "slideshow/$imageId"
    }
    object Edit : Screen("edit/{imageId}") {
        fun createRoute(imageId: Long) = "edit/$imageId"
    }
    object StartupLock : Screen("startup_lock")
}

@Composable
fun OpsecNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.StartupLock.route
    ) {
        composable(Screen.StartupLock.route) {
            StartupLockScreen(
                onNavigateToOpsec = {
                    navController.navigate(Screen.Opsec.route) {
                        popUpTo(Screen.StartupLock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Opsec.route) {
            MainContainerScreen(
                onImageClick = { imageId ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageId))
                },
                onViewTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onViewVault = {
                    navController.navigate(Screen.Vault.route)
                }
            )
        }

        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("imageId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getLong("imageId") ?: return@composable
            ImageViewerScreen(
                initialImageId = imageId,
                onBack = { navController.popBackStack() },
                onStartSlideshow = { id ->
                    navController.navigate(Screen.Slideshow.createRoute(id))
                },
                onEditImage = { id ->
                    navController.navigate(Screen.Edit.createRoute(id))
                }
            )
        }

        composable(Screen.Trash.route) {
            TrashScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Vault.route) {
            VaultScreen(
                onBack = { navController.popBackStack() },
                onImageClick = { imageId ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageId))
                }
            )
        }

        composable(
            route = Screen.Slideshow.route,
            arguments = listOf(
                navArgument("imageId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getLong("imageId") ?: return@composable
            SlideshowScreen(
                initialImageId = imageId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Edit.route,
            arguments = listOf(
                navArgument("imageId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getLong("imageId") ?: return@composable
            EditScreen(
                imageId = imageId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
