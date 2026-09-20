from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

replace_once(
    "app/src/main/java/com/sonharf/game/SonHarfPreferences.kt",
    'fun rememberLogin(context: Context): Boolean = prefs(context).getBoolean(REMEMBER_LOGIN, true)',
    'fun rememberLogin(context: Context): Boolean = prefs(context).getBoolean(REMEMBER_LOGIN, false)',
)

replace_once(
    "app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt",
    '''                            runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.signOut() }
                            SonHarfPreferences.setRememberLogin(context, false)
                            logoutDialog = false''',
    '''                            runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.signOut() }
                            RememberedCredentialVault.clear(context)
                            SonHarfPreferences.setRememberLogin(context, false)
                            logoutDialog = false''',
)

replace_once(
    "app/src/main/java/com/sonharf/game/FinalProfileScreen.kt",
    '''                                .onSuccess {
                                    showDeleteDialog = false
                                    (context as? Activity)?.recreate()
                                }''',
    '''                                .onSuccess {
                                    RememberedCredentialVault.clear(context)
                                    SonHarfPreferences.setRememberLogin(context, false)
                                    showDeleteDialog = false
                                    (context as? Activity)?.recreate()
                                }''',
)

print("account session cleanup patch applied")
