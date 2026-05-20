package com.opsec.space.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opsec.space.data.model.ImageItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<ImageItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(image: ImageItem)

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllImagesByDateAdded(profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0 ORDER BY COALESCE(dateTaken, dateAdded) DESC")
    fun getAllImagesByDateTaken(profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0 ORDER BY fileName ASC")
    fun getAllImagesByFileName(profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0 ORDER BY COALESCE(fileSize, 0) DESC")
    fun getAllImagesByFileSize(profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0 AND (fileName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY dateAdded DESC")
    fun searchImages(query: String, profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isVaulted = 1 AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getVaultedImages(profileId: Long): Flow<List<ImageItem>>

    @Query("SELECT * FROM images WHERE profileId = :profileId AND isDeleted = 1 ORDER BY deletedDate DESC")
    fun getDeletedImages(profileId: Long): Flow<List<ImageItem>>

    @Query("UPDATE images SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE images SET isVaulted = :isVaulted WHERE id = :id")
    suspend fun updateVaulted(id: Long, isVaulted: Boolean)

    @Query("UPDATE images SET isDeleted = :isDeleted, deletedDate = :deletedDate WHERE id = :id")
    suspend fun updateDeleted(id: Long, isDeleted: Boolean, deletedDate: Long?)

    @Query("UPDATE images SET fileName = :newName WHERE id = :id")
    suspend fun renameImage(id: Long, newName: String)

    @Query("UPDATE images SET folderName = :newFolder WHERE id = :id")
    suspend fun updateFolder(id: Long, newFolder: String)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM images WHERE uri = :uri")
    suspend fun deleteImageByUri(uri: String)

    @Query("SELECT * FROM images WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): ImageItem?

    @Query("SELECT * FROM images WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ImageItem?

    @Query("SELECT uri FROM images")
    suspend fun getAllUris(): List<String>

    @Query("DELETE FROM images WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("SELECT COUNT(*) FROM images WHERE profileId = :profileId AND isVaulted = 0 AND isDeleted = 0")
    fun getImageCount(profileId: Long): Flow<Int>
}
