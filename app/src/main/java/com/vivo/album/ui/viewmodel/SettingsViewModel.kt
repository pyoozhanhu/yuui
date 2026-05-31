package com.vivo.album.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlbumRepository(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("album_settings", Context.MODE_PRIVATE)

    fun getStorageRootPath(): String {
        val savedUri = prefs.getString("storage_root_uri", null)
        return savedUri ?: application.getExternalFilesDir(null)?.absolutePath
            ?: application.filesDir.absolutePath
    }

    fun migrateStorage(
        treeUri: Uri,
        onProgress: (Int, Int) -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDir = DocumentFile.fromTreeUri(getApplication(), treeUri)
                ?: run {
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                    return@launch
                }

            val allMedia = repository.getAllMedia()
            val total = allMedia.size
            var migrated = 0

            allMedia.forEach { media ->
                val oldFile = File(media.localPath)
                if (oldFile.exists()) {
                    val relativePath = getRelativePath(oldFile)
                    val newFileDocument = createNewFileInDocumentTree(rootDir, relativePath, oldFile)
                    
                    if (newFileDocument != null) {
                        copyFileContents(oldFile, newFileDocument)
                        
                        val newUri = newFileDocument.uri.toString()
                        repository.updateMediaPath(media.id, newUri)
                        
                        oldFile.delete()
                    }
                }
                migrated++
                withContext(Dispatchers.Main) {
                    onProgress(migrated, total)
                }
            }

            saveNewStorageRootUri(treeUri.toString())
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun getRelativePath(file: File): String {
        val oldRoot = getApplication<Application>().getExternalFilesDir(null)?.absolutePath
            ?: return file.name
        return file.absolutePath.substringAfter(oldRoot).trimStart('/')
    }

    private fun createNewFileInDocumentTree(
        root: DocumentFile,
        relativePath: String,
        oldFile: File
    ): DocumentFile? {
        val parts = relativePath.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null

        var current = root
        
        for (i in 0 until parts.size - 1) {
            val subDir = current.findFile(parts[i])
            current = subDir ?: current.createDirectory(parts[i])
                ?: return null
        }

        val fileName = parts.last()
        val mimeType = getMimeType(oldFile.extension)
        return current.createFile(mimeType, fileName)
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/webm"
            else -> "*/*"
        }
    }

    private fun copyFileContents(source: File, target: DocumentFile) {
        getApplication<Application>().contentResolver.openOutputStream(target.uri)?.use { output ->
            source.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun saveNewStorageRootUri(uriString: String) {
        prefs.edit().putString("storage_root_uri", uriString).apply()
    }
}
