package com.sonharf.game

import android.content.Context
import java.time.LocalDate

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

internal object EveCompanionRules {
    val foods = listOf(
        EveFood("biscuit", "Orman Bisküvisi", "🍪", 8, 5),
        EveFood("apple", "Tatlı Elma", "🍎", 12, 8),
        EveFood("berries", "Işık Meyveleri", "🫐", 18, 12),
    )

    const val STARTER_LEAVES = 40
    const val DAILY_GIFT_LEAVES = 18
    const val FRIENDSHIP_TARGET = 100
    const val STARTER_FRIENDSHIP_LEVEL = 3

    val styleIds = listOf("default_white", "leaf_charm", "forest_crown", "cozy_scarf")
    val roomIds = listOf("enchanted_forest", "cozy_nest", "starlight_grove")
}

/**
 * Small, local companion state. Existing installs keep the old affection key as friendship XP,
 * so this upgrade is backward compatible with the first Eve build.
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

    /** 0..99 progress inside the current friendship level. */
    var affection: Int
        get() = prefs.getInt("affection", 20).coerceIn(0, EveCompanionRules.FRIENDSHIP_TARGET - 1)
        private set(value) {
            prefs.edit().putInt("affection", value.coerceIn(0, EveCompanionRules.FRIENDSHIP_TARGET - 1)).apply()
        }

    var friendshipLevel: Int
        get() = prefs.getInt("friendship_level", EveCompanionRules.STARTER_FRIENDSHIP_LEVEL).coerceAtLeast(1)
        private set(value) {
            prefs.edit().putInt("friendship_level", value.coerceAtLeast(1)).apply()
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

    fun inventory(foodId: String): Int = prefs.getInt("food_$foodId", 0)

    fun vitals(): EveVitalSnapshot = EveVitalSnapshot(happiness, fullness, energy)

    /** Returns how many friendship points were actually awarded. */
    fun addFriendship(points: Int): Int {
        if (points <= 0) return 0
        val total = affection + points
        val levelsGained = total / EveCompanionRules.FRIENDSHIP_TARGET
        if (levelsGained > 0) friendshipLevel += levelsGained
        affection = total % EveCompanionRules.FRIENDSHIP_TARGET
        return points
    }

    fun pet(): Int {
        happiness = happiness + 7
        energy = energy + 1
        return addFriendship(3)
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
        val count = inventory(food.id)
        if (count <= 0) return false
        prefs.edit().putInt("food_${food.id}", count - 1).apply()
        fullness = fullness + when (food.id) {
            "berries" -> 18
            "apple" -> 14
            else -> 10
        }
        happiness = happiness + 4
        energy = energy + 2
        addFriendship(food.affection)
        return true
    }

    /** One-tap feed used by the main living-room action. Buys an apple when needed. */
    fun quickFeed(): Int {
        val apple = EveCompanionRules.foods.first { it.id == "apple" }
        if (inventory(apple.id) <= 0 && !buy(apple)) return 0
        return if (feed(apple)) apple.affection else 0
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
