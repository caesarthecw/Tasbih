package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SoundTone(
    val id: String,
    val labelAr: String,
    val vipOnly: Boolean = false,
) {
    WATER_DROP("water_drop", "قطرة ماء", false),
    WOOD_BEAD("wood_bead", "خرز خشبي", false),
    CRYSTAL_CHIME("crystal_chime", "رنين بلوري", true),
    RAIN_MIST("rain_mist", "رذاذ المطر", true);

    companion object {
        fun fromId(raw: String?): SoundTone =
            entries.firstOrNull { it.id == raw } ?: WATER_DROP
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "مستخدم",
    val coins: Long = 120L,
    val totalLifetimeTasbih: Long = 0L,
    val todayTasbih: Long = 0L,
    val lastActiveDate: String = "",
    val streakDays: Long = 1L,
    val activeDhikrId: String = "subhanallah",
    val selectedBeadColorId: String = "bead_amber",
    val customBeadHex: String = "#C78D3B",
    val selectedThemeId: String = "theme_emerald",
    val selectedBackgroundId: String = "bg_minimal_ivory",
    val selectedPlantId: String = "plant_olive",
    val soundEnabled: Boolean = true,
    val soundVolume: Double = 0.75,
    val soundTone: String = SoundTone.WATER_DROP.id,
    val hapticEnabled: Boolean = true,
    val unlockedItems: List<String> = listOf(
        "bead_amber",
        "bead_emerald",
        "theme_emerald",
        "bg_minimal_ivory",
        "plant_olive",
    ),
    val vipUnlocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DhikrItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phrase: String = "",
    val subtitle: String = "",
    val dailyGoal: Long = 33L,
    val todayCount: Long = 0L,
    val totalCount: Long = 0L,
    val lastUpdatedDate: String = "",
    val accentHex: String = "#0F6B56",
    val sortOrder: Long = 0L,
    val customEntry: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DailyLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val totalCount: Long = 0L,
    val coinsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PurchaseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val title: String = "",
    val purchaseType: String = "coin_unlock",
    val coinDelta: Long = 0L,
    val priceDisplay: String = "",
    val orderToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
