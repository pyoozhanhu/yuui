package com.vivo.album.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vivo.album.data.Category
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class AlbumHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlbumRepository(application)

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isSelectMode = MutableLiveData<Boolean>(false)
    val isSelectMode: LiveData<Boolean> = _isSelectMode

    private val _selectedIds = MutableLiveData<Set<String>>(emptySet())
    val selectedIds: LiveData<Set<String>> = _selectedIds

    private val _importProgress = MutableLiveData<Int>()
    val importProgress: LiveData<Int> = _importProgress

    fun loadAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            val albums = repository.getAllAlbumsWithMedia()
            _categories.postValue(albums)
        }
    }

    suspend fun getFavoriteCategoryId(): String? {
        return repository.getFavoriteCategoryId()
    }

    fun toggleCategoryExpansion(category: Category, position: Int) {
        if (_isSelectMode.value == true) return

        val list = _categories.value?.toMutableList() ?: return
        val updatedCategory = list[position].copy(isExpanded = !list[position].isExpanded)
        list[position] = updatedCategory
        _categories.value = list

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategoryExpanded(category.id, updatedCategory.isExpanded)
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCategory(name, "album")
            loadAlbums()
        }
    }

    fun toggleSelectMode() {
        val newMode = !(_isSelectMode.value ?: false)
        _isSelectMode.value = newMode
        if (!newMode) {
            _selectedIds.value = emptySet()
        }
    }

    fun cancelSelectMode() {
        _isSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleCategorySelection(category: Category, isChecked: Boolean) {
        val current = _selectedIds.value?.toMutableSet() ?: mutableSetOf()
        val categoryId = "category_${category.id}"

        if (isChecked) {
            current.add(categoryId)
        } else {
            current.remove(categoryId)
        }

        _selectedIds.value = current
    }

    fun toggleItemSelection(mediaItem: MediaItem, categoryId: String, isChecked: Boolean) {
        val current = _selectedIds.value?.toMutableSet() ?: mutableSetOf()
        val itemId = "image_${categoryId}_${mediaItem.id}"

        if (isChecked) {
            current.add(itemId)
        } else {
            current.remove(itemId)
        }

        _selectedIds.value = current
    }

    fun deleteSelected() {
        val selected = _selectedIds.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val categoryIdsToDelete = selected.filter { it.startsWith("category_") }
                .map { it.removePrefix("category_") }

            val imageIdsWithCategory = selected.filter { it.startsWith("image_") }
                .map { 
                    val parts = it.removePrefix("image_").split("_")
                    val categoryId = parts[0]
                    val mediaId = parts.drop(1).joinToString("_")
                    Pair(mediaId, categoryId)
                }

            categoryIdsToDelete.forEach { categoryId ->
                repository.deleteCategoryAndMoveToTrash(categoryId)
            }

            imageIdsWithCategory.forEach { (mediaId, categoryId) ->
                repository.moveMediaToTrash(categoryId, mediaId)
            }

            loadAlbums()
        }

        _isSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        val allIds = mutableSetOf<String>()
        _categories.value?.forEach { category ->
            allIds.add("category_${category.id}")
            category.items.forEach { item ->
                allIds.add("image_${category.id}_${item.id}")
            }
        }
        _selectedIds.value = allIds
    }

    fun getCurrentCategories(): List<Category> {
        return _categories.value ?: emptyList()
    }

    fun moveSelectedImagesToCategory(targetCategoryId: String) {
        val selected = _selectedIds.value ?: return
        val imageIds = selected.filter { it.startsWith("image_") }
            .map { it.removePrefix("image_") }

        if (imageIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.moveImagesToCategory(imageIds, targetCategoryId)
            withContext(Dispatchers.Main) {
                _isSelectMode.value = false
                _selectedIds.value = emptySet()
                loadAlbums()
            }
        }
    }

    fun createCategoryAndMoveSelected(categoryName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCategoryId = repository.addCategory(categoryName, "album")
            val imageIds = _selectedIds.value?.filter { it.startsWith("image_") }
                ?.map { it.removePrefix("image_") } ?: emptyList()
            
            if (imageIds.isNotEmpty()) {
                repository.moveImagesToCategory(imageIds, newCategoryId)
            }
            
            withContext(Dispatchers.Main) {
                _isSelectMode.value = false
                _selectedIds.value = emptySet()
                loadAlbums()
            }
        }
    }

    fun getImagePathsByIds(imageIds: List<String>): List<String> {
        val allItems = _categories.value?.flatMap { it.items } ?: emptyList()
        return allItems.filter { imageIds.contains(it.id) }.map { it.localPath }
    }

    suspend fun restoreSelectedFromTrash(selectedIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedIds.forEach { id ->
                val mediaId = id.removePrefix("image_")
                repository.restoreFromTrash(mediaId)
            }
            withContext(Dispatchers.Main) {
                loadAlbums()
            }
        }
    }

    fun importImagesToCategory(uris: List<android.net.Uri>, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = uris.size
            val imagePaths = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val path = copyImageToAppStorage(uri)
                if (path != null) imagePaths.add(path)
                _importProgress.postValue(((index + 1) * 100) / total)
            }
            if (imagePaths.isNotEmpty()) {
                repository.addImagesToCategory(categoryId, imagePaths)
                withContext(Dispatchers.Main) {
                    loadAlbums()
                    _importProgress.value = null
                    Toast.makeText(getApplication(), "已添加 ${imagePaths.size} 张图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createCategoryAndImportImages(categoryName: String, uris: List<android.net.Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryId = repository.addCategory(categoryName, "album")
            val total = uris.size
            val imagePaths = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val path = copyImageToAppStorage(uri)
                if (path != null) imagePaths.add(path)
                _importProgress.postValue(((index + 1) * 100) / total)
            }
            if (imagePaths.isNotEmpty()) {
                repository.addImagesToCategory(categoryId, imagePaths)
                withContext(Dispatchers.Main) {
                    loadAlbums()
                    _importProgress.value = null
                    Toast.makeText(getApplication(), "已创建分类\"$categoryName\"并添加 ${imagePaths.size} 张图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyImageToAppStorage(uri: android.net.Uri): String? {
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            val destFile = File(
                getApplication<Application>().getExternalFilesDir("images"),
                fileName
            )
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun permanentlyDeleteSelected(selectedIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedIds.forEach { id ->
                val mediaId = id.removePrefix("image_")
                repository.permanentlyDelete(mediaId)
            }
            withContext(Dispatchers.Main) {
                loadAlbums()
            }
        }
    }

    suspend fun getTrashItems(): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            repository.getMediaInTrash()
        }
    }

    suspend fun clearAllTrash() {
        withContext(Dispatchers.IO) {
            repository.clearTrash()
        }
    }
}
