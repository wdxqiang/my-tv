package com.lizongying.mytv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.SettingBinding
import com.lizongying.mytv.M3UParser


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
        binding.version.text = "https://github.com/lizongying/my-tv"

        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, isChecked ->
                SP.bootStartup = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        updateManager = UpdateManager(context, this, context.appVersionCode)
        binding.checkVersion.setOnClickListener(
            OnClickListenerCheckVersion(
                activity as MainActivity,
                updateManager
            )
        )

        binding.exit.setOnClickListener{
            requireActivity().finishAffinity()
        }

        // Import M3U button click listener
        binding.importM3u.setOnClickListener {
            importM3UFile()
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

    // Register for file selection result
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let {\ uri ->
                parseM3UFile(uri)
            }
        }
    }

    // Open file picker to select M3U file
    private fun importM3UFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        selectFileLauncher.launch(intent)
    }

    // Parse selected M3U file
    private fun parseM3UFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.let {\ stream ->
                val tvList = M3UParser.parseM3U(stream)
                if (tvList.isNotEmpty()) {
                    // Add parsed TV channels to custom sources
                    val customSources = SP.customSources
                    customSources.addAll(tvList)
                    SP.customSources = customSources
                    Toast.makeText(context, "导入成功: ${tvList.size}个频道", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未找到有效频道", Toast.LENGTH_SHORT).show()
                }
                stream.close()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
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

    companion object {
        const val TAG = "SettingFragment"
    }
}

