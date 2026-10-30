import Foundation
import ReplayKit
import Network
import CoreImage
import ImageIO

class ScreenMirrorManager: ObservableObject {
    @Published var isMirroring = false
    @Published var mirrorUrl = ""
    @Published var errorMessage: String?

    private let recorder = RPScreenRecorder.shared()
    private var listener: NWListener?
    private var activeConnections: [NWConnection] = []
    private let context = CIContext()
    private let queue = DispatchQueue(label: "com.cartv.mirror.queue")

    private var latestJpegData: Data?

    func startMirroring() {
        guard recorder.isAvailable else {
            errorMessage = "Screen recording is not available on this device."
            return
        }

        errorMessage = nil
        startHttpServer()

        recorder.startCapture(handler: { [weak self] sampleBuffer, sampleType, error in
            guard let self = self, error == nil else { return }
            if sampleType == .video {
                self.processVideoFrame(sampleBuffer)
            }
        }) { [weak self] error in
            DispatchQueue.main.async {
                if let error = error {
                    self?.errorMessage = error.localizedDescription
                    self?.stopMirroring()
                } else {
                    self?.isMirroring = true
                    let ip = self?.getIPAddress() ?? "172.20.10.1"
                    self?.mirrorUrl = "http://\(ip):8080"
                }
            }
        }
    }

    func stopMirroring() {
        recorder.stopCapture { [weak self] _ in
            DispatchQueue.main.async {
                self?.isMirroring = false
                self?.mirrorUrl = ""
                self?.stopHttpServer()
            }
        }
    }

    private func processVideoFrame(_ sampleBuffer: CMSampleBuffer) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        if let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) {
            let options: [CIImageRepresentationOption: Any] = [
                CIImageRepresentationOption(rawValue: kCGImageDestinationLossyFactor as String): 0.6
            ]
            if let jpegData = context.jpegRepresentation(of: ciImage, colorSpace: colorSpace, options: options) {
                self.latestJpegData = jpegData
                broadcastLatestFrame(jpegData)
            }
        }
    }

    private func startHttpServer() {
        do {
            let tcpParams = NWParameters.tcp
            listener = try NWListener(using: tcpParams, on: 8080)

            listener?.newConnectionHandler = { [weak self] connection in
                self?.handleIncomingConnection(connection)
            }

            listener?.start(queue: queue)
        } catch {
            DispatchQueue.main.async {
                self.errorMessage = "Failed to start local mirror server: \(error.localizedDescription)"
            }
        }
    }

    private func handleIncomingConnection(_ connection: NWConnection) {
        connection.start(queue: queue)
        activeConnections.append(connection)

        // Send HTTP 200 multipart header
        let boundary = "cartv_frame_boundary"
        let header = "HTTP/1.1 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=\(boundary)\r\nConnection: keep-alive\r\nCache-Control: no-cache\r\n\r\n"

        connection.send(content: header.data(using: .utf8), completion: .contentProcessed { _ in })
    }

    private func broadcastLatestFrame(_ data: Data) {
        let boundary = "cartv_frame_boundary"
        let frameHeader = "\r\n--\(boundary)\r\nContent-Type: image/jpeg\r\nContent-Length: \(data.count)\r\n\r\n"

        var packet = frameHeader.data(using: .utf8) ?? Data()
        packet.append(data)

        activeConnections.removeAll { connection in
            if connection.state != .ready {
                return true
            }
            connection.send(content: packet, completion: .contentProcessed { _ in })
            return false
        }
    }

    private func stopHttpServer() {
        for conn in activeConnections {
            conn.cancel()
        }
        activeConnections.removeAll()
        listener?.cancel()
        listener = nullListener()
    }

    private func nullListener() -> NWListener? {
        return nil
    }

    private func getIPAddress() -> String {
        var address: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        if getifaddrs(&ifaddr) == 0 {
            var ptr = ifaddr
            while ptr != nil {
                defer { ptr = ptr?.pointee.ifa_next }
                guard let interface = ptr?.pointee else { continue }
                let addrFamily = interface.ifa_addr.pointee.sa_family
                if addrFamily == UInt8(AF_INET) {
                    let name = String(cString: interface.ifa_name)
                    // en0 = Wi-Fi, bridge100 / ap0 = Personal Hotspot
                    if name == "en0" || name == "bridge100" || name == "ap0" {
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                                    &hostname, socklen_t(hostname.count),
                                    nil, socklen_t(0), NI_NUMERICHOST)
                        address = String(cString: hostname)
                        if name == "bridge100" || name == "ap0" {
                            break
                        }
                    }
                }
            }
            freeifaddrs(ifaddr)
        }
        return address ?? "172.20.10.1"
    }
}
