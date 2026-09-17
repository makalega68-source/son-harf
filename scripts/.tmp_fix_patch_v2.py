from pathlib import Path

p = Path('scripts/.tmp_apply_admin_help_package.py')
s = p.read_text(encoding='utf-8')

old = """replace_once(\n    app,\n    '''                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\\n                    )''',\n    '''                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\\n                        isAdmin = isAdmin,\\n                        onAdmin = { destination = PremiumDestination.ADMIN },\\n                    )'''\n)"""
new = """text = read(app)\nhome_route = '''                    PremiumDestination.HOME -> PremiumHomeScreen(\\n                        backend = backend,\\n                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },\\n                        onCompete = { destination = PremiumDestination.COMPETE },\\n                        onProfile = { destination = PremiumDestination.PROFILE },\\n                        onSocial = { destination = PremiumDestination.SOCIAL },\\n                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },\\n                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\\n                    )'''\nhome_route_admin = '''                    PremiumDestination.HOME -> PremiumHomeScreen(\\n                        backend = backend,\\n                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },\\n                        onCompete = { destination = PremiumDestination.COMPETE },\\n                        onProfile = { destination = PremiumDestination.PROFILE },\\n                        onSocial = { destination = PremiumDestination.SOCIAL },\\n                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },\\n                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },\\n                        isAdmin = isAdmin,\\n                        onAdmin = { destination = PremiumDestination.ADMIN },\\n                    )'''\nif text.count(home_route) != 1:\n    raise SystemExit(f'{app}: expected exactly one HOME route, got {text.count(home_route)}')\nwrite(app, text.replace(home_route, home_route_admin, 1))"""
if s.count(old) != 1:
    raise SystemExit(f'home replacement patch target count={s.count(old)}')
s = s.replace(old, new, 1)

old_access = '''@Serializable\ndata class AdminAccessDto(\n    val authorized: Boolean = false,\n    @SerialName("admin_role") val adminRole: String = "",\n)'''
new_access = '''@Serializable\ndata class AdminAccessDto(\n    val authorized: Boolean = false,\n    @SerialName("admin_role") val adminRole: String = "",\n    @SerialName("lifetime_vip") val lifetimeVip: Boolean = false,\n    @SerialName("unlimited_diamonds") val unlimitedDiamonds: Boolean = false,\n    @SerialName("unlimited_son_coin") val unlimitedSonCoin: Boolean = false,\n)'''
if s.count(old_access) != 1:
    raise SystemExit(f'AdminAccessDto patch target count={s.count(old_access)}')
s = s.replace(old_access, new_access, 1)

p.write_text(s, encoding='utf-8')
print('temporary patch script corrected (v2)')
