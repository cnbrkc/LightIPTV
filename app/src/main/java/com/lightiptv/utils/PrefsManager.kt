package com.lightiptv.utils

import android.content.Context
import android.content.SharedPreferences
import com.lightiptv.models.XtreamCredentials

class PrefsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("light_iptv", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_M3U_URL = "m3u_url"
        private const val KEY_M3U_CACHE = "m3u_cache"
        private const val KEY_XTREAM_SERVER = "xtream_server"
        private const val KEY_XTREAM_USERNAME = "xtream_username"
        private const val KEY_XTREAM_PASSWORD = "xtream_password"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_LAST_CHANNEL = "last_channel"
        private const val KEY_BUFFER_SIZE = "buffer_size"
        private const val KEY_DECODER_TYPE = "decoder_type"
    }
    
    var m3uUrl: String
        get() = prefs.getString(KEY_M3U_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_M3U_URL, value).apply()
    
    var m3uCache: String
        get() = prefs.getString(KEY_M3U_CACHE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_M3U_CACHE, value).apply()
    
    var sourceType: Int
        get() = prefs.getInt(KEY_SOURCE_TYPE, 0) // 0 = M3U, 1 = Xtream
        set(value) = prefs.edit().putInt(KEY_SOURCE_TYPE, value).apply()
    
    var xtreamCredentials: XtreamCredentials?
        get() {
            val server = prefs.getString(KEY_XTREAM_SERVER, null) ?: return null
            val username = prefs.getString(KEY_XTREAM_USERNAME, null) ?: return null
            val password = prefs.getString(KEY_XTREAM_PASSWORD, null) ?: return null
            return XtreamCredentials(server, username, password)
        }
        set(value) {
            prefs.edit().apply {
                if (value != null) {
                    putString(KEY_XTREAM_SERVER, value.server)
                    putString(KEY_XTREAM_USERNAME, value.username)
                    putString(KEY_XTREAM_PASSWORD, value.password)
                } else {
                    remove(KEY_XTREAM_SERVER)
                    remove(KEY_XTREAM_USERNAME)
                    remove(KEY_XTREAM_PASSWORD)
                }
            }.apply()
        }
    
    var lastChannelId: String
        get() = prefs.getString(KEY_LAST_CHANNEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CHANNEL, value).apply()
    
    var bufferSize: Int
        get() = prefs.getInt(KEY_BUFFER_SIZE, 2000) // 2 saniye default
        set(value) = prefs.edit().putInt(KEY_BUFFER_SIZE, value).apply()
    
    var decoderType: Int
        get() = prefs.getInt(KEY_DECODER_TYPE, 0) // 0 = Auto, 1 = Hardware, 2 = Software
        set(value) = prefs.edit().putInt(KEY_DECODER_TYPE, value).apply()
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
