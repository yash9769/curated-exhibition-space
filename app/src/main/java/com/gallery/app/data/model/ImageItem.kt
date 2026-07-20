package com.gallery.app.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1L,      // Associates this image with a specific profile
    val uri: String,               // Stored as string, converted to Uri when needed
    val fileName: String,
    val dateAdded: Long,           // Epoch millis when user added it to gallery
    val dateTaken: Long?,          // Epoch millis from EXIF / MediaStore, nullable
    val fileSize: Long?,           // In bytes, nullable
    val width: Int?,
    val height: Int?,
    val isVideo: Boolean = false,
    val duration: Long? = null,    // Duration in milliseconds, if video
    val isFavorite: Boolean = false,
    val isVaulted: Boolean = false,
    val folderName: String? = "Imported",
    
    // Trash tracking
    val isDeleted: Boolean = false,
    val deletedDate: Long? = null,

    // EXIF metadata
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val iso: Int? = null,
    val aperture: Double? = null,
    val shutterSpeed: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val dateModified: Long? = null
) {
    fun toUri(): Uri = Uri.parse(uri)
}
