package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DhikrItem
import com.example.model.SoundTone
import com.example.model.StoreCatalog
import com.example.model.UserProfile
import com.example.ui.components.AccountSyncDialog
import com.example.ui.components.AudioAndHapticsSheet
import com.example.ui.components.BeadColorQuickSheet
import com.example.ui.components.InteractiveMisbahaGardenStage
import com.example.ui.components.SceneBackdrop
import com.example.ui.theme.CoinGold
import com.example.viewmodel.OptimisticCounters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisbahaScreen(
    profile: UserProfile,
    adhkar: List<DhikrItem>,
    optimistic: OptimisticCounters,
    isSyncing: Boolean,
    userEmail: String?,
    onTapMisbaha: () -> Unit,
    onResetSession: () -> Unit,
    onSelectDhikr: (String) -> Unit,
    onSaveAudioPreferences: (Boolean, Float, SoundTone, Boolean) -> Unit,
    onPreviewTone: (SoundTone, Float) -> Unit,
    onSelectBeadHex: (String) -> Unit,
    onOpenStore: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDhikrSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val activeDhikr = adhkar.firstOrNull { it.id == profile.activeDhikrId } ?: adhkar.firstOrNull()
    val pendingForActive = if (activeDhikr != null && optimistic.dhikrId == activeDhikr.id) {
        optimistic.pendingDelta
    } else {
        0L
    }

    val displayedDhikrToday = (activeDhikr?.todayCount ?: 0L) + pendingForActive
    val displayedLifetimeTotal = profile.totalLifetimeTasbih + optimistic.pendingDelta
    val stageInfo = StoreCatalog.calculatePlantStage(displayedLifetimeTotal)
    val plantItem = StoreCatalog.findById(profile.selectedPlantId)
    val beadColor = StoreCatalog.parseHexColor(profile.customBeadHex)
    val beadHighlight = Color(
        red = (beadColor.red + 0.25f).coerceAtMost(1f),
        green = (beadColor.green + 0.25f).coerceAtMost(1f),
        blue = (beadColor.blue + 0.25f).coerceAtMost(1f),
        alpha = 1f,
    )
    val isDarkBg = StoreCatalog.findById(profile.selectedBackgroundId)?.isDarkScene == true ||
        profile.selectedThemeId == "theme_royal_night"
    val heroTextColor = if (isDarkBg) Color(0xFFF3F8FA) else MaterialTheme.colorScheme.onBackground

    SceneBackdrop(
        selectedBackgroundId = profile.selectedBackgroundId,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Bar: Dhikr Switcher + Coins + Sound + Account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        onClick = { showDhikrSheet = true },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("dhikr_picker_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Rounded.CloudSync else Icons.Rounded.CloudDone,
                                contentDescription = "حالة المزامنة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = activeDhikr?.phrase ?: "اختر الذكر",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "تبديل الذكر",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            onClick = onOpenStore,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                            modifier = Modifier.testTag("coins_badge"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MonetizationOn,
                                    contentDescription = "عملات غرس",
                                    tint = CoinGold,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "${profile.coins}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }

                        Surface(
                            onClick = { showAudioSheet = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            modifier = Modifier.size(42.dp).testTag("sound_settings_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (profile.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                    contentDescription = "إعدادات الصوت",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Surface(
                            onClick = { showAccountDialog = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            modifier = Modifier.size(42.dp).testTag("account_button"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = "الحساب والمزامنة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Active Dhikr Phrase & Animated Count Header
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = activeDhikr?.phrase ?: "سبحان الله",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!activeDhikr?.subtitle.isNullOrBlank()) {
                            Text(
                                text = activeDhikr?.subtitle ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AnimatedContent(targetState = displayedDhikrToday, label = "counter") { count ->
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Text(
                                    text = "/ ${activeDhikr?.dailyGoal ?: 33} اليوم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    onClick = { showColorSheet = true },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.testTag("quick_color_button"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(beadColor)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = "تخصيص لون الخرز",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onResetSession,
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("reset_session_button"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "تصفير عداد الجلسة",
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "جلسة: ${optimistic.sessionCount}",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Moving Beads + Water Droplets + Growing Plant Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    InteractiveMisbahaGardenStage(
                        beadColor = beadColor,
                        beadHighlight = beadHighlight,
                        plantId = profile.selectedPlantId,
                        plantStageIndex = stageInfo.stageIndex,
                        stageProgress = stageInfo.progressToNext,
                        tapSequence = optimistic.tapSequence,
                        hapticEnabled = profile.hapticEnabled,
                        onTapMisbaha = onTapMisbaha,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("misbaha_tap_area"),
                    )

                    if (optimistic.sessionCount == 0L) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Text(
                                text = "اضغط هنا لتحريك الخرز وسقي النبتة",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                // Bottom Plant Growth Progress Strip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = "${plantItem?.titleAr ?: "شجرة الزيتون"} • ${stageInfo.stageNameAr}",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = stageInfo.stageHintAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = if (stageInfo.remainingToNext > 0) {
                                    "متبقي ${stageInfo.remainingToNext}"
                                } else {
                                    "مكتملة"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { stageInfo.progressToNext },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50)),
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }

    if (showDhikrSheet) {
        ModalBottomSheet(onDismissRequest = { showDhikrSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("اختر الذكر الحالي", style = MaterialTheme.typography.titleLarge)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp),
                ) {
                    items(adhkar, key = { it.id }) { item ->
                        val isSelected = item.id == activeDhikr?.id
                        Surface(
                            onClick = {
                                onSelectDhikr(item.id)
                                showDhikrSheet = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.phrase, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "اليوم: ${item.todayCount} / ${item.dailyGoal} • الإجمالي: ${item.totalCount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioSheet) {
        AudioAndHapticsSheet(
            profile = profile,
            onDismiss = { showAudioSheet = false },
            onSave = onSaveAudioPreferences,
            onPreviewTone = onPreviewTone,
        )
    }

    if (showColorSheet) {
        BeadColorQuickSheet(
            profile = profile,
            onDismiss = { showColorSheet = false },
            onSelectHex = onSelectBeadHex,
            onOpenStore = onOpenStore,
        )
    }

    if (showAccountDialog) {
        AccountSyncDialog(
            displayName = profile.displayName,
            email = userEmail,
            totalLifetimeTasbih = displayedLifetimeTotal,
            streakDays = profile.streakDays,
            vipUnlocked = profile.vipUnlocked,
            onDismiss = { showAccountDialog = false },
            onSignOut = {
                showAccountDialog = false
                onSignOut()
            },
        )
    }
}
