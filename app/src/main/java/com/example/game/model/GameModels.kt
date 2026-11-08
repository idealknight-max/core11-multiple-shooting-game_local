package com.example.game.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    fun length(): Float = sqrt(x * x + y * y)

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2(0f, 0f)
    }

    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vector2(x * scale, y * scale)

    companion object {
        fun fromAngle(angleRad: Float): Vector2 = Vector2(cos(angleRad), sin(angleRad))
    }
}

enum class WeaponType(
    val displayName: String,
    val damage: Int,
    val fireRateMs: Long,
    val maxClip: Int,
    val maxReserve: Int,
    val spread: Float,
    val pellets: Int,
    val range: Float,
    val colorHex: Long,
    val description: String
) {
    RIFLE(
        displayName = "M4-A1 Rifle",
        damage = 25,
        fireRateMs = 170L,
        maxClip = 30,
        maxReserve = 120,
        spread = 0.035f,
        pellets = 1,
        range = 25f,
        colorHex = 0xFF00E5FF,
        description = "Balanced rapid fire combat rifle"
    ),
    SHOTGUN(
        displayName = "SPAS-12 Shotgun",
        damage = 18,
        fireRateMs = 650L,
        maxClip = 8,
        maxReserve = 32,
        spread = 0.12f,
        pellets = 6,
        range = 14f,
        colorHex = 0xFFFF9100,
        description = "Devastating close-range scatter shot"
    ),
    SNIPER(
        displayName = "AWP Sniper",
        damage = 85,
        fireRateMs = 1100L,
        maxClip = 5,
        maxReserve = 20,
        spread = 0.005f,
        pellets = 1,
        range = 35f,
        colorHex = 0xFFFF3D00,
        description = "High-precision heavy caliber rifle"
    ),
    PLASMA(
        displayName = "Plasma Cannon",
        damage = 48,
        fireRateMs = 320L,
        maxClip = 20,
        maxReserve = 60,
        spread = 0.012f,
        pellets = 1,
        range = 28f,
        colorHex = 0xFFD500F9,
        description = "Energy blaster with plasma bolts"
    )
}

enum class LootType(
    val title: String,
    val description: String,
    val colorHex: Long,
    val iconEmoji: String,
    val associatedWeapon: WeaponType? = null
) {
    MEDKIT("Trauma Kit", "+35 Health", 0xFFFF2A55, "💚"),
    ARMOR_PLATE("Kevlar Vest", "+50 Armor", 0xFF00E5FF, "🛡️"),
    AMMO_BOX("Military Ammo Can", "+30 Ammo", 0xFFFFB300, "📦"),
    SHOTGUN_DROP("SPAS-12 Shotgun", "High close-range firepower", 0xFFFF9100, "💥", WeaponType.SHOTGUN),
    SNIPER_DROP("AWP Sniper", "Long-range precision scope", 0xFFFF3D00, "🎯", WeaponType.SNIPER),
    PLASMA_DROP("Plasma Cannon", "Futuristic energy blaster", 0xFFD500F9, "⚡", WeaponType.PLASMA)
}

data class LootItem(
    val id: String,
    val type: LootType,
    val x: Float,
    val y: Float,
    var isAvailable: Boolean = true,
    var respawnTimerSec: Float = 0f
)

data class Player(
    val id: String,
    var name: String,
    var x: Float,
    var y: Float,
    var angle: Float, // in radians (0 = east, PI/2 = south, PI = west, 3PI/2 = north)
    var health: Int = 100,
    val maxHealth: Int = 100,
    var armor: Int = 50,
    val maxArmor: Int = 100,
    var kills: Int = 0,
    var deaths: Int = 0,
    var weapon: WeaponType = WeaponType.RIFLE, // Only ONE active gun at a time!
    var ammoInClip: Int = 30,
    var reserveAmmo: Int = 90,
    var isFiring: Boolean = false,
    var isReloading: Boolean = false,
    var isAimingDownSights: Boolean = false,
    var isCrouching: Boolean = false,
    var jumpOffset: Float = 0f,
    var isAlive: Boolean = true,
    val colorHex: Long = 0xFF00E5FF,
    val isBot: Boolean = false
) {
    fun takeDamage(rawDamage: Int): Int {
        if (!isAlive) return 0
        var remainingDmg = rawDamage
        
        // Armor absorbs 66% of the damage
        if (armor > 0) {
            val armorAbsorption = round(rawDamage * 0.66f).toInt().coerceAtLeast(1)
            val actualArmorAbsorbed = armorAbsorption.coerceAtMost(armor)
            armor -= actualArmorAbsorbed
            remainingDmg -= actualArmorAbsorbed
        }

        health = (health - remainingDmg).coerceAtLeast(0)
        if (health <= 0) {
            isAlive = false
            deaths++
        }
        return rawDamage
    }

    fun heal(amount: Int) {
        health = (health + amount).coerceAtMost(maxHealth)
    }

    fun addArmor(amount: Int) {
        armor = (armor + amount).coerceAtMost(maxArmor)
    }

    fun addAmmo(amount: Int) {
        reserveAmmo = (reserveAmmo + amount).coerceAtMost(weapon.maxReserve)
    }

    fun equipNewWeapon(newWeapon: WeaponType) {
        weapon = newWeapon
        ammoInClip = newWeapon.maxClip
        reserveAmmo = (newWeapon.maxReserve / 2).coerceAtLeast(newWeapon.maxClip * 2)
    }
}

data class DamagePopup(
    val id: Long,
    val damage: Int,
    val screenX: Float,
    val screenY: Float,
    val isHeadshot: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class KillFeedEntry(
    val id: Long,
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RoomInfo(
    val roomName: String,
    val hostIp: String,
    val hostPort: Int = 8888,
    val hostPlayerName: String,
    val pingMs: Long = 0L
)
