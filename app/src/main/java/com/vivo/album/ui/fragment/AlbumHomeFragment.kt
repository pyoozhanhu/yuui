package com.vivo.album.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.vivo.album.databinding.FragmentAlbumHomeBinding
import com.vivo.album.ui.activity.FullscreenImageActivity
import com.vivo.album.ui.adapter.CategoryAdapter
import com.vivo.album.ui.viewmodel.AlbumHomeViewModel
import java.io.File

class AlbumHomeFragment : Fragment() {

    private var _binding: FragmentAlbumHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlbumHomeViewModel
    private lateinit var adapter: CategoryAdapter
    private lateinit var pickImagesLauncher: ActivityResultLauncher<Intent>
    private var progressDialog: AlertDialog? = null
    private lateinit var tvEmpty: TextView

    companion object {
        private const val REQUEST_PICK_IMAGES = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AlbumHomeViewModel::class.java]

        adapter = CategoryAdapter(
            onHeaderClick = { category, position ->
                viewModel.toggleCategoryExpansion(category, position)
            },
            onThumbClick = { mediaItem, categoryPosition ->
                val category = adapter.categories[categoryPosition]
                val imagePaths = category.items.map { it.localPath }
                val clickedIndex = category.items.indexOf(mediaItem)

                if (imagePaths.isNotEmpty()) {
                    val intent = Intent(requireContext(), FullscreenImageActivity::class.java).apply {
                        putStringArrayListExtra("image_paths", ArrayList(imagePaths))
                        putExtra("start_position", clickedIndex)
                        putExtra("category_id", category.id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "该分类暂无图片",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onCategorySelect = { category, isChecked ->
                viewModel.toggleCategorySelection(category, isChecked)
            },
            onItemSelect = { mediaItem, categoryId, isChecked ->
                viewModel.toggleItemSelection(mediaItem, categoryId, isChecked)
            }
        )

        tvEmpty = binding.root.findViewById(R.id.tvEmpty)
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
            tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
        }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "该分类暂无图片",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onCategorySelect = { category, isChecked ->
                viewModel.toggleCategorySelection(category, isChecked)
            },
            onItemSelect = { mediaItem, categoryId, isChecked ->
                viewModel.toggleItemSelection(mediaItem, categoryId, isChecked)
            }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.categories = categories
        }

        viewModel.isSelectMode.observe(viewLifecycleOwner) { isSelectMode ->
            adapter.isSelectMode = isSelectMode
            
            binding.btnSelectMode.visibility = if (isSelectMode) View.GONE else View.VISIBLE
            binding.btnCancelSelect.visibility = if (isSelectMode) View.VISIBLE else View.GONE
            binding.tvTitle.visibility = if (isSelectMode) View.GONE else View.VISIBLE
            binding.tvSelectedCount.visibility = if (isSelectMode) View.VISIBLE else View.GONE
            binding.batchBar.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        }

        viewModel.selectedIds.observe(viewLifecycleOwner) { ids ->
            binding.tvSelectedCount.text = "已选择 ${ids.size} 项"
        }

        viewModel.loadAlbums()

        binding.btnNewCategory.setOnClickListener {
            showNewCategoryDialog()
        }

        binding.btnSearch.setOnClickListener {
            binding.etSearch.visibility = View.VISIBLE
            binding.etSearch.requestFocus()
            binding.tvTitle.visibility = View.GONE
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterCategories(s?.toString() ?: "")
            }
        })

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etSearch.text.toString().isBlank()) {
                binding.etSearch.visibility = View.GONE
                binding.tvTitle.visibility = View.VISIBLE
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            binding.etSearch.clearFocus()
            if (binding.etSearch.text.toString().isBlank()) {
                binding.etSearch.visibility = View.GONE
                binding.tvTitle.visibility = View.VISIBLE
            }
            true
        }
        }

        binding.btnTrash.setOnClickListener {
            openTrashActivity()
        }

        binding.btnSelectMode.setOnClickListener {
            viewModel.toggleSelectMode()
        }

        binding.btnCancelSelect.setOnClickListener {
            viewModel.cancelSelectMode()
        }

        binding.btnAdd.setOnClickListener {
            showAddMenu()
        }

        pickImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uris = mutableListOf<Uri>()
                result.data?.let { data ->
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            data.clipData!!.getItemAt(i).uri?.let { uris.add(it) }
                        }
                    } else if (data.data != null) {
                        uris.add(data.data!!)
                    }
                }
                if (uris.isNotEmpty()) {
                    showImportProgress()
                    showCategoryChooserForImages(uris)
                }
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            binding.etSearch.clearFocus()
            true
        }

        binding.btnBatchDelete.setOnClickListener {
            val count = viewModel.selectedIds.value?.size ?: 0
            if (count == 0) {
                Toast.makeText(requireContext(), "请选择要删除的项目", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除选中的 $count 项吗？项目将被移动到\"最近删除\"分类。")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteSelected()
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.btnBatchMove.setOnClickListener {
            showMoveToCategoryDialog()
        }

        binding.btnBatchShare.setOnClickListener {
            shareSelectedImages()
        }
    }

    private fun shareSelectedImages() {
        val selectedIds = viewModel.selectedIds.value ?: emptySet()
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择图片", Toast.LENGTH_SHORT).show()
            return
        }

        val imageIds = selectedIds.filter { it.startsWith("image_") }
            .map { it.removePrefix("image_") }
        
        if (imageIds.isEmpty()) {
            Toast.makeText(requireContext(), "请选择要分享的图片", Toast.LENGTH_SHORT).show()
            return
        }

        val imagePaths = viewModel.getImagePathsByIds(imageIds)
        if (imagePaths.isEmpty()) {
            Toast.makeText(requireContext(), "无法获取图片路径", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = imagePaths.map { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.filterNotNull()

        if (uris.isEmpty()) {
            Toast.makeText(requireContext(), "无法读取图片文件", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "分享到..."))
    }

    private fun showMoveToCategoryDialog() {
        val selectedIds = viewModel.selectedIds.value ?: emptySet()
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "请选择要移动的项目", Toast.LENGTH_SHORT).show()
            return
        }
    
        val allCategories = viewModel.getCurrentCategories().filter { 
            it.categoryType != "trash" && it.name != "最近删除" 
        }
    
        if (allCategories.isEmpty()) {
            Toast.makeText(requireContext(), "没有可用的分类", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryNames = allCategories.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("移动到分类")
            .setItems(categoryNames) { _, which ->
                val targetCategory = allCategories[which]
                
                val selectedImageIds = selectedIds.filter { it.startsWith("image_") }
                val selectedCategoryIds = selectedIds.filter { it.startsWith("category_") }
        
                if (selectedImageIds.isEmpty() && selectedCategoryIds.isEmpty()) {
                    Toast.makeText(requireContext(), "请选择要移动的项目", Toast.LENGTH_SHORT).show()
                    return@setItems
                }

                val moveCategoryIds = if (selectedCategoryIds.isNotEmpty()) {
                    selectedCategoryIds.map { it.removePrefix("category_") }
                } else {
                    selectedImageIds.map { it.removePrefix("image_").split("_")[0] }.distinct()
                }
    
                val canMove = moveCategoryIds.any { it != targetCategory.id }
                if (!canMove) {
                    Toast.makeText(requireContext(), "这些项目已在目标分类中", Toast.LENGTH_SHORT).show()
                    return@setItems
                }
    
                showMoveConfirmDialog(targetCategory.name) {
                    if (selectedImageIds.isNotEmpty()) {
                        val mediaIds = selectedImageIds.map { 
                            it.removePrefix("image_").split("_").drop(1).joinToString("_") 
                        }
                        moveImagesToCategory(mediaIds, targetCategory.id)
                    }
                }
            }
            .setNeutralButton("+ 新建分类") { _, _ ->
                showNewCategoryDialogForMove(selectedIds)
            }
            .show()
    }

    private fun showMoveConfirmDialog(targetCategoryName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认移动")
            .setMessage("确定要将选中的项目移动到\"$targetCategoryName\"吗？")
            .setPositiveButton("移动") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun moveImagesToCategory(mediaIds: List<String>, targetCategoryId: String) {
        viewModel.moveSelectedImagesToCategory(targetCategoryId)
        Toast.makeText(requireContext(), "已移动", Toast.LENGTH_SHORT).show()
    }

    private fun showNewCategoryDialogForMove(selectedIds: Set<String>) {
        val editText = EditText(requireContext())
        editText.hint = "请输入分类名称"

        AlertDialog.Builder(requireContext())
            .setTitle("新建分类")
            .setView(editText)
            .setPositiveButton("创建并移动") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.createCategoryAndMoveSelected(name)
                    Toast.makeText(requireContext(), "已创建分类\"$name\"并移动", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNewCategoryDialog() {
        val editText = EditText(requireContext())
        editText.hint = "请输入分类名称"

        AlertDialog.Builder(requireContext())
            .setTitle("新建分类")
            .setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.createCategory(name)
                } else {
                    Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddMenu() {
        val options = arrayOf("添加图片", "添加漫画", "添加视频")
        AlertDialog.Builder(requireContext())
            .setTitle("添加媒体")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImagesFromGallery()
                    1 -> Toast.makeText(requireContext(), "添加漫画功能暂缓实现", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), "添加视频功能暂缓实现", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun pickImagesFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImagesLauncher.launch(intent)
    }

    private fun showCategoryChooserForImages(uris: List<Uri>) {
        val categories = (viewModel as? AlbumHomeViewModel)?.categories?.value ?: emptyList()
        val categoryNames = categories.filter { it.name != "最近删除" }.map { it.name }.toMutableList()
        categoryNames.add(0, "+ 新建分类")
        
        AlertDialog.Builder(requireContext())
            .setTitle("保存到分类")
            .setItems(categoryNames.toTypedArray()) { _, which ->
                if (which == 0) {
                    showNewCategoryDialogForImport(uris)
                } else {
                    val targetCategory = categories[which - 1]
                    importImagesToCategory(uris, targetCategory.id)
                }
            }
            .show()
    }

    private fun showImportProgress() {
        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = false
            max = 100
        }
        progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("正在导入")
            .setView(progressBar)
            .setCancelable(false)
            .show()
        viewModel.importProgress.observe(viewLifecycleOwner) { progress ->
            if (progress == null) {
                progressDialog?.dismiss()
                progressDialog = null
            } else {
                progressBar.progress = progress
            }
        }
    }

    private fun showNewCategoryDialogForImport(uris: List<Uri>) {
        val editText = EditText(requireContext())
        editText.hint = "请输入分类名称"
        AlertDialog.Builder(requireContext())
            .setTitle("新建分类")
            .setView(editText)
            .setPositiveButton("创建并导入") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    importImagesToNewCategory(uris, name)
                } else {
                    Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun importImagesToNewCategory(uris: List<Uri>, categoryName: String) {
        viewModel.createCategoryAndImportImages(categoryName, uris)
    }

    private fun importImagesToCategory(uris: List<Uri>, categoryId: String) {
        viewModel.importImagesToCategory(uris, categoryId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openTrashActivity() {
        val intent = Intent(requireContext(), TrashActivity::class.java)
        startActivity(intent)
    }
}
