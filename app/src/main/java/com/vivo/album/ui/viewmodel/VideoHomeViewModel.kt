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

class VideoHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlbumRepository(application)

    private val _allCategories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _allCategories

    private val _importProgress = MutableLiveData<Int>()
    val importProgress: LiveData<Int> = _importProgress

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val videos = repository.getCategoriesWithMedia("video")
            _allCategories.postValue(videos)
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

    fun importVideosToCategory(uris: List<Uri>, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = uris.size
            val videoPaths = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val path = copyVideoToAppStorage(uri)
                if (path != null) videoPaths.add(path)
                _importProgress.postValue(((index + 1) * 100) / total)
            }
            if (videoPaths.isNotEmpty()) {
                repository.addVideosToCategory(categoryId, videoPaths)
                withContext(Dispatchers.Main) {
                    loadCategories()
                    _importProgress.value = null
                    Toast.makeText(getApplication(), "已添加 ${videoPaths.size} 个视频", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createCategoryAndImportVideos(categoryName: String, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryId = repository.addCategory(categoryName, "video")
            val total = uris.size
            val videoPaths = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val path = copyVideoToAppStorage(uri)
                if (path != null) videoPaths.add(path)
                _importProgress.postValue(((index + 1) * 100) / total)
            }
            if (videoPaths.isNotEmpty()) {
                repository.addVideosToCategory(categoryId, videoPaths)
                withContext(Dispatchers.Main) {
                    loadCategories()
                    _importProgress.value = null
                    Toast.makeText(getApplication(), "已创建分类\"$categoryName\"并添加 ${videoPaths.size} 个视频", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyVideoToAppStorage(uri: Uri): String? {
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            val timestamp = System.currentTimeMillis()
            val fileName = "VID_${timestamp}.mp4"
            val destFile = File(
                getApplication<Application>().getExternalFilesDir("videos"),
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
}
