package com.gallery.app.di

import android.content.Context
import androidx.room.Room
import com.gallery.app.data.database.GalleryDatabase
import com.gallery.app.data.database.ImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase = Room.databaseBuilder(
        context,
        GalleryDatabase::class.java,
        GalleryDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideImageDao(database: GalleryDatabase): ImageDao = database.imageDao()
}
