package com.autotv.player

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.autotv.player.data.Channel
import com.autotv.player.data.M3uParser
import com.autotv.player.ui.CarPlayerScreen
import com.autotv.player.ui.MirrorScreen
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showMirrorScreen by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Free public IPTV playlist by default
                channels = M3uParser.fetchPlaylist("https://iptv-org.github.io/iptv/index.m3u")
            } catch (e: Exception) {
                // Fallback demo channel
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

    val groups = remember(channels) {
        listOf("All") + channels.map { it.group }.distinct().sorted()
    }

    val filtered = remember(channels, selectedGroup) {
        if (selectedGroup == "All") channels else channels.filter { it.group == selectedGroup }
    }

    when {
        showMirrorScreen -> {
            MirrorScreen(onBack = { showMirrorScreen = false })
        }
        selectedChannel != null -> {
            CarPlayerScreen(channel = selectedChannel!!, onBack = { selectedChannel = null })
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

                    Button(
                        onClick = { showMirrorScreen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mirror to Car")
                    }
                }

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
                                selectedContainerColor = Color(0xFF2563EB),
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
                        CircularProgressIndicator(color = Color.Cyan)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { ch ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChannel = ch },
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
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.Cyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
