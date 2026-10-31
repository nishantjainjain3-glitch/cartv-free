package com.autotv.player

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.autotv.player.data.Channel
import com.autotv.player.data.M3uParser
import com.autotv.player.ui.CarPlayerScreen
import com.autotv.player.ui.MirrorScreen
import kotlinx.coroutines.launch

data class Preset(val name: String, val url: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("cartv_prefs", Context.MODE_PRIVATE) }

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showMirrorScreen by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var currentPlaylistUrl by remember {
        mutableStateOf(prefs.getString("playlist_url", "https://iptv-org.github.io/iptv/index.m3u") ?: "https://iptv-org.github.io/iptv/index.m3u")
    }
    var customInputUrl by remember { mutableStateOf("") }
    var favoriteIds by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    var isLoading by remember { mutableStateOf(true) }

    val presets = remember {
        listOf(
            Preset("Global IPTV (All Categories)", "https://iptv-org.github.io/iptv/index.m3u"),
            Preset("News & Current Affairs", "https://iptv-org.github.io/iptv/categories/news.m3u"),
            Preset("Sports Live", "https://iptv-org.github.io/iptv/categories/sports.m3u"),
            Preset("Movies & Cinema", "https://iptv-org.github.io/iptv/categories/movies.m3u"),
            Preset("Music TV", "https://iptv-org.github.io/iptv/categories/music.m3u")
        )
    }

    fun loadPlaylist(url: String) {
        isLoading = true
        currentPlaylistUrl = url
        prefs.edit().putString("playlist_url", url).apply()
        scope.launch {
            try {
                channels = M3uParser.fetchPlaylist(url)
            } catch (e: Exception) {
                channels = listOf(
                    Channel(
                        id = "demo_1",
                        name = "Big Buck Bunny (HLS Test)",
                        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                        group = "Demo"
                    )
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite(id: String) {
        val updated = favoriteIds.toMutableSet()
        if (updated.contains(id)) {
            updated.remove(id)
        } else {
            updated.add(id)
        }
        favoriteIds = updated
        prefs.edit().putStringSet("favorites", updated).apply()
    }

    LaunchedEffect(Unit) {
        loadPlaylist(currentPlaylistUrl)
    }

    val groups = remember(channels) {
        listOf("All", "★ Favorites") + channels.map { it.group }.distinct().sorted()
    }

    val filtered = remember(channels, selectedGroup, searchQuery, favoriteIds) {
        channels.filter { ch ->
            val matchesGroup = when (selectedGroup) {
                "★ Favorites" -> favoriteIds.contains(ch.id)
                "All" -> true
                else -> ch.group == selectedGroup
            }
            val matchesSearch = searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    when {
        showMirrorScreen -> {
            MirrorScreen(onBack = { showMirrorScreen = false })
        }
        selectedIndex != null -> {
            CarPlayerScreen(
                channels = channels,
                initialIndex = selectedIndex!!,
                onBack = { selectedIndex = null }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F0F))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.Cyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CarTV",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showPlaylistDialog = true },
                            modifier = Modifier.background(Color(0xFF222222), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists", tint = Color.White)
                        }

                        Button(
                            onClick = { showMirrorScreen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mirror")
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search channels...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Group Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { grp ->
                        val isSelected = grp == selectedGroup
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroup = grp },
                            label = { Text(grp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (grp == "★ Favorites") Color(0xFFF59E0B) else Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF222222),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Cyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading Channels & Guide...", color = Color.Gray)
                        }
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedGroup == "★ Favorites") "No favorites added yet" else "No channels found",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { ch ->
                            val isFav = favoriteIds.contains(ch.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val idx = channels.indexOfFirst { it.id == ch.id }
                                        if (idx != -1) selectedIndex = idx
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (ch.logoUrl != null) {
                                        AsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = ch.name,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Cyan)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = ch.group,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }

                                    IconButton(onClick = { toggleFavorite(ch.id) }) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) Color(0xFFF59E0B) else Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playlist Selection Dialog
            if (showPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showPlaylistDialog = false },
                    title = { Text("Select or Add Playlist", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Recommended Playlists:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            presets.forEach { preset ->
                                Button(
                                    onClick = {
                                        loadPlaylist(preset.url)
                                        showPlaylistDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentPlaylistUrl == preset.url) Color(0xFF2563EB) else Color(0xFF2A2A2A)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(preset.name, color = Color.White)
                                }
                            }

                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                            Text("Custom Playlist URL:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = customInputUrl,
                                onValueChange = { customInputUrl = it },
                                placeholder = { Text("https://example.com/playlist.m3u", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customInputUrl.isNotBlank()) {
                                    loadPlaylist(customInputUrl.trim())
                                }
                                showPlaylistDialog = false
                            },
                            enabled = customInputUrl.isNotBlank()
                        ) {
                            Text("Load")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPlaylistDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF1E1E1E)
                )
            }
        }
    }
}
