package com.opsec.space.ui.screens.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsec.space.data.repository.ProfileManager
import com.opsec.space.utils.BiometricHelper
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupLockViewModel @Inject constructor(
    private val profileManager: ProfileManager
) : ViewModel() {

    fun unlockProfile(profileId: Long, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            profileManager.setActiveProfile(profileId)
            onUnlocked()
        }
    }
}

@Composable
fun StartupLockScreen(
    onNavigateToOpsec: () -> Unit,
    viewModel: StartupLockViewModel = hiltViewModel()
) {
    val context = LocalContext.current as? FragmentActivity
    var screenHeight by remember { mutableIntStateOf(0) }

    // Tap counters
    var topTaps by remember { mutableIntStateOf(0) }
    var centerTaps by remember { mutableIntStateOf(0) }
    var bottomTaps by remember { mutableIntStateOf(0) }

    // Flag to start the 20-second timer and allow taps only after biometrics succeed
    var isBiometricsPassed by remember { mutableStateOf(false) }

    // On start, request biometrics immediately
    LaunchedEffect(Unit) {
        if (context != null) {
            BiometricHelper.authenticate(
                activity = context,
                onSuccess = {
                    isBiometricsPassed = true
                },
                onError = {
                    // Close the app if biometrics cancelled or failed
                    context.finish()
                }
            )
        } else {
            // Fallback
            isBiometricsPassed = true
        }
    }

    // 10-second timeout starts only after biometrics passed
    LaunchedEffect(isBiometricsPassed) {
        if (isBiometricsPassed) {
            delay(10000L) // Wait 10 seconds
            // Fallback to common opsec (Profile 1)
            viewModel.unlockProfile(1L, onNavigateToOpsec)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onSizeChanged { size ->
                screenHeight = size.height
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var maxPointers = 1
                    var yAvg = 0f

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val currentPointers = event.changes.filter { it.pressed }
                        if (currentPointers.size > maxPointers) {
                            maxPointers = currentPointers.size
                            yAvg = currentPointers.map { it.position.y }.average().toFloat()
                        }
                    } while (event.changes.any { it.pressed })

                    if (maxPointers == 3 && screenHeight > 0) {
                        val oneThird = screenHeight / 3f
                        when {
                            yAvg < oneThird -> {
                                topTaps++
                                if (topTaps >= 10 && isBiometricsPassed) {
                                    topTaps = 0
                                    viewModel.unlockProfile(1L, onNavigateToOpsec)
                                }
                            }
                            yAvg < oneThird * 2 -> {
                                centerTaps++
                                if (centerTaps >= 10 && isBiometricsPassed) {
                                    centerTaps = 0
                                    viewModel.unlockProfile(2L, onNavigateToOpsec)
                                }
                            }
                            else -> {
                                bottomTaps++
                                if (bottomTaps >= 10 && isBiometricsPassed) {
                                    bottomTaps = 0
                                    viewModel.unlockProfile(3L, onNavigateToOpsec)
                                }
                            }
                        }
                    } else {
                        // Reset if a non-3-finger tap occurs? The user didn't specify, but it's safer to just ignore it or reset.
                        // Let's just ignore non-3-finger taps, so they can keep trying.
                    }
                }
            }
    ) {
        // Pure white screen. No content.
    }
}
