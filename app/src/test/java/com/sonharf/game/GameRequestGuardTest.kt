package com.sonharf.game
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class GameRequestGuardTest {
 @Test fun `timeout returns failure so input can unlock`()=runBlocking {
  assertTrue(gameRequestResult(30) { delay(1000); 1 }.exceptionOrNull() is IOException)
  assertEquals(2,gameRequestResult { 2 }.getOrThrow())
 }
 @Test fun `screen cancellation propagates instead of becoming a stale response`()=runBlocking {
  val started=CompletableDeferred<Unit>();var returned=false
  val job=launch { gameRequestResult { started.complete(Unit);delay(1000) };returned=true }
  started.await();job.cancelAndJoin();assertFalse(returned)
 }
}
