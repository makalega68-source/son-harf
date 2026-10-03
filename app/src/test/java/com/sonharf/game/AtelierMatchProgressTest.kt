package com.sonharf.game
import org.junit.Assert.assertEquals
import org.junit.Test
class AtelierMatchProgressTest {
 @Test fun `long races have three phases and sufficient task sets`() {
  assertEquals(8,KelimeAtolyesiEngine.setsFor(180));assertEquals(12,KelimeAtolyesiEngine.setsFor(300))
  assertEquals(AtelierMatchPhase.WARMUP,atelierMatchPhase(180,180))
  assertEquals(AtelierMatchPhase.STRATEGY,atelierMatchPhase(180,120))
  assertEquals(AtelierMatchPhase.FINAL,atelierMatchPhase(180,60))
 }
 @Test fun `deadline catches up after a stalled frame and never goes negative`() {
  assertEquals(10,atelierRemainingSeconds(10000,0));assertEquals(1,atelierRemainingSeconds(10000,9999));assertEquals(0,atelierRemainingSeconds(10000,11000))
 }
}
