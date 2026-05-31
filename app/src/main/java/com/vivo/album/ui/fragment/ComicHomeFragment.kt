package com.vivo.album.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivo.album.R
import com.vivo.album.ui.activity.ComicDetailActivity
import com.vivo.album.ui.activity.ComicReadingActivity
import com.vivo.album.ui.adapter.ComicAdapter
import com.vivo.album.ui.viewmodel.ComicHomeViewModel

class ComicHomeFragment : Fragment() {
    private lateinit var viewModel: ComicHomeViewModel
    private lateinit var rvComics: RecyclerView
    private lateinit var adapter: ComicAdapter
    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSelectMode: ImageView
    private lateinit var btnCancelSelect: ImageView
    private lateinit var tvSelectedCount: TextView
    private lateinit var batchBar: View

    private var isSelectMode = false
    private val selectedIds = mutableSetOf<String>()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val persistablePermission = activity?.contentResolver?.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val folderPath = getRealPathFromUri(it)
            if (folderPath != null) {
                showNewComicTitleDialog(folderPath)
            } else {
                Toast.makeText(requireContext(), "无法获取文件夹路径", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comic_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvTitle)
        etSearch = view.findViewById(R.id.etSearch)
        rvComics = view.findViewById(R.id.rvComics)
        btnSelectMode = view.findViewById(R.id.btnSelectMode)
        btnCancelSelect = view.findViewById(R.id.btnCancelSelect)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        batchBar = view.findViewById(R.id.batchBar)

        val toolbar = view.findViewById<View>(R.id.toolbar) as Toolbar
        val btnSearch = view.findViewById<View>(R.id.btnSearch) as ImageView
        val btnNewComic = view.findViewById<View>(R.id.btnNewComic) as ImageView
        val btnBatchDelete = view.findViewById<View>(R.id.btnBatchDelete) as android.widget.Button
        val btnBatchMove = view.findViewById<View>(R.id.btnBatchMove) as android.widget.Button

        viewModel = ViewModelProvider(this)[ComicHomeViewModel::class.java]
        adapter = ComicAdapter(
            onComicClick = { mediaItem ->
                val intent = Intent(requireContext(), ComicDetailActivity::class.java).apply {
                    putExtra("comic_id", mediaItem.id)
                    putExtra("comic_title", mediaItem.title)
                    putExtra("comic_folder", mediaItem.localPath)
                }
                startActivity(intent)
            },
            onSelectionChanged = { comicId, isChecked ->
                if (isChecked) selectedIds.add(comicId)
                else selectedIds.remove(comicId)
                updateSelectModeUI()
            }
        )

        rvComics.layoutManager = GridLayoutManager(requireContext(), 2)
        rvComics.adapter = adapter

        btnSearch.setOnClickListener {
            etSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
            tvTitle.visibility = View.GONE
        }

        btnNewComic.setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        btnSelectMode.setOnClickListener {
            toggleSelectMode()
        }

        btnCancelSelect.setOnClickListener {
            toggleSelectMode()
        }

        btnBatchDelete.setOnClickListener {
            deleteSelected()
        }

        btnBatchMove.setOnClickListener {
            if (selectedIds.isEmpty()) {
                Toast.makeText(requireContext(), "请先选择要移动的漫画", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showMoveToCategoryDialog()
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterCategories(s?.toString() ?: "")
            }
        })

        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && etSearch.text.toString().isBlank()) {
                etSearch.visibility = View.GONE
                tvTitle.visibility = View.VISIBLE
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
        }

        viewModel.loadCategories()
    }

    private fun launchComicReader(folderPath: String, startPosition: Int) {
        // 此方法保留但不再使用，点击漫画文件夹现在打开详情页
    }

    private fun showNewComicTitleDialog(folderPath: String) {
        val editText = EditText(requireContext()).apply {
            hint = "输入漫画名称"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("新建漫画")
            .setMessage("请输入漫画名称")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.addComic(title, folderPath)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(requireContext(), uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("raw:")) {
                return docId.removePrefix("raw:")
            }
        }
        return uri.path
    }

    private fun toggleSelectMode() {
        isSelectMode = !isSelectMode
        if (!isSelectMode) {
            selectedIds.clear()
        }
        adapter.setSelectMode(isSelectMode, selectedIds.toSet())
        updateSelectModeUI()
    }

    private fun updateSelectModeUI() {
        batchBar.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        btnSelectMode.visibility = if (isSelectMode) View.GONE else View.VISIBLE
        btnCancelSelect.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        tvSelectedCount.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        tvSelectedCount.text = "已选择 ${selectedIds.size} 项"
    }

    private fun deleteSelected() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择要删除的漫画", Toast.LENGTH_SHORT).show()
            return
        }

        val categories = viewModel.getAllComicCategories()
        val selectedCategories = selectedIds.mapNotNull { id ->
            categories.find { it.items.firstOrNull()?.id == id }?.categoryType
        }
        
        val isComic = selectedCategories.all { it == "comic" }

        if (isComic) {
            showDeleteConfirmDialog()
        } else {
            showDeleteConfirmDialog()
        }
    }

    private fun showDeleteConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("将 ${selectedIds.size} 本漫画移至回收站？")
            .setPositiveButton("移至回收站") { _, _ ->
                viewModel.moveComicsToTrash(selectedIds.toList())
                selectedIds.clear()
                isSelectMode = false
                updateSelectModeUI()
                viewModel.loadCategories()
                Toast.makeText(requireContext(), "已移至回收站", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMoveToCategoryDialog() {
        viewModel.getAllComicCategories().let { categories ->
            val categoryNames = categories.map { it.name }.toMutableList()
            categoryNames.add(0, "+ 新建分类")

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("移动到分类")
                .setItems(categoryNames.toTypedArray()) { _, which ->
                    if (which == 0) {
                        showNewCategoryDialogForMove()
                    } else {
                        val targetCategory = categories[which - 1]
                        viewModel.moveComicsToCategory(selectedIds.toList(), targetCategory.id)
                        selectedIds.clear()
                        isSelectMode = false
                        updateSelectModeUI()
                        viewModel.loadCategories()
                        Toast.makeText(requireContext(), "已移动", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun showNewCategoryDialogForMove() {
        val editText = EditText(requireContext())
        editText.hint = "请输入分类名称"
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("新建分类")
            .setView(editText)
            .setPositiveButton("创建并移动") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.createComicCategoryAndMove(name, selectedIds.toList())
                    selectedIds.clear()
                    isSelectMode = false
                    updateSelectModeUI()
                    viewModel.loadCategories()
                    Toast.makeText(requireContext(), "已创建分类\"$name\"并移动", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
        }
        return uri.path
    }

    private fun launchComicReader(folderPath: String, startPosition: Int) {
        val intent = Intent(requireContext(), ComicReadingActivity::class.java).apply {
            putExtra("folder_path", folderPath)
            putExtra("start_position", startPosition)
        }
        startActivity(intent)
    }
}
