package com.example

import com.example.game.model.LootType
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NetworkProtocolTest {

  @Test
  fun testWeaponTypes() {
    assertEquals("M4-A1 Rifle", WeaponType.RIFLE.displayName)
    assertEquals(25, WeaponType.RIFLE.damage)
    assertEquals(30, WeaponType.RIFLE.maxClip)

    assertEquals("SPAS-12 Shotgun", WeaponType.SHOTGUN.displayName)
    assertEquals(6, WeaponType.SHOTGUN.pellets)

    assertEquals("AWP Sniper", WeaponType.SNIPER.displayName)
    assertEquals(85, WeaponType.SNIPER.damage)

    assertEquals("Plasma Cannon", WeaponType.PLASMA.displayName)
    assertEquals(48, WeaponType.PLASMA.damage)
  }

  @Test
  fun testLootTypes() {
    val medkit = LootType.MEDKIT
    assertEquals("Trauma Kit", medkit.title)
    assertEquals("+35 Health", medkit.description)

    val armor = LootType.ARMOR_PLATE
    assertEquals("Kevlar Vest", armor.title)
    assertEquals("+50 Armor", armor.description)

    assertNotNull(LootType.AMMO_BOX)
    assertNotNull(LootType.SHOTGUN_DROP)
    assertNotNull(LootType.SNIPER_DROP)
    assertNotNull(LootType.PLASMA_DROP)
  }
}
