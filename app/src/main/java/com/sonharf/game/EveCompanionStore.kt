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

internal object EveCompanionRules {
    val foods = listOf(
        EveFood("biscuit", "Orman Bisküvisi", "🍪", 8, 5),
        EveFood("apple", "Tatlı Elma", "🍎", 12, 8),
        EveFood("berries", "Işık Meyveleri", "🫐", 18, 12),
    )
    const val STARTER_LEAVES = 40
    const val DAILY_GIFT_LEAVES = 18
    const val MAX_AFFECTION = 100
}

internal class EveCompanionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("son_harf_eve_companion", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString("name", "Eve")?.ifBlank { "Eve" } ?: "Eve"
        set(value) { prefs.edit().putString("name", value.trim().take(18).ifBlank { "Eve" }).apply() }

    var leaves: Int
        get() = prefs.getInt("leaves", EveCompanionRules.STARTER_LEAVES)
        private set(value) { prefs.edit().putInt("leaves", value.coerceAtLeast(0)).apply() }

    var affection: Int
        get() = prefs.getInt("affection", 20)
        private set(value) { prefs.edit().putInt("affection", value.coerceIn(0, EveCompanionRules.MAX_AFFECTION)).apply() }

    fun inventory(foodId: String): Int = prefs.getInt("food_$foodId", 0)

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
        affection += food.affection
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
        return true
    }
}
