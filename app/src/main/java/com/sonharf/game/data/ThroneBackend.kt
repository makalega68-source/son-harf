package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable internal data class TournamentStanding(val rank:Int=0,@SerialName("user_id") val userId:String="",val name:String="",val score:Int=0,val stages:Int=0)
@Serializable internal data class TournamentStage(val stage:Int=0,val finished:Boolean=false,val xp:Int=0)
@Serializable internal data class AtelierTournament(
 @SerialName("server_time") val serverTime:String="",@SerialName("event_start") val eventStart:String="",
 @SerialName("next_start") val nextStart:String="",val active:Boolean=false,val stage:Int=1,
 @SerialName("stage_ends") val stageEnds:String="",val multiplier:Double=1.5,
 val rows:List<TournamentStanding> = emptyList(),val winners:List<TournamentStanding> = emptyList(),
 @SerialName("my_stages") val myStages:List<TournamentStage> = emptyList(),
)
@Serializable internal data class TournamentEntry(@SerialName("event_start") val eventStart:String,val stage:Int,val seconds:Int,@SerialName("seed_key") val seedKey:String,val multiplier:Double)
@Serializable internal data class TournamentResult(val xp:Int=0,val score:Int=0,@SerialName("already_saved") val alreadySaved:Boolean=false)
@Serializable internal data class ThroneRow(val rank:Int=0,@SerialName("user_id") val userId:String="",val name:String="",val xp:Long=0,@SerialName("avatar_path") val avatarPath:String?=null,val gender:String?=null,@SerialName("avatar_visibility") val avatarVisibility:String="public")
@Serializable internal data class ThroneMe(val rank:Int=0,val xp:Long=0)
@Serializable internal data class ThroneOwner(val name:String="",val xp:Long=0,@SerialName("week_start") val weekStart:String="",@SerialName("user_id") val userId:String="",@SerialName("expires_at") val expiresAt:String="",@SerialName("avatar_path") val avatarPath:String?=null,val gender:String?=null,@SerialName("avatar_visibility") val avatarVisibility:String="public")
@Serializable internal data class ThroneMission(val game:String,val id:String,val target:Int,val reward:Int,val progress:Int=0,val awarded:Boolean=false)
@Serializable internal data class ThroneGameTotal(val game:String,val xp:Long=0,val rounds:Int=0,val wins:Int=0,val tasks:Int=0)
@Serializable internal data class ThroneWeek(
 @SerialName("server_time") val serverTime:String="",@SerialName("week_start") val weekStart:String="",@SerialName("reset_at") val resetAt:String="",
 val rows:List<ThroneRow> = emptyList(),val me:ThroneMe=ThroneMe(),
 @SerialName("previous_owner") val previousOwner:ThroneOwner?=null,
 val missions:List<ThroneMission> = emptyList(),val breakdown:List<ThroneGameTotal> = emptyList(),
)
internal object ThroneBackend {
 suspend fun tournament():AtelierTournament=SupabaseProvider.client.postgrest.rpc("get_atelier_tournament_v1").decodeAs()
 suspend fun week():ThroneWeek=SupabaseProvider.client.postgrest.rpc("get_throne_week_v1").decodeAs()
 suspend fun start(language:String):TournamentEntry=SupabaseProvider.client.postgrest.rpc("start_atelier_tournament_v1",buildJsonObject{put("p_language",language)}).decodeAs()
 suspend fun finish(entry:TournamentEntry,score:Int,words:List<String>,tasks:Int):TournamentResult=SupabaseProvider.client.postgrest.rpc("finish_atelier_tournament_v1",buildJsonObject{
  put("p_event",entry.eventStart);put("p_stage",entry.stage);put("p_score",score);put("p_words",words.size);put("p_tasks",tasks);put("p_transcript",buildJsonArray{words.forEach{add(it)}})
 }).decodeAs()
}
