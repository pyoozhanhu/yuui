package com.vivo.album

import android.app.Application
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            val repository = AlbumRepository(applicationContext)
            val albums = repository.getAllAlbums()

            if (albums.isEmpty()) {
                repository.addCategory("所有照片", "album")
                repository.addCategory("最近删除", "album")
                repository.addCategory("我的漫画", "comic")
                repository.addCategory("我的视频", "video")
            }

            repository.ensureFavoriteCategoryExists()
        }
    }
}
