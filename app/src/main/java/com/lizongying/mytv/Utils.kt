package com.lizongying.mytv

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import com.google.gson.Gson
import com.lizongying.mytv.api.TimeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    private var between: Long = 0

    fun getDateFormat(format: String): String {
        return SimpleDateFormat(
            format,
            Locale.CHINA
        ).format(Date(System.currentTimeMillis() - between))
    }

    fun getDateTimestamp(): Long {
        return (System.currentTimeMillis() - between) / 1000
    }

    fun setBetween(currentTimeMillis: Long) {
        between = System.currentTimeMillis() - currentTimeMillis
    }

    suspend fun init() {
        var currentTimeMillis: Long = 0
        try {
            currentTimeMillis = getTimestampFromServer()
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
            // Use local time if server time retrieval fails
            currentTimeMillis = System.currentTimeMillis()
        }
        between = System.currentTimeMillis() - currentTimeMillis
    }

    /**
     * 从服务器获取时间戳
     * @return Long 时间戳
     */
    private suspend fun getTimestampFromServer(): Long {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder()
                .url("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        // If server returns error, use local time instead of throwing
                        return@use System.currentTimeMillis()
                    }
                    val string = response.body()?.string() ?: ""
                    if (string.isEmpty()) {
                        return@use System.currentTimeMillis()
                    }
                    try {
                        Gson().fromJson(string, TimeResponse::class.java).data.t.toLong()
                    } catch (jsonEx: Exception) {
                        // If JSON parsing fails, use local time
                        System.currentTimeMillis()
                    }
                }
            } catch (e: IOException) {
                // Handle network errors gracefully
                e.printStackTrace()
                System.currentTimeMillis()
            }
        }
    }

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun pxToDp(px: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun pxToDp(px: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun isTmallDevice() = Build.MANUFACTURER.equals("Tmall", ignoreCase = true)
}