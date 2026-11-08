package com.example

import com.example.game.model.LootType
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NetworkProtocolTest {

  @Test
  fun testWeaponTypes() {
    assertEquals("Assault Rifle", WeaponType.RIFLE.displayName)
    assertEquals(25, WeaponType.RIFLE.damage)
    assertEquals(30, WeaponType.RIFLE.maxClip)

    assertEquals("Tactical Shotgun", WeaponType.SHOTGUN.displayName)
    assertEquals(6, WeaponType.SHOTGUN.pellets)

    assertEquals("Plasma Cannon", WeaponType.PLASMA.displayName)
    assertEquals(45, WeaponType.PLASMA.damage)
  }

  @Test
  fun testLootTypes() {
    val medkit = LootType.MEDKIT
    assertEquals("MedKit", medkit.title)
    assertEquals("+35 Health", medkit.description)

    val armor = LootType.ARMOR_PLATE
    assertEquals("Armor Vest", armor.title)
    assertEquals("+50 Armor", armor.description)

    assertNotNull(LootType.AMMO_BOX)
    assertNotNull(LootType.SHOTGUN_DROP)
    assertNotNull(LootType.PLASMA_DROP)
  }
}
