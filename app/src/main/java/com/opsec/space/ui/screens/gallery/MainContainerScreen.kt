package com.opsec.space.ui.screens.opsec

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsec.space.ui.screens.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    onImageClick: (Long) -> Unit,
    onViewTrash: () -> Unit,
    onViewVault: () -> Unit,
    viewModel: OpsecViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    // Setup picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

    fun launchPicker() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        } else {
            legacyPickerLauncher.launch("*/*")
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Photo, contentDescription = "Photos") },
                    label = { Text("Photos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PhotoAlbum, contentDescription = "Albums") },
                    label = { Text("Albums") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            // Show FAB only on Photos (0) or Albums (1) tabs, and only when unlocked by the 10-tap secret
            AnimatedVisibility(
                visible = isUnlocked && (selectedTab == 0 || selectedTab == 1),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { launchPicker() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photos"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    // Photos Tab: Displays main timeline grid
                    OpsecScreen(
                        onImageClick = onImageClick,
                        viewModel = viewModel,
                        onViewVault = onViewVault,
                        onViewTrash = onViewTrash,
                        onViewSettings = { selectedTab = 3 }
                    )
                }
                1 -> {
                    // Albums Tab: Displays albums folder view
                    AlbumsTabScreen(
                        onImageClick = onImageClick,
                        viewModel = viewModel
                    )
                }
                2 -> {
                    // Search Tab: Displays interactive search
                    SearchTabScreen(
                        onImageClick = onImageClick,
                        viewModel = viewModel
                    )
                }
                3 -> {
                    // Settings Tab
                    SettingsScreen(
                        onViewTrash = onViewTrash
                    )
                }
            }
        }
    }
}
