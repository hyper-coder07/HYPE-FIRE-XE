package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BooyahGold
import com.example.ui.theme.CommonSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.MythicMagenta
import com.example.ui.theme.RareBlue
import com.example.ui.theme.SafeZoneEmerald
import com.example.ui.theme.StormCrimson
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GraphicsPreset(
    val label: String,
    val description: String,
    val defaultFps: Int,
    val resolutionScale: Double,
    val shadows: Boolean,
    val particles: Boolean
) {
    ULTRA_LOW(
        label = "Ultra Low (1GB RAM)",
        description = "Max battery & stability. Disables shadows & particles, 30 FPS cap.",
        defaultFps = 30,
        resolutionScale = 0.65,
        shadows = false,
        particles = false
    ),
    SMOOTH(
        label = "Smooth (Low-End)",
        description = "Crisp hit feedback with minimal GPU draw calls at 30 FPS.",
        defaultFps = 30,
        resolutionScale = 0.75,
        shadows = false,
        particles = true
    ),
    BALANCED(
        label = "Balanced (45 FPS)",
        description = "Optimal clarity and smooth gunplay on mid-range devices.",
        defaultFps = 45,
        resolutionScale = 0.85,
        shadows = true,
        particles = true
    ),
    HIGH(
        label = "Ultra 60 FPS",
        description = "Full-resolution arena, dynamic shadows, and rich VFX.",
        defaultFps = 60,
        resolutionScale = 1.0,
        shadows = true,
        particles = true
    );

    companion object {
        fun fromRaw(raw: String?): GraphicsPreset =
            entries.firstOrNull { it.name == raw } ?: BALANCED
    }
}

enum class CosmeticCategory(val displayName: String) {
    OUTFIT("Character Bundles"),
    WEAPON_SKIN("Weapon Skins"),
    PARACHUTE("Parachutes")
}

enum class CosmeticRarity(val label: String, val accent: Color) {
    COMMON("Common", CommonSlate),
    RARE("Rare", RareBlue),
    EPIC("Epic", EpicViolet),
    LEGENDARY("Legendary", BooyahGold),
    MYTHIC("Mythic", MythicMagenta)
}

enum class CurrencyType(val label: String) {
    GOLD("Gold"),
    DIAMONDS("Diamonds")
}

data class CosmeticItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: CosmeticCategory,
    val rarity: CosmeticRarity,
    val currency: CurrencyType,
    val price: Long,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val bonusTag: String
)

object CosmeticCatalog {
    val items: List<CosmeticItem> = listOf(
        // OUTFITS
        CosmeticItem(
            id = "outfit_recruit",
            name = "Island Recruit",
            subtitle = "Standard tactical vest & combat visor",
            category = CosmeticCategory.OUTFIT,
            rarity = CosmeticRarity.COMMON,
            currency = CurrencyType.GOLD,
            price = 0L,
            primaryColor = Color(0xFF455A64),
            secondaryColor = Color(0xFF90A4AE),
            glowColor = Color(0xFF78909C),
            bonusTag = "Starter Kit"
        ),
        CosmeticItem(
            id = "outfit_crimson_striker",
            name = "Crimson Striker",
            subtitle = "Flame-forged exo-armor with embers aura",
            category = CosmeticCategory.OUTFIT,
            rarity = CosmeticRarity.EPIC,
            currency = CurrencyType.GOLD,
            price = 1200L,
            primaryColor = StormCrimson,
            secondaryColor = FlameOrange,
            glowColor = FlameOrange,
            bonusTag = "Flame Trail"
        ),
        CosmeticItem(
            id = "outfit_cyber_ronin",
            name = "Cyber Ronin X",
            subtitle = "Holographic samurai helm & plasma trim",
            category = CosmeticCategory.OUTFIT,
            rarity = CosmeticRarity.MYTHIC,
            currency = CurrencyType.DIAMONDS,
            price = 250L,
            primaryColor = MythicMagenta,
            secondaryColor = BooyahGold,
            glowColor = ElectricCyan,
            bonusTag = "Mythic Aura"
        ),
        CosmeticItem(
            id = "outfit_neon_valkyrie",
            name = "Neon Valkyrie",
            subtitle = "Hyper-cyan aero suit with stealth hood",
            category = CosmeticCategory.OUTFIT,
            rarity = CosmeticRarity.LEGENDARY,
            currency = CurrencyType.DIAMONDS,
            price = 180L,
            primaryColor = ElectricCyan,
            secondaryColor = Color(0xFF0055FF),
            glowColor = ElectricCyan,
            bonusTag = "Aero Visor"
        ),
        CosmeticItem(
            id = "outfit_jade_phantom",
            name = "Jade Phantom",
            subtitle = "Bio-luminescent jungle recon cloak",
            category = CosmeticCategory.OUTFIT,
            rarity = CosmeticRarity.RARE,
            currency = CurrencyType.GOLD,
            price = 800L,
            primaryColor = SafeZoneEmerald,
            secondaryColor = Color(0xFF004D40),
            glowColor = SafeZoneEmerald,
            bonusTag = "Recon Spec"
        ),

        // WEAPON SKINS
        CosmeticItem(
            id = "weapon_factory",
            name = "Factory Spec",
            subtitle = "Matte tactical gunmetal finish",
            category = CosmeticCategory.WEAPON_SKIN,
            rarity = CosmeticRarity.COMMON,
            currency = CurrencyType.GOLD,
            price = 0L,
            primaryColor = Color(0xFF546E7A),
            secondaryColor = Color(0xFFCFD8DC),
            glowColor = Color(0xFF90A4AE),
            bonusTag = "Standard"
        ),
        CosmeticItem(
            id = "weapon_dragonfire",
            name = "Dragonfire Scar",
            subtitle = "Molten core tracers & gold muzzle flare",
            category = CosmeticCategory.WEAPON_SKIN,
            rarity = CosmeticRarity.LEGENDARY,
            currency = CurrencyType.DIAMONDS,
            price = 150L,
            primaryColor = FlameOrange,
            secondaryColor = BooyahGold,
            glowColor = FlameOrange,
            bonusTag = "Molten Tracer"
        ),
        CosmeticItem(
            id = "weapon_volt_vector",
            name = "Volt Pulse",
            subtitle = "Arc-lightning cyan plasma bolt VFX",
            category = CosmeticCategory.WEAPON_SKIN,
            rarity = CosmeticRarity.EPIC,
            currency = CurrencyType.GOLD,
            price = 1500L,
            primaryColor = ElectricCyan,
            secondaryColor = Color(0xFF18FFFF),
            glowColor = ElectricCyan,
            bonusTag = "Cyan Tracer"
        ),
        CosmeticItem(
            id = "weapon_hyper_orchid",
            name = "Hyper Orchid",
            subtitle = "Mythic amethyst prism burst rounds",
            category = CosmeticCategory.WEAPON_SKIN,
            rarity = CosmeticRarity.MYTHIC,
            currency = CurrencyType.DIAMONDS,
            price = 220L,
            primaryColor = MythicMagenta,
            secondaryColor = EpicViolet,
            glowColor = MythicMagenta,
            bonusTag = "Prism Tracer"
        ),

        // PARACHUTES
        CosmeticItem(
            id = "chute_standard",
            name = "Drop Wing",
            subtitle = "Reliable olive tactical canopy",
            category = CosmeticCategory.PARACHUTE,
            rarity = CosmeticRarity.COMMON,
            currency = CurrencyType.GOLD,
            price = 0L,
            primaryColor = Color(0xFF4CAF50),
            secondaryColor = Color(0xFFC8E6C9),
            glowColor = SafeZoneEmerald,
            bonusTag = "Standard"
        ),
        CosmeticItem(
            id = "chute_solar_flare",
            name = "Solar Flare",
            subtitle = "Blazing sunburst canopy with gold streaks",
            category = CosmeticCategory.PARACHUTE,
            rarity = CosmeticRarity.EPIC,
            currency = CurrencyType.GOLD,
            price = 900L,
            primaryColor = FlameOrange,
            secondaryColor = BooyahGold,
            glowColor = BooyahGold,
            bonusTag = "Solar Trail"
        ),
        CosmeticItem(
            id = "chute_cyber_wing",
            name = "Cyber Glide",
            subtitle = "Holographic neon-cyan delta glider",
            category = CosmeticCategory.PARACHUTE,
            rarity = CosmeticRarity.LEGENDARY,
            currency = CurrencyType.DIAMONDS,
            price = 120L,
            primaryColor = ElectricCyan,
            secondaryColor = MythicMagenta,
            glowColor = ElectricCyan,
            bonusTag = "Holo Wing"
        )
    )

    fun findById(id: String): CosmeticItem =
        items.firstOrNull { it.id == id } ?: items.first()

    fun findOutfit(id: String): CosmeticItem =
        items.firstOrNull { it.id == id && it.category == CosmeticCategory.OUTFIT }
            ?: items.first { it.category == CosmeticCategory.OUTFIT }

    fun findWeaponSkin(id: String): CosmeticItem =
        items.firstOrNull { it.id == id && it.category == CosmeticCategory.WEAPON_SKIN }
            ?: items.first { it.category == CosmeticCategory.WEAPON_SKIN }

    fun findParachute(id: String): CosmeticItem =
        items.firstOrNull { it.id == id && it.category == CosmeticCategory.PARACHUTE }
            ?: items.first { it.category == CosmeticCategory.PARACHUTE }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callsign: String = "Striker",
    val diamonds: Long = 600L,
    val goldCoins: Long = 2500L,
    val booyahs: Long = 0L,
    val eliminations: Long = 0L,
    val matchesPlayed: Long = 0L,
    val equippedOutfitId: String = "outfit_recruit",
    val equippedWeaponSkinId: String = "weapon_factory",
    val equippedParachuteId: String = "chute_standard",
    val ownedCosmeticIds: List<String> = listOf(
        "outfit_recruit",
        "weapon_factory",
        "chute_standard"
    ),
    val graphicsPreset: String = GraphicsPreset.BALANCED.name,
    val targetFps: Long = 45L,
    val resolutionScale: Double = 0.85,
    val shadowsEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val aimAssistEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val placement: Long = 1L,
    val eliminations: Long = 0L,
    val damageDealt: Long = 0L,
    val survivalSeconds: Long = 0L,
    val goldEarned: Long = 0L,
    val diamondsEarned: Long = 0L,
    val mode: String = "BERMUDA_BLITZ",
    val booyah: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
