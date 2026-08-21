import Foundation

struct AuthResponse: Decodable {
    let accessToken: String
    let user: AuthUser
    enum CodingKeys: String, CodingKey { case accessToken = "access_token", user }
}

struct AuthUser: Decodable { let id: String }

struct Profile: Codable, Identifiable {
    let id: String
    let displayName: String
    let avatarUrl: String?
    let isVip: Bool
    let diamonds: Int
    let wins: Int
    let losses: Int
    enum CodingKeys: String, CodingKey {
        case id, diamonds, wins, losses
        case displayName = "display_name"
        case avatarUrl = "avatar_url"
        case isVip = "is_vip"
    }
}

struct GameRoom: Codable, Identifiable, Equatable {
    let id: String
    let code: String
    let hostId: String
    let guestId: String?
    let status: String
    let language: String
    let hostScore: Int
    let guestScore: Int
    let currentPlayerId: String?
    let turnDeadline: String?
    let roundNo: Int
    let roundWordCount: Int
    let hostRounds: Int
    let guestRounds: Int
    let isBot: Bool
    let botName: String?
    let botTurn: Bool
    let gameMode: String
    let lastEvent: String?
    enum CodingKeys: String, CodingKey {
        case id, code, status, language
        case hostId = "host_id", guestId = "guest_id"
        case hostScore = "host_score", guestScore = "guest_score"
        case currentPlayerId = "current_player_id", turnDeadline = "turn_deadline"
        case roundNo = "round_no", roundWordCount = "round_word_count"
        case hostRounds = "host_rounds", guestRounds = "guest_rounds"
        case isBot = "is_bot", botName = "bot_name", botTurn = "bot_turn"
        case gameMode = "game_mode", lastEvent = "last_event"
    }
}

struct GameWord: Codable, Identifiable {
    let id: Int
    let roomId: String
    let playerId: String?
    let word: String
    let normalizedWord: String
    let isBot: Bool
    enum CodingKeys: String, CodingKey {
        case id, word
        case roomId = "room_id", playerId = "player_id"
        case normalizedWord = "normalized_word", isBot = "is_bot"
    }
}

struct ChatMessage: Codable, Identifiable {
    let id: Int
    let roomId: String
    let senderId: String
    let body: String
    enum CodingKeys: String, CodingKey { case id, body; case roomId = "room_id", senderId = "sender_id" }
}

struct QueueRow: Codable {
    let userId: String
    let roomId: String?
    enum CodingKeys: String, CodingKey { case userId = "user_id", roomId = "room_id" }
}
