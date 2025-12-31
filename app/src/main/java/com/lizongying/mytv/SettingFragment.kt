package com.lizongying.mytv

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.SettingBinding
import com.lizongying.mytv.M3UParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL



class SettingFragment : DialogFragment() {

    private var _binding: SettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateManager: UpdateManager

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext() // It‘s safe to get context here.
        _binding = SettingBinding.inflate(inflater, container, false)
        binding.versionName.text = "当前版本: v${context.appVersionName}"
        binding.version.text = "https://github.com/wdxqiang/my-tv"

        val mainActivity = requireActivity() as MainActivity
        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
                mainActivity.settingDelayHide()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                mainActivity.settingDelayHide()
            }
        }

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                mainActivity.settingDelayHide()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, isChecked ->
                SP.bootStartup = isChecked
                mainActivity.settingDelayHide()
            }
        }

        updateManager = UpdateManager(context, this, context.appVersionCode)
        binding.checkVersion.setOnClickListener(
            OnClickListenerCheckVersion(
                mainActivity,
                updateManager
            )
        )

        binding.exit.setOnClickListener{
            requireActivity().finishAffinity()
        }

        // Import M3U from URL button click listener
        binding.btnImportM3uUrl.setOnClickListener {
            val m3uUrl = binding.inputM3uUrl.text.toString().trim()
            if (m3uUrl.isNotEmpty()) {
                // Download and parse M3U from URL
                DownloadM3UTask().execute(m3uUrl)
            } else {
                Toast.makeText(context, "请输入有效的M3U URL", Toast.LENGTH_SHORT).show()
            }
        }



        // Initialize custom M3U URL EditText with saved value
        binding.editCustomM3uUrl.setText(SP.customM3UUrl)
        
        // Save custom M3U URL button click listener
        binding.btnSaveCustomM3uUrl.setOnClickListener {
            val customUrl = binding.editCustomM3uUrl.text.toString().trim()
            if (customUrl.isNotEmpty()) {
                SP.customM3UUrl = customUrl
                Toast.makeText(context, "自定义M3U URL已保存", Toast.LENGTH_SHORT).show()
                // Refresh TV channels list
                refreshTVChannels()
                mainActivity.settingDelayHide()
            } else {
                Toast.makeText(context, "请输入有效的M3U URL", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Clear custom M3U URL button click listener
        binding.btnClearCustomM3uUrl.setOnClickListener {
            SP.customM3UUrl = null
            binding.editCustomM3uUrl.setText("")
            Toast.makeText(context, "自定义M3U URL已清除", Toast.LENGTH_SHORT).show()
            // Refresh TV channels list
            refreshTVChannels()
            mainActivity.settingDelayHide()
        }
        
        // Manage custom channels button click listener
        binding.manageCustomChannels.setOnClickListener {
            showCustomChannels()
        }

        return binding.root
    }

    fun setVersionName(versionName: String) {
        if (_binding != null) {
            binding.versionName.text = versionName
        }
    }

    internal class OnClickListenerCheckVersion(
        private val mainActivity: MainActivity,
        private val updateManager: UpdateManager
    ) :
        View.OnClickListener {
        override fun onClick(view: View?) {
            mainActivity.settingDelayHide()
            updateManager.checkAndUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Open file picker to select M3U file
    private fun importM3UFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // Use startActivityForResult instead of selectFileLauncher
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let {
                parseM3UFile(it)
            }
        }
    }

    // Parse M3U from InputStream
    private fun parseM3UFromInputStream(inputStream: InputStream) {
        try {
            // Read the entire content for debugging
            val bytes = inputStream.readBytes()
            val content = String(bytes)
            Log.d("parseM3UFromInputStream", "M3U content: ${content.substring(0, Math.min(1000, content.length))}")
            
            // Parse the content
            val tvList = M3UParser.parseM3U(ByteArrayInputStream(bytes))
            Log.d("parseM3UFromInputStream", "Parsed ${tvList.size} channels")
            if (tvList.isNotEmpty()) {
                // Add parsed TV channels to custom sources
                val customSources = SP.customSources
                customSources.addAll(tvList)
                SP.customSources = customSources
                Toast.makeText(context, "导入成功: ${tvList.size}个频道", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "未找到有效频道", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // 确保错误信息不为null
            val errorMessage = e.message ?: "未知错误"
            Toast.makeText(context, "导入失败: $errorMessage", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Parse selected M3U file
    private fun parseM3UFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.use { inputStream ->
                parseM3UFromInputStream(inputStream)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // AsyncTask to download M3U file from URL
    private inner class DownloadM3UTask : AsyncTask<String, Void, InputStream?>() {
        override fun doInBackground(vararg urls: String): InputStream? {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urls[0])
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000 // 10 seconds timeout
                connection.readTimeout = 10000 // 10 seconds timeout
                
                // Check if the request was successful
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // Get input stream and read entire content
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    val content = String(bytes)
                    
                    Log.d("DownloadM3UTask", "First 500 characters of M3U file: ${content.substring(0, Math.min(500, content.length))}")
                    Log.d("DownloadM3UTask", "Content length: ${content.length} bytes")
                    
                    // Save content to file for debugging
                    try {
                        val file = File(context?.getExternalFilesDir(null), "downloaded_m3u.m3u")
                        file.writeBytes(bytes)
                        Log.d("DownloadM3UTask", "M3U content saved to: ${file.absolutePath}")
                    } catch (e: Exception) {
                        Log.e("DownloadM3UTask", "Error saving M3U file to disk: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // Return a new ByteArrayInputStream with the read content
                    return ByteArrayInputStream(bytes)
                } else {
                    // Log error response
                    Log.e("DownloadM3UTask", "HTTP Error: ${connection.responseCode} - ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                Log.e("DownloadM3UTask", "Error downloading M3U file: ${e.message}")
                e.printStackTrace()
            } finally {
                // Ensure connection is closed
                connection?.disconnect()
            }
            return null
        }

        override fun onPostExecute(inputStream: InputStream?) {
            if (inputStream != null) {
                try {
                    parseM3UFromInputStream(inputStream)
                } catch (e: Exception) {
                    Log.e("DownloadM3UTask", "Error processing M3U file: ${e.message}")
                    Toast.makeText(context, "处理M3U文件失败: ${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                } finally {
                    // Ensure input stream is closed
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(context, "下载M3U文件失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Show custom channels
    private fun showCustomChannels() {
        val customSources = SP.customSources
        if (customSources.isEmpty()) {
            Toast.makeText(context, "没有自定义频道", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a simple dialog to display custom channels
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("自定义频道 (${customSources.size})")
        
        val channelNames = customSources.mapIndexed { index, tv -> "${index + 1}. ${tv.title}" }.toTypedArray()
        
        builder.setItems(channelNames) { dialog, position ->
            // Allow user to delete selected channel
            android.app.AlertDialog.Builder(context)
                .setTitle("删除频道")
                .setMessage("确定要删除 ${customSources[position].title} 吗?")
                .setPositiveButton("删除") { _, _ ->
                    val updatedSources = SP.customSources
                    updatedSources.removeAt(position)
                    SP.customSources = updatedSources
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    // Refresh dialog
                    showCustomChannels()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        
        builder.setNegativeButton("关闭", null)
        builder.show()
    }

    // Refresh TV channels list by clearing the lazy delegate
    private fun refreshTVChannels() {
        // Clear the lazy delegate so it will be reinitialized next time it's accessed
        // This will reload the TV channels with the new M3U URL
        // Note: This is a workaround since we can't directly reset a lazy delegate
        // In a production app, you might want to use a different approach
        TVList::class.java.getDeclaredField("list").apply {
            isAccessible = true
            set(null, null)
        }
    }
    
    companion object {
        const val TAG = "SettingFragment"
    }
}

