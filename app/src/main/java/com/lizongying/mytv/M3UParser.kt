package com.lizongying.mytv

import android.util.Log
import com.lizongying.mytv.models.ProgramType
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern

object M3UParser {
    private const val TAG = "M3UParser"
    
    // 解析本地或网络.m3u文件
    fun parseM3U(inputStream: InputStream): List<TV> {
        val tvList = mutableListOf<TV>()
        var currentTitle = ""
        var currentUrl = ""
        var currentIndex = 10000 // 使用较大的起始id，避免与默认频道冲突
        
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line = line?.trim()
                    if (line.isNullOrEmpty()) continue
                    
                    if (line.startsWith("#EXTINF:")) {
                        // 解析频道信息行，格式如：#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV1" tvg-logo="http://example.com/logo.png",CCTV1 综合
                        currentTitle = parseTitleFromEXTINF(line)
                    } else if (!line.startsWith("#")) {
                        // 解析直播源URL
                        currentUrl = line
                        if (currentTitle.isNotEmpty() && currentUrl.isNotEmpty()) {
                            // 创建TV对象并添加到列表
                            val tv = TV(
                                id = currentIndex,
                                title = currentTitle,
                                alias = currentTitle,
                                videoUrl = listOf(currentUrl),
                                channel = "自定义",
                                programType = ProgramType.Y_PROTO
                            )
                            tvList.add(tv)
                            currentIndex++
                            // 重置当前信息
                            currentTitle = ""
                            currentUrl = ""
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error parsing M3U file: ${e.message}")
        }
        
        return tvList
    }
    
    // 从#EXTINF行解析频道名称
    private fun parseTitleFromEXTINF(extinfLine: String): String {
        // 匹配逗号后的内容作为频道名称
        val commaIndex = extinfLine.lastIndexOf(",")
        if (commaIndex != -1 && commaIndex < extinfLine.length - 1) {
            return extinfLine.substring(commaIndex + 1).trim()
        }
        return ""
    }
    
    // 从网络URL解析.m3u文件
    fun parseM3UFromUrl(url: String): List<TV> {
        try {
            val inputStream = URL(url).openStream()
            return parseM3U(inputStream)
        } catch (e: IOException) {
            Log.e(TAG, "Error parsing M3U from URL: ${e.message}")
            return emptyList()
        }
    }
}