import Foundation

struct Channel: Identifiable, Hashable {
    let id: String
    let name: String
    let streamUrl: String
    let logoUrl: String?
    let group: String
    let tvgId: String?
    var currentProgram: String? = nil
}

enum M3UParser {
    static func parse(content: String) -> [Channel] {
        var channels: [Channel] = []
        let lines = content.components(separatedBy: .newlines)

        var currentName = ""
        var currentLogo: String?
        var currentGroup = "General"
        var currentTvgId: String?

        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)

            if trimmed.hasPrefix("#EXTINF") {
                currentLogo = extractAttribute(named: "tvg-logo", from: trimmed)
                currentGroup = extractAttribute(named: "group-title", from: trimmed) ?? "General"
                currentTvgId = extractAttribute(named: "tvg-id", from: trimmed)

                if let commaIndex = trimmed.lastIndex(of: ",") {
                    let afterComma = trimmed[trimmed.index(after: commaIndex)...]
                    currentName = afterComma.trimmingCharacters(in: .whitespaces)
                } else {
                    currentName = "Channel \(channels.count + 1)"
                }
            } else if trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") {
                if !currentName.isEmpty {
                    channels.append(
                        Channel(
                            id: currentTvgId ?? UUID().uuidString,
                            name: currentName,
                            streamUrl: trimmed,
                            logoUrl: currentLogo,
                            group: currentGroup,
                            tvgId: currentTvgId
                        )
                    )
                }
                currentName = ""
                currentLogo = nil
                currentGroup = "General"
                currentTvgId = nil
            }
        }
        return channels
    }
    }

    private static func extractAttribute(named name: String, from line: String) -> String? {
        let pattern = "\(name)=\"([^\"]*)\""
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else { return nil }
        let nsLine = line as NSString
        let matches = regex.matches(in: line, options: [], range: NSRange(location: 0, length: nsLine.length))
        guard let firstMatch = matches.first, firstMatch.numberOfRanges > 1 else { return nil }
        let range = firstMatch.range(at: 1)
        return nsLine.substring(with: range)
    }

    static func fetch(from urlString: String) async throws -> [Channel] {
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        let (data, _) = try await URLSession.shared.data(from: url)
        guard let content = String(data: data, encoding: .utf8) ?? String(data: data, encoding: .ascii) else {
            throw URLError(.cannotDecodeContentData)
        }
        return parse(content: content)
    }
}
