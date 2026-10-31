import SwiftUI

struct PlaylistPreset: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let url: String
}

struct ContentView: View {
    @StateObject private var mirrorManager = ScreenMirrorManager()
    @State private var channels: [Channel] = []
    @State private var playlistUrl = "https://iptv-org.github.io/iptv/index.m3u"
    @State private var selectedIndex: Int? = nil
    @State private var selectedGroup = "All"
    @State private var searchQuery = ""
    @State private var isLoading = false
    @State private var showPlaylistSheet = false
    @State private var activeTab = 0

    // Persistent Favorites
    @AppStorage("favorite_channels") private var favoriteChannelIdsRaw: String = ""

    var favoriteChannelIds: Set<String> {
        Set(favoriteChannelIdsRaw.components(separatedBy: ",").filter { !$0.isEmpty })
    }

    // Default Preloaded Playlists
    let presets: [PlaylistPreset] = [
        PlaylistPreset(name: "Global IPTV (All Categories)", url: "https://iptv-org.github.io/iptv/index.m3u"),
        PlaylistPreset(name: "News & Current Affairs", url: "https://iptv-org.github.io/iptv/categories/news.m3u"),
        PlaylistPreset(name: "Sports Live", url: "https://iptv-org.github.io/iptv/categories/sports.m3u"),
        PlaylistPreset(name: "Movies & Cinema", url: "https://iptv-org.github.io/iptv/categories/movies.m3u"),
        PlaylistPreset(name: "Music TV", url: "https://iptv-org.github.io/iptv/categories/music.m3u")
    ]

    var groups: [String] {
        var set = Set(channels.map { $0.group })
        let list = Array(set).sorted()
        return ["All", "★ Favorites"] + list
    }

    var filteredChannels: [Channel] {
        channels.filter { ch in
            let matchesGroup: Bool
            if selectedGroup == "★ Favorites" {
                matchesGroup = favoriteChannelIds.contains(ch.id)
            } else if selectedGroup == "All" {
                matchesGroup = true
            } else {
                matchesGroup = (ch.group == selectedGroup)
            }

            let matchesSearch = searchQuery.isEmpty || ch.name.localizedCaseInsensitiveContains(searchQuery)
            return matchesGroup && matchesSearch
        }
    }

    var body: some View {
        TabView(selection: $activeTab) {
            // TAB 1: Live TV
            NavigationStack {
                VStack(spacing: 0) {
                    // Category Chips Bar
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
                                            .background(selectedGroup == grp ? (grp == "★ Favorites" ? Color.orange : Color.blue) : Color(white: 0.15))
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
                        VStack(spacing: 12) {
                            ProgressView()
                                .tint(.white)
                            Text("Loading Channels & Guide...")
                                .foregroundColor(.gray)
                        }
                        Spacer()
                    } else if filteredChannels.isEmpty {
                        Spacer()
                        VStack(spacing: 12) {
                            Image(systemName: selectedGroup == "★ Favorites" ? "star.slash" : "tv.slash")
                                .font(.system(size: 48))
                                .foregroundColor(.gray)
                            Text(selectedGroup == "★ Favorites" ? "No Favorites Added Yet" : "No Channels Found")
                                .font(.title3)
                                .foregroundColor(.gray)
                            if selectedGroup != "★ Favorites" {
                                Button("Add Playlist Link") {
                                    showPlaylistSheet = true
                                }
                                .buttonStyle(.borderedProminent)
                            }
                        }
                        Spacer()
                    } else {
                        List(Array(filteredChannels.enumerated()), id: \.element.id) { index, channel in
                            HStack(spacing: 16) {
                                Button(action: {
                                    if let originalIndex = channels.firstIndex(where: { $0.id == channel.id }) {
                                        selectedIndex = originalIndex
                                    }
                                }) {
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
                                    }
                                }
                                .buttonStyle(.plain)

                                Spacer()

                                // Favorite Star Button
                                Button(action: { toggleFavorite(id: channel.id) }) {
                                    Image(systemName: favoriteChannelIds.contains(channel.id) ? "star.fill" : "star")
                                        .font(.title3)
                                        .foregroundColor(favoriteChannelIds.contains(channel.id) ? .orange : Color(white: 0.35))
                                        .padding(8)
                                }
                                .buttonStyle(.plain)
                            }
                            .padding(.vertical, 4)
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
                            Image(systemName: "list.bullet.rectangle")
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
        .sheet(isPresented: Binding(
            get: { selectedIndex != nil },
            set: { if !$0 { selectedIndex = nil } }
        )) {
            if let idx = selectedIndex {
                PlayerView(channels: channels, currentIndex: idx)
                    .ignoresSafeArea()
            }
        }
        .sheet(isPresented: $showPlaylistSheet) {
            NavigationStack {
                VStack(spacing: 20) {
                    Text("Select or Add M3U Playlist")
                        .font(.headline)

                    // Presets
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Recommended Playlists:")
                            .font(.caption)
                            .foregroundColor(.gray)

                        ForEach(presets) { preset in
                            Button(action: {
                                playlistUrl = preset.url
                                showPlaylistSheet = false
                                loadPlaylist()
                            }) {
                                HStack {
                                    Text(preset.name)
                                        .foregroundColor(.white)
                                        .font(.subheadline)
                                    Spacer()
                                    if playlistUrl == preset.url {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                                .padding()
                                .background(Color(white: 0.12))
                                .cornerRadius(10)
                            }
                        }
                    }
                    .padding(.horizontal)

                    Divider().background(Color.gray)

                    Text("Custom Playlist URL:")
                        .font(.caption)
                        .foregroundColor(.gray)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    TextField("https://example.com/playlist.m3u", text: $playlistUrl)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .padding(.horizontal)

                    Button("Load Custom Playlist") {
                        showPlaylistSheet = false
                        loadPlaylist()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(playlistUrl.isEmpty)

                    Spacer()
                }
                .padding(.top, 24)
                .navigationTitle("Playlists")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showPlaylistSheet = false }
                    }
                }
            }
            .presentationDetents([.fraction(0.8)])
        }
        .onAppear {
            if channels.isEmpty {
                loadPlaylist()
            }
        }
    }

    private func toggleFavorite(id: String) {
        var current = favoriteChannelIds
        if current.contains(id) {
            current.remove(id)
        } else {
            current.insert(id)
        }
        favoriteChannelIdsRaw = current.joined(separator: ",")
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
