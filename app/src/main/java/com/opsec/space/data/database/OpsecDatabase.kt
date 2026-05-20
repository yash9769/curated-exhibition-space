package com.opsec.space.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.opsec.space.data.model.ImageItem

@Database(
    entities = [ImageItem::class],
    version = 5,
    exportSchema = false
)
abstract class OpsecDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        const val DATABASE_NAME = "opsec_database"
    }
}
