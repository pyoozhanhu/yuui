package com.vivo.album.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.vivo.album.data.Category
import com.vivo.album.data.CategoryMediaJoin
import com.vivo.album.data.MediaItem
import com.vivo.album.data.AppDatabase
import java.io.File

class AlbumRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)

    suspend fun getAllAlbums(): List<Category> {
        return db.categoryDao().getCategoriesByType("album")
    }

    suspend fun getAllCategories(): List<Category> {
        return db.categoryDao().getAllCategories()
    }

    suspend fun getMediaForCategory(categoryId: String): List<MediaItem> {
        return db.mediaItemDao().getMediaItemsForCategory(categoryId)
    }

    suspend fun addCategory(name: String, type: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val newCategory = Category(
            id = id,
            name = name,
            coverPath = "",
            categoryType = type
        )
        db.categoryDao().insertCategory(newCategory)
        return id
    }

    suspend fun deleteCategory(category: Category) {
        db.joinDao().deleteByCategoryId(category.id)
        db.categoryDao().deleteCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        db.categoryDao().updateCategory(category)
    }

    suspend fun addMediaToCategory(mediaId: String, categoryId: String) {
        val join = CategoryMediaJoin(categoryId = categoryId, mediaId = mediaId)
        db.joinDao().insert(join)
    }

    suspend fun removeMediaFromCategory(mediaId: String, categoryId: String) {
        val join = CategoryMediaJoin(categoryId = categoryId, mediaId = mediaId)
        db.joinDao().delete(join)
    }

    suspend fun addMediaItem(item: MediaItem) {
        db.mediaItemDao().insertMediaItem(item)
    }

    suspend fun updateMediaItem(item: MediaItem) {
        db.mediaItemDao().updateMediaItem(item)
    }

    suspend fun deleteMediaItem(item: MediaItem) {
        db.joinDao().deleteByMediaId(item.id)
        db.mediaItemDao().deleteMediaItem(item)
    }

    suspend fun getAllMediaItems(): List<MediaItem> {
        return db.mediaItemDao().getAllMediaItems()
    }

    suspend fun getAllMedia(): List<MediaItem> {
        return db.mediaItemDao().getAllMediaItems()
    }

    suspend fun updateMediaPath(mediaId: String, newPath: String) {
        db.mediaItemDao().updateLocalPath(mediaId, newPath)
    }

    suspend fun getMediaItemById(id: String): MediaItem? {
        return db.mediaItemDao().getMediaItemById(id)
    }

    suspend fun updateCategoryExpanded(categoryId: String, isExpanded: Boolean) {
        db.categoryDao().updateExpanded(categoryId, isExpanded)
    }

    suspend fun getAllAlbumsWithMedia(): List<Category> {
        val albums = db.categoryDao().getCategoriesByType("album")
        return albums.map { album ->
            val items = db.mediaItemDao().getMediaItemsForCategory(album.id)
            album.copy().apply { items = items }
        }
    }

    suspend fun getCategoriesWithMedia(categoryType: String): List<Category> {
        val categories = db.categoryDao().getCategoriesByType(categoryType)
        return categories.map { category ->
            val items = db.mediaItemDao().getMediaItemsForCategory(category.id)
            val categoryWithItems = category.copy().apply { items = items }
            
            if (categoryType == "comic" && items.isNotEmpty()) {
                val folderPath = items.first().localPath
                val folder = java.io.File(folderPath)
                if (folder.exists() && folder.isDirectory) {
                    val firstImage = folder.listFiles { file ->
                        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                    }?.sortedBy { it.name }?.firstOrNull()
                    if (firstImage != null) {
                        categoryWithItems.coverPath = firstImage.absolutePath
                    }
                }
            }
            
            categoryWithItems
        }
    }

    suspend fun getOrCreateTrashCategory(): Category {
        val trash = db.categoryDao().getCategoriesByType("trash").firstOrNull()
        if (trash != null) return trash

        val newTrash = Category(
            name = "最近删除",
            coverPath = "",
            categoryType = "trash"
        )
        db.categoryDao().insertCategory(newTrash)
        return db.categoryDao().getCategoriesByType("trash").first()
    }

    suspend fun moveMediaToTrash(fromCategoryId: String, mediaId: String) {
        val mediaItem = db.mediaItemDao().getMediaItemById(mediaId) ?: return
        val trashCategory = getOrCreateTrashCategory()

        db.mediaItemDao().updateOriginalCategoryId(mediaId, fromCategoryId)
        db.joinDao().deleteByMediaId(mediaId)

        val join = CategoryMediaJoin(
            categoryId = trashCategory.id,
            mediaId = mediaId
        )
        db.joinDao().insert(join)
    }

    suspend fun moveMediaToTrash(mediaId: String) {
        val currentCategoryId = db.joinDao().getCategoryIdForMedia(mediaId) ?: return
        moveMediaToTrash(currentCategoryId, mediaId)
    }

    suspend fun restoreFromTrash(mediaId: String) {
        val media = db.mediaItemDao().getMediaItemById(mediaId) ?: return
        val originalCategoryId = media.originalCategoryId ?: return
        val trashCategory = getOrCreateTrashCategory()

        db.joinDao().deleteByMediaAndCategory(mediaId, trashCategory.id)
        db.joinDao().insert(CategoryMediaJoin(originalCategoryId, mediaId))
        db.mediaItemDao().clearOriginalCategoryId(mediaId)
    }

    suspend fun permanentlyDelete(mediaId: String) {
        val media = db.mediaItemDao().getMediaItemById(mediaId) ?: return
        
        db.joinDao().deleteByMediaId(mediaId)
        db.mediaItemDao().deleteMediaItemById(mediaId)
        
        val file = java.io.File(media.localPath)
        if (file.exists()) {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }

    suspend fun moveComicsToTrash(comicIds: List<String>) {
        val trashCategory = getOrCreateTrashCategory()
        comicIds.forEach { comicId ->
            val currentCategoryId = db.joinDao().getCategoryIdForMedia(comicId)
            if (currentCategoryId != null) {
                db.mediaItemDao().updateOriginalCategoryId(comicId, currentCategoryId)
                db.joinDao().deleteByMediaId(comicId)
                db.joinDao().insert(CategoryMediaJoin(trashCategory.id, comicId))
            }
        }
    }

    suspend fun updateImagePath(mediaId: String, newPath: String) {
        db.mediaItemDao().updatePath(mediaId, newPath)
    }
}
        }
    }

    suspend fun getTrashCategory(): Category? {
        return db.categoryDao().getCategoriesByType("trash").firstOrNull()
    }

    suspend fun getMediaInTrash(): List<MediaItem> {
        val trash = getTrashCategory() ?: return emptyList()
        return db.mediaItemDao().getMediaItemsForCategory(trash.id)
    }

    suspend fun deleteCategoryAndMoveToTrash(categoryId: String) {
        val mediaItems = db.mediaItemDao().getMediaItemsForCategory(categoryId)
        
        mediaItems.forEach { item ->
            moveMediaToTrash(categoryId, item.id)
        }

        db.joinDao().deleteByCategoryId(categoryId)
        val category = db.categoryDao().getAllCategories().find { it.id == categoryId } ?: return
        db.categoryDao().deleteCategory(category)
    }

    suspend fun moveImagesToCategory(mediaIds: List<String>, targetCategoryId: String) {
        mediaIds.forEach { mediaId ->
            db.joinDao().updateCategoryForMedia(mediaId, targetCategoryId)
        }
    }

    suspend fun ensureFavoriteCategoryExists() {
        val fav = db.categoryDao().getFavoriteCategory()
        if (fav == null) {
            val id = java.util.UUID.randomUUID().toString()
            db.categoryDao().insertCategory(
                Category(id = id, name = "我的收藏", coverPath = "", categoryType = "album")
            )
        }
    }

    suspend fun getFavoriteCategoryId(): String? {
        return db.categoryDao().getFavoriteCategorySync()?.id
    }

    suspend fun addToFavorites(mediaId: String) {
        val favId = getFavoriteCategoryId() ?: return
        db.joinDao().insertWithIgnore(CategoryMediaJoin(favId, mediaId))
        db.mediaItemDao().updateFavorite(mediaId, true)
    }

    suspend fun removeFromFavorites(mediaId: String) {
        val favId = getFavoriteCategoryId() ?: return
        db.joinDao().deleteByMediaAndCategory(mediaId, favId)
        val count = db.joinDao().getCategoryCountForMedia(mediaId)
        if (count == 0) {
            db.mediaItemDao().updateFavorite(mediaId, false)
        }
    }

    suspend fun isFavorite(mediaId: String, categoryId: String?): Boolean {
        if (categoryId == null) return false
        return db.joinDao().exists(mediaId, categoryId) > 0
    }

    suspend fun getFavoriteItems(): List<MediaItem> {
        return db.mediaItemDao().getFavoriteItems()
    }

    suspend fun addImagesToCategory(categoryId: String, paths: List<String>) {
        val mediaItems = paths.mapIndexed { index, path ->
            MediaItem(
                id = java.util.UUID.randomUUID().toString(),
                localPath = path,
                title = "IMG_${System.currentTimeMillis() + index}",
                tags = emptyList(),
                favorite = false,
                mediaType = "image"
            )
        }
        db.mediaItemDao().insertAll(mediaItems)
        val joins = mediaItems.map { CategoryMediaJoin(categoryId, it.id) }
        db.joinDao().insertAll(joins)
    }

    suspend fun addVideosToCategory(categoryId: String, paths: List<String>) {
        val mediaItems = paths.mapIndexed { index, path ->
            MediaItem(
                id = java.util.UUID.randomUUID().toString(),
                localPath = path,
                title = "VID_${System.currentTimeMillis() + index}",
                tags = emptyList(),
                favorite = false,
                mediaType = "video",
                duration = getVideoDuration(path)
            )
        }
        db.mediaItemDao().insertAll(mediaItems)
        val joins = mediaItems.map { CategoryMediaJoin(categoryId, it.id) }
        db.joinDao().insertAll(joins)
    }

    private fun getVideoDuration(path: String): Int? {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
            retriever.release()
            duration
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteComics(comicIds: List<String>) {
        comicIds.forEach { comicId ->
            val comic = db.mediaItemDao().getMediaItemById(comicId) ?: return@forEach
            val folderPath = comic.localPath
            
            val mediaItemId = comicId
            
            db.joinDao().deleteByMediaId(mediaItemId)
            
            db.mediaItemDao().deleteMediaItemById(mediaItemId)
            
            try {
                val folder = java.io.File(folderPath)
                if (folder.exists() && folder.isDirectory) {
                    folder.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearTrash() {
        val trashCategory = getOrCreateTrashCategory()
        val trashItems = db.mediaItemDao().getMediaItemsForCategory(trashCategory.id)
        
        trashItems.forEach { media ->
            val file = File(media.localPath)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            db.mediaItemDao().deleteMediaItemById(media.id)
        }
        
        db.joinDao().deleteByCategoryId(trashCategory.id)
    }

    suspend fun moveComicsToCategory(comicIds: List<String>, targetCategoryId: String) {
        comicIds.forEach { comicId ->
            val currentCategoryId = db.joinDao().getCategoryIdForMedia(comicId)
            if (currentCategoryId != null) {
                db.joinDao().deleteByMediaAndCategory(comicId, currentCategoryId)
                db.joinDao().insert(CategoryMediaJoin(targetCategoryId, comicId))
            }
        }
    }

    fun getMediaPagingSource(categoryId: String): PagingSource<Int, MediaItem> {
        return db.mediaItemDao().getPagingSourceForCategory(categoryId)
    }
}
