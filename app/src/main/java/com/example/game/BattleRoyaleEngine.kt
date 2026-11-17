package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.model.CosmeticItem
import com.example.data.model.GameMode
import com.example.data.model.GraphicsConfig
import com.example.data.model.HudTelemetry
import com.example.data.model.KillFeedEntry
import com.example.data.model.MatchPhase
import com.example.data.model.WeaponType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class ObstacleKind { BUILDING, ROCK, TREE, CRATE }

data class Obstacle(
    val x: Float,
    val y: Float,
    val halfW: Float,
    val halfH: Float,
    val kind: ObstacleKind,
    val color: Color
)

enum class LootKind { WEAPON, ARMOR, MEDKIT, AMMO }

class LootEntity(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var kind: LootKind = LootKind.MEDKIT,
    var weaponType: WeaponType = WeaponType.M4A1_RIFLE,
    var isSupplyDrop: Boolean = false
)

class BulletEntity(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var remainingDist: Float = 0f,
    var damage: Int = 15,
    var ownerId: Int = -1,
    var ownerName: String = "",
    var weaponCode: String = "AR",
    var color: Color = Color.Yellow
)

class ParticleEntity(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 0.4f,
    var radius: Float = 4f,
    var color: Color = Color.Yellow
)

class FighterEntity(
    var id: Int = 0,
    var name: String = "Bot",
    var alive: Boolean = true,
    var isPlayer: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var aimAngle: Float = 0f,
    var hp: Int = 200,
    var maxHp: Int = 200,
    var armor: Int = 0,
    var maxArmor: Int = 100,
    var weapon: WeaponType = WeaponType.M4A1_RIFLE,
    var secondaryWeapon: WeaponType = WeaponType.MP40_SMG,
    var activeSlot: Int = 0,
    var ammoInMag: Int = 30,
    var reloadingUntilMs: Long = 0L,
    var lastShotMs: Long = 0L,
    var medkits: Int = 2,
    var primaryColor: Color = Color(0xFF455A64),
    var secondaryColor: Color = Color(0xFF90A4AE),
    var glowColor: Color = Color(0xFF78909C),
    var weaponTracerColor: Color = Color(0xFFFFD600),
    var aiTargetX: Float = 0f,
    var aiTargetY: Float = 0f,
    var aiRetargetAtMs: Long = 0L,
    var lastDamageTimeMs: Long = 0L
)

class BattleRoyaleEngine(
    val mode: GameMode,
    playerCallsign: String,
    outfit: CosmeticItem,
    weaponSkin: CosmeticItem,
    val parachuteSkin: CosmeticItem,
    var graphicsConfig: GraphicsConfig
) {
    val mapSize: Float = mode.mapSize
    val obstacles = ArrayList<Obstacle>(90)
    val fighters = ArrayList<FighterEntity>(mode.totalSurvivors)
    val bullets = Array(160) { BulletEntity() }
    val particles = Array(120) { ParticleEntity() }
    val lootItems = Array(56) { LootEntity() }

    val player: FighterEntity
    private val rng = Random(System.currentTimeMillis())

    // Controls
    var moveInputX: Float = 0f
    var moveInputY: Float = 0f
    var aimInputX: Float = 0f
    var aimInputY: Float = 0f
    var fireHeld: Boolean = false

    // Match & zone state
    var phase: MatchPhase = MatchPhase.AIRDROP
    private var airdropTimerSec: Float = 3.6f
    private var matchElapsedSec: Float = 0f
    var zoneCenterX: Float = mapSize * 0.5f
    var zoneCenterY: Float = mapSize * 0.5f
    var zoneRadius: Float = mapSize * 0.62f
    private var zoneTargetRadius: Float = mapSize * 0.42f
    private var zonePhase: Int = 1
    private var zonePhaseTimer: Float = 18f
    private var lastStormTickMs: Long = 0L
    private var supplyDropTimer: Float = 14f

    // Player stats & abilities
    var kills: Int = 0
    var damageDealt: Int = 0
    private var dashReadyAtMs: Long = 0L
    private var dashActiveUntilMs: Long = 0L
    private var hitMarkerAlpha: Float = 0f
    private var damageFlashAlpha: Float = 0f
    private val killFeed = ArrayDeque<KillFeedEntry>(5)
    private var killFeedSeq: Long = 1L

    // FPS telemetry
    var measuredFps: Int = graphicsConfig.targetFps
    var measuredFrameMs: Float = 16.6f

    private val botNames = listOf(
        "Kobra-X", "Viper99", "BlazeBR", "Phantom", "Titan", "Nova-7",
        "Zephyr", "Reaper", "Volt", "ApexFox", "GhostOps", "Scorpion",
        "Shadow", "Falcon", "Lynx", "Valkyrie", "Drako", "Ronin",
        "Onyx", "Inferno", "Striker", "Pulse", "Spectre"
    )

    init {
        generateIsland()

        val spawnCenter = mapSize * 0.5f
        player = FighterEntity(
            id = 0,
            name = playerCallsign.ifBlank { "Striker" },
            alive = true,
            isPlayer = true,
            x = spawnCenter + rng.nextFloat() * 240f - 120f,
            y = spawnCenter + rng.nextFloat() * 240f - 120f,
            hp = 200,
            maxHp = 200,
            armor = 50,
            maxArmor = 100,
            weapon = WeaponType.M4A1_RIFLE,
            secondaryWeapon = WeaponType.MP40_SMG,
            ammoInMag = WeaponType.M4A1_RIFLE.magSize,
            medkits = 2,
            primaryColor = outfit.primaryColor,
            secondaryColor = outfit.secondaryColor,
            glowColor = outfit.glowColor,
            weaponTracerColor = weaponSkin.glowColor
        )
        fighters.add(player)

        val botColors = listOf(
            Color(0xFFE53935) to Color(0xFFFF8A80),
            Color(0xFF1E88E5) to Color(0xFF82B1FF),
            Color(0xFF8E24AA) to Color(0xFFEA80FC),
            Color(0xFF43A047) to Color(0xFFB9F6CA),
            Color(0xFFFB8C00) to Color(0xFFFFD180)
        )

        for (i in 1 until mode.totalSurvivors) {
            val pair = botColors[i % botColors.size]
            val pos = findClearSpawn()
            val startWeapon = if (i % 3 == 0) WeaponType.MP40_SMG else WeaponType.M4A1_RIFLE
            fighters.add(
                FighterEntity(
                    id = i,
                    name = botNames[(i - 1) % botNames.size],
                    alive = true,
                    isPlayer = false,
                    x = pos.first,
                    y = pos.second,
                    hp = 140,
                    maxHp = 140,
                    armor = if (i % 4 == 0) 40 else 0,
                    weapon = startWeapon,
                    ammoInMag = startWeapon.magSize,
                    medkits = 1,
                    primaryColor = pair.first,
                    secondaryColor = pair.second,
                    glowColor = pair.second,
                    weaponTracerColor = Color(0xFFFF7043),
                    aiTargetX = mapSize * 0.5f,
                    aiTargetY = mapSize * 0.5f
                )
            )
        }

        // Seed island loot crates
        val weaponPool = WeaponType.entries
        for (i in 0 until 40) {
            val pos = findClearSpawn()
            val kind = when {
                i % 4 == 0 -> LootKind.WEAPON
                i % 4 == 1 -> LootKind.ARMOR
                i % 4 == 2 -> LootKind.MEDKIT
                else -> LootKind.AMMO
            }
            lootItems[i].apply {
                active = true
                x = pos.first
                y = pos.second
                this.kind = kind
                weaponType = weaponPool[i % weaponPool.size]
                isSupplyDrop = false
            }
        }
    }

    private fun generateIsland() {
        // Compounds / Tactical Buildings
        val compounds = listOf(
            Triple(0.28f, 0.28f, Color(0xFF2D3A4F)),
            Triple(0.72f, 0.28f, Color(0xFF344157)),
            Triple(0.50f, 0.50f, Color(0xFF3E3546)),
            Triple(0.28f, 0.72f, Color(0xFF2D3A4F)),
            Triple(0.72f, 0.72f, Color(0xFF344157))
        )
        compounds.forEach { (nx, ny, col) ->
            val cx = mapSize * nx
            val cy = mapSize * ny
            obstacles.add(Obstacle(cx - 75f, cy - 55f, 48f, 32f, ObstacleKind.BUILDING, col))
            obstacles.add(Obstacle(cx + 80f, cy + 50f, 44f, 36f, ObstacleKind.BUILDING, col))
            obstacles.add(Obstacle(cx, cy - 110f, 36f, 22f, ObstacleKind.CRATE, Color(0xFF8D6E63)))
        }

        // Rocks & Trees scattered safely
        for (i in 0 until 52) {
            val rx = 140f + rng.nextFloat() * (mapSize - 280f)
            val ry = 140f + rng.nextFloat() * (mapSize - 280f)
            val isTree = i % 2 == 0
            obstacles.add(
                Obstacle(
                    x = rx,
                    y = ry,
                    halfW = if (isTree) 22f else 26f,
                    halfH = if (isTree) 22f else 20f,
                    kind = if (isTree) ObstacleKind.TREE else ObstacleKind.ROCK,
                    color = if (isTree) Color(0xFF1B5E20) else Color(0xFF455A64)
                )
            )
        }
    }

    private fun findClearSpawn(): Pair<Float, Float> {
        repeat(20) {
            val sx = 140f + rng.nextFloat() * (mapSize - 280f)
            val sy = 140f + rng.nextFloat() * (mapSize - 280f)
            if (!collidesWithObstacle(sx, sy, 28f)) {
                return sx to sy
            }
        }
        return mapSize * 0.5f to mapSize * 0.5f
    }

    fun landImmediately() {
        if (phase == MatchPhase.AIRDROP) {
            phase = MatchPhase.COMBAT
            airdropTimerSec = 0f
            resolveObstacleOverlap(player, 20f)
        }
    }

    fun switchWeaponSlot(slot: Int) {
        if (phase != MatchPhase.COMBAT || !player.alive) return
        if (slot == player.activeSlot) return
        val oldWeapon = player.weapon
        player.weapon = player.secondaryWeapon
        player.secondaryWeapon = oldWeapon
        player.activeSlot = slot
        player.ammoInMag = player.weapon.magSize
        player.reloadingUntilMs = 0L
    }

    fun triggerReload() {
        if (phase != MatchPhase.COMBAT || !player.alive) return
        val now = System.currentTimeMillis()
        if (player.reloadingUntilMs <= now && player.ammoInMag < player.weapon.magSize) {
            player.reloadingUntilMs = now + player.weapon.reloadMs
        }
    }

    fun useMedkit() {
        if (phase != MatchPhase.COMBAT || !player.alive) return
        if (player.medkits > 0 && player.hp < player.maxHp) {
            player.medkits -= 1
            player.hp = (player.hp + 85).coerceAtMost(player.maxHp)
            spawnParticles(player.x, player.y, Color(0xFF00E676), 10)
        }
    }

    fun triggerDash() {
        if (phase != MatchPhase.COMBAT || !player.alive) return
        val now = System.currentTimeMillis()
        if (now >= dashReadyAtMs) {
            dashActiveUntilMs = now + 260L
            dashReadyAtMs = now + 2800L
            spawnParticles(player.x, player.y, Color(0xFF00E5FF), 8)
        }
    }

    fun step(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        val now = System.currentTimeMillis()

        hitMarkerAlpha = (hitMarkerAlpha - dt * 3.5f).coerceAtLeast(0f)
        damageFlashAlpha = (damageFlashAlpha - dt * 2.4f).coerceAtLeast(0f)

        if (phase == MatchPhase.FINISHED) {
            updateParticles(dt)
            return
        }

        matchElapsedSec += dt

        if (phase == MatchPhase.AIRDROP) {
            airdropTimerSec -= dt
            // Glide steering during parachute descent
            val glideSpeed = 340f
            player.x = (player.x + moveInputX * glideSpeed * dt).coerceIn(80f, mapSize - 80f)
            player.y = (player.y + moveInputY * glideSpeed * dt).coerceIn(80f, mapSize - 80f)
            if (airdropTimerSec <= 0f) {
                landImmediately()
            }
            return
        }

        // Combat phase
        updateZone(dt, now)
        updateSupplyDrops(dt)
        updatePlayer(dt, now)
        updateBots(dt, now)
        updateBullets(dt, now)
        updateParticles(dt)
        checkVictoryOrElimination()
    }

    private fun updateZone(dt: Float, now: Long) {
        zonePhaseTimer -= dt
        if (zonePhaseTimer <= 0f) {
            zonePhase += 1
            zonePhaseTimer = 16f
            zoneTargetRadius = (zoneTargetRadius * 0.62f).coerceAtLeast(160f)
        }
        if (zoneRadius > zoneTargetRadius) {
            zoneRadius = (zoneRadius - 22f * dt).coerceAtLeast(zoneTargetRadius)
        }
        if (now - lastStormTickMs >= 1000L) {
            lastStormTickMs = now
            val stormDmg = 5 + zonePhase * 4
            for (i in 0 until fighters.size) {
                val f = fighters[i]
                if (!f.alive) continue
                val d = hypot(f.x - zoneCenterX, f.y - zoneCenterY)
                if (d > zoneRadius) {
                    applyDamage(f, stormDmg, -1, "Safe Zone", "ZONE")
                }
            }
        }
    }

    private fun updateSupplyDrops(dt: Float) {
        supplyDropTimer -= dt
        if (supplyDropTimer <= 0f) {
            supplyDropTimer = 18f
            for (i in 0 until lootItems.size) {
                if (!lootItems[i].active) {
                    val angle = rng.nextFloat() * 2f * PI.toFloat()
                    val dist = rng.nextFloat() * (zoneRadius * 0.55f)
                    val dx = (zoneCenterX + cos(angle) * dist).coerceIn(120f, mapSize - 120f)
                    val dy = (zoneCenterY + sin(angle) * dist).coerceIn(120f, mapSize - 120f)
                    lootItems[i].apply {
                        active = true
                        x = dx
                        y = dy
                        kind = LootKind.WEAPON
                        weaponType = if (rng.nextBoolean()) WeaponType.AWM_SNIPER else WeaponType.M1887_SHOTGUN
                        isSupplyDrop = true
                    }
                    break
                }
            }
        }
    }

    private fun updatePlayer(dt: Float, now: Long) {
        if (!player.alive) return

        // Finish reload
        if (player.reloadingUntilMs in 1..now) {
            player.reloadingUntilMs = 0L
            player.ammoInMag = player.weapon.magSize
        }

        // Movement
        val mag = hypot(moveInputX, moveInputY)
        val isDashing = now < dashActiveUntilMs
        val baseSpeed = if (isDashing) 520f else 255f
        if (mag > 0.05f) {
            val nx = moveInputX / maxOf(1f, mag)
            val ny = moveInputY / maxOf(1f, mag)
            moveFighter(player, nx * baseSpeed * dt, ny * baseSpeed * dt, 18f)
            if (hypot(aimInputX, aimInputY) < 0.2f && !fireHeld) {
                player.aimAngle = atan2(ny, nx)
            }
        }

        // Aiming + Smart Mobile Aim-Assist
        val aimMag = hypot(aimInputX, aimInputY)
        if (aimMag > 0.2f) {
            player.aimAngle = atan2(aimInputY, aimInputX)
        }
        val shouldFire = fireHeld || aimMag > 0.45f
        if (shouldFire) {
            if (graphicsConfig.aimAssistEnabled) {
                findAimAssistTarget(player)?.let { target ->
                    val targetAngle = atan2(target.y - player.y, target.x - player.x)
                    player.aimAngle = targetAngle
                }
            }
            tryFireWeapon(player, now)
        }

        // Auto-pickup nearby loot
        for (i in 0 until lootItems.size) {
            val item = lootItems[i]
            if (!item.active) continue
            val d = hypot(item.x - player.x, item.y - player.y)
            if (d < 36f) {
                collectLoot(player, item)
            }
        }
    }

    private fun collectLoot(f: FighterEntity, item: LootEntity) {
        when (item.kind) {
            LootKind.ARMOR -> {
                if (f.armor >= f.maxArmor) return
                f.armor = (f.armor + 60).coerceAtMost(f.maxArmor)
            }
            LootKind.MEDKIT -> {
                if (f.medkits >= 5) return
                f.medkits += 1
            }
            LootKind.AMMO -> {
                f.ammoInMag = f.weapon.magSize
                f.reloadingUntilMs = 0L
            }
            LootKind.WEAPON -> {
                if (item.isSupplyDrop || item.weaponType != f.weapon) {
                    f.secondaryWeapon = f.weapon
                    f.weapon = item.weaponType
                    f.ammoInMag = f.weapon.magSize
                    f.reloadingUntilMs = 0L
                    if (item.isSupplyDrop) {
                        f.armor = f.maxArmor
                        f.medkits = (f.medkits + 1).coerceAtMost(5)
                    }
                } else {
                    f.ammoInMag = f.weapon.magSize
                }
            }
        }
        item.active = false
        if (f.isPlayer) {
            spawnParticles(f.x, f.y, Color(0xFFFFD600), 6)
        }
    }

    private fun findAimAssistTarget(shooter: FighterEntity): FighterEntity? {
        var best: FighterEntity? = null
        var bestDist = shooter.weapon.range.coerceAtMost(480f)
        for (i in 0 until fighters.size) {
            val cand = fighters[i]
            if (!cand.alive || cand.id == shooter.id) continue
            val d = hypot(cand.x - shooter.x, cand.y - shooter.y)
            if (d < bestDist && hasLineOfSight(shooter.x, shooter.y, cand.x, cand.y)) {
                bestDist = d
                best = cand
            }
        }
        return best
    }

    private fun updateBots(dt: Float, now: Long) {
        for (i in 0 until fighters.size) {
            val bot = fighters[i]
            if (!bot.alive || bot.isPlayer) continue

            if (bot.reloadingUntilMs in 1..now) {
                bot.reloadingUntilMs = 0L
                bot.ammoInMag = bot.weapon.magSize
            }

            // Use medkit if injured
            if (bot.hp < 65 && bot.medkits > 0) {
                bot.medkits -= 1
                bot.hp = (bot.hp + 70).coerceAtMost(bot.maxHp)
            }

            // Find nearest enemy in sight
            var enemy: FighterEntity? = null
            var nearestDist = 420f
            for (j in 0 until fighters.size) {
                val other = fighters[j]
                if (!other.alive || other.id == bot.id) continue
                val d = hypot(other.x - bot.x, other.y - bot.y)
                if (d < nearestDist) {
                    nearestDist = d
                    enemy = other
                }
            }

            val distToZoneCenter = hypot(bot.x - zoneCenterX, bot.y - zoneCenterY)
            if (distToZoneCenter > zoneRadius * 0.85f) {
                // Retreat inside safe zone
                val ang = atan2(zoneCenterY - bot.y, zoneCenterX - bot.x)
                moveFighter(bot, cos(ang) * 185f * dt, sin(ang) * 185f * dt, 18f)
                bot.aimAngle = ang
            } else if (enemy != null && nearestDist < bot.weapon.range * 0.9f &&
                hasLineOfSight(bot.x, bot.y, enemy.x, enemy.y)
            ) {
                // Engage target with strafe
                val ang = atan2(enemy.y - bot.y, enemy.x - bot.x)
                bot.aimAngle = ang
                if (nearestDist > 160f) {
                    moveFighter(bot, cos(ang) * 125f * dt, sin(ang) * 125f * dt, 18f)
                } else {
                    val strafeAng = ang + if (bot.id % 2 == 0) 1.4f else -1.4f
                    moveFighter(bot, cos(strafeAng) * 115f * dt, sin(strafeAng) * 115f * dt, 18f)
                }
                if (now - bot.lastShotMs > bot.weapon.fireRateMs + 140L) {
                    tryFireWeapon(bot, now)
                }
            } else {
                // Patrol
                if (now >= bot.aiRetargetAtMs || hypot(bot.aiTargetX - bot.x, bot.aiTargetY - bot.y) < 40f) {
                    val randAng = rng.nextFloat() * 2f * PI.toFloat()
                    val randRad = rng.nextFloat() * (zoneRadius * 0.72f)
                    bot.aiTargetX = (zoneCenterX + cos(randAng) * randRad).coerceIn(100f, mapSize - 100f)
                    bot.aiTargetY = (zoneCenterY + sin(randAng) * randRad).coerceIn(100f, mapSize - 100f)
                    bot.aiRetargetAtMs = now + 2200L + rng.nextLong(1800L)
                }
                val ang = atan2(bot.aiTargetY - bot.y, bot.aiTargetX - bot.x)
                bot.aimAngle = ang
                moveFighter(bot, cos(ang) * 155f * dt, sin(ang) * 155f * dt, 18f)
            }
        }
    }

    private fun tryFireWeapon(shooter: FighterEntity, now: Long) {
        if (shooter.reloadingUntilMs > now) return
        if (shooter.ammoInMag <= 0) {
            shooter.reloadingUntilMs = now + shooter.weapon.reloadMs
            return
        }
        if (now - shooter.lastShotMs < shooter.weapon.fireRateMs) return

        shooter.lastShotMs = now
        shooter.ammoInMag -= 1

        val muzzleX = shooter.x + cos(shooter.aimAngle) * 22f
        val muzzleY = shooter.y + sin(shooter.aimAngle) * 22f

        for (p in 0 until shooter.weapon.pellets) {
            val spread = (rng.nextFloat() - 0.5f) * shooter.weapon.spreadRad
            val angle = shooter.aimAngle + spread
            spawnBullet(
                x = muzzleX,
                y = muzzleY,
                vx = cos(angle) * shooter.weapon.bulletSpeed,
                vy = sin(angle) * shooter.weapon.bulletSpeed,
                dist = shooter.weapon.range,
                damage = shooter.weapon.damage,
                ownerId = shooter.id,
                ownerName = shooter.name,
                weaponCode = shooter.weapon.shortCode,
                color = shooter.weaponTracerColor
            )
        }
        if (shooter.isPlayer && graphicsConfig.particlesEnabled) {
            spawnParticles(muzzleX, muzzleY, shooter.weaponTracerColor, 3)
        }
        if (shooter.ammoInMag <= 0) {
            shooter.reloadingUntilMs = now + shooter.weapon.reloadMs
        }
    }

    private fun spawnBullet(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        dist: Float,
        damage: Int,
        ownerId: Int,
        ownerName: String,
        weaponCode: String,
        color: Color
    ) {
        for (i in 0 until bullets.size) {
            val b = bullets[i]
            if (!b.active) {
                b.active = true
                b.x = x
                b.y = y
                b.vx = vx
                b.vy = vy
                b.remainingDist = dist
                b.damage = damage
                b.ownerId = ownerId
                b.ownerName = ownerName
                b.weaponCode = weaponCode
                b.color = color
                return
            }
        }
    }

    private fun updateBullets(dt: Float, now: Long) {
        for (i in 0 until bullets.size) {
            val b = bullets[i]
            if (!b.active) continue
            val dx = b.vx * dt
            val dy = b.vy * dt
            b.x += dx
            b.y += dy
            b.remainingDist -= hypot(dx, dy)

            if (b.remainingDist <= 0f || b.x < 0f || b.x > mapSize || b.y < 0f || b.y > mapSize) {
                b.active = false
                continue
            }

            if (collidesWithSolidObstacle(b.x, b.y, 3f)) {
                b.active = false
                if (graphicsConfig.particlesEnabled) {
                    spawnParticles(b.x, b.y, Color(0xFFB0BEC5), 3)
                }
                continue
            }

            for (j in 0 until fighters.size) {
                val target = fighters[j]
                if (!target.alive || target.id == b.ownerId) continue
                val d = hypot(target.x - b.x, target.y - b.y)
                if (d < 21f) {
                    b.active = false
                    applyDamage(target, b.damage, b.ownerId, b.ownerName, b.weaponCode)
                    target.lastDamageTimeMs = now
                    if (graphicsConfig.particlesEnabled) {
                        spawnParticles(b.x, b.y, Color(0xFFFF2E54), 5)
                    }
                    break
                }
            }
        }
    }

    private fun applyDamage(
        victim: FighterEntity,
        rawDamage: Int,
        attackerId: Int,
        attackerName: String,
        weaponCode: String
    ) {
        if (!victim.alive) return
        var remaining = rawDamage
        if (victim.armor > 0 && weaponCode != "ZONE") {
            val absorbed = (rawDamage * 0.45f).toInt().coerceAtMost(victim.armor)
            victim.armor -= absorbed
            remaining -= absorbed
        }
        victim.hp = (victim.hp - remaining).coerceAtLeast(0)

        if (attackerId == player.id) {
            damageDealt += remaining
            hitMarkerAlpha = 1f
        }
        if (victim.isPlayer && weaponCode != "ZONE") {
            damageFlashAlpha = 0.85f
        }

        if (victim.hp <= 0) {
            victim.alive = false
            if (attackerId == player.id) {
                kills += 1
            }
            pushKillFeed(attackerName, victim.name, weaponCode, attackerId == player.id || victim.isPlayer)
            dropLootOnDeath(victim)
            if (graphicsConfig.particlesEnabled) {
                spawnParticles(victim.x, victim.y, Color(0xFFFF6B1A), 12)
            }
        }
    }

    private fun dropLootOnDeath(victim: FighterEntity) {
        for (i in 0 until lootItems.size) {
            if (!lootItems[i].active) {
                lootItems[i].apply {
                    active = true
                    x = victim.x
                    y = victim.y
                    kind = if (victim.id % 2 == 0) LootKind.MEDKIT else LootKind.WEAPON
                    weaponType = victim.weapon
                    isSupplyDrop = false
                }
                return
            }
        }
    }

    private fun pushKillFeed(killer: String, victim: String, weaponCode: String, involved: Boolean) {
        if (killFeed.size >= 4) {
            killFeed.removeFirst()
        }
        killFeed.addLast(
            KillFeedEntry(
                id = killFeedSeq++,
                killer = killer,
                victim = victim,
                weaponCode = weaponCode,
                isPlayerInvolved = involved
            )
        )
    }

    private fun checkVictoryOrElimination() {
        val aliveCount = fighters.count { it.alive }
        if (!player.alive) {
            phase = MatchPhase.FINISHED
            return
        }
        if (aliveCount <= 1 && player.alive) {
            phase = MatchPhase.FINISHED
        }
    }

    private fun moveFighter(f: FighterEntity, dx: Float, dy: Float, radius: Float) {
        val nextX = (f.x + dx).coerceIn(40f, mapSize - 40f)
        if (!collidesWithObstacle(nextX, f.y, radius)) {
            f.x = nextX
        }
        val nextY = (f.y + dy).coerceIn(40f, mapSize - 40f)
        if (!collidesWithObstacle(f.x, nextY, radius)) {
            f.y = nextY
        }
    }

    private fun resolveObstacleOverlap(f: FighterEntity, radius: Float) {
        if (!collidesWithObstacle(f.x, f.y, radius)) return
        val clear = findClearSpawn()
        f.x = clear.first
        f.y = clear.second
    }

    private fun collidesWithObstacle(x: Float, y: Float, r: Float): Boolean {
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            val dx = abs(x - o.x)
            val dy = abs(y - o.y)
            if (dx < o.halfW + r && dy < o.halfH + r) return true
        }
        return false
    }

    private fun collidesWithSolidObstacle(x: Float, y: Float, r: Float): Boolean {
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            if (o.kind == ObstacleKind.TREE) continue
            val dx = abs(x - o.x)
            val dy = abs(y - o.y)
            if (dx < o.halfW + r && dy < o.halfH + r) return true
        }
        return false
    }

    private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val steps = 8
        for (s in 1 until steps) {
            val t = s.toFloat() / steps
            val px = x1 + (x2 - x1) * t
            val py = y1 + (y2 - y1) * t
            if (collidesWithSolidObstacle(px, py, 2f)) return false
        }
        return true
    }

    private fun spawnParticles(x: Float, y: Float, color: Color, count: Int) {
        if (!graphicsConfig.particlesEnabled) return
        val cap = if (graphicsConfig.resolutionScale < 0.8f) (count / 2).coerceAtLeast(1) else count
        var spawned = 0
        for (i in 0 until particles.size) {
            val p = particles[i]
            if (!p.active) {
                val ang = rng.nextFloat() * 2f * PI.toFloat()
                val spd = 40f + rng.nextFloat() * 140f
                p.active = true
                p.x = x
                p.y = y
                p.vx = cos(ang) * spd
                p.vy = sin(ang) * spd
                p.maxLife = 0.25f + rng.nextFloat() * 0.25f
                p.life = p.maxLife
                p.radius = 2.5f + rng.nextFloat() * 3.5f
                p.color = color
                spawned++
                if (spawned >= cap) return
            }
        }
    }

    private fun updateParticles(dt: Float) {
        for (i in 0 until particles.size) {
            val p = particles[i]
            if (!p.active) continue
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) p.active = false
        }
    }

    fun buildTelemetry(): HudTelemetry {
        val now = System.currentTimeMillis()
        val aliveCount = fighters.count { it.alive }
        val isBooyah = player.alive && aliveCount <= 1
        val placement = if (isBooyah) 1 else (aliveCount + if (player.alive) 0 else 1).coerceIn(1, mode.totalSurvivors)
        val goldReward = ((mode.totalSurvivors - placement + 1) * 14 + kills * 45 + if (isBooyah) 180 else 0)
            .coerceIn(40, 2000)
        val diamondReward = ((if (isBooyah) 45 else 0) + kills * 5 + if (placement <= 5) 10 else 0)
            .coerceIn(0, 250)
        val distToZone = hypot(player.x - zoneCenterX, player.y - zoneCenterY)
        val dashFrac = ((dashReadyAtMs - now).coerceAtLeast(0L) / 2800f).coerceIn(0f, 1f)

        return HudTelemetry(
            phase = phase,
            altitudeMeters = ((airdropTimerSec / 3.6f) * 180f).toInt().coerceAtLeast(0),
            aliveCount = aliveCount,
            totalCount = mode.totalSurvivors,
            kills = kills,
            damageDealt = damageDealt,
            hp = player.hp,
            maxHp = player.maxHp,
            armor = player.armor,
            maxArmor = player.maxArmor,
            primaryWeapon = player.weapon,
            secondaryWeapon = player.secondaryWeapon,
            activeSlot = player.activeSlot,
            ammoInMag = player.ammoInMag,
            magCapacity = player.weapon.magSize,
            reloading = player.reloadingUntilMs > now,
            medkits = player.medkits,
            dashCooldownFraction = dashFrac,
            zonePhase = zonePhase,
            zoneSecondsRemaining = zonePhaseTimer.toInt().coerceAtLeast(0),
            inStorm = phase == MatchPhase.COMBAT && distToZone > zoneRadius,
            fps = measuredFps,
            frameTimeMs = measuredFrameMs,
            survivalSeconds = matchElapsedSec.toInt(),
            placement = placement,
            booyah = isBooyah,
            goldEarned = goldReward,
            diamondsEarned = diamondReward,
            killFeed = killFeed.toList(),
            hitMarkerAlpha = hitMarkerAlpha,
            damageFlashAlpha = damageFlashAlpha
        )
    }
}
