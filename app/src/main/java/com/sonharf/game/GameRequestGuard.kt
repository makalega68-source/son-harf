package com.sonharf.game

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

/** A stalled server request releases input; leaving the screen cancels it without stale UI writes. */
internal suspend fun <T> gameRequestResult(
    timeoutMillis: Long = 12_000L,
    block: suspend () -> T,
): Result<T> = try {
    Result.success(withTimeout(timeoutMillis) { block() })
} catch (error: TimeoutCancellationException) {
    currentCoroutineContext().ensureActive()
    Result.failure(IOException("network_timeout", error))
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
