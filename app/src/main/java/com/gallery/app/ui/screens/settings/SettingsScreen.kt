package com.gallery.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.app.ui.components.PINPad
import kotlinx.coroutines.flow.MutableStateFlow

object ThemeConfig {
    val darkThemeState = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
}

object SecurityConfig {
    private const val PREFS_NAME = "security_prefs"
    private const val KEY_PIN = "vault_pin"
    private const val KEY_BIOMETRICS = "use_biometrics"

    fun getPin(context: android.content.Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN, null)
    }

    fun savePin(context: android.content.Context, pin: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun useBiometrics(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRICS, false)
    }

    fun setUseBiometrics(context: android.content.Context, use: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRICS, use).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onViewTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val darkThemePref by ThemeConfig.darkThemeState.collectAsStateWithLifecycle()
    var hasPin by remember { mutableStateOf(SecurityConfig.getPin(context) != null) }
    var useBiometrics by remember { mutableStateOf(SecurityConfig.useBiometrics(context)) }
    
    var showPinDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    var enteredPin by remember { mutableStateOf("") }
    var pinSetupStep by remember { mutableStateOf(0) } // 0 = verify old, 1 = enter new, 2 = confirm new
    var tempPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Theme Customization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val themeLabel = when(darkThemePref) {
                    null -> "System Default"
                    true -> "Dark Mode"
                    false -> "Light Mode"
                }
                ListItem(
                    headlineContent = { Text("App Theme") },
                    supportingContent = { Text(themeLabel) },
                    modifier = Modifier.clickable { showThemeDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        // Security Vault Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Private Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                
                ListItem(
                    headlineContent = { Text(if (hasPin) "Change PIN Lock" else "Set PIN Lock") },
                    supportingContent = { Text(if (hasPin) "Update your private vault protection" else "Secure your private folder with a PIN") },
                    modifier = Modifier.clickable {
                        enteredPin = ""
                        tempPin = ""
                        pinError = null
                        pinSetupStep = if (hasPin) 0 else 1
                        showPinDialog = true
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (hasPin) {
                    ListItem(
                        headlineContent = { Text("Use Biometrics") },
                        supportingContent = { Text("Unlock Vault with Fingerprint or Face") },
                        trailingContent = {
                            Switch(
                                checked = useBiometrics,
                                onCheckedChange = {
                                    useBiometrics = it
                                    SecurityConfig.setUseBiometrics(context, it)
                                    Toast.makeText(context, if (it) "Biometrics Enabled" else "Biometrics Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Disable Vault PIN", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            SecurityConfig.savePin(context, null)
                            SecurityConfig.setUseBiometrics(context, false)
                            hasPin = false
                            useBiometrics = false
                            Toast.makeText(context, "Vault Protection Disabled", Toast.LENGTH_SHORT).show()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        // Trash Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("File Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                
                ListItem(
                    headlineContent = { Text("View Trash / Recently Deleted") },
                    supportingContent = { Text("Restore or permanently delete photos") },
                    modifier = Modifier.clickable { onViewTrash() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        // Info/About Card - Made by Yashodhan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Made by Yashodhan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select App Theme") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeConfig.darkThemeState.value = null
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(selected = darkThemePref == null, onClick = {
                            ThemeConfig.darkThemeState.value = null
                            showThemeDialog = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("System Default")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeConfig.darkThemeState.value = false
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(selected = darkThemePref == false, onClick = {
                            ThemeConfig.darkThemeState.value = false
                            showThemeDialog = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Light Mode")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeConfig.darkThemeState.value = true
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(selected = darkThemePref == true, onClick = {
                            ThemeConfig.darkThemeState.value = true
                            showThemeDialog = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Dark Mode")
                    }
                }
            },
            confirmButton = {}
        )
    }

    // PIN Pad Setup Dialog
    if (showPinDialog) {
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val titleText = when (pinSetupStep) {
                        0 -> "Enter Current PIN"
                        1 -> "Enter New 4-Digit PIN"
                        else -> "Confirm New 4-Digit PIN"
                    }

                    PINPad(
                        enteredPin = enteredPin,
                        title = titleText,
                        errorMessage = pinError,
                        onPinChange = { pin ->
                            enteredPin = pin
                            pinError = null
                            if (pin.length == 4) {
                                when (pinSetupStep) {
                                    0 -> {
                                        val actualPin = SecurityConfig.getPin(context)
                                        if (pin == actualPin) {
                                            pinSetupStep = 1
                                            enteredPin = ""
                                        } else {
                                            pinError = "Incorrect PIN"
                                            enteredPin = ""
                                        }
                                    }
                                    1 -> {
                                        tempPin = pin
                                        pinSetupStep = 2
                                        enteredPin = ""
                                    }
                                    2 -> {
                                        if (pin == tempPin) {
                                            SecurityConfig.savePin(context, pin)
                                            hasPin = true
                                            showPinDialog = false
                                            Toast.makeText(context, "PIN Saved Successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            pinError = "PINs do not match. Try again."
                                            enteredPin = ""
                                            pinSetupStep = 1
                                        }
                                    }
                                }
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
