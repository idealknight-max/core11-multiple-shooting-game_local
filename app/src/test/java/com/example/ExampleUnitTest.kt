package com.example

import com.example.game.engine.GameMap
import com.example.game.model.LootType
import com.example.game.model.Player
import com.example.game.model.Vector2
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun armor_absorbsDamageCorrectly() {
    val player = Player(
      id = "p1",
      name = "Striker",
      x = 5f,
      y = 5f,
      angle = 0f,
      health = 100,
      armor = 50
    )

    // Damage of 30: 66% (20) absorbed by armor, 10 to health
    player.takeDamage(30)
    assertEquals(30, player.armor)
    assertEquals(90, player.health)
    assertTrue(player.isAlive)
  }

  @Test
  fun player_diesWhenHealthDepleted() {
    val player = Player(
      id = "p2",
      name = "Striker",
      x = 5f,
      y = 5f,
      angle = 0f,
      health = 20,
      armor = 0
    )

    player.takeDamage(25)
    assertEquals(0, player.health)
    assertFalse(player.isAlive)
    assertEquals(1, player.deaths)
  }

  @Test
  fun loot_restoreHealthAndArmor() {
    val player = Player(
      id = "p3",
      name = "Striker",
      x = 5f,
      y = 5f,
      angle = 0f,
      health = 50,
      armor = 20
    )

    player.heal(35)
    assertEquals(85, player.health)

    player.addArmor(50)
    assertEquals(70, player.armor)

    // Does not exceed max
    player.heal(50)
    assertEquals(100, player.health)
  }

  @Test
  fun vector2_distanceAndMath() {
    val v1 = Vector2(0f, 0f)
    val v2 = Vector2(3f, 4f)
    assertEquals(5f, v1.distanceTo(v2), 0.001f)
  }

  @Test
  fun gameMap_outerBordersAreWalls() {
    assertTrue(GameMap.isWall(0.5f, 0.5f))
    assertTrue(GameMap.isWall(19.5f, 19.5f))
    // Spawn point (3.5, 3.5) should be free floor
    assertFalse(GameMap.isWall(3.5f, 3.5f))
  }
}
