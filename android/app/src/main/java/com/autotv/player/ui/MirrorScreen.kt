package com.autotv.player.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autotv.player.mirror.ScreenMirrorService

@Composable
fun MirrorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isMirroring by remember { mutableStateOf(false) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenMirrorService::class.java).apply {
                putExtra(ScreenMirrorService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenMirrorService.EXTRA_RESULT_DATA, result.data)
            }
            context.startForegroundService(serviceIntent)
            isMirroring = true
        }
    }

    val localIp = remember { getDeviceIp(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Car Screen Mirroring",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isMirroring) {
            Text(
                text = "● Mirroring Active",
                color = Color.Green,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open this URL in your car browser:\nhttp://$localIp:8080",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val stopIntent = Intent(context, ScreenMirrorService::class.java).apply {
                        action = ScreenMirrorService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    isMirroring = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Stop Mirroring", color = Color.White)
            }
        } else {
            Text(
                text = "Connect your car and phone to the same Wi-Fi or Hotspot, then start mirroring.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    launcher.launch(projectionManager.createScreenCaptureIntent())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Start Mirroring to Car", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Back to TV Channels", color = Color.LightGray)
        }
    }
}

private fun getDeviceIp(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    @Suppress("DEPRECATION")
    return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress) ?: "192.168.43.1"
}
