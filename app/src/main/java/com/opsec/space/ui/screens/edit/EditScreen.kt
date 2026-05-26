package com.opsec.space.ui.screens.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.opsec.space.data.model.ImageItem
import com.opsec.space.data.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {
    private val _imageItem = MutableStateFlow<ImageItem?>(null)
    val imageItem: StateFlow<ImageItem?> = _imageItem

    fun loadImage(id: Long) {
        viewModelScope.launch {
            val images = repository.getImages(com.opsec.space.utils.SortOrder.DATE_ADDED).first()
            val deleted = repository.getDeletedImages().first()
            _imageItem.value = deleted.find { it.id == id }
                ?: images.find { it.id == id }
        }
    }

    suspend fun saveEditedImage(context: android.content.Context, originalItem: ImageItem, bitmap: Bitmap, asCopy: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val originalUri = Uri.parse(originalItem.uri)
                val destFile = if (asCopy) {
                    val ext = originalItem.fileName.substringAfterLast(".", "jpg")
                    val baseName = originalItem.fileName.substringBeforeLast(".")
                    val newName = "${baseName}_edit_${System.currentTimeMillis()}.$ext"
                    context.filesDir.resolve("media").resolve(newName)
                } else {
                    File(originalUri.path ?: return@withContext false)
                }

                val parentFile = destFile.parentFile
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs()
                }

                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                if (asCopy) {
                    val newUri = Uri.fromFile(destFile).toString()
                    val newItem = originalItem.copy(
                        id = 0,
                        uri = newUri,
                        fileName = destFile.name,
                        dateAdded = System.currentTimeMillis(),
                        fileSize = destFile.length()
                    )
                    repository.addImages(listOf(Uri.fromFile(destFile)))
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

// Doodle stroke model
data class DoodleStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

// Text overlay model
data class TextOverlay(
    val text: String,
    val color: Color,
    val position: Offset
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    imageId: Long,
    onBack: () -> Unit,
    viewModel: EditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageItem by viewModel.imageItem.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Editor Modes: 0 = Adjustments, 1 = Filters, 2 = Draw, 3 = Text
    var activeMode by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }

    // Adjustments
    var brightness by remember { mutableFloatStateOf(0f) } // -100 to 100
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.5 to 2.0
    var saturation by remember { mutableFloatStateOf(1f) } // 0.0 to 2.0
    
    // Active filter
    var activeFilter by remember { mutableIntStateOf(0) } // 0 = None, 1 = Warm, 2 = Cold, 3 = B&W, 4 = Sepia, 5 = Invert

    // Drawing
    var doodleColor by remember { mutableStateOf(Color.Red) }
    var doodleWidth by remember { mutableFloatStateOf(10f) }
    val strokes = remember { mutableStateListOf<DoodleStroke>() }
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }

    // Text Overlays
    val textOverlays = remember { mutableStateListOf<TextOverlay>() }
    var showTextDialog by remember { mutableStateOf(false) }
    var newTextString by remember { mutableStateOf("") }
    var newTextColor by remember { mutableStateOf(Color.White) }

    // Load original bitmap
    LaunchedEffect(imageId) {
        viewModel.loadImage(imageId)
    }

    LaunchedEffect(imageItem) {
        imageItem?.let { item ->
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(item.uri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    originalBitmap = bmp
                    currentBitmap = bmp?.copy(Bitmap.Config.ARGB_8888, true)
                } catch (e: Exception) {
                    // Handle load error
                }
            }
        }
    }

    // Apply adjustments & filters live to currentBitmap
    LaunchedEffect(brightness, contrast, saturation, activeFilter, originalBitmap) {
        val original = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val adjusted = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(adjusted)
            val paint = Paint()

            // 1. ColorMatrix for Brightness, Contrast, Saturation
            val cm = ColorMatrix()
            
            // Saturation
            cm.setSaturation(saturation)
            
            // Contrast & Brightness
            val scale = contrast
            val translate = brightness
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            contrastMatrix.postConcat(cm)

            // 2. Filters
            when (activeFilter) {
                1 -> { // Warm / Amber
                    contrastMatrix.postConcat(ColorMatrix(floatArrayOf(
                        1.2f, 0f, 0f, 0f, 20f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 0.8f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
                2 -> { // Cold / Cyan
                    contrastMatrix.postConcat(ColorMatrix(floatArrayOf(
                        0.8f, 0f, 0f, 0f, -20f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 1.2f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
                3 -> { // Grayscale B&W
                    val bwMatrix = ColorMatrix()
                    bwMatrix.setSaturation(0f)
                    contrastMatrix.postConcat(bwMatrix)
                }
                4 -> { // Sepia
                    contrastMatrix.postConcat(ColorMatrix(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
                5 -> { // Invert
                    contrastMatrix.postConcat(ColorMatrix(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            }

            paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
            canvas.drawBitmap(original, 0f, 0f, paint)
            currentBitmap = adjusted
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        enabled = currentBitmap != null && !isSaving,
                        onClick = {
                            val originalBmp = currentBitmap ?: return@IconButton
                            isSaving = true
                            scope.launch {
                                val finalBmp = originalBmp.copy(Bitmap.Config.ARGB_8888, true)
                                val canvas = android.graphics.Canvas(finalBmp)
                                
                                val paint = Paint().apply {
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }

                                strokes.forEach { stroke ->
                                    paint.color = android.graphics.Color.argb(
                                        (stroke.color.alpha * 255).toInt(),
                                        (stroke.color.red * 255).toInt(),
                                        (stroke.color.green * 255).toInt(),
                                        (stroke.color.blue * 255).toInt()
                                    )
                                    paint.strokeWidth = stroke.width
                                    
                                    val path = android.graphics.Path()
                                    if (stroke.points.isNotEmpty()) {
                                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                        for (i in 1 until stroke.points.size) {
                                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                        }
                                        canvas.drawPath(path, paint)
                                    }
                                }

                                // Draw text overlays
                                val textPaint = Paint().apply {
                                    textSize = 48f
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }
                                textOverlays.forEach { overlay ->
                                    textPaint.color = android.graphics.Color.argb(
                                        (overlay.color.alpha * 255).toInt(),
                                        (overlay.color.red * 255).toInt(),
                                        (overlay.color.green * 255).toInt(),
                                        (overlay.color.blue * 255).toInt()
                                    )
                                    canvas.drawText(overlay.text, overlay.position.x, overlay.position.y, textPaint)
                                }

                                val item = imageItem ?: return@launch
                                val ok = viewModel.saveEditedImage(context, item, finalBmp, asCopy = true)
                                isSaving = false
                                if (ok) {
                                    Toast.makeText(context, "Saved as copy successfully", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save Copy")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview & Draw Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                currentBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeMode) {
                                if (activeMode == 2) { // Doodle draw mode
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStrokePoints.clear()
                                            currentStrokePoints.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentStrokePoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            strokes.add(
                                                DoodleStroke(
                                                    points = currentStrokePoints.toList(),
                                                    color = doodleColor,
                                                    width = doodleWidth
                                                )
                                            )
                                            currentStrokePoints.clear()
                                        }
                                    )
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                            )

                            // Render completed strokes
                            strokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    for (i in 0 until stroke.points.size - 1) {
                                        drawLine(
                                            color = stroke.color,
                                            start = stroke.points[i],
                                            end = stroke.points[i + 1],
                                            strokeWidth = stroke.width
                                        )
                                    }
                                }
                            }

                            // Render current drawing stroke
                            if (currentStrokePoints.size > 1) {
                                for (i in 0 until currentStrokePoints.size - 1) {
                                    drawLine(
                                        color = doodleColor,
                                        start = currentStrokePoints[i],
                                        end = currentStrokePoints[i + 1],
                                        strokeWidth = doodleWidth
                                    )
                                }
                            }
                        }

                        // Render text overlays (Compose overlays)
                        textOverlays.forEach { overlay ->
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(
                                        x = (overlay.position.x / 3f).dp,
                                        y = (overlay.position.y / 3f).dp
                                    )
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Text(overlay.text, color = overlay.color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                } ?: CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            // Edit Tools Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeMode) {
                        0 -> { // Adjustments
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Brightness: ${brightness.toInt()}", fontSize = 12.sp)
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -100f..100f
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Contrast: ${"%.1f".format(contrast)}x", fontSize = 12.sp)
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.5f..2.0f
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Saturation: ${"%.1f".format(saturation)}x", fontSize = 12.sp)
                                Slider(
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0.0f..2.0f
                                )
                            }
                        }
                        1 -> { // Filters
                            val filtersList = listOf("None", "Warm", "Cold", "Grayscale", "Sepia", "Invert")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                filtersList.forEachIndexed { index, name ->
                                    FilterChip(
                                        selected = activeFilter == index,
                                        onClick = { activeFilter = index },
                                        label = { Text(name, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                        2 -> { // Draw
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White, Color.Black)
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (doodleColor == color) 3.dp else 1.dp,
                                                color = if (doodleColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { doodleColor = color }
                                    )
                                }
                                IconButton(onClick = { strokes.clear() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Canvas")
                                }
                            }
                        }
                        3 -> { // Text Overlay
                            Button(onClick = { showTextDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Text Overlay")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Modes selection bottom bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = { activeMode = 0 }) {
                            Icon(Icons.Default.Tune, contentDescription = "Adjust", tint = if (activeMode == 0) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { activeMode = 1 }) {
                            Icon(Icons.Default.PhotoFilter, contentDescription = "Filters", tint = if (activeMode == 1) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { activeMode = 2 }) {
                            Icon(Icons.Default.Gesture, contentDescription = "Draw", tint = if (activeMode == 2) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { activeMode = 3 }) {
                            Icon(Icons.Default.TextFields, contentDescription = "Text", tint = if (activeMode == 3) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(
                            onClick = {
                                val bmp = currentBitmap ?: return@IconButton
                                scope.launch {
                                    withContext(Dispatchers.Default) {
                                        val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                                        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                        originalBitmap = rotated
                                        currentBitmap = rotated
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                        }
                        IconButton(
                            onClick = {
                                val bmp = currentBitmap ?: return@IconButton
                                scope.launch {
                                    withContext(Dispatchers.Default) {
                                        val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f) }
                                        val flipped = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                        originalBitmap = flipped
                                        currentBitmap = flipped
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal")
                        }
                    }
                }
            }
        }
    }

    // Text Overlay dialogue picker
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Text Overlay") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTextString,
                        onValueChange = { newTextString = it },
                        placeholder = { Text("Enter text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val textColors = listOf(Color.White, Color.Yellow, Color.Red, Color.Cyan, Color.Green)
                        textColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (newTextColor == color) 3.dp else 1.dp,
                                        color = if (newTextColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { newTextColor = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTextString.isNotBlank()) {
                            textOverlays.add(
                                TextOverlay(
                                    text = newTextString,
                                    color = newTextColor,
                                    position = Offset(300f, 500f)
                                )
                            )
                            newTextString = ""
                            showTextDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
