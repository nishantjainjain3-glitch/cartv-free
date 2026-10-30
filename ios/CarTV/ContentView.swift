import SwiftUI

struct ContentView: View {
    @StateObject private var mirrorManager = ScreenMirrorManager()
    @State private var channels: [Channel] = []
    @State private var playlistUrl = "https://iptv-org.github.io/iptv/index.m3u"
    @State private var selectedChannel: Channel?
    @State private var selectedGroup = "All"
    @State private var searchQuery = ""
    @State private var isLoading = false
    @State private var showPlaylistSheet = false
    @State private var activeTab = 0

    var groups: [String] {
        var set = Set(channels.map { $0.group })
        return ["All"] + Array(set).sorted()
    }

    var filteredChannels: [Channel] {
        channels.filter { ch in
            (selectedGroup == "All" || ch.group == selectedGroup) &&
            (searchQuery.isEmpty || ch.name.localizedCaseInsensitiveContains(searchQuery))
        }
    }

    var body: some View {
        TabView(selection: $activeTab) {
            // TAB 1: Live TV / Playlists
            NavigationStack {
                VStack(spacing: 0) {
                    // Group Filter Bar
                    if !groups.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(groups, id: \.self) { grp in
                                    Button(action: { selectedGroup = grp }) {
                                        Text(grp)
                                            .font(.subheadline)
                                            .fontWeight(selectedGroup == grp ? .bold : .regular)
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 8)
                                            .background(selectedGroup == grp ? Color.blue : Color(white: 0.15))
                                            .foregroundColor(.white)
                                            .cornerRadius(20)
                                    }
                                }
                            }
                            .padding(.horizontal)
                            .padding(.vertical, 8)
                        }
                    }

                    // Channel List
                    if isLoading {
                        Spacer()
                        ProgressView("Loading Playlist...")
                            .tint(.white)
                        Spacer()
                    } else if filteredChannels.isEmpty {
                        Spacer()
                        VStack(spacing: 12) {
                            Image(systemName: "tv.slash")
                                .font(.system(size: 48))
                                .foregroundColor(.gray)
                            Text("No Channels Found")
                                .font(.title3)
                                .foregroundColor(.gray)
                            Button("Add Playlist Link") {
                                showPlaylistSheet = true
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        Spacer()
                    } else {
                        List(filteredChannels) { channel in
                            Button(action: { selectedChannel = channel }) {
                                HStack(spacing: 16) {
                                    if let logoUrl = channel.logoUrl, let url = URL(string: logoUrl) {
                                        AsyncImage(url: url) { image in
                                            image.resizable().aspectRatio(contentMode: .fit)
                                        } placeholder: {
                                            Image(systemName: "tv")
                                                .foregroundColor(.gray)
                                        }
                                        .frame(width: 44, height: 44)
                                        .cornerRadius(8)
                                    } else {
                                        Image(systemName: "play.tv.fill")
                                            .font(.title2)
                                            .frame(width: 44, height: 44)
                                            .background(Color(white: 0.15))
                                            .cornerRadius(8)
                                            .foregroundColor(.blue)
                                    }

                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(channel.name)
                                            .font(.headline)
                                            .foregroundColor(.white)
                                        Text(channel.group)
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .foregroundColor(Color(white: 0.3))
                                }
                                .padding(.vertical, 4)
                            }
                            .listRowBackground(Color(white: 0.08))
                        }
                        .listStyle(.plain)
                    }
                }
                .navigationTitle("CarTV Live")
                .searchable(text: $searchQuery, prompt: "Search channels")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(action: { showPlaylistSheet = true }) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title3)
                        }
                    }
                }
            }
            .tabItem {
                Label("Live TV", systemImage: "tv")
            }
            .tag(0)

            // TAB 2: Screen Mirroring to Car
            NavigationStack {
                VStack(spacing: 24) {
                    Spacer()

                    Image(systemName: mirrorManager.isMirroring ? "car.rear.and.tire.marks" : "car.fill")
                        .font(.system(size: 72))
                        .foregroundColor(mirrorManager.isMirroring ? .green : .blue)

                    Text("Mirror Screen to Car")
                        .font(.title)
                        .fontWeight(.bold)

                    if mirrorManager.isMirroring {
                        VStack(spacing: 12) {
                            Text("● Broadcasting Live")
                                .foregroundColor(.green)
                                .font(.headline)

                            Text("Open this URL on your car's screen:")
                                .font(.subheadline)
                                .foregroundColor(.gray)

                            Text(mirrorManager.mirrorUrl)
                                .font(.system(size: 20, weight: .bold, design: .monospaced))
                                .padding()
                                .background(Color(white: 0.15))
                                .cornerRadius(12)
                                .foregroundColor(.yellow)
                        }
                        .padding(.horizontal)

                        Button(action: { mirrorManager.stopMirroring() }) {
                            Text("Stop Mirroring")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.red)
                                .foregroundColor(.white)
                                .cornerRadius(16)
                        }
                        .padding(.horizontal, 32)
                    } else {
                        Text("Connect your car's screen to your iPhone Personal Hotspot or Wi-Fi, then press Start.")
                            .font(.body)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)

                        Button(action: { mirrorManager.startMirroring() }) {
                            HStack {
                                Image(systemName: "airplayvideo")
                                Text("Start Car Mirroring")
                            }
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(16)
                        }
                        .padding(.horizontal, 32)
                    }

                    if let err = mirrorManager.errorMessage {
                        Text(err)
                            .foregroundColor(.red)
                            .font(.caption)
                    }

                    Spacer()
                }
                .navigationTitle("Screen Mirror")
                .background(Color.black.ignoresSafeArea())
            }
            .tabItem {
                Label("Car Mirror", systemImage: "airplayvideo")
            }
            .tag(1)
        }
        .sheet(item: $selectedChannel) { channel in
            PlayerView(streamUrl: channel.streamUrl)
                .ignoresSafeArea()
        }
        .sheet(isPresented: $showPlaylistSheet) {
            NavigationStack {
                VStack(spacing: 20) {
                    Text("Enter M3U Playlist Link")
                        .font(.headline)

                    TextField("https://example.com/playlist.m3u", text: $playlistUrl)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .padding(.horizontal)

                    Button("Download & Load Channels") {
                        showPlaylistSheet = false
                        loadPlaylist()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(playlistUrl.isEmpty)

                    Spacer()
                }
                .padding(.top, 24)
                .navigationTitle("Add Playlist")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showPlaylistSheet = false }
                    }
                }
            }
            .presentationDetents([.fraction(0.4)])
        }
        .onAppear {
            if channels.isEmpty {
                loadPlaylist()
            }
        }
    }

    private func loadPlaylist() {
        guard !playlistUrl.isEmpty else { return }
        isLoading = true
        Task {
            do {
                let parsed = try await M3UParser.fetch(from: playlistUrl)
                await MainActor.run {
                    self.channels = parsed
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
}
