package com.gallery.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.gallery.app.data.database.ImageDao
import com.gallery.app.data.model.ImageItem
import com.gallery.app.utils.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    private val profileManager: ProfileManager,
    @ApplicationContext private val context: Context
) {

    fun getImages(sortOrder: SortOrder, isAscending: Boolean = false): Flow<List<ImageItem>> {
        val baseFlow = profileManager.activeProfileId.flatMapLatest { profileId ->
            when (sortOrder) {
                SortOrder.DATE_ADDED -> imageDao.getAllImagesByDateAdded(profileId)
                SortOrder.DATE_TAKEN -> imageDao.getAllImagesByDateTaken(profileId)
                SortOrder.FILE_NAME -> imageDao.getAllImagesByFileName(profileId)
                SortOrder.FILE_SIZE -> imageDao.getAllImagesByFileSize(profileId)
            }
        }
        return baseFlow.map { list ->
            if (isAscending) {
                when (sortOrder) {
                    SortOrder.DATE_ADDED -> list.sortedBy { it.dateAdded }
                    SortOrder.DATE_TAKEN -> list.sortedBy { it.dateTaken ?: it.dateAdded }
                    SortOrder.FILE_NAME -> list.sortedBy { it.fileName.lowercase() }
                    SortOrder.FILE_SIZE -> list.sortedBy { it.fileSize ?: 0L }
                }
            } else {
                when (sortOrder) {
                    SortOrder.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
                    SortOrder.DATE_TAKEN -> list.sortedByDescending { it.dateTaken ?: it.dateAdded }
                    SortOrder.FILE_NAME -> list.sortedByDescending { it.fileName.lowercase() }
                    SortOrder.FILE_SIZE -> list.sortedByDescending { it.fileSize ?: 0L }
                }
            }
        }
    }

    fun getVaultedImages(): Flow<List<ImageItem>> = profileManager.activeProfileId.flatMapLatest {
        imageDao.getVaultedImages(it)
    }

    fun getDeletedImages(): Flow<List<ImageItem>> = profileManager.activeProfileId.flatMapLatest {
        imageDao.getDeletedImages(it)
    }

    fun getAlbums(): Flow<Map<String, List<ImageItem>>> {
        return getImages(SortOrder.DATE_ADDED).map { images ->
            images.groupBy { it.folderName ?: "Imported" }
        }
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        imageDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateVaulted(id: Long, isVaulted: Boolean) {
        imageDao.updateVaulted(id, isVaulted)
    }

    suspend fun moveToTrash(id: Long) {
        imageDao.updateDeleted(id, true, System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(id: Long) {
        imageDao.updateDeleted(id, false, null)
    }

    suspend fun renameImage(id: Long, newName: String) {
        val item = imageDao.findById(id) ?: return
        val ext = item.fileName.substringAfterLast(".", "")
        val finalName = if (ext.isNotEmpty() && !newName.endsWith(".$ext", ignoreCase = true)) {
            "$newName.$ext"
        } else {
            newName
        }

        try {
            val uri = Uri.parse(item.uri)
            if (uri.scheme == "file") {
                val oldFile = File(uri.path ?: "")
                if (oldFile.exists()) {
                    val newFile = File(oldFile.parentFile, "${System.currentTimeMillis()}_$finalName")
                    if (oldFile.renameTo(newFile)) {
                        val newUriStr = Uri.fromFile(newFile).toString()
                        // Update both fileName and URI since physical file path changed
                        imageDao.deleteImageById(id)
                        imageDao.insertImage(item.copy(id = id, fileName = finalName, uri = newUriStr))
                        return
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to renaming db record only
        }
        imageDao.renameImage(id, finalName)
    }

    suspend fun moveImageToFolder(id: Long, folderName: String) {
        imageDao.updateFolder(id, folderName)
    }

    suspend fun duplicateImage(imageItem: ImageItem) {
        try {
            val uri = Uri.parse(imageItem.uri)
            if (uri.scheme == "file") {
                val oldFile = File(uri.path ?: "")
                if (oldFile.exists()) {
                    val ext = imageItem.fileName.substringAfterLast(".", "")
                    val baseName = imageItem.fileName.substringBeforeLast(".")
                    val copyName = "${baseName}_copy" + (if (ext.isNotEmpty()) ".$ext" else "")
                    val newFile = File(oldFile.parentFile, "${System.currentTimeMillis()}_$copyName")
                    oldFile.inputStream().use { input ->
                        newFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val duplicatedItem = imageItem.copy(
                        id = 0,
                        profileId = profileManager.activeProfileId.first(),
                        uri = Uri.fromFile(newFile).toString(),
                        fileName = copyName,
                        dateAdded = System.currentTimeMillis()
                    )
                    imageDao.insertImage(duplicatedItem)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun searchImages(query: String): Flow<List<ImageItem>> =
        profileManager.activeProfileId.flatMapLatest { imageDao.searchImages(query, it) }

    fun getImageCount(): Flow<Int> = profileManager.activeProfileId.flatMapLatest {
        imageDao.getImageCount(it)
    }

    private fun copyUriToLocal(uri: Uri, fileName: String): Uri? {
        return try {
            val contentResolver = context.contentResolver
            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
            val mediaDir = context.filesDir.resolve("media")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            val destinationFile = mediaDir.resolve(uniqueFileName)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Uri.fromFile(destinationFile)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addImages(uris: List<Uri>) {
        val newImages = uris.mapNotNull { uri ->
            // Skip if already exists
            val existing = imageDao.findByUri(uri.toString())
            if (existing != null) return@mapNotNull null

            // Extract basic info from system picker first
            val tempItem = buildImageItem(uri)

            // Copy to local app private files directory
            val localUri = copyUriToLocal(uri, tempItem.fileName) ?: return@mapNotNull null

            // Read detailed EXIF data from local file
            var exifCamera: String? = null
            var exifLens: String? = null
            var exifIso: Int? = null
            var exifAperture: Double? = null
            var exifShutterSpeed: String? = null
            var exifLat: Double? = null
            var exifLon: Double? = null
            var exifDateTaken: Long? = tempItem.dateTaken
            var exifDateMod: Long? = null

            try {
                val localFile = File(localUri.path ?: "")
                if (localFile.exists()) {
                    exifDateMod = localFile.lastModified()
                    if (!tempItem.isVideo) {
                        val exifInterface = ExifInterface(localFile.absolutePath)
                        exifCamera = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
                            ?: exifInterface.getAttribute(ExifInterface.TAG_MAKE)
                        exifLens = exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL)
                        exifIso = exifInterface.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                            .takeIf { it > 0 }
                            ?: exifInterface.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
                            .takeIf { it > 0 }
                        exifAperture = exifInterface.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                            .takeIf { it > 0.0 }
                        exifShutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)

                        val latLong = FloatArray(2)
                        if (exifInterface.getLatLong(latLong)) {
                            exifLat = latLong[0].toDouble()
                            exifLon = latLong[1].toDouble()
                        }

                        val dateStr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                        if (dateStr != null) {
                            val parsedDate = parseExifDate(dateStr)
                            if (parsedDate != null) {
                                exifDateTaken = parsedDate
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore EXIF errors
            }

            tempItem.copy(
                profileId = profileManager.activeProfileId.first(),
                uri = localUri.toString(),
                cameraModel = exifCamera,
                lensModel = exifLens,
                iso = exifIso,
                aperture = exifAperture,
                shutterSpeed = exifShutterSpeed,
                gpsLatitude = exifLat,
                gpsLongitude = exifLon,
                dateTaken = exifDateTaken,
                dateModified = exifDateMod
            )
        }
        if (newImages.isNotEmpty()) {
            imageDao.insertImages(newImages)
        }
    }

    private fun parseExifDate(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    suspend fun removeImage(imageItem: ImageItem) {
        try {
            val uri = Uri.parse(imageItem.uri)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(imageItem.uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Ignore
        }
        imageDao.deleteImageById(imageItem.id)
    }

    suspend fun emptyTrash() {
        val currentProfileId = profileManager.activeProfileId.first()
        val deletedItems = imageDao.getDeletedImages(currentProfileId).first()
        deletedItems.forEach { removeImage(it) }
    }

    suspend fun cleanupRevokedPermissions() {
        // Automatically purge trash older than 30 days
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            val currentProfileId = profileManager.activeProfileId.first()
            val deletedItems = imageDao.getDeletedImages(currentProfileId).first()
            val oldItems = deletedItems.filter { it.deletedDate != null && it.deletedDate < thirtyDaysAgo }
            oldItems.forEach { removeImage(it) }
        } catch (e: Exception) {
            // Ignore
        }

        val allUris = imageDao.getAllUris()
        val revokedUris = allUris.filter { uriString ->
            !isUriAccessible(uriString)
        }
        if (revokedUris.isNotEmpty()) {
            revokedUris.forEach { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            imageDao.deleteByUris(revokedUris)
        }
    }

    private fun isUriAccessible(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                File(uri.path ?: "").exists()
            } else {
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun buildImageItem(uri: Uri): ImageItem {
        val contentResolver = context.contentResolver
        var fileName = ""
        var fileSize: Long? = null
        var dateTaken: Long? = null
        var width: Int? = null
        var height: Int? = null
        var folderName: String? = null

        val mimeType = contentResolver.getType(uri)
        val isVideo = mimeType?.startsWith("video/") == true
        var duration: Long? = null

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: ""
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        if (isVideo) {
            try {
                val projection = arrayOf(
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DATE_TAKEN,
                    "bucket_display_name"
                )
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                        val widthIdx = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                        val heightIdx = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                        val dateTakenIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                        val bucketIdx = cursor.getColumnIndex("bucket_display_name")
                        if (durationIdx != -1 && !cursor.isNull(durationIdx)) {
                            duration = cursor.getLong(durationIdx)
                        }
                        if (widthIdx != -1 && !cursor.isNull(widthIdx)) {
                            width = cursor.getInt(widthIdx)
                        }
                        if (heightIdx != -1 && !cursor.isNull(heightIdx)) {
                            height = cursor.getInt(heightIdx)
                        }
                        if (dateTakenIdx != -1 && !cursor.isNull(dateTakenIdx)) {
                            dateTaken = cursor.getLong(dateTakenIdx)
                        }
                        if (bucketIdx != -1 && !cursor.isNull(bucketIdx)) {
                            folderName = cursor.getString(bucketIdx)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    "bucket_display_name"
                )
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                        val widthIndex = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                        val heightIndex = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                        val bucketIdx = cursor.getColumnIndex("bucket_display_name")
                        if (dateTakenIndex != -1 && !cursor.isNull(dateTakenIndex)) {
                            dateTaken = cursor.getLong(dateTakenIndex)
                        }
                        if (widthIndex != -1 && !cursor.isNull(widthIndex)) {
                            width = cursor.getInt(widthIndex)
                        }
                        if (heightIndex != -1 && !cursor.isNull(heightIndex)) {
                            height = cursor.getInt(heightIndex)
                        }
                        if (bucketIdx != -1 && !cursor.isNull(bucketIdx)) {
                            folderName = cursor.getString(bucketIdx)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (fileName.isEmpty()) {
            fileName = uri.lastPathSegment ?: "Unknown"
        }

        if (folderName.isNullOrBlank() || folderName == "media" || folderName.contains("photopicker")) {
            val pathSegments = uri.pathSegments
            folderName = if (pathSegments != null && pathSegments.size > 1) {
                val segment = pathSegments[pathSegments.size - 2]
                if (segment != "media" && !segment.contains("photopicker")) segment else "Imported"
            } else {
                "Imported"
            }
        }

        return ImageItem(
            profileId = 1L, // Replaced below in addImages
            uri = uri.toString(),
            fileName = fileName,
            dateAdded = System.currentTimeMillis(),
            dateTaken = dateTaken,
            fileSize = fileSize,
            width = width,
            height = height,
            isVideo = isVideo,
            duration = duration,
            isFavorite = false,
            isVaulted = false,
            folderName = folderName
        )
    }
}
