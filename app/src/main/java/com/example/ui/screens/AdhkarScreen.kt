package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DailyLog
import com.example.model.DhikrItem
import com.example.model.StoreCatalog
import com.example.model.UserProfile
import com.example.ui.components.SceneBackdrop

@Composable
fun AdhkarScreen(
    profile: UserProfile,
    adhkar: List<DhikrItem>,
    dailyLogs: List<DailyLog>,
    onSelectAndOpenMisbaha: (String) -> Unit,
    onAddCustomDhikr: (phrase: String, subtitle: String, goal: Long, accentHex: String) -> Unit,
    onUpdateDhikr: (dhikrId: String, phrase: String, subtitle: String, goal: Long, accentHex: String) -> Unit,
    onResetDhikrToday: (String) -> Unit,
    onDeleteDhikr: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DhikrItem?>(null) }

    SceneBackdrop(
        selectedBackgroundId = profile.selectedBackgroundId,
        modifier = modifier,
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_dhikr_fab"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "إضافة ذكر جديد")
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        DailySummaryCard(
                            profile = profile,
                            dailyLogs = dailyLogs,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "قائمة الأذكار اليومية",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "${adhkar.size} أذكار",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (adhkar.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("لا توجد أذكار بعد", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "اضغط على زر + لإضافة ذكرك الخاص وحفظه سحابياً",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(adhkar, key = { it.id }) { item ->
                            DhikrRowCard(
                                item = item,
                                isActive = item.id == profile.activeDhikrId,
                                onUseInMisbaha = { onSelectAndOpenMisbaha(item.id) },
                                onEdit = { editingItem = item },
                                onResetToday = { onResetDhikrToday(item.id) },
                                onDelete = if (item.customEntry) { { onDeleteDhikr(item.id) } } else null,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        DhikrEditorDialog(
            initialItem = null,
            onDismiss = { showCreateDialog = false },
            onSave = { phrase, subtitle, goal, hex ->
                onAddCustomDhikr(phrase, subtitle, goal, hex)
                showCreateDialog = false
            },
        )
    }

    editingItem?.let { item ->
        DhikrEditorDialog(
            initialItem = item,
            onDismiss = { editingItem = null },
            onSave = { phrase, subtitle, goal, hex ->
                onUpdateDhikr(item.id, phrase, subtitle, goal, hex)
                editingItem = null
            },
        )
    }
}

@Composable
private fun DailySummaryCard(
    profile: UserProfile,
    dailyLogs: List<DailyLog>,
) {
    val recentLogs = remember(dailyLogs) {
        dailyLogs.take(7).reversed()
    }
    val maxCount = (recentLogs.maxOfOrNull { it.totalCount } ?: 33L).coerceAtLeast(33L)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "حصاد الذكر اليومي",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "محفوظ ومتزامن عبر السحابة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "${profile.streakDays} يوم",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("تسبيحات اليوم", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${profile.todayTasbih}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الإجمالي الكلي", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${profile.totalLifetimeTasbih}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (recentLogs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "نشاط الأيام الأخيرة",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        recentLogs.forEach { log ->
                            val fraction = (log.totalCount.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1f)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Text(
                                    text = "${log.totalCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height((40f * fraction).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = log.dateKey.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DhikrRowCard(
    item: DhikrItem,
    isActive: Boolean,
    onUseInMisbaha: () -> Unit,
    onEdit: () -> Unit,
    onResetToday: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val accent = StoreCatalog.parseHexColor(item.accentHex, MaterialTheme.colorScheme.primary)
    val progress = (item.todayCount.toFloat() / item.dailyGoal.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dhikr_card_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.18f),
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.phrase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "اليوم: ${item.todayCount} / ${item.dailyGoal} • الكلي: ${item.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "تعديل الذكر", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onResetToday) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تصفير اليوم", modifier = Modifier.size(20.dp))
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "حذف الذكر",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Button(
                    onClick = onUseInMisbaha,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isActive) "نشط الآن" else "سبّح الآن")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DhikrEditorDialog(
    initialItem: DhikrItem?,
    onDismiss: () -> Unit,
    onSave: (phrase: String, subtitle: String, goal: Long, accentHex: String) -> Unit,
) {
    var phrase by remember { mutableStateOf(initialItem?.phrase ?: "") }
    var subtitle by remember { mutableStateOf(initialItem?.subtitle ?: "") }
    var goalText by remember { mutableStateOf((initialItem?.dailyGoal ?: 33L).toString()) }
    var accentHex by remember { mutableStateOf(initialItem?.accentHex ?: "#0F6B56") }

    val palette = listOf("#0F6B56", "#C68B2C", "#0E7490", "#8E4460", "#4F7A52", "#9C5B2E")
    val parsedGoal = goalText.toLongOrNull()?.coerceIn(1L, 100000L) ?: 33L
    val isValid = phrase.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "إضافة ذكر جديد" else "تعديل الذكر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { if (it.length <= 200) phrase = it },
                    label = { Text("نص الذكر") },
                    placeholder = { Text("مثال: سبحان الله وبحمده") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("dhikr_phrase_input"),
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { if (it.length <= 200) subtitle = it },
                    label = { Text("وصف أو فضل الذكر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            goalText = input
                        }
                    },
                    label = { Text("الهدف اليومي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dhikr_goal_input"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("لون البطاقة", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            val selected = hex.equals(accentHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StoreCatalog.parseHexColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable { accentHex = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(phrase.trim(), subtitle.trim(), parsedGoal, accentHex) },
                enabled = isValid,
                modifier = Modifier.testTag("save_dhikr_button"),
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
