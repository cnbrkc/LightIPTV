package com.lightiptv.parser

import com.lightiptv.models.Category
import com.lightiptv.models.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object M3UParser {
    
    private val EXTINF_REGEX = Regex("""#EXTINF:-?\d+\s*(.*)""")
    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""")
    private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""")
    private val TVG_LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""")
    private val GROUP_REGEX = Regex("""group-title="([^"]*)"""")
    
    suspend fun parse(content: String): List<Category> = withContext(Dispatchers.Default) {
        val categories = mutableMapOf<String, Category>()
        val lines = content.lines()
        var i = 0
        var channelId = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("#EXTINF")) {
                val match = EXTINF_REGEX.find(line)
                if (match != null) {
                    val info = match.groupValues[1]
                    
                    // Sonraki satır URL olmalı
                    var urlLine = ""
                    for (j in (i + 1) until minOf(i + 5, lines.size)) {
                        val nextLine = lines[j].trim()
                        if (nextLine.isNotEmpty() && !nextLine.startsWith("#")) {
                            urlLine = nextLine
                            i = j
                            break
                        }
                    }
                    
                    if (urlLine.isNotEmpty()) {
                        val tvgId = TVG_ID_REGEX.find(info)?.groupValues?.get(1) ?: ""
                        val tvgName = TVG_NAME_REGEX.find(info)?.groupValues?.get(1) ?: ""
                        val logo = TVG_LOGO_REGEX.find(info)?.groupValues?.get(1) ?: ""
                        val group = GROUP_REGEX.find(info)?.groupValues?.get(1) ?: "Uncategorized"
                        
                        // Kanal adı - son virgülden sonrası
                        val name = if (info.contains(",")) {
                            info.substringAfterLast(",").trim()
                        } else {
                            tvgName.ifEmpty { "Channel ${++channelId}" }
                        }
                        
                        val channel = Channel(
                            id = (++channelId).toString(),
                            name = name,
                            url = urlLine,
                            logo = logo,
                            group = group,
                            epgId = tvgId
                        )
                        
                        val category = categories.getOrPut(group) {
                            Category(id = group.hashCode().toString(), name = group)
                        }
                        category.channels.add(channel)
                    }
                }
            }
            i++
        }
        
        categories.values.toList().sortedBy { it.name }
    }
}
