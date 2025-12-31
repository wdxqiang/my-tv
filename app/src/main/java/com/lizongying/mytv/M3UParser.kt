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
        var currentGroup = ""
        var currentIndex = 10000 // 使用较大的起始id，避免与默认频道冲突
        
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var lineCount = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    lineCount++
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue
                    
                    Log.d(TAG, "Line $lineCount: $trimmedLine")
                    
                    if (trimmedLine.startsWith("#EXTGRP:")) {
                        // 解析分类信息行，格式如：#EXTGRP:央视
                        currentGroup = parseGroupFromEXTGRP(trimmedLine)
                        Log.d(TAG, "Parsed group from #EXTGRP: $currentGroup")
                    } else if (trimmedLine.startsWith("#EXTINF:")) {
                        // 解析频道信息行，格式如：#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV1" tvg-logo="http://example.com/logo.png",CCTV1 综合
                        // 同时解析频道名称和group-title属性
                        currentTitle = parseTitleFromEXTINF(trimmedLine)
                        val groupFromExtinf = parseGroupFromTitleAttribute(trimmedLine)
                        if (groupFromExtinf.isNotEmpty()) {
                            currentGroup = groupFromExtinf
                            Log.d(TAG, "Parsed group from group-title: $currentGroup")
                        }
                        Log.d(TAG, "Parsed title: $currentTitle")
                    } else if (!trimmedLine.startsWith("#")) {
                        // 解析直播源URL
                        currentUrl = trimmedLine
                        Log.d(TAG, "Parsed URL: $currentUrl")
                        Log.d(TAG, "Current state - Title: '$currentTitle', URL: '$currentUrl', Group: '$currentGroup'")
                        if (currentTitle.isNotEmpty() && currentUrl.isNotEmpty()) {
                            // 确定频道分类
                            val channelCategory = if (currentGroup.isNotEmpty()) {
                                currentGroup
                            } else {
                                // 如果没有#EXTGRP标签，尝试从频道名称中提取分类
                                extractCategoryFromTitle(currentTitle)
                            }
                            
                            // 创建TV对象并添加到列表
                            val tv = TV(
                                id = currentIndex,
                                title = currentTitle,
                                alias = currentTitle,
                                videoUrl = listOf(currentUrl),
                                channel = channelCategory,
                                programType = ProgramType.Y_PROTO,
                                pid = "",  // 空pid表示不需要调用Request.fetchData
                                needToken = false,
                                mustToken = false
                            )
                            tvList.add(tv)
                            Log.d(TAG, "Created TV object: $tv")
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
            e.printStackTrace()
        }
        
        Log.d(TAG, "Total parsed TV channels: ${tvList.size}")
        return tvList
    }
    
    // 从#EXTINF行解析频道名称
    private fun parseTitleFromEXTINF(extinfLine: String): String {
        // 匹配逗号后的内容作为频道名称
        val commaIndex = extinfLine.lastIndexOf(",")
        val title = if (commaIndex != -1 && commaIndex < extinfLine.length - 1) {
            extinfLine.substring(commaIndex + 1).trim()
        } else {
            // 如果没有逗号，返回整个行（去掉#EXTINF:前缀）
            Log.w(TAG, "No comma found in #EXTINF line: $extinfLine")
            extinfLine.replace("#EXTINF:", "").trim()
        }
        
        // 如果解析结果为空，使用一个默认名称
        if (title.isEmpty()) {
            Log.w(TAG, "Empty title parsed from #EXTINF line: $extinfLine")
            return "Unknown Channel"
        }
        
        return title
    }
    
    // 从#EXTGRP行解析分类信息
    private fun parseGroupFromEXTGRP(extgrpLine: String): String {
        // 格式：#EXTGRP:分类名称
        return extgrpLine.substring(8).trim()
    }
    
    // 从#EXTINF行的group-title属性解析分类信息
    private fun parseGroupFromTitleAttribute(extinfLine: String): String {
        // 匹配group-title="分类名称"格式
        val pattern = Pattern.compile("group-title=\"([^\"]+)\"")
        val matcher = pattern.matcher(extinfLine)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }
    
    // 从频道名称中提取分类信息
    private fun extractCategoryFromTitle(title: String): String {
        // 常见的分类前缀匹配
        val categoryPrefixes = mapOf(
            "央视" to listOf("CCTV", "央视"),
            "地方" to listOf("北京", "上海", "广东", "江苏", "浙江", "湖南", "湖北", "河南", "河北", "山东", "山西", "陕西", "甘肃", "青海", "宁夏", "新疆", "西藏", "四川", "重庆", "云南", "贵州", "广西", "海南", "辽宁", "吉林", "黑龙江", "安徽", "福建", "江西", "内蒙古", "天津", "兵团"),
            "港澳台" to listOf("凤凰", "翡翠", "明珠", "本港", "亚视", "中天", "东森", "TVBS", "华视", "中视", "民视", "公视", "三立", "八大", "台视"),
            "电影" to listOf("电影", "Movie", "Film"),
            "电视剧" to listOf("电视剧", "Drama", "Series"),
            "综艺" to listOf("综艺", "Variety"),
            "体育" to listOf("体育", "Sports", "NBA", "足球", "篮球", "英超", "西甲", "意甲", "德甲", "欧冠"),
            "新闻" to listOf("新闻", "News"),
            "少儿" to listOf("少儿", "动画", "Cartoon", "Kids"),
            "音乐" to listOf("音乐", "Music", "MTV"),
            "纪实" to listOf("纪实", "纪录", "Documentary"),
            "科教" to listOf("科教", "科学", "教育", "Science", "Education")
        )
        
        // 遍历分类前缀，查找匹配
        for ((category, prefixes) in categoryPrefixes) {
            for (prefix in prefixes) {
                if (title.startsWith(prefix)) {
                    return category
                }
            }
        }
        
        // 默认分类
        return "用户频道"
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