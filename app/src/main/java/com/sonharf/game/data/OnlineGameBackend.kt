package com.sonharf.game.data

import com.sonharf.game.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable data class ProfileDto(val id:String,@SerialName("display_name") val displayName:String,@SerialName("avatar_url") val avatarUrl:String?=null,@SerialName("avatar_visibility") val avatarVisibility:String="hidden",@SerialName("allow_match_chat") val allowMatchChat:Boolean=true,@SerialName("presence_status") val presenceStatus:String="offline",@SerialName("last_seen_at") val lastSeenAt:String?=null,@SerialName("chat_suspended_until") val chatSuspendedUntil:String?=null,@SerialName("is_vip") val isVip:Boolean=false,val diamonds:Int=0,val wins:Int=0,val losses:Int=0)
@Serializable data class GameRoomDto(val id:String,val code:String,@SerialName("host_id") val hostId:String,@SerialName("guest_id") val guestId:String?=null,val status:String,val language:String="tr",@SerialName("host_score") val hostScore:Int=0,@SerialName("guest_score") val guestScore:Int=0,@SerialName("host_streak") val hostStreak:Int=0,@SerialName("guest_streak") val guestStreak:Int=0,@SerialName("valid_word_count") val validWordCount:Int=0,@SerialName("final_moves_remaining") val finalMovesRemaining:Int=0,@SerialName("last_event") val lastEvent:String?=null,@SerialName("last_event_player_id") val lastEventPlayerId:String?=null,@SerialName("current_player_id") val currentPlayerId:String?=null,@SerialName("winner_id") val winnerId:String?=null,@SerialName("turn_deadline") val turnDeadline:String?=null,@SerialName("round_no") val roundNo:Int=1,@SerialName("round_word_count") val roundWordCount:Int=0,@SerialName("host_rounds") val hostRounds:Int=0,@SerialName("guest_rounds") val guestRounds:Int=0,@SerialName("rematch_of") val rematchOf:String?=null,@SerialName("host_rematch") val hostRematch:Boolean=false,@SerialName("guest_rematch") val guestRematch:Boolean=false)
@Serializable data class GameWordDto(val id:Long,@SerialName("room_id") val roomId:String,@SerialName("player_id") val playerId:String,val word:String,@SerialName("normalized_word") val normalizedWord:String,@SerialName("created_at") val createdAt:String)
@Serializable data class ChatMessageDto(val id:Long,@SerialName("room_id") val roomId:String,@SerialName("sender_id") val senderId:String,val body:String,@SerialName("created_at") val createdAt:String)
@Serializable data class TriviaQuestionDto(val id:Long,val language:String,val question:String,@SerialName("option_a") val optionA:String,@SerialName("option_b") val optionB:String,@SerialName("option_c") val optionC:String,@SerialName("option_d") val optionD:String)
@Serializable data class TriviaRoundDto(val id:String,@SerialName("room_id") val roomId:String,val milestone:Int,@SerialName("bonus_points") val bonusPoints:Int,@SerialName("question_id") val questionId:Long,@SerialName("reveal_at") val revealAt:String,@SerialName("winner_id") val winnerId:String?=null,@SerialName("resolved_at") val resolvedAt:String?=null)
@Serializable data class MatchmakingQueueDto(@SerialName("user_id") val userId:String,val language:String,val status:String,@SerialName("room_id") val roomId:String?=null)
@Serializable data class FriendshipDto(@SerialName("user_id") val userId:String,@SerialName("friend_id") val friendId:String,val status:String,@SerialName("requested_by") val requestedBy:String)
@Serializable data class GameInviteDto(val id:String,@SerialName("sender_id") val senderId:String,@SerialName("receiver_id") val receiverId:String,val language:String,val status:String,@SerialName("room_id") val roomId:String?=null,@SerialName("expires_at") val expiresAt:String)
@Serializable private data class ProfileWrite(val id:String,@SerialName("display_name") val displayName:String)
@Serializable private data class ChatWrite(@SerialName("room_id") val roomId:String,@SerialName("sender_id") val senderId:String,val body:String)

object SupabaseProvider { val configured:Boolean get()=BuildConfig.SUPABASE_URL.isNotBlank()&&BuildConfig.SUPABASE_KEY.isNotBlank(); val client:SupabaseClient by lazy { require(configured); createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY){install(Auth);install(Postgrest);install(Realtime)} } }
class OnlineGameBackend(private val supabase:SupabaseClient=SupabaseProvider.client){
 suspend fun ensurePlayer(displayName:String):ProfileDto{if(supabase.auth.currentUserOrNull()==null)supabase.auth.signInAnonymously();val id=requireNotNull(supabase.auth.currentUserOrNull()?.id);val p=supabase.from("profiles").upsert(ProfileWrite(id,displayName.trim().ifBlank{"Oyuncu"}.take(24))){select()}.decodeSingle<ProfileDto>();runCatching{setPresence("online")};return p}
 fun currentUserId():String?=supabase.auth.currentUserOrNull()?.id
 suspend fun setPresence(status:String){supabase.postgrest.rpc("set_presence",buildJsonObject{put("p_status",status)})}
 suspend fun startRandomMatchmaking(language:String){supabase.postgrest.rpc("join_random_matchmaking",buildJsonObject{put("p_language",language)})}
 suspend fun pollRandomMatchmakingRoom():GameRoomDto?{supabase.postgrest.rpc("poll_random_matchmaking");val me=currentUserId()?:return null;val q=supabase.from("matchmaking_queue").select{filter{eq("user_id",me)}}.decodeList<MatchmakingQueueDto>().firstOrNull()?:return null;return q.roomId?.let{getRoom(it)}}
 suspend fun cancelRandomMatchmaking(){supabase.postgrest.rpc("cancel_random_matchmaking")}
 suspend fun submitWord(roomId:String,word:String):GameRoomDto=supabase.postgrest.rpc("submit_word_v2",buildJsonObject{put("p_room_id",roomId);put("p_word",word.trim())}).decodeSingle()
 suspend fun claimTurnTimeout(roomId:String):GameRoomDto=supabase.postgrest.rpc("claim_turn_timeout",buildJsonObject{put("p_room_id",roomId)}).decodeSingle()
 suspend fun answerTrivia(roundId:String,answerIndex:Int):GameRoomDto=supabase.postgrest.rpc("answer_trivia_v2",buildJsonObject{put("p_round_id",roundId);put("p_answer_index",answerIndex)}).decodeSingle()
 suspend fun forfeit(roomId:String):GameRoomDto=supabase.postgrest.rpc("forfeit_room",buildJsonObject{put("p_room_id",roomId)}).decodeSingle()
 suspend fun requestRematch(roomId:String):GameRoomDto=supabase.postgrest.rpc("request_rematch",buildJsonObject{put("p_room_id",roomId)}).decodeSingle()
 suspend fun sendChat(roomId:String,text:String){val id=requireNotNull(currentUserId());val b=text.trim().take(300);require(b.isNotEmpty());supabase.from("chat_messages").insert(ChatWrite(roomId,id,b))}
 suspend fun blockUser(userId:String){supabase.postgrest.rpc("block_user",buildJsonObject{put("p_blocked_id",userId)})}
 suspend fun reportUser(userId:String,roomId:String,reason:String):Int=supabase.postgrest.rpc("report_player",buildJsonObject{put("p_reported_id",userId);put("p_reason",reason);put("p_room_id",roomId)}).decodeSingle()
 suspend fun setPhotoAccess(viewerId:String,allowed:Boolean){supabase.postgrest.rpc("set_photo_access",buildJsonObject{put("p_viewer_id",viewerId);put("p_allowed",allowed)})}
 suspend fun sendFriendRequest(friendId:String){supabase.postgrest.rpc("send_friend_request",buildJsonObject{put("p_friend_id",friendId)})}
 suspend fun respondFriendRequest(friendId:String,accept:Boolean){supabase.postgrest.rpc("respond_friend_request",buildJsonObject{put("p_friend_id",friendId);put("p_accept",accept)})}
 suspend fun getFriendships(): List<FriendshipDto> = supabase.from("friendships").select().decodeList()
 suspend fun getProfile(id:String):ProfileDto=supabase.from("profiles").select{filter{eq("id",id)}}.decodeSingle()
 suspend fun getFriends():List<Pair<FriendshipDto,ProfileDto>>{val me=currentUserId()?:return emptyList();return getFriendships().filter{it.status=="accepted"}.mapNotNull{f->runCatching{f to getProfile(if(f.userId==me)f.friendId else f.userId)}.getOrNull()}}
 suspend fun getIncomingFriendRequests():List<Pair<FriendshipDto,ProfileDto>>{val me=currentUserId()?:return emptyList();return getFriendships().filter{it.status=="pending"&&it.requestedBy!=me}.mapNotNull{f->runCatching{f to getProfile(f.requestedBy)}.getOrNull()}}
 suspend fun inviteFriend(friendId:String,language:String):GameInviteDto=supabase.postgrest.rpc("invite_friend_to_game",buildJsonObject{put("p_friend_id",friendId);put("p_language",language)}).decodeSingle()
 suspend fun getIncomingGameInvites():List<GameInviteDto>{val me=currentUserId()?:return emptyList();return supabase.from("game_invites").select{filter{eq("receiver_id",me)}}.decodeList<GameInviteDto>().filter{it.status=="pending"}}
 suspend fun respondGameInvite(inviteId:String,accept:Boolean):GameRoomDto?{supabase.postgrest.rpc("respond_game_invite",buildJsonObject{put("p_invite_id",inviteId);put("p_accept",accept)});if(!accept)return null;val inv=supabase.from("game_invites").select{filter{eq("id",inviteId)}}.decodeSingle<GameInviteDto>();return inv.roomId?.let{getRoom(it)}}
 suspend fun getRoom(id:String):GameRoomDto=supabase.from("game_rooms").select{filter{eq("id",id)}}.decodeSingle()
 suspend fun getWords(id:String): List<GameWordDto> = supabase.from("game_words").select{filter{eq("room_id",id)}}.decodeList<GameWordDto>().sortedBy{it.id}
 suspend fun getChat(id:String): List<ChatMessageDto> = supabase.from("chat_messages").select{filter{eq("room_id",id)}}.decodeList<ChatMessageDto>().sortedBy{it.id}
 suspend fun getActiveTriviaRound(id:String):TriviaRoundDto?=supabase.from("trivia_rounds").select{filter{eq("room_id",id)}}.decodeList<TriviaRoundDto>().filter{it.resolvedAt==null}.maxByOrNull{it.milestone}
 suspend fun getTriviaQuestion(id:Long):TriviaQuestionDto=supabase.from("trivia_questions").select{filter{eq("id",id)}}.decodeSingle()
 fun observeRoom(id:String,intervalMs:Long=700): Flow<GameRoomDto> = flow{var p:GameRoomDto?=null;while(currentCoroutineContext().isActive){val n=getRoom(id);if(n!=p){emit(n);p=n};delay(intervalMs)}}
 fun observeWords(id:String,intervalMs:Long=700): Flow<List<GameWordDto>> = flow{var p=listOf<GameWordDto>();while(currentCoroutineContext().isActive){val n=getWords(id);if(n!=p){emit(n);p=n};delay(intervalMs)}}
 fun observeChat(id:String,intervalMs:Long=900): Flow<List<ChatMessageDto>> = flow{var p=listOf<ChatMessageDto>();while(currentCoroutineContext().isActive){val n=getChat(id);if(n!=p){emit(n);p=n};delay(intervalMs)}}
}
