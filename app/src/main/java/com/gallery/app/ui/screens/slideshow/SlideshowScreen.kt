package com.gallery.app.ui.screens.slideshow

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.data.model.ImageItem
import com.gallery.app.data.repository.ImageRepository
import com.gallery.app.utils.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {
    val images: StateFlow<List<ImageItem>> = repository.getImages(SortOrder.DATE_ADDED)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideshowScreen(
    initialImageId: Long,
    onBack: () -> Unit,
    viewModel: SlideshowViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsStateWithLifecycle()

    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var intervalSeconds by remember { mutableIntStateOf(3) }
    var isLooping by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    
    // 0 = Fade/Crossfade, 1 = Slide Left, 2 = Slide Up
    var transitionEffect by remember { mutableIntStateOf(0) }

    // Find initial index
    LaunchedEffect(images, initialImageId) {
        if (images.isNotEmpty()) {
            val idx = images.indexOfFirst { it.id == initialImageId }
            if (idx != -1) {
                currentIndex = idx
            }
        }
    }

    // Auto-advance slideshow timer
    LaunchedEffect(isPlaying, currentIndex, intervalSeconds, images.size) {
        if (isPlaying && images.isNotEmpty()) {
            delay(intervalSeconds * 1000L)
            val nextIndex = currentIndex + 1
            if (nextIndex < images.size) {
                currentIndex = nextIndex
            } else if (isLooping) {
                currentIndex = 0
            } else {
                isPlaying = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        if (images.isNotEmpty() && currentIndex < images.size) {
            val currentItem = images[currentIndex]

            // Animated content transitions
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    when (transitionEffect) {
                        1 -> { // Slide horizontally
                            (slideInHorizontally(animationSpec = tween(600)) { it } + fadeIn(animationSpec = tween(600)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(600)) { -it } + fadeOut(animationSpec = tween(600)))
                        }
                        2 -> { // Slide vertically
                            (slideInVertically(animationSpec = tween(600)) { it } + fadeIn(animationSpec = tween(600)))
                                .togetherWith(slideOutVertically(animationSpec = tween(600)) { -it } + fadeOut(animationSpec = tween(600)))
                        }
                        else -> { // Crossfade
                            fadeIn(animationSpec = tween(800))
                                .togetherWith(fadeOut(animationSpec = tween(800)))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "slideshow_transition"
            ) { targetItem ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(targetItem.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Dark scrim at top and bottom for readability
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Slideshow", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Slideshow",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    if (images.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1} / ${images.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Bottom Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Playback settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed / Interval selector
                        Button(
                            onClick = {
                                intervalSeconds = when (intervalSeconds) {
                                    2 -> 3
                                    3 -> 5
                                    5 -> 2
                                    else -> 3
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("${intervalSeconds}s Interval", color = Color.White, fontSize = 12.sp)
                        }

                        // Transition style selector
                        Button(
                            onClick = {
                                transitionEffect = (transitionEffect + 1) % 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Transform, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            val effectText = when(transitionEffect) {
                                1 -> "Slide H"
                                2 -> "Slide V"
                                else -> "Crossfade"
                            }
                            Text(effectText, color = Color.White, fontSize = 12.sp)
                        }

                        // Loop toggle
                        IconButton(onClick = { isLooping = !isLooping }) {
                            Icon(
                                imageVector = if (isLooping) Icons.Default.Loop else Icons.Default.TrendingFlat,
                                contentDescription = "Loop Toggle",
                                tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }

                    // Main Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                } else if (isLooping && images.isNotEmpty()) {
                                    currentIndex = images.size - 1
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        
                        Spacer(Modifier.width(24.dp))

                        FilledIconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        IconButton(
                            onClick = {
                                if (currentIndex < images.size - 1) {
                                    currentIndex++
                                } else if (isLooping) {
                                    currentIndex = 0
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
