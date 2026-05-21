package com.opsec.space.di

import android.content.Context
import androidx.room.Room
import com.opsec.space.data.database.OpsecDatabase
import com.opsec.space.data.database.ImageDao
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
    fun provideOpsecDatabase(
        @ApplicationContext context: Context
    ): OpsecDatabase = Room.databaseBuilder(
        context,
        OpsecDatabase::class.java,
        OpsecDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideImageDao(database: OpsecDatabase): ImageDao = database.imageDao()
}
