package com.vivo.album.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivo.album.R
import com.vivo.album.ui.activity.FullscreenVideoActivity
import com.vivo.album.ui.adapter.VideoAdapter
import com.vivo.album.ui.viewmodel.VideoHomeViewModel

class VideoHomeFragment : Fragment() {
    private lateinit var viewModel: VideoHomeViewModel
    private lateinit var rvVideos: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnAddVideo: ImageView
    private var progressDialog: AlertDialog? = null
    private lateinit var tvEmpty: TextView

    private val videoPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
                showCategoryChooserForVideos(uris)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvTitle)
        etSearch = view.findViewById(R.id.etSearch)
        rvVideos = view.findViewById(R.id.rvVideos)

        val toolbar = view.findViewById<View>(R.id.toolbar) as Toolbar
        val btnSearch = view.findViewById<View>(R.id.btnSearch) as ImageView
        btnAddVideo = view.findViewById<View>(R.id.btnAddVideo) as ImageView

        viewModel = ViewModelProvider(this)[VideoHomeViewModel::class.java]
        adapter = VideoAdapter(
            onVideoClick = { categoryId, videos, position ->
                launchFullscreenPlayer(videos, position)
            }
        )

        rvVideos.layoutManager = GridLayoutManager(requireContext(), 1)
        rvVideos.adapter = adapter
        tvEmpty = view.findViewById(R.id.tvEmpty)

        btnAddVideo.setOnClickListener {
            pickVideosFromGallery()
        }

        btnSearch.setOnClickListener {
            etSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
            tvTitle.visibility = View.GONE
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
            btnAddVideo.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
            tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadCategories()
    }

    private fun pickVideosFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        videoPickerLauncher.launch(intent)
    }

    private fun showCategoryChooserForVideos(uris: List<Uri>) {
        val categories = viewModel.categories.value ?: return
        val categoryNames = categories.map { it.name }.toMutableList()
        categoryNames.add(0, "+ 新建分类")
        
        AlertDialog.Builder(requireContext())
            .setTitle("保存到分类")
            .setItems(categoryNames.toTypedArray()) { _, which ->
                if (which == 0) {
                    showNewCategoryDialogForVideos(uris)
                } else {
                    val targetCategory = categories[which - 1]
                    viewModel.importVideosToCategory(uris, targetCategory.id)
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

    private fun showNewCategoryDialogForVideos(uris: List<Uri>) {
        val editText = EditText(requireContext())
        editText.hint = "分类名称"
        
        AlertDialog.Builder(requireContext())
            .setTitle("新建分类")
            .setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createCategoryAndImportVideos(name, uris)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun launchFullscreenPlayer(videos: List<String>, startPosition: Int) {
        val intent = Intent(requireContext(), FullscreenVideoActivity::class.java).apply {
            putStringArrayListExtra("video_paths", ArrayList(videos))
            putExtra("start_position", startPosition)
        }
        startActivity(intent)
    }
}
