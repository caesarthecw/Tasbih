package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SoundTone
import com.example.model.StoreCatalog
import com.example.model.UserProfile
import kotlin.math.roundToInt

@Composable
fun SceneBackdrop(
    selectedBackgroundId: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgItem = StoreCatalog.findById(selectedBackgroundId)
    val baseColor = MaterialTheme.colorScheme.background
    val containerTint = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = modifier.fillMaxSize()) {
        if (bgItem?.backgroundDrawableRes != null) {
            Image(
                painter = painterResource(id = bgItem.backgroundDrawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val scrimColor = if (bgItem.isDarkScene) {
                Color(0xFF07131E).copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.background.copy(alpha = 0.48f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
            )
        } else {
            val topColor = if (selectedBackgroundId == "bg_emerald_mist") {
                Color(0xFFD5EFE5)
            } else {
                baseColor
            }
            val midColor = if (selectedBackgroundId == "bg_emerald_mist") {
                Color(0xFFEFF9F4)
            } else {
                containerTint.copy(alpha = 0.35f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(topColor, midColor, baseColor)
                        )
                    )
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioAndHapticsSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, volume: Float, tone: SoundTone, haptic: Boolean) -> Unit,
    onPreviewTone: (tone: SoundTone, volume: Float) -> Unit,
) {
    var soundEnabled by remember(profile.soundEnabled) { mutableStateOf(profile.soundEnabled) }
    var volume by remember(profile.soundVolume) { mutableFloatStateOf(profile.soundVolume.toFloat()) }
    var selectedTone by remember(profile.soundTone) { mutableStateOf(SoundTone.fromId(profile.soundTone)) }
    var hapticEnabled by remember(profile.hapticEnabled) { mutableStateOf(profile.hapticEnabled) }

    val hasAdvancedSounds = profile.vipUnlocked || profile.unlockedItems.contains("feat_crystal_sound")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "التحكم بالصوت والاهتزاز",
                style = MaterialTheme.typography.titleLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text("صوت قطرة الماء والخرز", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (soundEnabled) "مفعّل أثناء التسبيح" else "صامت",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        onSave(it, volume, selectedTone, hapticEnabled)
                    },
                    modifier = Modifier.testTag("sound_enabled_switch"),
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("درجة الصوت", style = MaterialTheme.typography.labelLarge)
                    Text("${(volume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = {
                        onSave(soundEnabled, volume, selectedTone, hapticEnabled)
                        if (soundEnabled) onPreviewTone(selectedTone, volume)
                    },
                    enabled = soundEnabled,
                    valueRange = 0.05f..1f,
                    modifier = Modifier.testTag("sound_volume_slider"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نغمة التسبيح", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SoundTone.entries.forEach { tone ->
                        val unlocked = !tone.vipOnly || hasAdvancedSounds
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = {
                                if (unlocked) {
                                    selectedTone = tone
                                    onSave(soundEnabled, volume, tone, hapticEnabled)
                                    onPreviewTone(tone, volume)
                                }
                            },
                            enabled = unlocked,
                            label = { Text(tone.labelAr) },
                            leadingIcon = {
                                if (!unlocked) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                } else if (selectedTone == tone) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                        )
                    }
                }
                if (!hasAdvancedSounds) {
                    Text(
                        text = "يمكنك فتح نغمات الرنين البلوري ورذاذ المطر من المتجر.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text("الاهتزاز الخفيف", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "إحساس لمسي عند تحريك كل خرزة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        onSave(soundEnabled, volume, selectedTone, it)
                    },
                    modifier = Modifier.testTag("haptic_enabled_switch"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BeadColorQuickSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSelectHex: (String) -> Unit,
    onOpenStore: () -> Unit,
) {
    val hasColorLab = profile.vipUnlocked || profile.unlockedItems.contains("feat_custom_color")
    val unlockedBeadColors = StoreCatalog.allItems.filter {
        it.category == com.example.model.StoreCategory.BEAD_COLOR &&
            (profile.vipUnlocked || it.coinPrice == 0L || profile.unlockedItems.contains(it.id))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("تخصيص لون الخرز", style = MaterialTheme.typography.titleLarge)

            Text("ألوانك المفتوحة", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                unlockedBeadColors.forEach { item ->
                    val swatch = StoreCatalog.parseHexColor(item.previewColorHex)
                    val isSelected = profile.customBeadHex.equals(item.previewColorHex, ignoreCase = true)
                    ColorCircleSwatch(
                        color = swatch,
                        selected = isSelected,
                        label = item.titleAr,
                        onClick = {
                            onSelectHex(item.previewColorHex)
                            onDismiss()
                        },
                    )
                }
            }

            HorizontalDivider()

            Text("مختبر الألوان الحر", style = MaterialTheme.typography.labelLarge)
            if (hasColorLab) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StoreCatalog.customColorSwatches.forEach { hex ->
                        val color = StoreCatalog.parseHexColor(hex)
                        val isSelected = profile.customBeadHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.6f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    onSelectHex(hex)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("افتح مختبر الألوان الكاملة", style = MaterialTheme.typography.titleMedium)
                            Text("خصّص خرزك بأي درجة لونية من المتجر", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            onDismiss()
                            onOpenStore()
                        }) {
                            Text("المتجر")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCircleSwatch(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AccountSyncDialog(
    displayName: String,
    email: String?,
    totalLifetimeTasbih: Long,
    streakDays: Long,
    vipUnlocked: Boolean,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text("الحساب والمزامنة السحابية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!email.isNullOrBlank()) {
                    Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("إجمالي التسبيحات: $totalLifetimeTasbih", style = MaterialTheme.typography.bodyLarge)
                Text("سلسلة الأيام المتواصلة: $streakDays يوم", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (vipUnlocked) "العضوية: حزمة البستان الذهبي (VIP)" else "العضوية: أساسية",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "تتم مزامنة أذكارك وعملاتك ونمو نبتتك تلقائياً عبر السحابة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSignOut,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("sign_out_button"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("تسجيل الخروج")
            }
        },
    )
}
