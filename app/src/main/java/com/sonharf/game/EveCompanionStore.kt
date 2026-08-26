package com.sonharf.game

import android.content.Context
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

internal data class EveFood(
    val id: String,
    val titleTr: String,
    val emoji: String,
    val price: Int,
    val affection: Int,
)

internal data class EveVitalSnapshot(
    val happiness: Int,
    val fullness: Int,
    val energy: Int,
)

internal data class EveProgressSnapshot(
    val level: Int,
    val xp: Int,
    val xpToNextLevel: Int,
    val gold: Int,
    val diamonds: Int,
    val dailyFeedCount: Int,
    val maxDailyFeed: Int,
    val dailyPetCount: Int,
    val maxDailyPet: Int,
)

internal object EveCompanionRules {
    val foods = listOf(
        EveFood("biscuit", "Orman Bisküvisi", "🍪", 8, 5),
        EveFood("apple", "Tatlı Elma", "🍎", 12, 8),
        EveFood("berries", "Işık Meyveleri", "🫐", 18, 12),
    )

    const val STARTER_LEAVES = 40
    const val DAILY_GIFT_LEAVES = 18
    const val BASE_LEVEL_XP = 100
    const val STARTER_FRIENDSHIP_LEVEL = 3
    const val MAX_DAILY_FEED = 5
    const val MAX_DAILY_PET = 10
    const val PET_XP = 10
    const val FEED_XP = 25

    val featureUnlocks = linkedMapOf(
        10 to "Kişisel Giydirme & Kostüm Odası",
        20 to "Akıllı Soru İpucu Radarı (Ekstra %20 Netlik)",
        30 to "Özel Mini Oyunlar (Bonus XP Alanı)",
        40 to "Gelişmiş Sohbet & Günlük Görevler",
        50 to "Efsanevi Yoldaş Rozeti & Altın Çark",
    )

    val styleIds = listOf("default_white", "leaf_charm", "forest_crown", "cozy_scarf")
    val roomIds = listOf("enchanted_forest", "cozy_nest", "starlight_grove")

    fun xpTarget(level: Int): Int =
        (BASE_LEVEL_XP * level.coerceAtLeast(1).toDouble().pow(1.25)).toInt().coerceAtLeast(BASE_LEVEL_XP)

    fun levelRewardGold(level: Int): Int = level.coerceAtLeast(1) * 50

    fun levelRewardDiamonds(level: Int): Int = floor(level.coerceAtLeast(1) / 5.0).toInt() * 5 + 1
}

/**
 * Local companion progression and care state.
 *
 * The original `affection` and `friendship_level` preference keys are intentionally retained so
 * existing installs migrate without losing progress. `affection` now represents XP inside the
 * current dynamic level target instead of a fixed 0..99 meter.
 */
internal class EveCompanionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("son_harf_eve_companion", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString("name", "Eve")?.ifBlank { "Eve" } ?: "Eve"
        set(value) {
            prefs.edit().putString("name", value.trim().take(18).ifBlank { "Eve" }).apply()
        }

    var leaves: Int
        get() = prefs.getInt("leaves", EveCompanionRules.STARTER_LEAVES)
        private set(value) {
            prefs.edit().putInt("leaves", value.coerceAtLeast(0)).apply()
        }

    var friendshipLevel: Int
        get() = prefs.getInt("friendship_level", EveCompanionRules.STARTER_FRIENDSHIP_LEVEL).coerceAtLeast(1)
        private set(value) {
            prefs.edit().putInt("friendship_level", value.coerceAtLeast(1)).apply()
        }

    val xpToNextLevel: Int
        get() = EveCompanionRules.xpTarget(friendshipLevel)

    /** XP progress inside the current dynamic companion level. */
    var affection: Int
        get() = prefs.getInt("affection", 20).coerceIn(0, (xpToNextLevel - 1).coerceAtLeast(0))
        private set(value) {
            prefs.edit().putInt("affection", value.coerceIn(0, (xpToNextLevel - 1).coerceAtLeast(0))).apply()
        }

    var companionGold: Int
        get() = prefs.getInt("companion_gold", 0).coerceAtLeast(0)
        private set(value) {
            prefs.edit().putInt("companion_gold", value.coerceAtLeast(0)).apply()
        }

    var companionDiamonds: Int
        get() = prefs.getInt("companion_diamonds", 0).coerceAtLeast(0)
        private set(value) {
            prefs.edit().putInt("companion_diamonds", value.coerceAtLeast(0)).apply()
        }

    var happiness: Int
        get() = prefs.getInt("happiness", 82).coerceIn(0, 100)
        private set(value) {
            prefs.edit().putInt("happiness", value.coerceIn(0, 100)).apply()
        }

    var fullness: Int
        get() = prefs.getInt("fullness", 68).coerceIn(0, 100)
        private set(value) {
            prefs.edit().putInt("fullness", value.coerceIn(0, 100)).apply()
        }

    var energy: Int
        get() = prefs.getInt("energy", 90).coerceIn(0, 100)
        private set(value) {
            prefs.edit().putInt("energy", value.coerceIn(0, 100)).apply()
        }

    var selectedStyle: String
        get() = prefs.getString("selected_style", "default_white")
            ?.takeIf(EveCompanionRules.styleIds::contains) ?: "default_white"
        private set(value) {
            prefs.edit().putString("selected_style", value).apply()
        }

    var selectedRoom: String
        get() = prefs.getString("selected_room", "enchanted_forest")
            ?.takeIf(EveCompanionRules.roomIds::contains) ?: "enchanted_forest"
        private set(value) {
            prefs.edit().putString("selected_room", value).apply()
        }

    private fun resetDailyCountersIfNeeded(today: LocalDate = LocalDate.now()) {
        val currentDay = today.toString()
        if (prefs.getString("daily_action_day", "") == currentDay) return
        prefs.edit()
            .putString("daily_action_day", currentDay)
            .putInt("daily_feed_count", 0)
            .putInt("daily_pet_count", 0)
            .apply()
    }

    val dailyFeedCount: Int
        get() {
            resetDailyCountersIfNeeded()
            return prefs.getInt("daily_feed_count", 0).coerceIn(0, EveCompanionRules.MAX_DAILY_FEED)
        }

    val dailyPetCount: Int
        get() {
            resetDailyCountersIfNeeded()
            return prefs.getInt("daily_pet_count", 0).coerceIn(0, EveCompanionRules.MAX_DAILY_PET)
        }

    fun canFeedToday(): Boolean = dailyFeedCount < EveCompanionRules.MAX_DAILY_FEED

    fun canPetToday(): Boolean = dailyPetCount < EveCompanionRules.MAX_DAILY_PET

    fun inventory(foodId: String): Int = prefs.getInt("food_$foodId", 0)

    fun vitals(): EveVitalSnapshot = EveVitalSnapshot(happiness, fullness, energy)

    fun progress(): EveProgressSnapshot = EveProgressSnapshot(
        level = friendshipLevel,
        xp = affection,
        xpToNextLevel = xpToNextLevel,
        gold = companionGold,
        diamonds = companionDiamonds,
        dailyFeedCount = dailyFeedCount,
        maxDailyFeed = EveCompanionRules.MAX_DAILY_FEED,
        dailyPetCount = dailyPetCount,
        maxDailyPet = EveCompanionRules.MAX_DAILY_PET,
    )

    /**
     * Adds progression XP and performs as many level-ups as necessary.
     * Returns the XP amount accepted, preserving the old addFriendship(Int) API semantics.
     */
    fun addFriendship(points: Int): Int {
        if (points <= 0) return 0
        resetDailyCountersIfNeeded()

        var pending = affection + points
        var currentTarget = xpToNextLevel
        while (pending >= currentTarget) {
            pending -= currentTarget
            friendshipLevel += 1
            companionGold += EveCompanionRules.levelRewardGold(friendshipLevel)
            companionDiamonds += EveCompanionRules.levelRewardDiamonds(friendshipLevel)
            currentTarget = xpToNextLevel
        }
        affection = pending
        return points
    }

    fun pet(): Int {
        resetDailyCountersIfNeeded()
        if (!canPetToday()) return 0
        prefs.edit().putInt("daily_pet_count", dailyPetCount + 1).apply()
        happiness = happiness + 7
        energy = energy + 1
        return addFriendship(EveCompanionRules.PET_XP)
    }

    fun chatBond(): Int {
        happiness = happiness + 3
        return addFriendship(2)
    }

    fun buy(food: EveFood): Boolean {
        if (leaves < food.price) return false
        leaves -= food.price
        prefs.edit().putInt("food_${food.id}", inventory(food.id) + 1).apply()
        return true
    }

    fun feed(food: EveFood): Boolean {
        resetDailyCountersIfNeeded()
        if (!canFeedToday()) return false
        val count = inventory(food.id)
        if (count <= 0) return false

        prefs.edit()
            .putInt("food_${food.id}", count - 1)
            .putInt("daily_feed_count", dailyFeedCount + 1)
            .apply()
        fullness = fullness + when (food.id) {
            "berries" -> 18
            "apple" -> 14
            else -> 10
        }
        happiness = happiness + 4
        energy = energy + 2
        addFriendship(EveCompanionRules.FEED_XP)
        return true
    }

    /** One-tap feed used by the main living-room action. Buys an apple when needed. */
    fun quickFeed(): Int {
        if (!canFeedToday()) return 0
        val apple = EveCompanionRules.foods.first { it.id == "apple" }
        if (inventory(apple.id) <= 0 && !buy(apple)) return 0
        return if (feed(apple)) EveCompanionRules.FEED_XP else 0
    }

    /** XP hook for companion mini-games or other non-ranked mascot activities. */
    fun playGame(score: Int): Int {
        val earnedXp = maxOf(20, (score * 1.5).toInt())
        addFriendship(earnedXp)
        return earnedXp
    }

    fun unlockedFeatures(): List<String> =
        EveCompanionRules.featureUnlocks.filterKeys { friendshipLevel >= it }.values.toList()

    fun featureUnlockedAtCurrentLevel(): String? = EveCompanionRules.featureUnlocks[friendshipLevel]
        ?: if (friendshipLevel % 10 == 0) "10 Seviye Katı Bonusu" else null

    fun greeting(random: Random = Random.Default): String {
        val options = when {
            friendshipLevel < 10 -> listOf(
                "Agugu! Hoş geldin!",
                "Miyav! Geldiin!",
                "Seni gördüm... Mutlu!",
            )
            friendshipLevel < 30 -> listOf(
                "Hoş geldin! Seni gerçekten çok özlemiştim.",
                "Sonunda geldin! Bugün neler oynayacağız?",
                "Bak seni beklerken enerjimi topladım!",
            )
            else -> listOf(
                "Hoş geldin şampiyon! Yokluğunda buralar çok sessizdi.",
                "Gelişinle enerjim tavan yaptı, bugün hangi rekoru kırıyoruz?",
                "Harika bir gün! Seninle seviye atlamak için sabırsızlanıyorum.",
            )
        }
        return options[random.nextInt(options.size)]
    }

    /**
     * Progressive hint wording. This is a companion utility only; it is intentionally not wired
     * into ranked match scoring so the competitive core remains fair.
     */
    fun getHint(category: String?, firstLetter: String?, directHint: String?): String = when {
        friendshipLevel < 10 -> "Hımm... Bilmiyorum ki..."
        friendshipLevel < 25 -> "Sadece şunu hatırlıyorum: Kategori sanırım '${category.orEmpty()}'!"
        friendshipLevel < 45 -> "İlk harfi fısıldayabilirim: '${firstLetter.orEmpty()}' ile başlıyor!"
        else -> "Bunu kesin biliyorum: ${directHint.orEmpty()}"
    }

    fun selectStyle(id: String): Boolean {
        if (id !in EveCompanionRules.styleIds) return false
        selectedStyle = id
        return true
    }

    fun selectRoom(id: String): Boolean {
        if (id !in EveCompanionRules.roomIds) return false
        selectedRoom = id
        return true
    }

    fun giftAvailable(today: LocalDate = LocalDate.now()): Boolean =
        prefs.getString("last_gift_day", "") != today.toString()

    fun claimDailyGift(today: LocalDate = LocalDate.now()): Boolean {
        if (!giftAvailable(today)) return false
        leaves += EveCompanionRules.DAILY_GIFT_LEAVES
        val bonus = EveCompanionRules.foods[(today.dayOfYear + today.year) % EveCompanionRules.foods.size]
        prefs.edit()
            .putString("last_gift_day", today.toString())
            .putInt("food_${bonus.id}", inventory(bonus.id) + 1)
            .apply()
        happiness = happiness + 5
        addFriendship(4)
        return true
    }
}
