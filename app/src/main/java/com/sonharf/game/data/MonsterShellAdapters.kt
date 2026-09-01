package com.sonharf.game.data

/** Minimal adapter used by the new Monster home shell. */
suspend fun OnlineGameBackend.getLeaderboardV2(limit: Int): List<LeaderboardV2Row> =
    getLeaderboardV2(language = "tr", period = "all", limit = limit)
