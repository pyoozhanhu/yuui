package com.vivo.album.data.dao

import androidx.room.*
import com.vivo.album.data.CategoryMediaJoin

@Dao
interface JoinDao {
    @Insert
    suspend fun insert(join: CategoryMediaJoin)

    @Insert
    suspend fun insertAll(joins: List<CategoryMediaJoin>)

    @Delete
    suspend fun delete(join: CategoryMediaJoin)

    @Query("DELETE FROM category_media_join WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: String)

    @Query("DELETE FROM category_media_join WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)

    @Query("SELECT * FROM category_media_join WHERE categoryId = :categoryId")
    suspend fun getJoinsByCategoryId(categoryId: String): List<CategoryMediaJoin>

    @Query("SELECT * FROM category_media_join WHERE mediaId = :mediaId")
    suspend fun getJoinsByMediaId(mediaId: String): List<CategoryMediaJoin>

    @Query("UPDATE category_media_join SET categoryId = :newCategoryId WHERE mediaId = :mediaId")
    suspend fun updateCategoryForMedia(mediaId: String, newCategoryId: String)

    @Query("DELETE FROM category_media_join WHERE mediaId = :mediaId AND categoryId = :categoryId")
    suspend fun deleteByMediaAndCategory(mediaId: String, categoryId: String)

    @Query("SELECT categoryId FROM category_media_join WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getCategoryIdForMedia(mediaId: String): String?

    @Query("SELECT COUNT(*) FROM category_media_join WHERE mediaId = :mediaId AND categoryId = :categoryId")
    suspend fun exists(mediaId: String, categoryId: String): Int

    @Query("SELECT COUNT(*) FROM category_media_join WHERE mediaId = :mediaId")
    suspend fun getCategoryCountForMedia(mediaId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWithIgnore(join: CategoryMediaJoin): Long
}
