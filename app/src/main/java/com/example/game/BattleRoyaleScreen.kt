package com.example.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CosmeticCatalog
import com.example.data.model.GameMode
import com.example.data.model.GraphicsConfig
import com.example.data.model.GraphicsPreset
import com.example.data.model.HudTelemetry
import com.example.data.model.MatchPhase
import com.example.data.model.PlayerProfile
import com.example.ui.theme.BooyahGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SafeZoneEmerald
import com.example.ui.theme.StormCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun BattleRoyaleScreen(
    profile: PlayerProfile,
    mode: GameMode,
    onMatchFinished: (HudTelemetry) -> Unit,
    onExitMatch: () -> Unit
) {
    BackHandler { onExitMatch() }

    val outfit = remember(profile.equippedOutfitId) {
        CosmeticCatalog.findOutfit(profile.equippedOutfitId)
    }
    val weaponSkin = remember(profile.equippedWeaponSkinId) {
        CosmeticCatalog.findWeaponSkin(profile.equippedWeaponSkinId)
    }
    val parachute = remember(profile.equippedParachuteId) {
        CosmeticCatalog.findParachute(profile.equippedParachuteId)
    }

    var activeConfig by remember(profile.graphicsPreset, profile.targetFps) {
        mutableStateOf(GraphicsConfig.fromProfile(profile))
    }

    val engine = remember(mode, profile.userId) {
        BattleRoyaleEngine(
            mode = mode,
            playerCallsign = profile.callsign,
            outfit = outfit,
            weaponSkin = weaponSkin,
            parachuteSkin = parachute,
            graphicsConfig = activeConfig
        )
    }

    LaunchedEffect(activeConfig) {
        engine.graphicsConfig = activeConfig
    }

    var telemetry by remember { mutableStateOf(engine.buildTelemetry()) }
    var renderTick by remember { mutableIntStateOf(0) }
    var resultReported by remember { mutableStateOf(false) }

    LaunchedEffect(engine) {
        var lastNs = System.nanoTime()
        var fpsFrames = 0
        var fpsAccumMs = 0f
        while (isActive) {
            val frameStartNs = System.nanoTime()
            val dt = ((frameStartNs - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
            lastNs = frameStartNs

            engine.step(dt)
            renderTick++

            val workMs = (System.nanoTime() - frameStartNs) / 1_000_000f
            fpsFrames++
            val budgetMs = (1000L / activeConfig.targetFps.coerceIn(30, 60))
            val sleepMs = (budgetMs - workMs.toLong()).coerceAtLeast(4L)
            delay(sleepMs)

            val totalFrameMs = (System.nanoTime() - frameStartNs) / 1_000_000f
            fpsAccumMs += totalFrameMs
            if (fpsAccumMs >= 500f) {
                engine.measuredFps = ((fpsFrames * 1000f) / fpsAccumMs).toInt().coerceIn(1, 60)
                engine.measuredFrameMs = workMs
                fpsFrames = 0
                fpsAccumMs = 0f
            }

            telemetry = engine.buildTelemetry()
            if (telemetry.phase == MatchPhase.FINISHED && !resultReported) {
                resultReported = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. High-FPS Viewport-Culled Game World Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("battle_royale_canvas")
        ) {
            // Read renderTick to invalidate draw cleanly
            val tick = renderTick
            if (tick < 0) return@Canvas
            drawBattleWorld(engine, activeConfig)
        }

        // Storm & Damage screen-edge flash
        if (telemetry.inStorm || telemetry.damageFlashAlpha > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (telemetry.inStorm) 8.dp else 4.dp,
                        color = StormCrimson.copy(
                            alpha = maxOf(
                                if (telemetry.inStorm) 0.45f else 0f,
                                telemetry.damageFlashAlpha * 0.7f
                            )
                        )
                    )
            )
        }

        // 2. Tactical HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Left: Minimap + Low-End Telemetry Pill
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MinimapRadar(
                    engine = engine,
                    renderTick = renderTick,
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface.copy(alpha = 0.85f))
                        .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                )
                // Live FPS & Low-End Mode Quick Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurface.copy(alpha = 0.88f),
                    modifier = Modifier
                        .testTag("hud_fps_toggle")
                        .clickable {
                            val nextPreset = when (activeConfig.preset) {
                                GraphicsPreset.ULTRA_LOW -> GraphicsPreset.BALANCED
                                GraphicsPreset.SMOOTH -> GraphicsPreset.BALANCED
                                GraphicsPreset.BALANCED -> GraphicsPreset.HIGH
                                GraphicsPreset.HIGH -> GraphicsPreset.ULTRA_LOW
                            }
                            activeConfig = activeConfig.copy(
                                preset = nextPreset,
                                targetFps = nextPreset.defaultFps,
                                resolutionScale = nextPreset.resolutionScale.toFloat(),
                                shadowsEnabled = nextPreset.shadows,
                                particlesEnabled = nextPreset.particles
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Cycle performance preset",
                            tint = SafeZoneEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${telemetry.fps} FPS • ${activeConfig.preset.name.take(5)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeZoneEmerald
                        )
                    }
                }
            }

            // Top Center: Alive / Kills / Zone Timer
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALIVE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                "${telemetry.aliveCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("KILLS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                "${telemetry.kills}",
                                style = MaterialTheme.typography.titleLarge,
                                color = FlameOrange
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (telemetry.phase == MatchPhase.AIRDROP) "DROP" else "ZONE ${telemetry.zonePhase}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (telemetry.inStorm) StormCrimson else ElectricCyan
                            )
                            Text(
                                if (telemetry.phase == MatchPhase.AIRDROP) "${telemetry.altitudeMeters}m"
                                else "${telemetry.zoneSecondsRemaining}s",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (telemetry.inStorm) StormCrimson else ElectricCyan
                            )
                        }
                    }
                }
            }

            // Top Right: Kill Feed & Exit Button
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onExitMatch,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurface.copy(alpha = 0.85f))
                        .testTag("exit_match_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Exit match",
                        tint = TextPrimary
                    )
                }

                telemetry.killFeed.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (entry.isPlayerInvolved) {
                            FlameOrange.copy(alpha = 0.85f)
                        } else {
                            ObsidianSurface.copy(alpha = 0.78f)
                        }
                    ) {
                        Text(
                            text = "${entry.killer} [${entry.weaponCode}] ${entry.victim}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.isPlayerInvolved) ObsidianBg else TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Center-Bottom: HP, Armor & Active Weapon Cards
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .widthIn(max = 310.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (telemetry.phase == MatchPhase.AIRDROP) {
                    Button(
                        onClick = { engine.landImmediately() },
                        colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("land_now_button")
                    ) {
                        Text("LAND NOW", fontWeight = FontWeight.Bold)
                    }
                }

                // Armor & HP Bars
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurface.copy(alpha = 0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "HP ${telemetry.hp}/${telemetry.maxHp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                            Text(
                                "VEST ${telemetry.armor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (telemetry.armor / telemetry.maxArmor.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricCyan,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )
                        LinearProgressIndicator(
                            progress = { (telemetry.hp / telemetry.maxHp.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (telemetry.hp > 70) SafeZoneEmerald else StormCrimson,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )
                    }
                }

                // Weapon Slot Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeaponSlotChip(
                        label = telemetry.primaryWeapon.displayName,
                        ammo = if (telemetry.reloading) "RELOAD" else "${telemetry.ammoInMag}/${telemetry.magCapacity}",
                        selected = true,
                        onClick = { engine.switchWeaponSlot(0) },
                        modifier = Modifier.testTag("weapon_slot_primary")
                    )
                    WeaponSlotChip(
                        label = telemetry.secondaryWeapon.displayName,
                        ammo = "SWAP",
                        selected = false,
                        onClick = { engine.switchWeaponSlot(1 - telemetry.activeSlot) },
                        modifier = Modifier.testTag("weapon_slot_secondary")
                    )
                }
            }

            // Bottom-Left: Smooth Virtual Movement Joystick
            if (telemetry.phase != MatchPhase.FINISHED) {
                VirtualJoystick(
                    label = "MOVE",
                    accentColor = ElectricCyan,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 10.dp)
                        .testTag("move_joystick"),
                    onVectorChanged = { x, y ->
                        engine.moveInputX = x
                        engine.moveInputY = y
                    }
                )
            }

            // Bottom-Right: Fire Button + Aim Joystick + Tactical Abilities
            if (telemetry.phase == MatchPhase.COMBAT) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TacticalActionCircle(
                            icon = Icons.Default.LocalHospital,
                            badge = "${telemetry.medkits}",
                            color = SafeZoneEmerald,
                            contentDescription = "Use Medkit",
                            tag = "medkit_button",
                            onClick = { engine.useMedkit() }
                        )
                        TacticalActionCircle(
                            icon = Icons.Default.Refresh,
                            badge = "RLD",
                            color = BooyahGold,
                            contentDescription = "Reload weapon",
                            tag = "reload_button",
                            onClick = { engine.triggerReload() }
                        )
                        TacticalActionCircle(
                            icon = Icons.Default.Bolt,
                            badge = if (telemetry.dashCooldownFraction > 0.05f) "WAIT" else "DASH",
                            color = ElectricCyan,
                            contentDescription = "Tactical Dash",
                            tag = "dash_button",
                            onClick = { engine.triggerDash() }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tap-to-fire quick trigger button
                        Surface(
                            shape = CircleShape,
                            color = FlameOrange,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("fire_button")
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.any { it.pressed }
                                            engine.fireHeld = pressed
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Fire weapon",
                                    tint = ObsidianBg,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Smooth 360 Aim & Auto-Fire Joystick
                        VirtualJoystick(
                            label = "AIM",
                            accentColor = FlameOrange,
                            modifier = Modifier.testTag("aim_joystick"),
                            onVectorChanged = { x, y ->
                                engine.aimInputX = x
                                engine.aimInputY = y
                            }
                        )
                    }
                }
            }
        }

        // 3. Match Summary Overlay (BOOYAH! / ELIMINATED)
        if (telemetry.phase == MatchPhase.FINISHED) {
            MatchResultOverlay(
                telemetry = telemetry,
                onClaimAndExit = { onMatchFinished(telemetry) }
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onVectorChanged: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 110f

    Box(
        modifier = modifier
            .size(124.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = hypot(raw.x, raw.y)
                        val clamped = if (dist > maxRadiusPx) {
                            Offset(raw.x / dist * maxRadiusPx, raw.y / dist * maxRadiusPx)
                        } else {
                            raw
                        }
                        knobOffset = clamped
                        onVectorChanged(clamped.x / maxRadiusPx, clamped.y / maxRadiusPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.44f
            drawCircle(
                color = ObsidianSurface.copy(alpha = 0.55f),
                radius = outerR,
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.5f),
                radius = outerR,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = outerR * 0.42f,
                center = center + Offset(knobOffset.x * 0.45f, knobOffset.y * 0.45f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun TacticalActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    color: Color,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = ObsidianSurface.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.75f)),
        modifier = Modifier
            .size(50.dp)
            .testTag(tag)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size(18.dp))
            Text(badge, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

@Composable
private fun WeaponSlotChip(
    label: String,
    ammo: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) FlameOrange.copy(alpha = 0.22f) else ObsidianSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) FlameOrange else Color.White.copy(alpha = 0.14f)
        ),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) FlameOrange else TextSecondary
            )
            Text(
                text = ammo,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun MinimapRadar(
    engine: BattleRoyaleEngine,
    renderTick: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (renderTick < 0) return@Canvas
        val scale = size.minDimension / engine.mapSize
        drawRect(color = Color(0xFF102027), size = size)

        // Safe zone circle
        drawCircle(
            color = ElectricCyan,
            radius = engine.zoneRadius * scale,
            center = Offset(engine.zoneCenterX * scale, engine.zoneCenterY * scale),
            style = Stroke(width = 2.5f)
        )

        // Obstacles (sampled)
        for (i in 0 until engine.obstacles.size step 2) {
            val o = engine.obstacles[i]
            drawRect(
                color = Color(0xFF37474F),
                topLeft = Offset((o.x - o.halfW) * scale, (o.y - o.halfH) * scale),
                size = Size(o.halfW * 2f * scale, o.halfH * 2f * scale)
            )
        }

        // Supply drops
        for (i in 0 until engine.lootItems.size) {
            val l = engine.lootItems[i]
            if (l.active && l.isSupplyDrop) {
                drawCircle(
                    color = BooyahGold,
                    radius = 4f,
                    center = Offset(l.x * scale, l.y * scale)
                )
            }
        }

        // Player dot
        drawCircle(
            color = SafeZoneEmerald,
            radius = 5f,
            center = Offset(engine.player.x * scale, engine.player.y * scale)
        )
    }
}

@Composable
private fun MatchResultOverlay(
    telemetry: HudTelemetry,
    onClaimAndExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.82f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (telemetry.booyah) BooyahGold else StormCrimson
            ),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Match rank trophy",
                    tint = if (telemetry.booyah) BooyahGold else FlameOrange,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = if (telemetry.booyah) "BOOYAH!" else "RANK #${telemetry.placement}",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (telemetry.booyah) BooyahGold else TextPrimary
                )
                Text(
                    text = if (telemetry.booyah) "Last Survivor Standing on DropZone Island!"
                    else "Survived ${telemetry.survivalSeconds}s against ${telemetry.totalCount} fighters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("KILLS", "${telemetry.kills}", FlameOrange)
                    StatColumn("DAMAGE", "${telemetry.damageDealt}", ElectricCyan)
                    StatColumn("GOLD", "+${telemetry.goldEarned}", BooyahGold)
                    StatColumn("DIAMONDS", "+${telemetry.diamondsEarned}", Color(0xFFE040FB))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClaimAndExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (telemetry.booyah) BooyahGold else FlameOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("claim_match_rewards_button")
                ) {
                    Text(
                        text = "CLAIM REWARDS & RETURN",
                        color = ObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

private fun DrawScope.drawBattleWorld(
    engine: BattleRoyaleEngine,
    config: GraphicsConfig
) {
    val camX = engine.player.x
    val camY = engine.player.y
    val halfW = size.width * 0.5f
    val halfH = size.height * 0.5f

    fun toScreenX(wx: Float) = (wx - camX) + halfW
    fun toScreenY(wy: Float) = (wy - camY) + halfH

    // Island ground
    drawRect(color = Color(0xFF17271E), size = size)

    // Tactical grid lines (density scaled by resolutionScale for low-end GPU optimization)
    val gridStep = if (config.resolutionScale < 0.75f) 180f else 110f
    val startX = ((camX - halfW) / gridStep).toInt() * gridStep
    val startY = ((camY - halfH) / gridStep).toInt() * gridStep
    var gx = startX
    while (gx < camX + halfW + gridStep) {
        val sx = toScreenX(gx)
        drawLine(
            color = Color(0xFF1F3529),
            start = Offset(sx, 0f),
            end = Offset(sx, size.height),
            strokeWidth = 1.5f
        )
        gx += gridStep
    }
    var gy = startY
    while (gy < camY + halfH + gridStep) {
        val sy = toScreenY(gy)
        drawLine(
            color = Color(0xFF1F3529),
            start = Offset(0f, sy),
            end = Offset(size.width, sy),
            strokeWidth = 1.5f
        )
        gy += gridStep
    }

    // Island boundary ocean border
    val mapLeft = toScreenX(0f)
    val mapTop = toScreenY(0f)
    drawRect(
        color = ElectricCyan.copy(alpha = 0.28f),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(engine.mapSize, engine.mapSize),
        style = Stroke(width = 8f)
    )

    // Safe Zone Ring
    drawCircle(
        color = ElectricCyan.copy(alpha = 0.75f),
        radius = engine.zoneRadius,
        center = Offset(toScreenX(engine.zoneCenterX), toScreenY(engine.zoneCenterY)),
        style = Stroke(width = 5f)
    )

    val cullMargin = 120f

    // Loot Pickups (Viewport Culled)
    for (i in 0 until engine.lootItems.size) {
        val item = engine.lootItems[i]
        if (!item.active) continue
        val sx = toScreenX(item.x)
        val sy = toScreenY(item.y)
        if (sx < -cullMargin || sx > size.width + cullMargin || sy < -cullMargin || sy > size.height + cullMargin) continue

        val col = when {
            item.isSupplyDrop -> BooyahGold
            item.kind == LootKind.MEDKIT -> SafeZoneEmerald
            item.kind == LootKind.ARMOR -> ElectricCyan
            item.kind == LootKind.WEAPON -> FlameOrange
            else -> Color(0xFFFFCA28)
        }
        val boxRadius = if (item.isSupplyDrop) 18f else 11f
        if (item.isSupplyDrop) {
            drawCircle(color = BooyahGold.copy(alpha = 0.25f), radius = 30f, center = Offset(sx, sy))
        }
        drawRoundRect(
            color = col,
            topLeft = Offset(sx - boxRadius, sy - boxRadius),
            size = Size(boxRadius * 2f, boxRadius * 2f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }

    // Obstacles (Viewport Culled + Optional Shadows)
    for (i in 0 until engine.obstacles.size) {
        val o = engine.obstacles[i]
        val sx = toScreenX(o.x)
        val sy = toScreenY(o.y)
        if (sx + o.halfW < -cullMargin || sx - o.halfW > size.width + cullMargin ||
            sy + o.halfH < -cullMargin || sy - o.halfH > size.height + cullMargin
        ) continue

        if (config.shadowsEnabled) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(sx - o.halfW + 6f, sy - o.halfH + 8f),
                size = Size(o.halfW * 2f, o.halfH * 2f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
        if (o.kind == ObstacleKind.TREE) {
            drawCircle(color = o.color, radius = o.halfW, center = Offset(sx, sy))
            drawCircle(color = Color(0xFF2E7D32), radius = o.halfW * 0.6f, center = Offset(sx - 3f, sy - 3f))
        } else {
            drawRoundRect(
                color = o.color,
                topLeft = Offset(sx - o.halfW, sy - o.halfH),
                size = Size(o.halfW * 2f, o.halfH * 2f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(sx - o.halfW, sy - o.halfH),
                size = Size(o.halfW * 2f, o.halfH * 2f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2f)
            )
        }
    }

    // Bullets
    for (i in 0 until engine.bullets.size) {
        val b = engine.bullets[i]
        if (!b.active) continue
        val sx = toScreenX(b.x)
        val sy = toScreenY(b.y)
        if (sx < -40f || sx > size.width + 40f || sy < -40f || sy > size.height + 40f) continue
        drawLine(
            color = b.color,
            start = Offset(sx, sy),
            end = Offset(sx - b.vx * 0.018f, sy - b.vy * 0.018f),
            strokeWidth = 5f
        )
    }

    // Particles
    if (config.particlesEnabled) {
        for (i in 0 until engine.particles.size) {
            val p = engine.particles[i]
            if (!p.active) continue
            val sx = toScreenX(p.x)
            val sy = toScreenY(p.y)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius,
                center = Offset(sx, sy)
            )
        }
    }

    // Fighters (Viewport Culled)
    for (i in 0 until engine.fighters.size) {
        val f = engine.fighters[i]
        if (!f.alive) continue
        val sx = toScreenX(f.x)
        val sy = toScreenY(f.y)
        if (sx < -80f || sx > size.width + 80f || sy < -80f || sy > size.height + 80f) continue

        if (config.shadowsEnabled) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 18f,
                center = Offset(sx + 4f, sy + 6f)
            )
        }

        // Weapon barrel
        val barrelEnd = Offset(sx + cos(f.aimAngle) * 26f, sy + sin(f.aimAngle) * 26f)
        drawLine(
            color = f.weaponTracerColor,
            start = Offset(sx, sy),
            end = barrelEnd,
            strokeWidth = 6f
        )

        // Character armor ring & core
        drawCircle(color = f.glowColor.copy(alpha = 0.25f), radius = 23f, center = Offset(sx, sy))
        drawCircle(color = f.primaryColor, radius = 17f, center = Offset(sx, sy))
        drawCircle(color = f.secondaryColor, radius = 9f, center = Offset(sx, sy))

        if (f.isPlayer) {
            drawCircle(
                color = Color.White,
                radius = 18f,
                center = Offset(sx, sy),
                style = Stroke(width = 2.5f)
            )
        } else {
            // Enemy health bar
            val hpFrac = (f.hp / f.maxHp.toFloat()).coerceIn(0f, 1f)
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(sx - 18f, sy - 28f),
                size = Size(36f, 5f)
            )
            drawRect(
                color = StormCrimson,
                topLeft = Offset(sx - 18f, sy - 28f),
                size = Size(36f * hpFrac, 5f)
            )
        }
    }

    // Player Parachute during Airdrop Phase
    if (engine.phase == MatchPhase.AIRDROP) {
        val px = halfW
        val py = halfH - 34f
        val chute = engine.parachuteSkin
        val chutePath = Path().apply {
            moveTo(px - 36f, py)
            quadraticTo(px, py - 38f, px + 36f, py)
            close()
        }
        drawPath(path = chutePath, color = chute.primaryColor)
        drawPath(path = chutePath, color = chute.secondaryColor, style = Stroke(width = 3f))
        drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(px - 28f, py), end = Offset(halfW, halfH))
        drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(px + 28f, py), end = Offset(halfW, halfH))
    }

    // Center Hit Marker
    if (engine.buildTelemetry().hitMarkerAlpha > 0.05f) {
        val a = engine.buildTelemetry().hitMarkerAlpha
        val cx = halfW
        val cy = halfH
        drawLine(BooyahGold.copy(alpha = a), Offset(cx - 14f, cy - 14f), Offset(cx - 5f, cy - 5f), 3.5f)
        drawLine(BooyahGold.copy(alpha = a), Offset(cx + 14f, cy - 14f), Offset(cx + 5f, cy - 5f), 3.5f)
        drawLine(BooyahGold.copy(alpha = a), Offset(cx - 14f, cy + 14f), Offset(cx - 5f, cy + 5f), 3.5f)
        drawLine(BooyahGold.copy(alpha = a), Offset(cx + 14f, cy + 14f), Offset(cx + 5f, cy + 5f), 3.5f)
    }
}
