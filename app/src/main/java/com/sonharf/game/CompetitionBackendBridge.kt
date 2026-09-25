package com.sonharf.game

import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.claimPreviousWeeklyTournamentReward as dataClaimPreviousWeeklyTournamentReward
import com.sonharf.game.data.getMatchHistory as dataGetMatchHistory
import com.sonharf.game.data.getRivalHistory as dataGetRivalHistory
import com.sonharf.game.data.getWeeklyTournament as dataGetWeeklyTournament
import com.sonharf.game.data.getWeeklyTournamentHistory as dataGetWeeklyTournamentHistory
import com.sonharf.game.data.getWeeklyTournamentLeaderboard as dataGetWeeklyTournamentLeaderboard
import com.sonharf.game.data.inviteFriendToWordArena as dataInviteFriendToWordArena
import com.sonharf.game.data.joinWeeklyTournament as dataJoinWeeklyTournament

/**
 * Keeps competition UI callers in the app package while the existing server-authoritative
 * competition operations remain implemented in the data package. No RPC or business rule is
 * duplicated here; every function delegates directly to the established backend extension.
 */
internal suspend fun OnlineGameBackend.getWeeklyTournament() =
    this.dataGetWeeklyTournament()

internal suspend fun OnlineGameBackend.joinWeeklyTournament() =
    this.dataJoinWeeklyTournament()

internal suspend fun OnlineGameBackend.getWeeklyTournamentLeaderboard(limit: Int = 50) =
    this.dataGetWeeklyTournamentLeaderboard(limit)

internal suspend fun OnlineGameBackend.getWeeklyTournamentHistory(limit: Int = 12) =
    this.dataGetWeeklyTournamentHistory(limit)

internal suspend fun OnlineGameBackend.claimPreviousWeeklyTournamentReward() =
    this.dataClaimPreviousWeeklyTournamentReward()

internal suspend fun OnlineGameBackend.getRivalHistory(limit: Int = 30) =
    this.dataGetRivalHistory(limit)

internal suspend fun OnlineGameBackend.getMatchHistory(limit: Int = 30) =
    this.dataGetMatchHistory(limit)

internal suspend fun OnlineGameBackend.inviteFriendToWordArena(userId: String, language: String) =
    this.dataInviteFriendToWordArena(userId, language)
