package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BillingPackage
import com.example.model.PurchaseRecord
import com.example.model.StoreCatalog
import com.example.model.StoreCategory
import com.example.model.StoreItem
import com.example.model.UserProfile
import com.example.ui.components.SceneBackdrop
import com.example.ui.theme.CoinGold

@Composable
fun StoreScreen(
    profile: UserProfile,
    purchases: List<PurchaseRecord>,
    sandboxCheckoutPackage: BillingPackage?,
    onStoreItemClick: (StoreItem) -> Unit,
    onPurchasePackageClick: (Activity?, BillingPackage) -> Unit,
    onConfirmSandboxPurchase: (BillingPackage) -> Unit,
    onDismissSandboxCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == StoreCategory.ALL) {
            StoreCatalog.allItems
        } else {
            StoreCatalog.allItems.filter { it.category == selectedCategory }
        }
    }

    SceneBackdrop(
        selectedBackgroundId = profile.selectedBackgroundId,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    WalletHeaderCard(profile = profile)
                }

                // Coin Packs & VIP Play Billing section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "شحن العملات والباقات الحصرية (Google Play)",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "اشحن عملات غرس أو افتح جميع الخلفيات والثيمات والنباتات الحصرية دفعة واحدة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(StoreCatalog.billingPackages, key = { it.sku }) { pkg ->
                    BillingPackageCard(
                        pkg = pkg,
                        isVipAlreadyOwned = pkg.unlocksVip && profile.vipUnlocked,
                        onBuyClick = { onPurchasePackageClick(activity, pkg) },
                    )
                }

                // Category filter chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "متجر الثيمات والنباتات والخلفيات",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(StoreCategory.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.labelAr) },
                                )
                            }
                        }
                    }
                }

                items(filteredItems, key = { it.id }) { item ->
                    val unlocked = profile.vipUnlocked ||
                        item.coinPrice == 0L ||
                        profile.unlockedItems.contains(item.id)
                    val equipped = isItemEquipped(profile, item)
                    StoreItemCard(
                        item = item,
                        unlocked = unlocked,
                        equipped = equipped,
                        userCoins = profile.coins,
                        onActionClick = { onStoreItemClick(item) },
                    )
                }

                if (purchases.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text("سجل المشتريات والعملات", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    items(purchases.take(8), key = { it.id }) { record ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = record.priceDisplay,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                val deltaText = if (record.coinDelta >= 0) "+${record.coinDelta}" else "${record.coinDelta}"
                                Text(
                                    text = "$deltaText عملة",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.coinDelta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    sandboxCheckoutPackage?.let { pkg ->
        SandboxCheckoutDialog(
            pkg = pkg,
            onConfirm = { onConfirmSandboxPurchase(pkg) },
            onDismiss = onDismissSandboxCheckout,
        )
    }
}

@Composable
private fun WalletHeaderCard(profile: UserProfile) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_wallet_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "محفظة عملات غرس",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = CoinGold,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = "${profile.coins}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "عملة",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = "تكسب عملات مجانية مع كل ١٠ و٣٣ تسبيحة أو عبر الشحن",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            if (profile.vipUnlocked) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CoinGold,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("VIP", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingPackageCard(
    pkg: BillingPackage,
    isVipAlreadyOwned: Boolean,
    onBuyClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pkg.unlocksVip) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pkg.unlocksVip) 3.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (pkg.unlocksVip) {
                            Brush.linearGradient(listOf(CoinGold, Color(0xFFB5720C)))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (pkg.unlocksVip) Icons.Rounded.AutoAwesome else Icons.Rounded.MonetizationOn,
                    contentDescription = null,
                    tint = if (pkg.unlocksVip) Color.White else CoinGold,
                    modifier = Modifier.size(26.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = pkg.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    pkg.badgeAr?.let { badge ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = pkg.descriptionAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onBuyClick,
                enabled = !isVipAlreadyOwned,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pkg.unlocksVip) CoinGold else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.testTag("buy_package_${pkg.sku}"),
            ) {
                Text(
                    text = if (isVipAlreadyOwned) "مفعّلة" else pkg.priceDisplay,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    item: StoreItem,
    unlocked: Boolean,
    equipped: Boolean,
    userCoins: Long,
    onActionClick: () -> Unit,
) {
    val primaryColor = StoreCatalog.parseHexColor(item.previewColorHex)
    val secondaryColor = StoreCatalog.parseHexColor(item.secondaryColorHex)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_item_${item.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.backgroundDrawableRes != null) {
                Image(
                    painter = painterResource(id = item.backgroundDrawableRes),
                    contentDescription = item.titleAr,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.category) {
                        StoreCategory.PLANT -> Icon(Icons.Rounded.Spa, contentDescription = null, tint = Color.White)
                        StoreCategory.FEATURE -> Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
                        StoreCategory.BEAD_COLOR -> Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                        else -> {}
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (item.vipOnly) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CoinGold.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = "حصري",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoinGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = item.descriptionAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when {
                equipped -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("مفعّل", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                unlocked -> {
                    OutlinedButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (item.category == StoreCategory.FEATURE) "مفتوح" else "تفعيل")
                    }
                }
                else -> {
                    val canAfford = userCoins >= item.coinPrice
                    Button(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canAfford) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            imageVector = if (canAfford) Icons.Rounded.ShoppingBag else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${item.coinPrice}")
                    }
                }
            }
        }
    }
}

private fun isItemEquipped(profile: UserProfile, item: StoreItem): Boolean {
    return when (item.category) {
        StoreCategory.BEAD_COLOR -> profile.selectedBeadColorId == item.id
        StoreCategory.BACKGROUND -> profile.selectedBackgroundId == item.id
        StoreCategory.THEME -> profile.selectedThemeId == item.id
        StoreCategory.PLANT -> profile.selectedPlantId == item.id
        StoreCategory.FEATURE -> profile.vipUnlocked || profile.unlockedItems.contains(item.id)
        StoreCategory.ALL -> false
    }
}

@Composable
private fun SandboxCheckoutDialog(
    pkg: BillingPackage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("تأكيد عملية الشراء") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pkg.titleAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(pkg.descriptionAr, style = MaterialTheme.typography.bodyMedium)
                Text("المبلغ: ${pkg.priceDisplay}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "وضع المعاينة التجريبية للفوترة (Play Billing Sandbox): لم يتم ربط متجر Google Play الفعلي في المحاكي بعد، سيُنفّذ الشراء تجريبياً ويُحفظ السجل في السحابة.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_sandbox_purchase_button"),
            ) {
                Text("إتمام الدفع (${pkg.priceDisplay})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
