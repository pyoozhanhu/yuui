package com.vivo.album.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.vivo.album.R
import com.vivo.album.data.MediaItem
import java.io.File

class ThumbnailAdapter(
    private val layoutType: LayoutType = LayoutType.GRID,
    private val isSelectMode: () -> Boolean = { false },
    private val selectedIds: () -> Set<String> = { emptySet() },
    private val categoryId: String = "",
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemSelect: (MediaItem, Boolean) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbViewHolder>() {

    enum class LayoutType {
        GRID,
        HORIZONTAL
    }

    private var items: List<MediaItem> = emptyList()

    fun submitList(list: List<MediaItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val layoutRes = when (layoutType) {
            LayoutType.HORIZONTAL -> R.layout.item_thumbnail_horizontal
            LayoutType.GRID -> R.layout.item_thumbnail
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return ThumbViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        val item = items[position]
        val itemId = "image_${categoryId}_${item.id}"
        val context = holder.itemView.context

        holder.imageView.setOnClickListener {
            if (isSelectMode()) {
                holder.cbSelect.isChecked = !holder.cbSelect.isChecked
            } else {
                onItemClick(item)
            }
        }

        holder.cbSelect.visibility = if (isSelectMode()) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedIds().contains(itemId)
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            onItemSelect(item, isChecked)
        }

        // 支持 URI 和文件路径两种格式
        val loadObject = if (item.localPath.startsWith("content://")) {
            Uri.parse(item.localPath)
        } else {
            File(item.localPath)
        }

        Glide.with(context)
            .load(loadObject)
            .placeholder(R.drawable.ic_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.imageView)
    }

    override fun getItemCount() = items.size

    inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = if (layoutType == LayoutType.HORIZONTAL) {
            (itemView as FrameLayout).findViewById(R.id.ivThumb)
        } else {
            (itemView as FrameLayout).findViewById(R.id.ivThumb)
        }
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
    }
}
