package com.example.data

import android.content.Context
import com.example.R
import com.example.model.DailyLog
import com.example.model.DhikrItem
import com.example.model.PurchaseRecord
import com.example.model.SoundTone
import com.example.model.StoreCatalog
import com.example.model.StoreCategory
import com.example.model.StoreItem
import com.example.model.UserProfile
import com.example.model.BillingPackage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisbahaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchedUser(userId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")
        check(uid == userId) { "تغير الحساب المسجل؛ تم رفض العملية." }
        return uid
    }

    fun todayDateKey(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }

    fun observeAdhkar(userId: String): Flow<List<DhikrItem>> =
        db.collection("users").document(userId)
            .collection("adhkar")
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DhikrItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeDailyLogs(userId: String): Flow<List<DailyLog>> =
        db.collection("users").document(userId)
            .collection("dailyLogs")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(14)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observePurchases(userId: String): Flow<List<PurchaseRecord>> =
        db.collection("users").document(userId)
            .collection("purchases")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PurchaseRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun ensureUserInitialized(userId: String, displayName: String): Result<Unit> {
        val userDocPath = "users/$userId"
        return try {
            requireMatchedUser(userId)
            val today = todayDateKey()
            val userRef = db.collection("users").document(userId)
            val userSnap = userRef.get().await()

            val safeName = displayName.trim().ifBlank { "مستخدم غرس" }.take(60)
            if (!userSnap.exists()) {
                val initialProfile = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "coins" to 120L,
                    "totalLifetimeTasbih" to 0L,
                    "todayTasbih" to 0L,
                    "lastActiveDate" to today,
                    "streakDays" to 1L,
                    "activeDhikrId" to "subhanallah",
                    "selectedBeadColorId" to "bead_amber",
                    "customBeadHex" to "#C78D3B",
                    "selectedThemeId" to "theme_emerald",
                    "selectedBackgroundId" to "bg_minimal_ivory",
                    "selectedPlantId" to "plant_olive",
                    "soundEnabled" to true,
                    "soundVolume" to 0.75,
                    "soundTone" to SoundTone.WATER_DROP.id,
                    "hapticEnabled" to true,
                    "unlockedItems" to StoreCatalog.defaultUnlockedIds.toList(),
                    "vipUnlocked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                userRef.set(initialProfile).await()
            } else {
                val existingDate = userSnap.getString("lastActiveDate") ?: today
                if (existingDate != today) {
                    val prevToday = userSnap.getLong("todayTasbih") ?: 0L
                    val currentStreak = userSnap.getLong("streakDays") ?: 1L
                    val nextStreak = if (prevToday > 0L) currentStreak + 1L else 1L
                    userRef.update(
                        mapOf(
                            "todayTasbih" to 0L,
                            "lastActiveDate" to today,
                            "streakDays" to nextStreak,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            }

            val adhkarCol = userRef.collection("adhkar")
            val existingAdhkar = adhkarCol.get().await()
            if (existingAdhkar.isEmpty) {
                val batch = db.batch()
                defaultAdhkarPresets().forEachIndexed { index, preset ->
                    val docRef = adhkarCol.document(preset.first)
                    batch.set(
                        docRef,
                        mapOf(
                            "userId" to userId,
                            "phrase" to preset.second,
                            "subtitle" to preset.third,
                            "dailyGoal" to preset.fourth,
                            "todayCount" to 0L,
                            "totalCount" to 0L,
                            "lastUpdatedDate" to today,
                            "accentHex" to preset.fifth,
                            "sortOrder" to index.toLong(),
                            "customEntry" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            } else {
                val batch = db.batch()
                var needsRollover = false
                for (doc in existingAdhkar.documents) {
                    if (doc.getString("lastUpdatedDate") != today) {
                        needsRollover = true
                        batch.update(
                            doc.reference,
                            mapOf(
                                "todayCount" to 0L,
                                "lastUpdatedDate" to today,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                }
                if (needsRollover) {
                    batch.commit().await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun syncTasbihIncrement(
        userId: String,
        dhikrId: String,
        delta: Long,
        coinsReward: Long,
        newTodayTotal: Long,
        newLifetimeTotal: Long,
        newCoins: Long,
        streakDays: Long,
        newDhikrToday: Long,
        newDhikrTotal: Long,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireMatchedUser(userId)
            require(delta > 0L)
            val today = todayDateKey()
            val userRef = db.collection("users").document(userId)
            val dhikrRef = userRef.collection("adhkar").document(dhikrId)
            val logRef = userRef.collection("dailyLogs").document(today)

            val logSnap = logRef.get().await()
            val batch = db.batch()

            batch.update(
                userRef,
                mapOf(
                    "totalLifetimeTasbih" to newLifetimeTotal,
                    "todayTasbih" to newTodayTotal,
                    "lastActiveDate" to today,
                    "streakDays" to streakDays,
                    "coins" to newCoins,
                    "activeDhikrId" to dhikrId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.update(
                dhikrRef,
                mapOf(
                    "todayCount" to newDhikrToday,
                    "totalCount" to newDhikrTotal,
                    "lastUpdatedDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            if (logSnap.exists()) {
                val prevEarned = logSnap.getLong("coinsEarned") ?: 0L
                batch.update(
                    logRef,
                    mapOf(
                        "totalCount" to newTodayTotal,
                        "coinsEarned" to (prevEarned + coinsReward),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.set(
                    logRef,
                    mapOf(
                        "userId" to userId,
                        "dateKey" to today,
                        "totalCount" to newTodayTotal,
                        "coinsEarned" to coinsReward,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge()
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        updates: Map<String, Any>,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireMatchedUser(userId)
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(userId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomDhikr(
        userId: String,
        phrase: String,
        subtitle: String,
        dailyGoal: Long,
        accentHex: String,
        sortOrder: Long,
    ): Result<String> {
        val path = "users/$userId/adhkar"
        return try {
            requireMatchedUser(userId)
            val cleanPhrase = phrase.trim().take(200)
            require(cleanPhrase.isNotEmpty()) { "يرجى إدخال نص الذكر" }
            val cleanSubtitle = subtitle.trim().take(200)
            val safeGoal = dailyGoal.coerceIn(1L, 100000L)
            val safeHex = if (Regex("^#[0-9A-Fa-f]{6}$").matches(accentHex)) accentHex else "#0F6B56"

            val docRef = db.collection("users").document(userId).collection("adhkar").document()
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "phrase" to cleanPhrase,
                    "subtitle" to cleanSubtitle,
                    "dailyGoal" to safeGoal,
                    "todayCount" to 0L,
                    "totalCount" to 0L,
                    "lastUpdatedDate" to todayDateKey(),
                    "accentHex" to safeHex,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "customEntry" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDhikrDetails(
        userId: String,
        dhikrId: String,
        phrase: String,
        subtitle: String,
        dailyGoal: Long,
        accentHex: String,
    ): Result<Unit> {
        val path = "users/$userId/adhkar/$dhikrId"
        return try {
            requireMatchedUser(userId)
            val cleanPhrase = phrase.trim().take(200)
            require(cleanPhrase.isNotEmpty()) { "يرجى إدخال نص الذكر" }
            val safeHex = if (Regex("^#[0-9A-Fa-f]{6}$").matches(accentHex)) accentHex else "#0F6B56"

            db.collection("users").document(userId)
                .collection("adhkar").document(dhikrId)
                .update(
                    mapOf(
                        "phrase" to cleanPhrase,
                        "subtitle" to subtitle.trim().take(200),
                        "dailyGoal" to dailyGoal.coerceIn(1L, 100000L),
                        "accentHex" to safeHex,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetDhikrToday(userId: String, dhikrId: String): Result<Unit> {
        val path = "users/$userId/adhkar/$dhikrId"
        return try {
            requireMatchedUser(userId)
            db.collection("users").document(userId)
                .collection("adhkar").document(dhikrId)
                .update(
                    mapOf(
                        "todayCount" to 0L,
                        "lastUpdatedDate" to todayDateKey(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomDhikr(userId: String, dhikrId: String): Result<Unit> {
        val path = "users/$userId/adhkar/$dhikrId"
        return try {
            requireMatchedUser(userId)
            db.collection("users").document(userId)
                .collection("adhkar").document(dhikrId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun unlockAndEquipItemWithCoins(
        userId: String,
        item: StoreItem,
        profile: UserProfile,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireMatchedUser(userId)
            val effectiveCost = if (profile.vipUnlocked) 0L else item.coinPrice
            require(profile.coins >= effectiveCost) { "رصيد العملات غير كافٍ" }

            val userRef = db.collection("users").document(userId)
            val purchaseRef = userRef.collection("purchases").document()
            val updatedUnlocked = (profile.unlockedItems + item.id).distinct()

            val userUpdates = mutableMapOf<String, Any>(
                "coins" to (profile.coins - effectiveCost),
                "unlockedItems" to updatedUnlocked,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            when (item.category) {
                StoreCategory.BEAD_COLOR -> userUpdates["selectedBeadColorId"] = item.id
                StoreCategory.BACKGROUND -> userUpdates["selectedBackgroundId"] = item.id
                StoreCategory.THEME -> userUpdates["selectedThemeId"] = item.id
                StoreCategory.PLANT -> userUpdates["selectedPlantId"] = item.id
                else -> {}
            }

            val batch = db.batch()
            batch.update(userRef, userUpdates)
            batch.set(
                purchaseRef,
                mapOf(
                    "userId" to userId,
                    "productId" to item.id,
                    "title" to item.titleAr,
                    "purchaseType" to "coin_unlock",
                    "coinDelta" to -effectiveCost,
                    "priceDisplay" to "$effectiveCost عملة",
                    "orderToken" to "coin_${purchaseRef.id}",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun fulfillBillingPackage(
        userId: String,
        pkg: BillingPackage,
        orderToken: String,
        profile: UserProfile,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireMatchedUser(userId)
            val userRef = db.collection("users").document(userId)
            val purchaseRef = userRef.collection("purchases").document()
            val newCoins = (profile.coins + pkg.coinsGranted).coerceAtMost(10_000_000L)

            val userUpdates = mutableMapOf<String, Any>(
                "coins" to newCoins,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (pkg.unlocksVip) {
                userUpdates["vipUnlocked"] = true
                userUpdates["unlockedItems"] = StoreCatalog.allItems.map { it.id }.distinct()
            }

            val batch = db.batch()
            batch.update(userRef, userUpdates)
            batch.set(
                purchaseRef,
                mapOf(
                    "userId" to userId,
                    "productId" to pkg.sku,
                    "title" to pkg.titleAr,
                    "purchaseType" to if (pkg.unlocksVip) "play_billing_vip" else "play_billing_coins",
                    "coinDelta" to pkg.coinsGranted,
                    "priceDisplay" to pkg.priceDisplay,
                    "orderToken" to orderToken.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private data class DhikrPreset(
        val first: String,
        val second: String,
        val third: String,
        val fourth: Long,
        val fifth: String,
    )

    private fun defaultAdhkarPresets(): List<DhikrPreset> = listOf(
        DhikrPreset("subhanallah", "سبحان الله", "تنزيه الله وتقديسه", 33L, "#0F6B56"),
        DhikrPreset("alhamdulillah", "الحمد لله", "الشكر والثناء على نعم الله", 33L, "#C68B2C"),
        DhikrPreset("allahuakbar", "الله أكبر", "تعظيم الخالق جل وعلا", 33L, "#0E7490"),
        DhikrPreset("lailahaillallah", "لا إله إلا الله", "كلمة التوحيد الخالصة", 100L, "#8E4460"),
        DhikrPreset("astaghfirullah", "أستغفر الله العظيم", "طلب المغفرة والسكينة", 100L, "#4F7A52"),
        DhikrPreset("salawat", "اللهم صلِّ وسلم على نبينا محمد", "الصلاة والسلام على رسول الله", 100L, "#9C5B2E"),
    )
}
