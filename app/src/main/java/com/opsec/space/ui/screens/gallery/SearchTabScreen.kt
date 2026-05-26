package com.opsec.space.ui.screens.opsec

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsec.space.data.model.ImageItem
import com.opsec.space.ui.components.ImageThumbnail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTabScreen(
    onImageClick: (Long) -> Unit,
    viewModel: OpsecViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    // 0 = All, 1 = Photos, 2 = Videos, 3 = Favorites
    var filterType by remember { mutableIntStateOf(0) }

    // Client-side advanced search filtering
    val searchResults = remember(searchQuery, filterType, uiState.images) {
        uiState.images.filter { item ->
            // Apply media filter
            val matchesFilter = when (filterType) {
                1 -> !item.isVideo
                2 -> item.isVideo
                3 -> item.isFavorite
                else -> true
            }
            if (!matchesFilter) return@filter false

            if (searchQuery.isBlank()) return@filter true

            // Match text query against: filename, folder/album name, camera, lens, date taken, date added
            val q = searchQuery.lowercase().trim()
            val dateStr = SimpleDateFormat("MMMM yyyy dd d EEEE", Locale.getDefault())
                .format(Date(item.dateTaken ?: item.dateAdded)).lowercase()

            item.fileName.lowercase().contains(q) ||
            (item.folderName?.lowercase()?.contains(q) == true) ||
            (item.cameraModel?.lowercase()?.contains(q) == true) ||
            (item.lensModel?.lowercase()?.contains(q) == true) ||
            dateStr.contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search filename, date, album, camera...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == 0,
                onClick = { filterType = 0 },
                label = { Text("All", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterType == 1,
                onClick = { filterType = 1 },
                label = { Text("Photos", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterType == 2,
                onClick = { filterType = 2 },
                label = { Text("Videos", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterType == 3,
                onClick = { filterType = 3 },
                label = { Text("Favorites", fontSize = 11.sp) }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Grid of results
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Type to search your opsec" else "No matching items found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "${searchResults.size} results found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(searchResults, key = { it.id }) { image ->
                    ImageThumbnail(
                        image = image,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(image.id) }
                    )
                }
            }
        }
    }
}
