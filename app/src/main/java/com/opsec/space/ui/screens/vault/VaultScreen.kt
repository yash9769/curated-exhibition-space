package com.opsec.space.ui.screens.vault

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.opsec.space.data.model.ImageItem
import com.opsec.space.data.repository.ImageRepository
import com.opsec.space.ui.components.ImageThumbnail
import com.opsec.space.ui.components.PINPad
import com.opsec.space.ui.screens.settings.SecurityConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    val vaultedImages: StateFlow<List<ImageItem>> = repository.getVaultedImages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun unvaultImage(id: Long) {
        viewModelScope.launch {
            repository.updateVaulted(id, false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    onImageClick: (Long) -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val vaultedImages by viewModel.vaultedImages.collectAsStateWithLifecycle()
    
    val actualPin = remember { SecurityConfig.getPin(context) }
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    
    var selectedImageForAction by remember { mutableStateOf<ImageItem?>(null) }

    if (actualPin == null) {
        // No PIN configured
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Private Vault") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Vault PIN Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please go to Settings first to set up a secure PIN for your Private Vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    } else if (!isUnlocked) {
        // Locked Screen: Prompts PINPad
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vault Locked") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                PINPad(
                    enteredPin = enteredPin,
                    errorMessage = pinError,
                    onPinChange = { pin ->
                        enteredPin = pin
                        pinError = null
                        if (pin.length == 4) {
                            if (pin == actualPin) {
                                isUnlocked = true
                                enteredPin = ""
                            } else {
                                pinError = "Incorrect PIN"
                                enteredPin = ""
                            }
                        }
                    }
                )
            }
        }
    } else {
        // Vault Content screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Private Vault") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isUnlocked = false }) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (vaultedImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Vault is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add photos to Vault from the photo viewer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(vaultedImages, key = { it.id }) { image ->
                            ImageThumbnail(
                                image = image,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { selectedImageForAction = image }
                            )
                        }
                    }
                }
            }
        }
    }

    // Action dialog for vaulted image
    if (selectedImageForAction != null) {
        val image = selectedImageForAction!!
        AlertDialog(
            onDismissRequest = { selectedImageForAction = null },
            title = { Text("Vault Option") },
            text = { Text("Would you like to view this image or restore it back to the regular opsec?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImageClick(image.id)
                        selectedImageForAction = null
                    }
                ) {
                    Text("View Fullscreen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.unvaultImage(image.id)
                        Toast.makeText(context, "Restored to Opsec: ${image.fileName}", Toast.LENGTH_SHORT).show()
                        selectedImageForAction = null
                    }
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Remove from Vault")
                }
            }
        )
    }
}
