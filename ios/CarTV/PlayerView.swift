import SwiftUI
import AVKit
import MediaPlayer

struct PlayerView: View {
    let channels: [Channel]
    @State var currentIndex: Int
    var onDismiss: (() -> Void)? = nil

    @State private var player: AVPlayer?
    @State private var playerController: AVPlayerViewController?
    @Environment(\.dismiss) private var dismiss

    var currentChannel: Channel {
        channels[currentIndex]
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if player != nil {
                CustomVideoPlayer(player: player!)
                    .ignoresSafeArea()
            } else {
                ProgressView("Connecting to stream...")
                    .foregroundColor(.white)
            }

            // In-Car Overlay Controls
            VStack {
                HStack {
                    Button(action: {
                        stopPlayback()
                        dismiss()
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "chevron.left")
                            Text("Channels")
                        }
                        .font(.headline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(white: 0.15, opacity: 0.85))
                        .foregroundColor(.white)
                        .cornerRadius(20)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text(currentChannel.name)
                            .font(.headline)
                            .foregroundColor(.white)
                            .lineLimit(1)
                        Text(currentChannel.group)
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                .padding()

                Spacer()

                // Bottom Channel Skipping Bar for Vehicle Use
                HStack(spacing: 24) {
                    Button(action: previousChannel) {
                        Image(systemName: "backward.fill")
                            .font(.title2)
                            .padding()
                            .background(Color(white: 0.15, opacity: 0.85))
                            .foregroundColor(.white)
                            .clipShape(Circle())
                    }
                    .disabled(currentIndex <= 0)
                    .opacity(currentIndex <= 0 ? 0.4 : 1.0)

                    Text("\(currentIndex + 1) / \(channels.count)")
                        .font(.subheadline)
                        .foregroundColor(.gray)

                    Button(action: nextChannel) {
                        Image(systemName: "forward.fill")
                            .font(.title2)
                            .padding()
                            .background(Color(white: 0.15, opacity: 0.85))
                            .foregroundColor(.white)
                            .clipShape(Circle())
                    }
                    .disabled(currentIndex >= channels.count - 1)
                    .opacity(currentIndex >= channels.count - 1 ? 0.4 : 1.0)
                }
                .padding(.bottom, 24)
            }
        }
        .onAppear {
            setupPlayer(for: currentChannel)
            setupRemoteCommands()
        }
        .onDisappear {
            stopPlayback()
        }
    }

    private func setupPlayer(for channel: Channel) {
        guard let url = URL(string: channel.streamUrl) else { return }
        let asset = AVURLAsset(url: url)
        let item = AVPlayerItem(asset: asset)
        let newPlayer = AVPlayer(playerItem: item)
        self.player = newPlayer
        newPlayer.play()
        updateNowPlaying(channel: channel)
    }

    private func nextChannel() {
        if currentIndex < channels.count - 1 {
            currentIndex += 1
            setupPlayer(for: currentChannel)
        }
    }

    private func previousChannel() {
        if currentIndex > 0 {
            currentIndex -= 1
            setupPlayer(for: currentChannel)
        }
    }

    private func stopPlayback() {
        player?.pause()
        player = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    private func updateNowPlaying(channel: Channel) {
        var info = [String: Any]()
        info[MPMediaItemPropertyTitle] = channel.name
        info[MPMediaItemPropertyArtist] = channel.group
        info[MPNowPlayingInfoPropertyIsLiveStream] = true
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func setupRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.nextTrackCommand.addTarget { _ in
            DispatchQueue.main.async {
                self.nextChannel()
            }
            return .success
        }

        commandCenter.previousTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.addTarget { _ in
            DispatchQueue.main.async {
                self.previousChannel()
            }
            return .success
        }

        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { _ in
            self.player?.play()
            return .success
        }

        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { _ in
            self.player?.pause()
            return .success
        }
    }
}

struct CustomVideoPlayer: UIViewControllerRepresentable {
    let player: AVPlayer

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = true
        controller.allowsPictureInPicturePlayback = true
        controller.canStartPictureInPictureAutomaticallyFromInline = true
        controller.videoGravity = .resizeAspect
        return controller
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {
        uiViewController.player = player
    }
}
