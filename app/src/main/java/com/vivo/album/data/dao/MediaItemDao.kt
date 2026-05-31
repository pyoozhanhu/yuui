package com.vivo.album.data.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.vivo.album.data.MediaItem

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE id IN (SELECT mediaId FROM category_media_join WHERE categoryId = :categoryId)")
    suspend fun getMediaItemsForCategory(categoryId: String): List<MediaItem>

    @Insert
    suspend fun insertMediaItem(item: MediaItem)

    @Insert
    suspend fun insertAll(items: List<MediaItem>)

    @Update
    suspend fun updateMediaItem(item: MediaItem)

    @Delete
    suspend fun deleteMediaItem(item: MediaItem)

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItems(): List<MediaItem>

    @Query("UPDATE media_items SET localPath = :newPath WHERE id = :mediaId")
    suspend fun updateLocalPath(mediaId: String, newPath: String)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItemById(id: String): MediaItem?

    @Query("UPDATE media_items SET originalCategoryId = :categoryId WHERE id = :mediaId")
    suspend fun updateOriginalCategoryId(mediaId: String, categoryId: String)

    @Query("UPDATE media_items SET originalCategoryId = NULL WHERE id = :mediaId")
    suspend fun clearOriginalCategoryId(mediaId: String)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: String)

    @Query("UPDATE media_items SET favorite = :isFav WHERE id = :mediaId")
    suspend fun updateFavorite(mediaId: String, isFav: Boolean)

    @Query("UPDATE media_items SET localPath = :newPath WHERE id = :mediaId")
    suspend fun updatePath(mediaId: String, newPath: String)

    @Query("SELECT * FROM media_items WHERE favorite = 1")
    suspend fun getFavoriteItems(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE id IN (SELECT mediaId FROM category_media_join WHERE categoryId = :categoryId)")
    fun getPagingSourceForCategory(categoryId: String): PagingSource<Int, MediaItem>
}
