package com.autotv.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "General"
)

object M3uParser {
    private val client = OkHttpClient()

    suspend fun fetchPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        parse(body)
    }

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup = "General"

        val extinfRegex = Regex("""#EXTINF:-1.*?(?:tvg-logo="([^"]*)")?.*?(?:group-title="([^"]*)")?,(.*)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                val match = extinfRegex.find(trimmed)
                if (match != null) {
                    currentLogo = match.groups[1]?.value?.takeIf { it.isNotBlank() }
                    currentGroup = match.groups[2]?.value?.takeIf { it.isNotBlank() } ?: "General"
                    currentName = match.groups[3]?.value?.trim() ?: "Channel ${channels.size + 1}"
                } else {
                    currentName = trimmed.substringAfterLast(",").trim()
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (currentName.isNotEmpty()) {
                    channels.add(
                        Channel(
                            id = trimmed.hashCode().toString(),
                            name = currentName,
                            streamUrl = trimmed,
                            logoUrl = currentLogo,
                            group = currentGroup
                        )
                    )
                }
                currentName = ""
                currentLogo = null
                currentGroup = "General"
            }
        }
        return channels
    }
}
