package com.vivo.album.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.vivo.album.R
import com.vivo.album.data.MediaItem
import com.vivo.album.databinding.ActivityTrashBinding
import com.vivo.album.ui.adapter.TrashAdapter
import com.vivo.album.ui.viewmodel.AlbumHomeViewModel
import kotlinx.coroutines.launch
import android.content.Intent

class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrashBinding
    private lateinit var viewModel: AlbumHomeViewModel
    private lateinit var adapter: TrashAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AlbumHomeViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "最近删除"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = TrashAdapter(
            onItemClick = { mediaItem ->
                when (mediaItem.mediaType) {
                    "comic" -> {
                        val intent = Intent(this, ComicDetailActivity::class.java).apply {
                            putExtra("comic_id", mediaItem.id)
                            putExtra("comic_title", mediaItem.title)
                            putExtra("comic_folder", mediaItem.localPath)
                        }
                        startActivity(intent)
                    }
                    "image" -> {
                        val intent = Intent(this, FullscreenImageActivity::class.java).apply {
                            putStringArrayListExtra("image_paths", ArrayList(listOf(mediaItem.localPath)))
                            putExtra("start_position", 0)
                        }
                        startActivity(intent)
                    }
                    else -> {
                        Toast.makeText(this, "不支持的媒体类型", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onItemSelect = { mediaItem, isChecked ->
                toggleSelection(mediaItem.id, isChecked)
            }
        )

        binding.rvTrash.layoutManager = GridLayoutManager(this, 3)
        binding.rvTrash.adapter = adapter

        binding.btnSelectMode.setOnClickListener {
            toggleSelectMode()
        }

        binding.btnCancelSelect.setOnClickListener {
            adapter.isSelectMode = false
            updateSelectModeUI()
        }

        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmDialog()
        }

        binding.btnRestore.setOnClickListener {
            restoreSelected()
        }

        binding.btnPermanentlyDelete.setOnClickListener {
            permanentlyDeleteSelected()
        }

        loadTrashItems()
    }

    private var selectedIds = mutableSetOf<String>()

    private fun toggleSelection(mediaId: String, isChecked: Boolean) {
        if (isChecked) {
            selectedIds.add(mediaId)
        } else {
            selectedIds.remove(mediaId)
        }
        updateSelectedCount()
    }

    private fun toggleSelectMode() {
        adapter.isSelectMode = !adapter.isSelectMode
        if (!adapter.isSelectMode) {
            selectedIds.clear()
        }
        updateSelectModeUI()
    }

    private fun updateSelectModeUI() {
        binding.btnSelectMode.visibility = if (adapter.isSelectMode) View.GONE else View.VISIBLE
        binding.btnCancelSelect.visibility = if (adapter.isSelectMode) View.VISIBLE else View.GONE
        binding.batchBar.visibility = if (adapter.isSelectMode) View.VISIBLE else View.GONE
        binding.tvSelectedCount.visibility = if (adapter.isSelectMode) View.VISIBLE else View.GONE
        updateSelectedCount()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectedCount() {
        binding.tvSelectedCount.text = "已选择 ${selectedIds.size} 项"
    }

    private fun loadTrashItems() {
        lifecycleScope.launch {
            val items = viewModel.getTrashItems()
            adapter.submitList(items)
            binding.btnClearAll.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun restoreSelected() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请选择要还原的项目", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认还原")
            .setMessage("确定要还原选中的 ${selectedIds.size} 项吗？")
            .setPositiveButton("还原") { _, _ ->
                lifecycleScope.launch {
                    viewModel.restoreSelectedFromTrash(selectedIds)
                    selectedIds.clear()
                    adapter.isSelectMode = false
                    updateSelectModeUI()
                    loadTrashItems()
                    Toast.makeText(this@TrashActivity, "已还原", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun permanentlyDeleteSelected() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请选择要删除的项目", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认彻底删除")
            .setMessage("确定要彻底删除选中的 ${selectedIds.size} 项吗？此操作不可恢复！")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    viewModel.permanentlyDeleteSelected(selectedIds)
                    selectedIds.clear()
                    adapter.isSelectMode = false
                    updateSelectModeUI()
                    loadTrashItems()
                    Toast.makeText(this@TrashActivity, "已彻底删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllConfirmDialog() {
        lifecycleScope.launch {
            val count = viewModel.getTrashItems().size
            if (count == 0) {
                Toast.makeText(this@TrashActivity, "回收站为空", Toast.LENGTH_SHORT).show()
                return@launch
            }

            AlertDialog.Builder(this@TrashActivity)
                .setTitle("清空回收站")
                .setMessage("确定要永久清空回收站中的所有内容吗？\n此操作不可恢复！")
                .setPositiveButton("清空") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.clearAllTrash()
                        loadTrashItems()
                        adapter.isSelectMode = false
                        selectedIds.clear()
                        updateSelectModeUI()
                        Toast.makeText(this@TrashActivity, "已清空回收站", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
