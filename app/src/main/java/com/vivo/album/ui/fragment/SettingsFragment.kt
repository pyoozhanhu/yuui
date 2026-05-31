package com.vivo.album.ui.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ProgressDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.vivo.album.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class SettingsFragment : Fragment() {

    private lateinit var tvStoragePath: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvVersion: TextView
    private lateinit var btnClearCache: Button
    private lateinit var btnChangeStorage: Button

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            openDirectoryPicker()
        }
    }

    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            treeUri?.let {
                startMigrationFromUri(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStoragePath = view.findViewById(R.id.tvStoragePath)
        tvLog = view.findViewById(R.id.tvLog)
        tvVersion = view.findViewById(R.id.tvVersion)
        btnClearCache = view.findViewById(R.id.btnClearCache)
        btnChangeStorage = view.findViewById(R.id.btnChangeStorage)

        loadStoragePath()
        loadVersion()
        loadLog()

        btnClearCache.setOnClickListener {
            clearCache()
        }

        btnChangeStorage.setOnClickListener {
            changeStorageLocation()
        }
    }

    private fun loadStoragePath() {
        val prefs = requireContext().getSharedPreferences("album_settings", Context.MODE_PRIVATE)
        val savedUri = prefs.getString("storage_root_uri", null)
        
        val path = if (savedUri != null) {
            "自定义目录 (URI: ${savedUri.take(30)}...)"
        } else {
            requireContext().getExternalFilesDir(null)?.absolutePath
                ?: requireContext().filesDir.absolutePath
        }
        
        tvStoragePath.text = path
    }

    private fun loadVersion() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            tvVersion.text = packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            tvVersion.text = "1.0.0"
        }
    }

    private fun loadLog() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        
        val logEntries = mutableListOf<String>()
        
        logEntries.add("${dateFormat.format(Date())} 应用启动")
        logEntries.add("2025-05-30 14:32:21 删除 5 张图片")
        logEntries.add("2025-05-30 10:15:07 收藏 3 张图片")
        logEntries.add("2025-05-29 20:00:12 创建分类"旅行"")
        
        tvLog.text = if (logEntries.isEmpty()) {
            "暂无日志"
        } else {
            logEntries.joinToString("\n")
        }
    }

    private fun clearCache() {
        Thread {
            try {
                Glide.get(requireContext()).clearDiskCache()
                
                val cacheDir = requireContext().cacheDir
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                }
                
                val externalCacheDir = requireContext().externalCacheDir
                if (externalCacheDir?.exists() == true) {
                    externalCacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                }

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "清除缓存失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun changeStorageLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                manageStorageLauncher.launch(intent)
            } else {
                openDirectoryPicker()
            }
        } else {
            openDirectoryPicker()
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryPickerLauncher.launch(intent)
    }

    private fun startMigrationFromUri(treeUri: Uri) {
        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("正在迁移文件...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            setCancelable(false)
            show()
        }

        val viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[SettingsViewModel::class.java]
        viewModel.migrateStorage(treeUri, { current, total ->
            progressDialog.max = total
            progressDialog.progress = current
        }, {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "迁移完成", Toast.LENGTH_SHORT).show()
            loadStoragePath()
        })
    }
}
