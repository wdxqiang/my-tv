package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import com.lizongying.mytv.TV
import com.lizongying.mytv.models.ProgramType

object SP {
    // Name of the sp file TODO Should use a meaningful name and do migrations
    private const val SP_FILE_NAME = "MainActivity"

    // If Change channel with up and down in reversed order or not
    private const val KEY_CHANNEL_REVERSAL = "channel_reversal"

    // If use channel num to select channel or not
    private const val KEY_CHANNEL_NUM = "channel_num"

    private const val KEY_TIME = "time"

    // If start app on device boot or not
    private const val KEY_BOOT_STARTUP = "boot_startup"

    private const val KEY_GRID = "grid"

    // Position in list of the selected channel item
    private const val KEY_POSITION = "position"

    // guid
    private const val KEY_GUID = "guid"
    
    // custom m3u sources
    private const val KEY_CUSTOM_SOURCES = "custom_sources"

    private lateinit var sp: SharedPreferences

    /**
     * The method must be invoked as early as possible(At least before using the keys)
     */
    fun init(context: Context) {
        sp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
    }

    var channelReversal: Boolean
        get() = sp.getBoolean(KEY_CHANNEL_REVERSAL, false)
        set(value) = sp.edit().putBoolean(KEY_CHANNEL_REVERSAL, value).apply()

    var channelNum: Boolean
        get() = sp.getBoolean(KEY_CHANNEL_NUM, true)
        set(value) = sp.edit().putBoolean(KEY_CHANNEL_NUM, value).apply()

    var time: Boolean
        get() = sp.getBoolean(KEY_TIME, true)
        set(value) = sp.edit().putBoolean(KEY_TIME, value).apply()

    var bootStartup: Boolean
        get() = sp.getBoolean(KEY_BOOT_STARTUP, false)
        set(value) = sp.edit().putBoolean(KEY_BOOT_STARTUP, value).apply()

    var grid: Boolean
        get() = sp.getBoolean(KEY_GRID, false)
        set(value) = sp.edit().putBoolean(KEY_GRID, value).apply()

    var itemPosition: Int
        get() = sp.getInt(KEY_POSITION, 0)
        set(value) = sp.edit().putInt(KEY_POSITION, value).apply()

    var guid: String
        get() = sp.getString(KEY_GUID, "") ?: ""
        set(value) = sp.edit().putString(KEY_GUID, value).apply()

    private val gson = Gson()
    private val customSourcesType: Type = object : TypeToken<MutableList<TV>>() {}.type
    
    var customSources: MutableList<TV>
        get() {
            val json = sp.getString(KEY_CUSTOM_SOURCES, null)
            return if (json != null) {
                gson.fromJson(json, customSourcesType)
            } else {
                mutableListOf()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            sp.edit().putString(KEY_CUSTOM_SOURCES, json).apply()
        }
}