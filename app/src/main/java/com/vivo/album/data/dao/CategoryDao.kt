package com.vivo.album.data.dao

import androidx.room.*
import com.vivo.album.data.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE categoryType = :type ORDER BY name")
    suspend fun getCategoriesByType(type: String): List<Category>

    @Insert
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Query("UPDATE categories SET isExpanded = :isExpanded WHERE id = :categoryId")
    suspend fun updateExpanded(categoryId: String, isExpanded: Boolean)

    @Query("SELECT * FROM categories WHERE name = '我的收藏' AND categoryType = 'album' LIMIT 1")
    suspend fun getFavoriteCategory(): Category?

    @Query("SELECT * FROM categories WHERE name = '我的收藏' AND categoryType = 'album' LIMIT 1")
    suspend fun getFavoriteCategorySync(): Category?
}
