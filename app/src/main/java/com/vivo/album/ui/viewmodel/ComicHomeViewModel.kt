package com.vivo.album.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vivo.album.data.Category
import com.vivo.album.data.MediaItem
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ComicHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlbumRepository(application)

    private val _allCategories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _allCategories

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val comics = repository.getCategoriesWithMedia("comic")
            _allCategories.postValue(comics)
        }
    }

    fun filterCategories(query: String) {
        val all = _allCategories.value ?: return
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter { it.name.contains(query, ignoreCase = true) }
        }
        _allCategories.postValue(filtered)
    }

    fun addComic(title: String, folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAllCategories()
                .filter { it.categoryType == "comic" }
                .find { it.name == title }
            
            if (existing != null) {
                _allCategories.postValue(_allCategories.value)
                return@launch
            }

            val categoryId = repository.addCategory(title, "comic")
            
            val comicId = UUID.randomUUID().toString()
            val comic = MediaItem(
                id = comicId,
                localPath = folderPath,
                title = title,
                tags = emptyList(),
                favorite = false,
                mediaType = "comic"
            )
            repository.addMediaItem(comic)
            repository.addMediaToCategory(comicId, categoryId)

            loadCategories()
        }
    }

    fun deleteComics(comicIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComics(comicIds)
        }
    }

    fun getAllComicCategories(): List<Category> {
        return repository.getAllCategories().filter { it.categoryType == "comic" }
    }

    fun moveComicsToCategory(comicIds: List<String>, targetCategoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveComicsToCategory(comicIds, targetCategoryId)
        }
    }

    fun createComicCategoryAndMove(categoryName: String, comicIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCategoryId = repository.addCategory(categoryName, "comic")
            repository.moveComicsToCategory(comicIds, newCategoryId)
        }
    }

    suspend fun getAllComicCategories(): List<Category> {
        return repository.getAllCategories().filter { it.categoryType == "comic" }
    }

    fun moveComicsToTrash(comicIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveComicsToTrash(comicIds)
        }
    }
}
