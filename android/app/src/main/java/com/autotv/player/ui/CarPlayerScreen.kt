package com.autotv.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.autotv.player.data.Channel

@OptIn(UnstableApi::class)
@Composable
fun CarPlayerScreen(
    channels: List<Channel>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val currentChannel = channels.getOrNull(currentIndex) ?: return

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(currentChannel.streamUrl) {
        player.setMediaItem(MediaItem.fromUri(currentChannel.streamUrl))
        player.prepare()
        player.play()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA222222)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Channels", color = Color.White)
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = currentChannel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentChannel.group,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val activity = context as? Activity
                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA222222)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("PiP", color = Color.White)
            }
        }

        // Bottom Channel Switching Overlay for Vehicle Dashboards
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IconButton(
                onClick = {
                    if (currentIndex > 0) {
                        currentIndex--
                    }
                },
                enabled = currentIndex > 0,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xAA222222), CircleShape)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous Channel",
                    tint = if (currentIndex > 0) Color.White else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "${currentIndex + 1} / ${channels.size}",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )

            IconButton(
                onClick = {
                    if (currentIndex < channels.size - 1) {
                        currentIndex++
                    }
                },
                enabled = currentIndex < channels.size - 1,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xAA222222), CircleShape)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next Channel",
                    tint = if (currentIndex < channels.size - 1) Color.White else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
