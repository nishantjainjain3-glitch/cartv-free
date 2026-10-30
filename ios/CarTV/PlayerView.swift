import SwiftUI
import AVKit

struct PlayerView: UIViewControllerRepresentable {
    let streamUrl: String
    var onDismiss: (() -> Void)? = nil

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.showsPlaybackControls = true
        controller.allowsPictureInPicturePlayback = true
        controller.canStartPictureInPictureAutomaticallyFromInline = true
        controller.videoGravity = .resizeAspect

        if let url = URL(string: streamUrl) {
            let asset = AVURLAsset(url: url)
            let item = AVPlayerItem(asset: asset)
            let player = AVPlayer(playerItem: item)
            controller.player = player
            player.play()
        }

        return controller
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {}
}
