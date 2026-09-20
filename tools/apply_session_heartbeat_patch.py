from pathlib import Path


def replace_exact(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new), encoding="utf-8")


replace_exact(
    "app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt",
    '''    fun observeRoom(id: String, intervalMs: Long = 700): Flow<GameRoomDto> = flow {
        var previous: GameRoomDto? = null
        while (currentCoroutineContext().isActive) {
            val result = runCatching { getRoom(id) }
            if (result.isSuccess) {
                val next = result.getOrThrow()
                if (next != previous) {
                    emit(next)
                    previous = next
                }
                delay(intervalMs)
            } else {
                delay(1200)
            }
        }
    }''',
    '''    fun observeRoom(id: String, intervalMs: Long = 700): Flow<GameRoomDto> = flow {
        var previous: GameRoomDto? = null
        var lastHeartbeatAt = 0L
        while (currentCoroutineContext().isActive) {
            val now = System.nanoTime()
            val heartbeatDue = now - lastHeartbeatAt >= 4_000_000_000L
            val result = if (heartbeatDue) {
                lastHeartbeatAt = now
                runCatching { heartbeatRoom(id) }
            } else {
                runCatching { getRoom(id) }
            }
            if (result.isSuccess) {
                val next = result.getOrThrow()
                if (next != previous) {
                    emit(next)
                    previous = next
                }
                delay(intervalMs)
            } else {
                delay(1200)
            }
        }
    }''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/MainActivity.kt",
    '''        private var startupSessionPolicyApplied = false
    }

    private var passwordRecoveryRequested by mutableStateOf(false)''',
    '''        private var startupSessionPolicyApplied = false
        private const val PASSWORD_RECOVERY_STATE_KEY = "password_recovery_requested"
    }

    private var passwordRecoveryRequested by mutableStateOf(false)''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/MainActivity.kt",
    '''    override fun onDestroy() {
        runCatching { SonHarfBackgroundMusic.release() }
        runCatching { SonHarfSoundFx.release() }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nothing optional is allowed to prevent the first frame from being rendered.''',
    '''    override fun onDestroy() {
        runCatching { SonHarfBackgroundMusic.release() }
        runCatching { SonHarfSoundFx.release() }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PASSWORD_RECOVERY_STATE_KEY, passwordRecoveryRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordRecoveryRequested = savedInstanceState?.getBoolean(PASSWORD_RECOVERY_STATE_KEY) == true

        // Nothing optional is allowed to prevent the first frame from being rendered.''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/MainActivity.kt",
    '''                    passwordRecoveryRequested = passwordRecoveryRequested,
                    onPasswordRecoveryFinished = { passwordRecoveryRequested = false },''',
    '''                    passwordRecoveryRequested = passwordRecoveryRequested,
                    onPasswordRecoveryFinished = {
                        passwordRecoveryRequested = false
                        if (isPasswordRecoveryDeepLink(intent)) {
                            setIntent(Intent(intent).apply { data = null })
                        }
                    },''',
)

print("session/heartbeat patch applied")
