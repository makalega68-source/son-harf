from pathlib import Path

backend=Path('app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt')
t=backend.read_text()
t=t.replace('    val rating: Int = 1000,\n)', '    val rating: Int = 1000,\n    @SerialName("bot_difficulty") val botDifficulty: String = "normal",\n)', 1)
t=t.replace('    @SerialName("bot_name") val botName: String? = null,\n    @SerialName("bot_difficulty") val botDifficulty: String = "normal",\n    @SerialName("bot_turn") val botTurn: Boolean = false,', '    @SerialName("bot_name") val botName: String? = null,\n    @SerialName("bot_turn") val botTurn: Boolean = false,', 1)
backend.write_text(t)

screen=Path('app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt')
s=screen.read_text()
s=s.replace('val difficulty = when (active.botDifficulty.lowercase()) {', 'val difficulty = when (profile?.botDifficulty?.lowercase()) {', 1)
screen.write_text(s)
