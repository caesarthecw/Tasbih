package com.example.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MisbahaAudioEngine
import com.example.billing.BillingEvent
import com.example.billing.BillingLaunchResult
import com.example.billing.BillingManager
import com.example.data.MisbahaRepository
import com.example.model.BillingPackage
import com.example.model.DailyLog
import com.example.model.DhikrItem
import com.example.model.PurchaseRecord
import com.example.model.SoundTone
import com.example.model.StoreCatalog
import com.example.model.StoreCategory
import com.example.model.StoreItem
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class OptimisticCounters(
    val dhikrId: String = "",
    val pendingDelta: Long = 0L,
    val sessionCount: Long = 0L,
    val tapSequence: Int = 0,
)

class MisbahaViewModel(
    private val repository: MisbahaRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String,
    private val audioEngine: MisbahaAudioEngine,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _optimistic = MutableStateFlow(OptimisticCounters())
    val optimistic: StateFlow<OptimisticCounters> = _optimistic.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _sandboxCheckoutPackage = MutableStateFlow<BillingPackage?>(null)
    val sandboxCheckoutPackage: StateFlow<BillingPackage?> = _sandboxCheckoutPackage.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var syncJob: Job? = null

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing profile", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل ملفك الشخصي"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val adhkarState: StateFlow<UiState<List<DhikrItem>>> =
        repository.observeAdhkar(currentUserId)
            .map<List<DhikrItem>, UiState<List<DhikrItem>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing adhkar", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل الأذكار"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val dailyLogsState: StateFlow<UiState<List<DailyLog>>> =
        repository.observeDailyLogs(currentUserId)
            .map<List<DailyLog>, UiState<List<DailyLog>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing daily logs", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل السجل اليومي"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val purchasesState: StateFlow<UiState<List<PurchaseRecord>>> =
        repository.observePurchases(currentUserId)
            .map<List<PurchaseRecord>, UiState<List<PurchaseRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing purchases", root)
                emit(UiState.Error(root.localizedMessage ?: "تعذر تحميل سجل المشتريات"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(currentUserId, currentUserDisplayName)
                .onFailure {
                    _snackbarMessages.tryEmit("تعذر مزامنة البيانات الأولية مع السحابة")
                }
        }
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.events.collect { event ->
                when (event) {
                    is BillingEvent.PurchaseVerified -> {
                        val pkg = StoreCatalog.billingPackages.firstOrNull { it.sku == event.sku }
                        val profile = currentProfileOrNull()
                        if (pkg != null && profile != null) {
                            repository.fulfillBillingPackage(
                                userId = currentUserId,
                                pkg = pkg,
                                orderToken = event.purchaseToken,
                                profile = profile,
                            ).onSuccess {
                                _snackbarMessages.tryEmit("تم تأكيد الدفع وتفعيل ${pkg.titleAr}")
                            }.onFailure {
                                _snackbarMessages.tryEmit("تعذر حفظ عملية الشراء في السحابة")
                            }
                        }
                    }
                    is BillingEvent.PurchaseMessage -> {
                        _snackbarMessages.tryEmit(event.messageAr)
                    }
                }
            }
        }
    }

    fun currentProfileOrNull(): UserProfile? =
        (userProfileState.value as? UiState.Success)?.data

    fun currentAdhkarOrEmpty(): List<DhikrItem> =
        (adhkarState.value as? UiState.Success)?.data ?: emptyList()

    fun onTasbihTap() {
        val profile = currentProfileOrNull() ?: return
        val adhkar = currentAdhkarOrEmpty()
        val activeDhikr = adhkar.firstOrNull { it.id == profile.activeDhikrId }
            ?: adhkar.firstOrNull()
            ?: return

        // Play synthesized water/wood/crystal sound
        audioEngine.playTap(
            tone = SoundTone.fromId(profile.soundTone),
            volume = profile.soundVolume.toFloat(),
            enabled = profile.soundEnabled,
        )

        val currentOpt = _optimistic.value
        val baseDelta = if (currentOpt.dhikrId == activeDhikr.id) currentOpt.pendingDelta else 0L
        if (currentOpt.dhikrId.isNotBlank() && currentOpt.dhikrId != activeDhikr.id && currentOpt.pendingDelta > 0L) {
            flushPendingSync(forceImmediate = true)
        }

        _optimistic.value = OptimisticCounters(
            dhikrId = activeDhikr.id,
            pendingDelta = baseDelta + 1L,
            sessionCount = currentOpt.sessionCount + 1L,
            tapSequence = currentOpt.tapSequence + 1,
        )

        val newLifetime = profile.totalLifetimeTasbih + baseDelta + 1L
        if (newLifetime % 33L == 0L) {
            _snackbarMessages.tryEmit("مبارك! أتممت دورة ٣٣ تسبيحة (+٢ عملة غرس)")
        }

        scheduleCoalescedSync()
    }

    private fun scheduleCoalescedSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(450L)
            flushPendingSyncInternal()
        }
    }

    fun flushPendingSync(forceImmediate: Boolean = false) {
        if (forceImmediate) {
            syncJob?.cancel()
            viewModelScope.launch { flushPendingSyncInternal() }
        }
    }

    private suspend fun flushPendingSyncInternal() {
        val snap = _optimistic.value
        if (snap.pendingDelta <= 0L || snap.dhikrId.isBlank()) return

        val profile = currentProfileOrNull() ?: return
        val dhikr = currentAdhkarOrEmpty().firstOrNull { it.id == snap.dhikrId } ?: return
        val delta = snap.pendingDelta

        // Calculate coin rewards: 1 coin per 10 tasbihat + 2 bonus coins on every 33 milestone
        val oldMilestones10 = profile.totalLifetimeTasbih / 10L
        val newLifetime = profile.totalLifetimeTasbih + delta
        val newMilestones10 = newLifetime / 10L
        val oldMilestones33 = profile.totalLifetimeTasbih / 33L
        val newMilestones33 = newLifetime / 33L
        val coinReward = (newMilestones10 - oldMilestones10) + (newMilestones33 - oldMilestones33) * 2L

        _isSyncing.value = true
        _optimistic.value = snap.copy(pendingDelta = 0L)

        val result = repository.syncTasbihIncrement(
            userId = currentUserId,
            dhikrId = dhikr.id,
            delta = delta,
            coinsReward = coinReward,
            newTodayTotal = profile.todayTasbih + delta,
            newLifetimeTotal = newLifetime,
            newCoins = (profile.coins + coinReward).coerceAtMost(10_000_000L),
            streakDays = profile.streakDays.coerceAtLeast(1L),
            newDhikrToday = dhikr.todayCount + delta,
            newDhikrTotal = dhikr.totalCount + delta,
        )
        _isSyncing.value = false

        result.onFailure {
            // Restore unsynced delta on transient failure so taps are never lost
            val latest = _optimistic.value
            if (latest.dhikrId == dhikr.id) {
                _optimistic.value = latest.copy(pendingDelta = latest.pendingDelta + delta)
            }
            _snackbarMessages.tryEmit("تعذر المزامنة السحابية الآن، سيتم إعادة المحاولة")
        }
    }

    fun resetSessionCounter() {
        _optimistic.value = _optimistic.value.copy(sessionCount = 0L)
    }

    fun selectActiveDhikr(dhikrId: String) {
        viewModelScope.launch {
            flushPendingSyncInternal()
            repository.updatePreferences(currentUserId, mapOf("activeDhikrId" to dhikrId))
                .onFailure { _snackbarMessages.tryEmit("تعذر تغيير الذكر النشط") }
        }
    }

    fun updateAudioPreferences(
        soundEnabled: Boolean,
        soundVolume: Float,
        soundTone: SoundTone,
        hapticEnabled: Boolean,
    ) {
        viewModelScope.launch {
            repository.updatePreferences(
                currentUserId,
                mapOf(
                    "soundEnabled" to soundEnabled,
                    "soundVolume" to soundVolume.toDouble().coerceIn(0.0, 1.0),
                    "soundTone" to soundTone.id,
                    "hapticEnabled" to hapticEnabled,
                )
            ).onFailure {
                _snackbarMessages.tryEmit("تعذر حفظ إعدادات الصوت")
            }
        }
    }

    fun previewSound(tone: SoundTone, volume: Float) {
        audioEngine.playTap(tone, volume, enabled = true)
    }

    fun setCustomBeadHex(hex: String) {
        viewModelScope.launch {
            repository.updatePreferences(
                currentUserId,
                mapOf("customBeadHex" to hex)
            ).onFailure {
                _snackbarMessages.tryEmit("تعذر تحديث لون الخرز")
            }
        }
    }

    fun addCustomDhikr(phrase: String, subtitle: String, goal: Long, accentHex: String) {
        viewModelScope.launch {
            val nextOrder = (currentAdhkarOrEmpty().maxOfOrNull { it.sortOrder } ?: 0L) + 1L
            repository.addCustomDhikr(
                userId = currentUserId,
                phrase = phrase,
                subtitle = subtitle,
                dailyGoal = goal,
                accentHex = accentHex,
                sortOrder = nextOrder,
            ).onSuccess { newId ->
                repository.updatePreferences(currentUserId, mapOf("activeDhikrId" to newId))
                _snackbarMessages.tryEmit("تمت إضافة الذكر ومزامنته سحابياً")
            }.onFailure {
                _snackbarMessages.tryEmit("تعذر إضافة الذكر")
            }
        }
    }

    fun updateDhikr(dhikrId: String, phrase: String, subtitle: String, goal: Long, accentHex: String) {
        viewModelScope.launch {
            repository.updateDhikrDetails(currentUserId, dhikrId, phrase, subtitle, goal, accentHex)
                .onSuccess { _snackbarMessages.tryEmit("تم تحديث الذكر") }
                .onFailure { _snackbarMessages.tryEmit("تعذر تحديث الذكر") }
        }
    }

    fun resetDhikrToday(dhikrId: String) {
        viewModelScope.launch {
            repository.resetDhikrToday(currentUserId, dhikrId)
                .onSuccess { _snackbarMessages.tryEmit("تم تصفير عداد اليوم لهذا الذكر") }
                .onFailure { _snackbarMessages.tryEmit("تعذر تصفير العداد") }
        }
    }

    fun deleteDhikr(dhikrId: String) {
        val profile = currentProfileOrNull()
        val remaining = currentAdhkarOrEmpty().filter { it.id != dhikrId }
        viewModelScope.launch {
            if (profile?.activeDhikrId == dhikrId && remaining.isNotEmpty()) {
                repository.updatePreferences(currentUserId, mapOf("activeDhikrId" to remaining.first().id))
            }
            repository.deleteCustomDhikr(currentUserId, dhikrId)
                .onSuccess { _snackbarMessages.tryEmit("تم حذف الذكر") }
                .onFailure { _snackbarMessages.tryEmit("تعذر حذف الذكر") }
        }
    }

    fun equipOrUnlockStoreItem(item: StoreItem) {
        val profile = currentProfileOrNull() ?: return
        val isUnlocked = profile.vipUnlocked ||
            item.coinPrice == 0L ||
            profile.unlockedItems.contains(item.id)

        if (isUnlocked) {
            equipStoreItem(item)
        } else {
            val effectiveCost = item.coinPrice
            if (profile.coins < effectiveCost) {
                _snackbarMessages.tryEmit("رصيدك (${profile.coins} عملة) لا يكفي، سبّح أكثر أو اشحن العملات")
                return
            }
            viewModelScope.launch {
                repository.unlockAndEquipItemWithCoins(currentUserId, item, profile)
                    .onSuccess {
                        if (item.category == StoreCategory.BEAD_COLOR) {
                            repository.updatePreferences(
                                currentUserId,
                                mapOf("customBeadHex" to item.previewColorHex)
                            )
                        }
                        _snackbarMessages.tryEmit("تم فتح وتفعيل ${item.titleAr}!")
                    }
                    .onFailure {
                        _snackbarMessages.tryEmit("تعذر إتمام عملية الشراء بالعملات")
                    }
            }
        }
    }

    private fun equipStoreItem(item: StoreItem) {
        val updates = when (item.category) {
            StoreCategory.BEAD_COLOR -> mapOf(
                "selectedBeadColorId" to item.id,
                "customBeadHex" to item.previewColorHex,
            )
            StoreCategory.BACKGROUND -> mapOf("selectedBackgroundId" to item.id)
            StoreCategory.THEME -> mapOf("selectedThemeId" to item.id)
            StoreCategory.PLANT -> mapOf("selectedPlantId" to item.id)
            else -> emptyMap()
        }
        if (updates.isEmpty()) {
            _snackbarMessages.tryEmit("${item.titleAr} مفعّلة في حسابك بالفعل")
            return
        }
        viewModelScope.launch {
            repository.updatePreferences(currentUserId, updates)
                .onSuccess { _snackbarMessages.tryEmit("تم تفعيل ${item.titleAr}") }
                .onFailure { _snackbarMessages.tryEmit("تعذر تطبيق التخصيص") }
        }
    }

    fun startPackagePurchase(activity: Activity?, pkg: BillingPackage) {
        when (val launch = billingManager.launchPurchase(activity, pkg)) {
            BillingLaunchResult.LaunchedPlaySheet -> {
                // Play Store billing sheet is displayed
            }
            is BillingLaunchResult.RequiresSandboxCheckout -> {
                _sandboxCheckoutPackage.value = launch.pkg
            }
        }
    }

    fun dismissSandboxCheckout() {
        _sandboxCheckoutPackage.value = null
    }

    fun confirmSandboxPurchase(pkg: BillingPackage) {
        val profile = currentProfileOrNull() ?: return
        _sandboxCheckoutPackage.value = null
        viewModelScope.launch {
            val token = "sandbox_gp_${UUID.randomUUID().toString().take(12)}"
            repository.fulfillBillingPackage(
                userId = currentUserId,
                pkg = pkg,
                orderToken = token,
                profile = profile,
            ).onSuccess {
                _snackbarMessages.tryEmit("تمت إضافة ${pkg.titleAr} إلى حسابك بنجاح!")
            }.onFailure {
                _snackbarMessages.tryEmit("تعذر تسجيل عملية الدفع في السحابة")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        billingManager.endConnection()
    }

    private companion object {
        const val TAG = "MisbahaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
