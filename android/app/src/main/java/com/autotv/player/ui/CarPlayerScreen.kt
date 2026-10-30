package com.autotv.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    channel: Channel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x99222222))
            ) {
                Text("Channels", color = Color.White)
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x99222222))
            ) {
                Text("PiP", color = Color.White)
            }
        }
    }
}
