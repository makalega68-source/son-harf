package com.sonharf.game

import java.time.Instant
import java.time.ZoneId

internal fun tournamentTimeMillis(value:String):Long = runCatching { com.sonharf.game.data.requireServerInstant(value).toEpochMilli() }.getOrDefault(0L)
internal fun tournamentClockText(target:Long,now:Long):String {
 val seconds=((target-now).coerceAtLeast(0)+999)/1000
 return "%02d:%02d:%02d".format(java.util.Locale.ROOT,seconds/3600,seconds/60%60,seconds%60)
}
internal fun tournamentNextRegular(now:Long):Long {
 val local=Instant.ofEpochMilli(now).atZone(ZoneId.of("Europe/Istanbul"))
 val next=local.withMinute(0).withSecond(0).withNano(0).plusHours(if(local.hour%2==0)2 else 1)
 return next.toInstant().toEpochMilli()
}
