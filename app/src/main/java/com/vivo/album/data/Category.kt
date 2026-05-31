package com.vivo.album.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Suppress("UNCHECKED_CAST")
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var name: String,
    var coverPath: String,
    val categoryType: String,
    var isExpanded: Boolean = false
) {
    @Transient
    var items: List<com.vivo.album.data.MediaItem> = emptyList()

    val itemCount: Int get() = items.size
    val itemList: List<Any> get() = items
}

@Entity(tableName = "category_media_join")
data class CategoryMediaJoin(
    val categoryId: String,
    val mediaId: String
)
