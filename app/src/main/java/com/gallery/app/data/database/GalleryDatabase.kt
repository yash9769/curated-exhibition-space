package com.gallery.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gallery.app.data.model.ImageItem

@Database(
    entities = [ImageItem::class],
    version = 5,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        const val DATABASE_NAME = "gallery_database"
    }
}
