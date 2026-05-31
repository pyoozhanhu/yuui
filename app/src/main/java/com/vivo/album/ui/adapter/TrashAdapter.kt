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
import com.vivo.album.R
import com.vivo.album.data.MediaItem
import java.io.File

class TrashAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemSelect: (MediaItem, Boolean) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    var isSelectMode: Boolean = false
    private var items: List<MediaItem> = emptyList()

    fun submitList(list: List<MediaItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thumbnail, parent, false)
        return TrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // 支持 URI 和文件路径两种格式
        val loadObject = if (item.localPath.startsWith("content://")) {
            Uri.parse(item.localPath)
        } else {
            File(item.localPath)
        }

        Glide.with(context)
            .load(loadObject)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.imageView)

        holder.cbSelect.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        holder.imageView.setOnClickListener {
            if (isSelectMode) {
                holder.cbSelect.isChecked = !holder.cbSelect.isChecked
            } else {
                onItemClick(item)
            }
        }

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            onItemSelect(item, isChecked)
        }
    }

    override fun getItemCount() = items.size

    class TrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = (itemView as FrameLayout).findViewById(R.id.ivThumb)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
    }
}
