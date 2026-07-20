package com.gallery.app.ui.screens.viewer

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.data.model.ImageItem
import com.gallery.app.ui.components.VideoPlayer
import com.gallery.app.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    initialImageId: Long,
    onBack: () -> Unit,
    onStartSlideshow: (Long) -> Unit,
    onEditImage: (Long) -> Unit,
    viewModel: ImageViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsStateWithLifecycle()
    val showControls by viewModel.showControls.collectAsStateWithLifecycle()
    val showInfo by viewModel.showInfo.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<ImageItem?>(null) }
    
    // Rename Dialog State
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    
    // Move Album Dialog State
    var showMoveAlbumDialog by remember { mutableStateOf(false) }
    var moveAlbumInput by remember { mutableStateOf("") }

    val initialIndex = remember(images, initialImageId) {
        images.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.size }
    )

    // Auto-navigate to correct page when images load
    LaunchedEffect(initialIndex, images.size) {
        if (images.isNotEmpty() && pagerState.currentPage != initialIndex) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    val currentImage = if (images.isNotEmpty() && pagerState.currentPage < images.size) {
        images[pagerState.currentPage]
    } else null

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No images", color = Color.White)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page < images.size) {
                    val mediaItem = images[page]
                    
                    // Gesture Wrapper for Swipe Down to Close / Swipe Up for Info
                    var swipeOffsetY by remember { mutableFloatStateOf(0f) }
                    
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            swipeOffsetY += dragAmount
                                        },
                                        onDragEnd = {
                                            if (swipeOffsetY > 150f) {
                                                onBack() // Swipe Down -> Close
                                            } else if (swipeOffsetY < -150f) {
                                                viewModel.toggleInfo() // Swipe Up -> Show Details
                                            }
                                            swipeOffsetY = 0f
                                        }
                                    )
                                }
                        ) {
                        if (mediaItem.isVideo) {
                            VideoPlayer(
                                videoUri = mediaItem.toUri(),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ZoomableImage(
                                image = mediaItem,
                                onTap = viewModel::toggleControls,
                                modifier = Modifier.graphicsLayer(translationY = swipeOffsetY)
                            )
                        }
                    }
                }
            }
        }

        // Top bar controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = if (images.isNotEmpty()) {
                            "${pagerState.currentPage + 1} / ${images.size}"
                        } else "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row {
                        IconButton(onClick = { currentImage?.let { onStartSlideshow(it.id) } }) {
                            Icon(
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = "Slideshow",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = viewModel::toggleInfo) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom bar controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share button
                IconButton(
                    onClick = {
                        currentImage?.let { image ->
                            try {
                                val uri = image.toUri()
                                val shareUri = if (uri.scheme == "file") {
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        java.io.File(uri.path ?: "")
                                    )
                                } else {
                                    uri
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    type = if (image.isVideo) "video/*" else "image/*"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Media")
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                // Favorite button
                IconButton(
                    onClick = { currentImage?.let { viewModel.toggleFavorite(it) } }
                ) {
                    Icon(
                        imageVector = if (currentImage?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentImage?.isFavorite == true) Color(0xFFFF4B4B) else Color.White
                    )
                }

                // Edit image button (Only show for photos)
                if (currentImage != null && !currentImage.isVideo) {
                    IconButton(
                        onClick = { onEditImage(currentImage.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Image",
                            tint = Color.White
                        )
                    }
                }

                // Delete / Remove button
                IconButton(
                    onClick = {
                        currentImage?.let {
                            imageToDelete = it
                            showDeleteDialog = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B6B)
                    )
                }

                // More Options 3 dots
                var showMoreDropdown by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMoreDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreDropdown,
                        onDismissRequest = { showMoreDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    renameInput = it.fileName.substringBeforeLast(".")
                                    showRenameDialog = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    viewModel.duplicateImage(it)
                                    Toast.makeText(context, "Duplicated image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Album") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    moveAlbumInput = it.folderName ?: ""
                                    showMoveAlbumDialog = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Private Vault") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    viewModel.moveToVault(it.id, true)
                                    Toast.makeText(context, "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Set as Wallpaper") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    try {
                                        val attachIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                            setDataAndType(it.toUri(), if (it.isVideo) "video/*" else "image/*")
                                            putExtra("mimeType", if (it.isVideo) "video/*" else "image/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(attachIntent, "Set as Wallpaper"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot set wallpaper", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open With Another App") },
                            onClick = {
                                showMoreDropdown = false
                                currentImage?.let {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(it.toUri(), if (it.isVideo) "video/*" else "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(viewIntent, "Open with..."))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Info bottom sheet
    if (showInfo && currentImage != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::toggleInfo,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ImageInfoSheet(image = currentImage)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && imageToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                imageToDelete = null
            },
            title = { Text("Move to Trash") },
            text = {
                Text("Move \"${imageToDelete?.fileName}\" to the Trash?\n\nYou can restore it from settings anytime within 30 days.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageToDelete?.let {
                            viewModel.removeImage(it)
                            Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                            if (images.size <= 1) onBack()
                        }
                        showDeleteDialog = false
                        imageToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    imageToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && currentImage != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                Column {
                    Text("Enter new name:")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameImage(currentImage.id, renameInput.trim())
                            Toast.makeText(context, "Renamed file", Toast.LENGTH_SHORT).show()
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to Album Dialog
    if (showMoveAlbumDialog && currentImage != null) {
        AlertDialog(
            onDismissRequest = { showMoveAlbumDialog = false },
            title = { Text("Move to Album") },
            text = {
                Column {
                    Text("Enter Album (Folder) name:")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = moveAlbumInput,
                        onValueChange = { moveAlbumInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (moveAlbumInput.isNotBlank()) {
                            viewModel.moveImage(currentImage.id, moveAlbumInput.trim())
                            Toast.makeText(context, "Moved to album: ${moveAlbumInput.trim()}", Toast.LENGTH_SHORT).show()
                            showMoveAlbumDialog = false
                        }
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ZoomableImage(
    image: ImageItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        val maxX = (scale - 1f) * 400f
        val maxY = (scale - 1f) * 600f
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
            y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState, enabled = scale > 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.toUri())
                .crossfade(true)
                .build(),
            contentDescription = image.fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
private fun ImageInfoSheet(image: ImageItem) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()) // Allow scrolling for rich metadata list
    ) {
        Text(
            text = "Image Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        InfoRow(label = "Filename", value = image.fileName)
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "Resolution",
            value = ImageUtils.formatResolution(image.width, image.height)
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "Date Added",
            value = ImageUtils.formatDate(image.dateAdded)
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "Date Taken",
            value = ImageUtils.formatDate(image.dateTaken)
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "File Size",
            value = ImageUtils.formatFileSize(context, image.fileSize)
        )

        // Show EXIF info if available
        image.cameraModel?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Camera Model", value = it)
        }
        image.lensModel?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Lens Model", value = it)
        }
        image.iso?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "ISO", value = it.toString())
        }
        image.aperture?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Aperture", value = "f/${"%.1f".format(it)}")
        }
        image.shutterSpeed?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Shutter Speed", value = "${it}s")
        }
        if (image.gpsLatitude != null && image.gpsLongitude != null) {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "GPS Location", value = "${"%.4f".format(image.gpsLatitude)}, ${"%.4f".format(image.gpsLongitude)}")
        }
        image.dateModified?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Date Modified", value = ImageUtils.formatDate(it))
        }
        
        Spacer(Modifier.height(8.dp))
        // Parse physical path from Uri
        val pathString = Uri.parse(image.uri).path ?: image.uri
        InfoRow(label = "Path", value = pathString)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.4f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// Remember vertical scroll state for sheets
@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
