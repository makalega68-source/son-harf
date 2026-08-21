import SwiftUI

@main
struct SonHarfIOSApp: App {
    @StateObject private var session = AppSession()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(session)
                .preferredColorScheme(.light)
        }
    }
}

@MainActor
final class AppSession: ObservableObject {
    @Published var token: String? = UserDefaults.standard.string(forKey: "sonharf_token")
    @Published var userId: String? = UserDefaults.standard.string(forKey: "sonharf_user_id")
    @Published var language = UserDefaults.standard.string(forKey: "sonharf_language") ?? "tr"

    var isAuthenticated: Bool { token != nil && userId != nil }

    func save(token: String, userId: String) {
        self.token = token
        self.userId = userId
        UserDefaults.standard.set(token, forKey: "sonharf_token")
        UserDefaults.standard.set(userId, forKey: "sonharf_user_id")
    }

    func signOut() {
        token = nil
        userId = nil
        UserDefaults.standard.removeObject(forKey: "sonharf_token")
        UserDefaults.standard.removeObject(forKey: "sonharf_user_id")
    }

    func setLanguage(_ value: String) {
        language = value == "en" ? "en" : "tr"
        UserDefaults.standard.set(language, forKey: "sonharf_language")
    }
}
