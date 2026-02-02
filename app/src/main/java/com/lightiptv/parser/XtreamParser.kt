package com.lightiptv.parser

import com.lightiptv.models.Category
import com.lightiptv.models.Channel
import com.lightiptv.models.XtreamCredentials
import com.lightiptv.models.XtreamUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XtreamParser(private val credentials: XtreamCredentials) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    suspend fun authenticate(): XtreamUserInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}"
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val userInfo = json.optJSONObject("user_info") ?: return@withContext null
                
                XtreamUserInfo(
                    username = userInfo.optString("username"),
                    status = userInfo.optString("status"),
                    expDate = userInfo.optString("exp_date"),
                    activeCons = userInfo.optInt("active_cons", 0),
                    maxConnections = userInfo.optInt("max_connections", 1)
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getLiveCategories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_live_categories"
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: return@withContext emptyList())
                (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    Category(
                        id = obj.optString("category_id"),
                        name = obj.optString("category_name")
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getLiveStreams(categoryId: String? = null): List<Channel> = withContext(Dispatchers.IO) {
        try {
            var url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_live_streams"
            if (categoryId != null) {
                url += "&category_id=$categoryId"
            }
            
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: return@withContext emptyList())
                (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    
                    Channel(
                        id = streamId,
                        name = obj.optString("name"),
                        url = credentials.getLiveUrl(streamId),
                        logo = obj.optString("stream_icon"),
                        group = obj.optString("category_id"),
                        epgId = obj.optString("epg_channel_id"),
                        isLive = true
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getVodCategories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_vod_categories"
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: return@withContext emptyList())
                (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    Category(
                        id = obj.optString("category_id"),
                        name = obj.optString("category_name")
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getVodStreams(categoryId: String? = null): List<Channel> = withContext(Dispatchers.IO) {
        try {
            var url = "${credentials.playerApiUrl}?username=${credentials.username}&password=${credentials.password}&action=get_vod_streams"
            if (categoryId != null) {
                url += "&category_id=$categoryId"
            }
            
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: return@withContext emptyList())
                (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val extension = obj.optString("container_extension", "mp4")
                    
                    Channel(
                        id = streamId,
                        name = obj.optString("name"),
                        url = credentials.getVodUrl(streamId, extension),
                        logo = obj.optString("stream_icon"),
                        group = obj.optString("category_id"),
                        isLive = false
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
