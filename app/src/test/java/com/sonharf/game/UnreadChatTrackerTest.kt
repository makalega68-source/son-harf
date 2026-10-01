package com.sonharf.game
import org.junit.Assert.assertEquals
import org.junit.Test
class UnreadChatTrackerTest {
 @Test fun `own reply does not clear incoming unread and read survives polling`() {
  val t=UnreadChatTracker()
  assertEquals(1,t.update("a",listOf(1L to true),false))
  assertEquals(1,t.update("a",listOf(1L to true,2L to false),false))
  t.markRead("a")
  assertEquals(0,t.update("a",listOf(1L to true,2L to false),false))
  assertEquals(1,t.update("a",listOf(1L to true,2L to false,3L to true),false))
  assertEquals(1,t.update("a",listOf(1L to true),false))
 }
 @Test fun `rooms and persisted read watermarks stay independent`() {
  val t=UnreadChatTracker();t.restore("a",10)
  assertEquals(1,t.update("a",listOf(10L to true,11L to true,11L to true),false))
  assertEquals(1,t.update("b",listOf(1L to true),false))
  assertEquals(0,t.update("a",listOf(11L to true),true))
  assertEquals(11L,t.seenId("a"));assertEquals(1,t.count("b"))
 }
}
