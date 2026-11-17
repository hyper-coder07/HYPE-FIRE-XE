package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.data.model.CosmeticCatalog
import com.example.data.model.GameMode
import com.example.data.model.GraphicsPreset
import com.example.data.model.MatchRecord
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
fun LobbyScreen(
    profile: PlayerProfile,
    selectedMode: GameMode,
    recentMatches: List<MatchRecord>,
    onSelectMode: (GameMode) -> Unit,
    onDropInClicked: () -> Unit,
    onOpenOptimizer: () -> Unit
) {
    val equippedOutfit = CosmeticCatalog.findOutfit(profile.equippedOutfitId)
    val equippedWeapon = CosmeticCatalog.findWeaponSkin(profile.equippedWeaponSkinId)
    val equippedChute = CosmeticCatalog.findParachute(profile.equippedParachuteId)
    val preset = GraphicsPreset.fromRaw(profile.graphicsPreset)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Wallet & Callsign Bar
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.callsign.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${equippedOutfit.name} • ${equippedWeapon.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyBadge(
                        icon = Icons.Default.MonetizationOn,
                        value = "${profile.goldCoins}",
                        tint = BooyahGold,
                        label = "Gold"
                    )
                    CurrencyBadge(
                        icon = Icons.Default.Diamond,
                        value = "${profile.diamonds}",
                        tint = MythicMagenta,
                        label = "Diamonds"
                    )
                }
            }
        }

        // 2. Hero Island Showcase Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(205.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lobby_island),
                        contentDescription = "DropZone Island tactical lobby banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        ObsidianBg.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )

                    // Low-End Optimizer Status Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurface.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafeZoneEmerald.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .testTag("lobby_optimizer_pill")
                            .clickable(onClick = onOpenOptimizer)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Device optimizer preset",
                                tint = SafeZoneEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${preset.label} • ${profile.targetFps} FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeZoneEmerald
                            )
                        }
                    }

                    // Active Loadout Preview Strip
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "EQUIPPED LOADOUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = BooyahGold
                            )
                            Text(
                                text = "${equippedOutfit.name}  |  ${equippedChute.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        CosmeticSwatchPreview(
                            primary = equippedOutfit.primaryColor,
                            secondary = equippedOutfit.secondaryColor,
                            glow = equippedWeapon.glowColor
                        )
                    }
                }
            }
        }

        // 3. Mode Selection + DROP IN CTA
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameMode.entries.forEach { mode ->
                        val isSelected = mode == selectedMode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) FlameOrange.copy(alpha = 0.16f) else ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) FlameOrange else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .testTag("mode_card_${mode.id}")
                                .clickable { onSelectMode(mode) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) FlameOrange else TextPrimary
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDropInClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("drop_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Drop into match",
                        tint = ObsidianBg,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "DROP IN • ${selectedMode.title.uppercase()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = ObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Career Combat Stats
        item {
            Row(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CareerStatCard(
                    title = "BOOYAHS",
                    value = "${profile.booyahs}",
                    accent = BooyahGold,
                    modifier = Modifier.weight(1f)
                )
                CareerStatCard(
                    title = "ELIMS",
                    value = "${profile.eliminations}",
                    accent = FlameOrange,
                    modifier = Modifier.weight(1f)
                )
                CareerStatCard(
                    title = "MATCHES",
                    value = "${profile.matchesPlayed}",
                    accent = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Recent Match Log Header
        item {
            Row(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT DEPLOYMENTS",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Combat history",
                    tint = TextSecondary
                )
            }
        }

        if (recentMatches.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurfaceVariant,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "No matches yet",
                            tint = BooyahGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No deployments recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap DROP IN to parachute onto DropZone Island and earn Gold & Diamonds!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(recentMatches, key = { it.id.ifBlank { "${it.placement}_${it.survivalSeconds}_${it.hashCode()}" } }) { match ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (match.booyah) BooyahGold.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (match.booyah) "BOOYAH! #1" else "RANK #${match.placement}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (match.booyah) BooyahGold else TextPrimary
                            )
                            Text(
                                text = "${match.mode.replace('_', ' ')} • ${match.survivalSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "${match.eliminations} KILLS",
                                style = MaterialTheme.typography.labelMedium,
                                color = FlameOrange
                            )
                            Text(
                                text = "+${match.goldEarned}G",
                                style = MaterialTheme.typography.labelMedium,
                                color = BooyahGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun CosmeticSwatchPreview(primary: Color, secondary: Color, glow: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(primary, secondary)))
                .border(1.5.dp, glow, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun CareerStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        }
    }
}
