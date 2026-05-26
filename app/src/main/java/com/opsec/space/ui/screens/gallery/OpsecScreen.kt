package com.opsec.space.ui.screens.opsec

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsec.space.data.model.ImageItem
import com.opsec.space.ui.components.EmptyState
import com.opsec.space.ui.components.ImageThumbnail
import com.opsec.space.utils.SortOrder
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TimelineItem {
    data class Header(val title: String) : TimelineItem()
    data class Photo(val image: ImageItem) : TimelineItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OpsecScreen(
    onImageClick: (Long) -> Unit,
    onViewVault: () -> Unit,
    onViewTrash: () -> Unit,
    onViewSettings: () -> Unit,
    viewModel: OpsecViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedImages = remember { mutableStateListOf<ImageItem>() }

    // Pinch-to-zoom grid columns state
    var columnCount by remember { mutableIntStateOf(3) }
    var scale by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale *= zoomChange
        if (scale > 1.3f) {
            if (columnCount > 2) {
                columnCount--
                scale = 1f
            }
        } else if (scale < 0.7f) {
            if (columnCount < 6) {
                columnCount++
                scale = 1f
            }
        }
    }

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Credits Dialog (5-tap on MoreVert)
    var moreVertTapCount by remember { mutableIntStateOf(0) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Process images into timeline items grouped by Month/Year
    val timelineItems = remember(uiState.images) {
        val list = mutableListOf<TimelineItem>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        var currentHeader: String? = null
        uiState.images.forEach { image ->
            val date = Date(image.dateTaken ?: image.dateAdded)
            val header = sdf.format(date)
            if (header != currentHeader) {
                currentHeader = header
                list.add(TimelineItem.Header(header))
            }
            list.add(TimelineItem.Photo(image))
        }
        list
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                if (isSelectionMode) {
                    // Contextual Top App Bar for Selection Mode
                    TopAppBar(
                        title = { Text("${selectedImages.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedImages.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            // Select All
                            IconButton(onClick = {
                                selectedImages.clear()
                                selectedImages.addAll(uiState.images)
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                            // Batch Share
                            IconButton(
                                enabled = selectedImages.isNotEmpty(),
                                onClick = {
                                    val uris = ArrayList<Uri>().apply {
                                        addAll(selectedImages.map { it.toUri() })
                                    }
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND_MULTIPLE
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        type = "image/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Selected"))
                                    isSelectionMode = false
                                    selectedImages.clear()
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            // Batch Favorite
                            IconButton(
                                enabled = selectedImages.isNotEmpty(),
                                onClick = {
                                    val allFavorite = selectedImages.all { it.isFavorite }
                                    viewModel.batchFavorite(selectedImages, !allFavorite)
                                    Toast.makeText(context, if (allFavorite) "Removed from favorites" else "Added to favorites", Toast.LENGTH_SHORT).show()
                                    isSelectionMode = false
                                    selectedImages.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedImages.all { it.isFavorite }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite"
                                )
                            }
                            // Batch Delete
                            IconButton(
                                enabled = selectedImages.isNotEmpty(),
                                onClick = {
                                    viewModel.batchDelete(selectedImages)
                                    Toast.makeText(context, "Moved to trash", Toast.LENGTH_SHORT).show()
                                    isSelectionMode = false
                                    selectedImages.clear()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            // Move to Vault / Folder overflow
                            var showSelectActionsDropdown by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSelectActionsDropdown = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More selection options")
                                }
                                DropdownMenu(
                                    expanded = showSelectActionsDropdown,
                                    onDismissRequest = { showSelectActionsDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Move to Album") },
                                        onClick = {
                                            showSelectActionsDropdown = false
                                            showMoveFolderDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move to Private Vault") },
                                        onClick = {
                                            showSelectActionsDropdown = false
                                            viewModel.batchMoveToVault(selectedImages, true)
                                            Toast.makeText(context, "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                                            isSelectionMode = false
                                            selectedImages.clear()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    )
                } else {
                    // Regular Top App Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "Opsec",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.onOpsecTitleTap() }
                                )
                            )
                        },
                        actions = {
                            IconButton(onClick = viewModel::toggleSearchBar) {
                                Icon(
                                    imageVector = if (uiState.showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (uiState.showSearchBar) "Close Search" else "Search"
                                )
                            }
                            IconButton(onClick = viewModel::toggleSortDirection) {
                                Icon(
                                    imageVector = if (uiState.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = if (uiState.isAscending) "Sort Ascending" else "Sort Descending"
                                )
                            }
                            Box {
                                IconButton(onClick = { viewModel.toggleSortMenu() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort"
                                    )
                                }
                                DropdownMenu(
                                    expanded = uiState.showSortMenu,
                                    onDismissRequest = viewModel::dismissSortMenu
                                ) {
                                    SortOrder.entries.forEach { sort ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (sort) {
                                                        SortOrder.DATE_ADDED -> "Date Added"
                                                        SortOrder.DATE_TAKEN -> "Date Taken"
                                                        SortOrder.FILE_NAME -> "Filename"
                                                        SortOrder.FILE_SIZE -> "File Size"
                                                    }
                                                )
                                            },
                                            onClick = { viewModel.onSortOrderChanged(sort) },
                                            leadingIcon = {
                                                RadioButton(
                                                    selected = uiState.sortOrder == sort,
                                                    onClick = { viewModel.onSortOrderChanged(sort) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            // More options three-dots dropdown
                            Box {
                                IconButton(onClick = { 
                                    moreVertTapCount++
                                    if (moreVertTapCount >= 5) {
                                        showCreditsDialog = true
                                        moreVertTapCount = 0
                                    }
                                    showOverflowMenu = true 
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Start Slideshow") },
                                        onClick = {
                                            showOverflowMenu = false
                                            if (uiState.images.isNotEmpty()) {
                                                onImageClick(uiState.images.first().id) // Or trigger slideshow directly
                                            } else {
                                                Toast.makeText(context, "No photos to play", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.Slideshow, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Private Vault") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onViewVault()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Trash / Deleted") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onViewTrash()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onViewSettings()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Search bar
                AnimatedVisibility(
                    visible = uiState.showSearchBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.searchQuery,
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onSearch = {},
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = { Text("Search by filename...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {}
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isLoading && uiState.images.isEmpty()) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 4.dp,
                        top = 4.dp,
                        bottom = if (isUnlocked) 88.dp else 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformState)
                ) {
                    items(
                        items = timelineItems,
                        span = { item ->
                            if (item is TimelineItem.Header) {
                                GridItemSpan(columnCount)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { item ->
                        when (item) {
                            is TimelineItem.Header -> {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            is TimelineItem.Photo -> {
                                val image = item.image
                                val isSelected = selectedImages.contains(image)

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) {
                                                    if (isSelected) {
                                                        selectedImages.remove(image)
                                                        if (selectedImages.isEmpty()) isSelectionMode = false
                                                    } else {
                                                        selectedImages.add(image)
                                                    }
                                                } else {
                                                    onImageClick(image.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    isSelectionMode = true
                                                    selectedImages.clear()
                                                    selectedImages.add(image)
                                                }
                                            }
                                        )
                                ) {
                                    ImageThumbnail(
                                        image = image,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked == true) {
                                                    selectedImages.add(image)
                                                } else {
                                                    selectedImages.remove(image)
                                                    if (selectedImages.isEmpty()) isSelectionMode = false
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Move to folder dialog
    if (showMoveFolderDialog) {
        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            title = { Text("Move items to Album") },
            text = {
                Column {
                    Text("Enter Album Name:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Vacation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.batchMoveToFolder(selectedImages, folderNameInput.trim())
                            Toast.makeText(context, "Moved to ${folderNameInput.trim()}", Toast.LENGTH_SHORT).show()
                            folderNameInput = ""
                            showMoveFolderDialog = false
                            isSelectionMode = false
                            selectedImages.clear()
                        }
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Credits Dialog
    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = { Text("About") },
            text = { Text("Made by Yashodhan\nUnder the guidance of Vedant Dighe") },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
