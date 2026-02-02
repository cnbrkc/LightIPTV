package com.lightiptv.models

data class XtreamCredentials(
    val server: String,
    val username: String,
    val password: String
) {
    val baseUrl: String
        get() = server.removeSuffix("/")
    
    val playerApiUrl: String
        get() = "$baseUrl/player_api.php"
    
    val liveStreamUrl: String
        get() = "$baseUrl/live/$username/$password"
    
    val vodStreamUrl: String
        get() = "$baseUrl/movie/$username/$password"
    
    val seriesStreamUrl: String
        get() = "$baseUrl/series/$username/$password"
        
    fun getLiveUrl(streamId: String, extension: String = "m3u8"): String {
        return "$liveStreamUrl/$streamId.$extension"
    }
    
    fun getVodUrl(streamId: String, extension: String): String {
        return "$vodStreamUrl/$streamId.$extension"
    }
}

data class XtreamUserInfo(
    val username: String,
    val status: String,
    val expDate: String,
    val activeCons: Int,
    val maxConnections: Int
)
