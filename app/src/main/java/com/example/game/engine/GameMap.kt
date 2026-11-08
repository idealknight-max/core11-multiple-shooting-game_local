package com.example.game.engine

import com.example.game.model.LootItem
import com.example.game.model.LootType
import com.example.game.model.Vector2
import kotlin.math.floor

object GameMap {
    // 20 x 20 arena grid
    // 0 = Empty floor
    // 1 = Outer Border Wall (Solid Titanium)
    // 2 = Tactical Barrier (Reinforced Steel)
    // 3 = Energy Pillar (Glowing Cyan)
    // 4 = Supply Crates (Industrial Amber)
    val GRID = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 2, 2, 0, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 2, 2, 0, 1),
        intArrayOf(1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1),
        intArrayOf(1, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 4, 4, 0, 0, 2, 0, 0, 2, 0, 0, 4, 4, 0, 0, 0, 1),
        intArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 2, 0, 0, 4, 0, 0, 4, 0, 0, 2, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 2, 0, 0, 4, 0, 0, 4, 0, 0, 2, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1),
        intArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 4, 4, 0, 0, 2, 0, 0, 2, 0, 0, 4, 4, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 1),
        intArrayOf(1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1),
        intArrayOf(1, 0, 2, 2, 0, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 2, 2, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    val MAP_WIDTH = GRID[0].size
    val MAP_HEIGHT = GRID.size

    val SPAWN_POINTS = listOf(
        Vector2(3.5f, 3.5f),   // Team Alpha (West-North)
        Vector2(16.5f, 16.5f), // Team Beta (East-South)
        Vector2(16.5f, 3.5f),  // Spawn 3 (East-North)
        Vector2(3.5f, 16.5f)   // Spawn 4 (West-South)
    )

    fun createInitialLoots(): List<LootItem> = listOf(
        // High Tier Center Loot
        LootItem("loot_plasma_center", LootType.PLASMA_DROP, 9.5f, 9.5f),
        LootItem("loot_armor_center_1", LootType.ARMOR_PLATE, 10.5f, 9.5f),
        LootItem("loot_armor_center_2", LootType.ARMOR_PLATE, 9.5f, 10.5f),
        
        // Mid field powerups
        LootItem("loot_shotgun_west", LootType.SHOTGUN_DROP, 4.5f, 9.5f),
        LootItem("loot_shotgun_east", LootType.SHOTGUN_DROP, 15.5f, 9.5f),

        // Health kits
        LootItem("loot_med_1", LootType.MEDKIT, 9.5f, 3.5f),
        LootItem("loot_med_2", LootType.MEDKIT, 10.5f, 3.5f),
        LootItem("loot_med_3", LootType.MEDKIT, 9.5f, 16.5f),
        LootItem("loot_med_4", LootType.MEDKIT, 10.5f, 16.5f),

        // Armor Plates
        LootItem("loot_armor_1", LootType.ARMOR_PLATE, 3.5f, 6.5f),
        LootItem("loot_armor_2", LootType.ARMOR_PLATE, 16.5f, 13.5f),

        // Ammo crates
        LootItem("loot_ammo_1", LootType.AMMO_BOX, 6.5f, 4.5f),
        LootItem("loot_ammo_2", LootType.AMMO_BOX, 13.5f, 4.5f),
        LootItem("loot_ammo_3", LootType.AMMO_BOX, 6.5f, 15.5f),
        LootItem("loot_ammo_4", LootType.AMMO_BOX, 13.5f, 15.5f)
    )

    fun isWall(x: Float, y: Float): Boolean {
        val gridX = floor(x).toInt()
        val gridY = floor(y).toInt()
        if (gridX < 0 || gridX >= MAP_WIDTH || gridY < 0 || gridY >= MAP_HEIGHT) {
            return true
        }
        return GRID[gridY][gridX] > 0
    }

    fun getWallType(gridX: Int, gridY: Int): Int {
        if (gridX < 0 || gridX >= MAP_WIDTH || gridY < 0 || gridY >= MAP_HEIGHT) return 1
        return GRID[gridY][gridX]
    }

    /**
     * Smooth collision sliding with safety radius
     */
    fun resolveCollision(currentX: Float, currentY: Float, targetX: Float, targetY: Float, radius: Float = 0.28f): Pair<Float, Float> {
        var finalX = currentX
        var finalY = currentY

        // Test X movement
        val testX = targetX
        val canMoveX = !isWall(testX - radius, currentY - radius) &&
                !isWall(testX + radius, currentY - radius) &&
                !isWall(testX - radius, currentY + radius) &&
                !isWall(testX + radius, currentY + radius)

        if (canMoveX) {
            finalX = testX
        }

        // Test Y movement
        val testY = targetY
        val canMoveY = !isWall(finalX - radius, testY - radius) &&
                !isWall(finalX + radius, testY - radius) &&
                !isWall(finalX - radius, testY + radius) &&
                !isWall(finalX + radius, testY + radius)

        if (canMoveY) {
            finalY = testY
        }

        return Pair(finalX, finalY)
    }
}
