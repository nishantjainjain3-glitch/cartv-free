package com.autotv.player.mirror

import android.content.Context
import android.graphics.ImageFormat
import android.media.ImageReader
import android.view.Surface
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class LocalStreamServer(val port: Int = 8080, context: Context) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2)

    val surface: Surface get() = imageReader.surface

    fun start() {
        isRunning = true
        thread(start = true, name = "CarMirrorHttpThread") {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                // Socket closed
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread(start = true) {
            try {
                val output: OutputStream = socket.getOutputStream()
                val boundary = "cartv_frame_boundary"
                val header = ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Connection: keep-alive\r\n\r\n").toByteArray()
                output.write(header)

                while (isRunning && !socket.isClosed) {
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        image.close()

                        val frameHeader = ("--$boundary\r\n" +
                                "Content-Type: image/jpeg\r\n" +
                                "Content-Length: ${bytes.size}\r\n\r\n").toByteArray()
                        output.write(frameHeader)
                        output.write(bytes)
                        output.write("\r\n".toByteArray())
                        output.flush()
                    }
                    Thread.sleep(33) // ~30 fps
                }
            } catch (e: Exception) {
                // Client disconnected
            } finally {
                try { socket.close() } catch (ignored: Exception) {}
            }
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (ignored: Exception) {}
        try { imageReader.close() } catch (ignored: Exception) {}
    }
}
