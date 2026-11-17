package com.example.data.model

enum class GameMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val totalSurvivors: Int,
    val mapSize: Float
) {
    BERMUDA_BLITZ(
        id = "BERMUDA_BLITZ",
        title = "Bermuda Blitz",
        subtitle = "24 Survivors • Fast Shrinking Zone",
        totalSurvivors = 24,
        mapSize = 2200f
    ),
    CLASH_DROP(
        id = "CLASH_DROP",
        title = "Clash Drop",
        subtitle = "16 Survivors • High-Action Arena",
        totalSurvivors = 16,
        mapSize = 1600f
    )
}

enum class WeaponType(
    val displayName: String,
    val shortCode: String,
    val damage: Int,
    val fireRateMs: Long,
    val magSize: Int,
    val reloadMs: Long,
    val bulletSpeed: Float,
    val range: Float,
    val spreadRad: Float,
    val pellets: Int
) {
    M4A1_RIFLE(
        displayName = "SCAR-L Assault",
        shortCode = "AR",
        damage = 22,
        fireRateMs = 135L,
        magSize = 30,
        reloadMs = 1350L,
        bulletSpeed = 1050f,
        range = 620f,
        spreadRad = 0.055f,
        pellets = 1
    ),
    MP40_SMG(
        displayName = "MP40 Cobra",
        shortCode = "SMG",
        damage = 15,
        fireRateMs = 82L,
        magSize = 36,
        reloadMs = 1100L,
        bulletSpeed = 940f,
        range = 430f,
        spreadRad = 0.095f,
        pellets = 1
    ),
    M1887_SHOTGUN(
        displayName = "M1887 Plasma",
        shortCode = "SG",
        damage = 19,
        fireRateMs = 620L,
        magSize = 6,
        reloadMs = 1500L,
        bulletSpeed = 880f,
        range = 290f,
        spreadRad = 0.24f,
        pellets = 5
    ),
    AWM_SNIPER(
        displayName = "AWM Glacier",
        shortCode = "SR",
        damage = 78,
        fireRateMs = 900L,
        magSize = 5,
        reloadMs = 1800L,
        bulletSpeed = 1520f,
        range = 920f,
        spreadRad = 0.01f,
        pellets = 1
    )
}

enum class MatchPhase {
    AIRDROP,
    COMBAT,
    FINISHED
}

data class GraphicsConfig(
    val preset: GraphicsPreset = GraphicsPreset.BALANCED,
    val targetFps: Int = 45,
    val resolutionScale: Float = 0.85f,
    val shadowsEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val aimAssistEnabled: Boolean = true
) {
    companion object {
        fun fromProfile(profile: PlayerProfile): GraphicsConfig = GraphicsConfig(
            preset = GraphicsPreset.fromRaw(profile.graphicsPreset),
            targetFps = profile.targetFps.toInt().coerceIn(30, 60),
            resolutionScale = profile.resolutionScale.toFloat().coerceIn(0.6f, 1.0f),
            shadowsEnabled = profile.shadowsEnabled,
            particlesEnabled = profile.particlesEnabled,
            aimAssistEnabled = profile.aimAssistEnabled
        )
    }
}

data class KillFeedEntry(
    val id: Long,
    val killer: String,
    val victim: String,
    val weaponCode: String,
    val isPlayerInvolved: Boolean
)

data class HudTelemetry(
    val phase: MatchPhase = MatchPhase.AIRDROP,
    val altitudeMeters: Int = 180,
    val aliveCount: Int = 24,
    val totalCount: Int = 24,
    val kills: Int = 0,
    val damageDealt: Int = 0,
    val hp: Int = 200,
    val maxHp: Int = 200,
    val armor: Int = 50,
    val maxArmor: Int = 100,
    val primaryWeapon: WeaponType = WeaponType.M4A1_RIFLE,
    val secondaryWeapon: WeaponType = WeaponType.MP40_SMG,
    val activeSlot: Int = 0,
    val ammoInMag: Int = 30,
    val magCapacity: Int = 30,
    val reloading: Boolean = false,
    val medkits: Int = 2,
    val dashCooldownFraction: Float = 0f,
    val zonePhase: Int = 1,
    val zoneSecondsRemaining: Int = 25,
    val inStorm: Boolean = false,
    val fps: Int = 45,
    val frameTimeMs: Float = 16.0f,
    val survivalSeconds: Int = 0,
    val placement: Int = 1,
    val booyah: Boolean = false,
    val goldEarned: Int = 0,
    val diamondsEarned: Int = 0,
    val killFeed: List<KillFeedEntry> = emptyList(),
    val hitMarkerAlpha: Float = 0f,
    val damageFlashAlpha: Float = 0f
)
