package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CosmeticCatalog
import com.example.data.model.CosmeticCategory
import com.example.data.model.CosmeticItem
import com.example.data.model.CurrencyType
import com.example.data.model.PlayerProfile
import com.example.ui.theme.BooyahGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.MythicMagenta
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SafeZoneEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShopScreen(
    profile: PlayerProfile,
    onBuyOrEquip: (CosmeticItem) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<CosmeticCategory?>(null) }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == null) CosmeticCatalog.items
        else CosmeticCatalog.items.filter { it.category == selectedCategory }
    }

    val mythicItem = remember { CosmeticCatalog.findById("outfit_cyber_ronin") }
    val mythicOwned = profile.ownedCosmeticIds.contains(mythicItem.id)
    val mythicEquipped = profile.equippedOutfitId == mythicItem.id

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Wallet
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COSMETIC ARMORY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Vibrant skins render live in arena",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyBadge(Icons.Default.MonetizationOn, "${profile.goldCoins}", BooyahGold, "Gold")
                    CurrencyBadge(Icons.Default.Diamond, "${profile.diamonds}", MythicMagenta, "Diamonds")
                }
            }
        }

        // Featured Mythic Bundle Hero
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MythicMagenta.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shop_mythic_bundle),
                        contentDescription = "Featured Cyber Ronin X mythic bundle",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianBg.copy(alpha = 0.92f),
                                        ObsidianBg.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .widthIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MythicMagenta.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "MYTHIC FEATURED DROP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MythicMagenta,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = mythicItem.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = mythicItem.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { onBuyOrEquip(mythicItem) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mythicEquipped) SafeZoneEmerald else MythicMagenta
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("featured_mythic_button")
                        ) {
                            Text(
                                text = when {
                                    mythicEquipped -> "EQUIPPED"
                                    mythicOwned -> "EQUIP BUNDLE"
                                    else -> "UNLOCK • ${mythicItem.price} DIAMONDS"
                                },
                                color = ObsidianBg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Row
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FlameOrange,
                        selectedLabelColor = ObsidianBg
                    ),
                    modifier = Modifier.testTag("filter_all")
                )
                CosmeticCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName.substringBefore(" ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlameOrange,
                            selectedLabelColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("filter_${cat.name}")
                    )
                }
            }
        }

        // Cosmetic Item Cards
        items(filteredItems, key = { it.id }) { item ->
            val owned = profile.ownedCosmeticIds.contains(item.id)
            val equipped = when (item.category) {
                CosmeticCategory.OUTFIT -> profile.equippedOutfitId == item.id
                CosmeticCategory.WEAPON_SKIN -> profile.equippedWeaponSkinId == item.id
                CosmeticCategory.PARACHUTE -> profile.equippedParachuteId == item.id
            }
            val canAfford = when (item.currency) {
                CurrencyType.GOLD -> profile.goldCoins >= item.price
                CurrencyType.DIAMONDS -> profile.diamonds >= item.price
            }

            CosmeticShopCard(
                item = item,
                owned = owned,
                equipped = equipped,
                canAfford = canAfford,
                onAction = { onBuyOrEquip(item) }
            )
        }
    }
}

@Composable
private fun CosmeticShopCard(
    item: CosmeticItem,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (equipped) 2.dp else 1.dp,
            color = if (equipped) SafeZoneEmerald else item.rarity.accent.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vibrant Artwork Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                item.primaryColor.copy(alpha = 0.45f),
                                ObsidianSurfaceVariant
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        color = item.glowColor.copy(alpha = 0.25f),
                        radius = size.minDimension * 0.36f,
                        center = Offset(cx, cy)
                    )
                    when (item.category) {
                        CosmeticCategory.OUTFIT -> {
                            // Stylized tactical chestplate emblem
                            val path = Path().apply {
                                moveTo(cx, cy - 26f)
                                lineTo(cx + 24f, cy - 10f)
                                lineTo(cx + 18f, cy + 22f)
                                lineTo(cx, cy + 30f)
                                lineTo(cx - 18f, cy + 22f)
                                lineTo(cx - 24f, cy - 10f)
                                close()
                            }
                            drawPath(path, color = item.primaryColor)
                            drawPath(path, color = item.secondaryColor, style = Stroke(width = 4f))
                            drawCircle(color = item.glowColor, radius = 7f, center = Offset(cx, cy - 2f))
                        }
                        CosmeticCategory.WEAPON_SKIN -> {
                            // Stylized tactical rifle + glowing tracer
                            drawRect(
                                color = item.primaryColor,
                                topLeft = Offset(cx - 38f, cy - 8f),
                                size = Size(64f, 14f)
                            )
                            drawRect(
                                color = item.secondaryColor,
                                topLeft = Offset(cx - 16f, cy + 6f),
                                size = Size(12f, 18f)
                            )
                            drawLine(
                                color = item.glowColor,
                                start = Offset(cx + 28f, cy - 2f),
                                end = Offset(cx + 52f, cy - 2f),
                                strokeWidth = 5f
                            )
                        }
                        CosmeticCategory.PARACHUTE -> {
                            // Delta canopy glider
                            val canopy = Path().apply {
                                moveTo(cx - 36f, cy + 4f)
                                quadraticTo(cx, cy - 36f, cx + 36f, cy + 4f)
                                close()
                            }
                            drawPath(canopy, color = item.primaryColor)
                            drawPath(canopy, color = item.secondaryColor, style = Stroke(width = 4f))
                            drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - 26f, cy + 4f), Offset(cx, cy + 26f), 2f)
                            drawLine(Color.White.copy(alpha = 0.7f), Offset(cx + 26f, cy + 4f), Offset(cx, cy + 26f), 2f)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = item.rarity.accent.copy(alpha = 0.22f),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                ) {
                    Text(
                        text = item.rarity.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = item.rarity.accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (equipped) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Equipped",
                        tint = SafeZoneEmerald,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = item.bonusTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onAction,
                enabled = owned || canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        equipped -> SafeZoneEmerald
                        owned -> ElectricCyan
                        item.currency == CurrencyType.DIAMONDS -> MythicMagenta
                        else -> BooyahGold
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("shop_action_${item.id}")
            ) {
                Text(
                    text = when {
                        equipped -> "EQUIPPED"
                        owned -> "EQUIP"
                        item.currency == CurrencyType.DIAMONDS -> "${item.price} DIAMONDS"
                        else -> "${item.price} GOLD"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = ObsidianBg,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
