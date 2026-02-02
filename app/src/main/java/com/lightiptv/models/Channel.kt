package com.lightiptv.models

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = "",
    val epgId: String = "",
    val isLive: Boolean = true
)

data class Category(
    val id: String,
    val name: String,
    val channels: MutableList<Channel> = mutableListOf()
)
