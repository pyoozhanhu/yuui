package com.vivo.album.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localPath: String,
    var title: String,
    var tags: List<String>,
    var favorite: Boolean = false,
    val createdTime: Date = Date(),
    val mediaType: String,
    val duration: Int? = null,
    var originalCategoryId: String? = null
)
