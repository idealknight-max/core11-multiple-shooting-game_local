package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.engine.GameMap
import com.example.game.engine.Raycaster3D
import com.example.game.model.DamagePopup
import com.example.game.model.KillFeedEntry
import com.example.game.model.LootItem
import com.example.game.model.LootType
import com.example.game.model.Player
import com.example.game.model.RoomInfo
import com.example.game.model.Vector2
import com.example.game.model.WeaponType
import com.example.game.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameMode {
    LOBBY,
    PLAYING,
    GAME_OVER
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val networkManager = NetworkManager(application)
    val raycaster = Raycaster3D()

    private val localPlayerId = UUID.randomUUID().toString().take(6)
    val playerName = MutableStateFlow("Striker_${Random.nextInt(100, 999)}")

    private val _gameMode = MutableStateFlow(GameMode.LOBBY)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _isMultiplayer = MutableStateFlow(false)
    val isMultiplayer: StateFlow<Boolean> = _isMultiplayer.asStateFlow()

    // Local player state
    private val _localPlayer = MutableStateFlow(
        Player(
            id = localPlayerId,
            name = playerName.value,
            x = 3.5f,
            y = 3.5f,
            angle = 0f,
            health = 100,
            armor = 50,
            weapon = WeaponType.RIFLE,
            colorHex = 0xFF00E5FF
        )
    )
    val localPlayer: StateFlow<Player> = _localPlayer.asStateFlow()

    // Remote friend player state
    private val _remotePlayer = MutableStateFlow<Player?>(null)
    val remotePlayer: StateFlow<Player?> = _remotePlayer.asStateFlow()

    // Bot players (for solo or bot fill)
    private val _bots = MutableStateFlow<List<Player>>(emptyList())
    val bots: StateFlow<List<Player>> = _bots.asStateFlow()

    // Loots in the arena
    private val _loots = MutableStateFlow<List<LootItem>>(GameMap.createInitialLoots())
    val loots: StateFlow<List<LootItem>> = _loots.asStateFlow()

    // Floating damage numbers
    private val _damagePopups = MutableStateFlow<List<DamagePopup>>(emptyList())
    val damagePopups: StateFlow<List<DamagePopup>> = _damagePopups.asStateFlow()

    // Kill feed
    private val _killFeed = MutableStateFlow<List<KillFeedEntry>>(emptyList())
    val killFeed: StateFlow<List<KillFeedEntry>> = _killFeed.asStateFlow()

    // Loot banner alert
    private val _lootAlert = MutableStateFlow<String?>(null)
    val lootAlert: StateFlow<String?> = _lootAlert.asStateFlow()

    // Hitmarker indicator state
    private val _hitmarkerActive = MutableStateFlow(false)
    val hitmarkerActive: StateFlow<Boolean> = _hitmarkerActive.asStateFlow()

    // Muzzle flash animation trigger
    private val _isMuzzleFlashing = MutableStateFlow(false)
    val isMuzzleFlashing: StateFlow<Boolean> = _isMuzzleFlashing.asStateFlow()

    // Recoil gun kick
    private val _gunKick = MutableStateFlow(0f)
    val gunKick: StateFlow<Float> = _gunKick.asStateFlow()

    // Respawn countdown
    private val _respawnSeconds = MutableStateFlow(0)
    val respawnSeconds: StateFlow<Int> = _respawnSeconds.asStateFlow()

    // Game stats
    private val _winnerName = MutableStateFlow<String?>(null)
    val winnerName: StateFlow<String?> = _winnerName.asStateFlow()

    private var gameLoopJob: Job? = null
    private var netListenJob: Job? = null
    private var lastShotTime = 0L

    // Virtual Joystick Inputs
    private var moveDir = Vector2(0f, 0f)
    private var lookDeltaX = 0f

    init {
        // Observe network incoming messages
        observeNetwork()
    }

    private fun observeNetwork() {
        netListenJob = viewModelScope.launch {
            networkManager.incomingMessages.collect { msg ->
                handleNetworkMessage(msg)
            }
        }
    }

    fun setJoystickMovement(x: Float, y: Float) {
        moveDir = Vector2(x, y)
    }

    fun addLookDelta(dx: Float) {
        lookDeltaX += dx
    }

    /**
     * Start Solo practice with Bots
     */
    fun startSoloGame() {
        _isMultiplayer.value = false
        networkManager.stopAll()
        initGameSession(withBots = true)
    }

    /**
     * Host a Local Wi-Fi Multiplayer Match
     */
    fun hostGame() {
        _isMultiplayer.value = true
        networkManager.startHosting(playerName.value)
        initGameSession(withBots = false)

        // Wait for connection and then start match
        viewModelScope.launch {
            networkManager.isConnected.collect { connected ->
                if (connected) {
                    _remotePlayer.value = Player(
                        id = "guest_player",
                        name = "Friend",
                        x = 16.5f,
                        y = 16.5f,
                        angle = PI.toFloat(),
                        colorHex = 0xFFFF2A55
                    )
                }
            }
        }
    }

    /**
     * Join a friend's Wi-Fi Match
     */
    fun joinGame(hostIp: String) {
        _isMultiplayer.value = true
        networkManager.connectToHost(hostIp)
        initGameSession(withBots = false)

        viewModelScope.launch {
            networkManager.isConnected.collect { connected ->
                if (connected) {
                    // Send my join packet
                    networkManager.sendMessage("JOIN:${localPlayerId}:${playerName.value}")
                    _remotePlayer.value = Player(
                        id = "host_player",
                        name = "Host Friend",
                        x = 3.5f,
                        y = 3.5f,
                        angle = 0f,
                        colorHex = 0xFFFF2A55
                    )
                }
            }
        }
    }

    private fun initGameSession(withBots: Boolean) {
        val spawn = GameMap.SPAWN_POINTS[0]
        _localPlayer.value = Player(
            id = localPlayerId,
            name = playerName.value,
            x = spawn.x,
            y = spawn.y,
            angle = 0f,
            health = 100,
            armor = 50,
            weapon = WeaponType.RIFLE,
            colorHex = 0xFF00E5FF
        )

        _loots.value = GameMap.createInitialLoots()
        _damagePopups.value = emptyList()
        _killFeed.value = emptyList()
        _winnerName.value = null
        _gameMode.value = GameMode.PLAYING

        if (withBots) {
            val botSpawn1 = GameMap.SPAWN_POINTS[1]
            val botSpawn2 = GameMap.SPAWN_POINTS[2]
            _bots.value = listOf(
                Player(
                    id = "bot_1",
                    name = "Viper Bot",
                    x = botSpawn1.x,
                    y = botSpawn1.y,
                    angle = PI.toFloat(),
                    colorHex = 0xFFFF5722,
                    isBot = true
                ),
                Player(
                    id = "bot_2",
                    name = "Titan Bot",
                    x = botSpawn2.x,
                    y = botSpawn2.y,
                    angle = (PI * 0.5).toFloat(),
                    weapon = WeaponType.SHOTGUN,
                    colorHex = 0xFFE91E63,
                    isBot = true
                )
            )
        } else {
            _bots.value = emptyList()
        }

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            var netSyncTimer = 0L

            while (isActive && _gameMode.value == GameMode.PLAYING) {
                val now = System.currentTimeMillis()
                val deltaMs = (now - lastTime).coerceAtMost(50L)
                val dt = deltaMs / 1000f
                lastTime = now

                updatePlayerPhysics(dt)
                updateLoots(dt)
                updateBots(dt)
                updateEffects(dt)

                // Network sync (~30 fps)
                netSyncTimer += deltaMs
                if (netSyncTimer >= 33L && _isMultiplayer.value && networkManager.isConnected.value) {
                    netSyncTimer = 0L
                    sendLocalStatePacket()
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun updatePlayerPhysics(dt: Float) {
        val player = _localPlayer.value
        if (!player.isAlive) return

        // 1. Update Angle from look touch
        if (abs(lookDeltaX) > 0.001f) {
            val sensitivity = 0.0055f
            player.angle += lookDeltaX * sensitivity
            while (player.angle > 2 * PI) player.angle -= (2 * PI).toFloat()
            while (player.angle < 0) player.angle += (2 * PI).toFloat()
            lookDeltaX = 0f
        }

        // 2. Update Movement from Joystick
        if (moveDir.length() > 0.1f) {
            val speed = 3.6f // units per second
            val forwardX = cos(player.angle)
            val forwardY = sin(player.angle)
            val rightX = -sin(player.angle)
            val rightY = cos(player.angle)

            // moveDir.y: -1 is up/forward, 1 is down/backward
            // moveDir.x: -1 is left, 1 is right
            val targetDx = (forwardX * -moveDir.y + rightX * moveDir.x) * speed * dt
            val targetDy = (forwardY * -moveDir.y + rightY * moveDir.x) * speed * dt

            val (newX, newY) = GameMap.resolveCollision(
                currentX = player.x,
                currentY = player.y,
                targetX = player.x + targetDx,
                targetY = player.y + targetDy
            )
            player.x = newX
            player.y = newY
        }

        // Check weapon cooldown reset
        if (player.isFiring && System.currentTimeMillis() - lastShotTime > 120) {
            player.isFiring = false
            _isMuzzleFlashing.value = false
        }
    }

    private fun updateLoots(dt: Float) {
        val currentLoots = _loots.value
        val player = _localPlayer.value
        var changed = false

        for (loot in currentLoots) {
            if (!loot.isAvailable) {
                loot.respawnTimerSec -= dt
                if (loot.respawnTimerSec <= 0f) {
                    loot.isAvailable = true
                    changed = true
                }
            } else if (player.isAlive) {
                // Check if player walked over loot
                val dist = Vector2(player.x, player.y).distanceTo(Vector2(loot.x, loot.y))
                if (dist < 0.75f) {
                    // Pick up loot!
                    loot.isAvailable = false
                    loot.respawnTimerSec = 15f // 15 sec respawn
                    applyLootPickup(loot.type)
                    changed = true

                    if (_isMultiplayer.value) {
                        networkManager.sendMessage("LOOT:${loot.id}:${player.id}")
                    }
                }
            }
        }

        if (changed) {
            _loots.value = currentLoots.toList()
        }
    }

    private fun applyLootPickup(type: LootType) {
        val player = _localPlayer.value
        when (type) {
            LootType.MEDKIT -> {
                player.heal(35)
                showLootAlert("💚 Picked up MedKit! (+35 HP)")
            }
            LootType.ARMOR_PLATE -> {
                player.addArmor(50)
                showLootAlert("🛡️ Picked up Heavy Armor! (+50 Shield)")
            }
            LootType.AMMO_BOX -> {
                player.addAmmo(30)
                showLootAlert("📦 Picked up Ammo Crate! (+30 Ammo)")
            }
            LootType.SHOTGUN_DROP -> {
                player.weapon = WeaponType.SHOTGUN
                player.ammoInClip = WeaponType.SHOTGUN.maxClip
                player.reserveAmmo = 24
                showLootAlert("💥 Acquired Tactical Shotgun!")
            }
            LootType.PLASMA_DROP -> {
                player.weapon = WeaponType.PLASMA
                player.ammoInClip = WeaponType.PLASMA.maxClip
                player.reserveAmmo = 40
                showLootAlert("⚡ Acquired Plasma Cannon!")
            }
        }
    }

    private fun showLootAlert(msg: String) {
        _lootAlert.value = msg
        viewModelScope.launch {
            delay(2200)
            if (_lootAlert.value == msg) {
                _lootAlert.value = null
            }
        }
    }

    /**
     * Shoot weapon
     */
    fun fireWeapon() {
        val player = _localPlayer.value
        if (!player.isAlive || player.isReloading) return

        val now = System.currentTimeMillis()
        if (now - lastShotTime < player.weapon.fireRateMs) return

        if (player.ammoInClip <= 0) {
            reloadWeapon()
            return
        }

        lastShotTime = now
        player.ammoInClip--
        player.isFiring = true
        _isMuzzleFlashing.value = true
        _gunKick.value = 1f

        // Gun kick decay
        viewModelScope.launch {
            delay(100)
            _gunKick.value = 0f
            _isMuzzleFlashing.value = false
        }

        // Hitscan raycast calculation
        val weapon = player.weapon
        for (p in 0 until weapon.pellets) {
            val spreadOffset = if (weapon.spread > 0) {
                (Random.nextFloat() - 0.5f) * weapon.spread
            } else 0f
            val shotAngle = player.angle + spreadOffset

            testShotHit(player, shotAngle, weapon)
        }

        // Notify multiplayer peer
        if (_isMultiplayer.value) {
            networkManager.sendMessage("SHOOT:${player.id}:${weapon.name}:${player.x}:${player.y}:${player.angle}")
        }
    }

    private fun testShotHit(shooter: Player, shotAngle: Float, weapon: WeaponType) {
        val targets = mutableListOf<Player>()
        _remotePlayer.value?.let { if (it.isAlive) targets.add(it) }
        targets.addAll(_bots.value.filter { it.isAlive })

        val rayDir = Vector2.fromAngle(shotAngle)
        var closestTarget: Player? = null
        var closestDist = weapon.range

        for (target in targets) {
            val toTarget = Vector2(target.x - shooter.x, target.y - shooter.y)
            val dist = toTarget.length()

            if (dist < closestDist) {
                // Angle between shot and target
                val targetAngle = atan2(toTarget.y, toTarget.x)
                var angleDiff = abs(shotAngle - targetAngle)
                while (angleDiff > PI) angleDiff = (2 * PI - angleDiff).toFloat()

                // Target hit radius tolerance based on distance
                val hitAngleTolerance = (0.45f / dist).coerceIn(0.02f, 0.28f)
                if (angleDiff < hitAngleTolerance) {
                    // Check if a wall is blocking line of sight
                    if (!isWallBetween(shooter.x, shooter.y, target.x, target.y)) {
                        closestDist = dist
                        closestTarget = target
                    }
                }
            }
        }

        closestTarget?.let { target ->
            val isHeadshot = Random.nextFloat() < 0.25f // 25% headshot chance
            val damage = if (isHeadshot) (weapon.damage * 1.5f).toInt() else weapon.damage

            val appliedDamage = target.takeDamage(damage)

            // Hitmarker trigger
            _hitmarkerActive.value = true
            viewModelScope.launch {
                delay(120)
                _hitmarkerActive.value = false
            }

            // Damage popup
            val popup = DamagePopup(
                id = System.currentTimeMillis() + Random.nextInt(1000),
                damage = appliedDamage,
                screenX = 0f,
                screenY = 0f,
                isHeadshot = isHeadshot
            )
            _damagePopups.value = _damagePopups.value + popup

            // Check if killed
            if (!target.isAlive) {
                shooter.kills++
                recordKill(shooter.name, target.name, weapon.displayName)
                checkMatchWin(shooter)
            }

            // Send HIT message to peer if targeting remote friend
            if (_isMultiplayer.value && target.id == _remotePlayer.value?.id) {
                networkManager.sendMessage("HIT:${target.id}:$damage:$isHeadshot:${shooter.id}")
            }
        }
    }

    private fun isWallBetween(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = sqrt(dx * dx + dy * dy)
        val steps = (dist * 4).toInt().coerceAtLeast(1)

        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            val cx = x1 + dx * t
            val cy = y1 + dy * t
            if (GameMap.isWall(cx, cy)) {
                return true
            }
        }
        return false
    }

    fun reloadWeapon() {
        val player = _localPlayer.value
        if (player.isReloading || player.reserveAmmo <= 0 || player.ammoInClip >= player.weapon.maxClip) return

        player.isReloading = true
        viewModelScope.launch {
            delay(1400) // 1.4s reload time
            val needed = player.weapon.maxClip - player.ammoInClip
            val toLoad = needed.coerceAtMost(player.reserveAmmo)
            player.ammoInClip += toLoad
            player.reserveAmmo -= toLoad
            player.isReloading = false
        }
    }

    fun switchWeapon(weaponType: WeaponType) {
        val player = _localPlayer.value
        if (player.weapon == weaponType || player.isReloading) return
        player.weapon = weaponType
        player.ammoInClip = weaponType.maxClip
        player.reserveAmmo = weaponType.maxReserve / 2
    }

    private fun recordKill(killer: String, victim: String, weapon: String) {
        val entry = KillFeedEntry(
            id = System.currentTimeMillis() + Random.nextInt(100),
            killerName = killer,
            victimName = victim,
            weaponName = weapon
        )
        val current = _killFeed.value.toMutableList()
        current.add(0, entry)
        if (current.size > 4) current.removeLast()
        _killFeed.value = current
    }

    private fun checkMatchWin(killer: Player) {
        if (killer.kills >= 10) {
            _winnerName.value = killer.name
            _gameMode.value = GameMode.GAME_OVER
        }
    }

    private fun respawnPlayer(player: Player) {
        val spawn = GameMap.SPAWN_POINTS.random()
        player.x = spawn.x
        player.y = spawn.y
        player.health = 100
        player.armor = 50
        player.ammoInClip = player.weapon.maxClip
        player.isAlive = true
    }

    private fun handleLocalPlayerDeath() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _respawnSeconds.value = i
                delay(1000)
            }
            _respawnSeconds.value = 0
            val player = _localPlayer.value
            respawnPlayer(player)
            if (_isMultiplayer.value) {
                networkManager.sendMessage("RESPAWN:${player.id}:${player.x}:${player.y}")
            }
        }
    }

    private fun updateBots(dt: Float) {
        val player = _localPlayer.value
        val botList = _bots.value
        for (bot in botList) {
            if (!bot.isAlive) continue

            // 1. Bot Movement: wander or stalk player
            val toPlayer = Vector2(player.x - bot.x, player.y - bot.y)
            val dist = toPlayer.length()

            val desiredAngle = atan2(toPlayer.y, toPlayer.x)
            bot.angle = desiredAngle

            // Move towards player if far, back up if too close
            val speed = 2.0f
            val moveDirMultiplier = when {
                dist > 7.0f -> 1.0f
                dist < 3.0f -> -0.5f
                else -> (Random.nextFloat() - 0.5f) * 0.4f
            }

            val targetDx = cos(bot.angle) * speed * dt * moveDirMultiplier
            val targetDy = sin(bot.angle) * speed * dt * moveDirMultiplier

            val (nx, ny) = GameMap.resolveCollision(bot.x, bot.y, bot.x + targetDx, bot.y + targetDy)
            bot.x = nx
            bot.y = ny

            // 2. Bot Shooting: if line of sight to player
            if (dist < 18f && !isWallBetween(bot.x, bot.y, player.x, player.y)) {
                if (Random.nextFloat() < 0.035f && player.isAlive) { // shoot chance per tick
                    bot.isFiring = true
                    val dmg = (bot.weapon.damage * 0.7f).toInt().coerceAtLeast(10)
                    player.takeDamage(dmg)

                    if (!player.isAlive) {
                        bot.kills++
                        recordKill(bot.name, player.name, bot.weapon.displayName)
                        checkMatchWin(bot)
                        handleLocalPlayerDeath()
                    }

                    viewModelScope.launch {
                        delay(100)
                        bot.isFiring = false
                    }
                }
            }
        }
    }

    private fun updateEffects(dt: Float) {
        // Prune old damage popups
        val now = System.currentTimeMillis()
        _damagePopups.value = _damagePopups.value.filter { now - it.createdAt < 900 }
    }

    private fun sendLocalStatePacket() {
        val p = _localPlayer.value
        // Format: STATE:id:x:y:angle:health:armor:weapon:isFiring:ammo:kills:deaths
        val packet = "STATE:${p.id}:${p.x}:${p.y}:${p.angle}:${p.health}:${p.armor}:${p.weapon.name}:${p.isFiring}:${p.ammoInClip}:${p.kills}:${p.deaths}"
        networkManager.sendMessage(packet)
    }

    private fun handleNetworkMessage(msg: String) {
        val parts = msg.split(":")
        if (parts.isEmpty()) return

        when (parts[0]) {
            "JOIN" -> {
                if (parts.size >= 3) {
                    val peerId = parts[1]
                    val peerName = parts[2]
                    _remotePlayer.value = Player(
                        id = peerId,
                        name = peerName,
                        x = 16.5f,
                        y = 16.5f,
                        angle = PI.toFloat(),
                        colorHex = 0xFFFF2A55
                    )
                    // Reply with my info
                    val p = _localPlayer.value
                    networkManager.sendMessage("HELLO:${p.id}:${p.name}")
                }
            }
            "HELLO" -> {
                if (parts.size >= 3) {
                    val peerId = parts[1]
                    val peerName = parts[2]
                    _remotePlayer.value = Player(
                        id = peerId,
                        name = peerName,
                        x = 16.5f,
                        y = 16.5f,
                        angle = PI.toFloat(),
                        colorHex = 0xFFFF2A55
                    )
                }
            }
            "STATE" -> {
                if (parts.size >= 12) {
                    val peerId = parts[1]
                    val rx = parts[2].toFloatOrNull() ?: 16.5f
                    val ry = parts[3].toFloatOrNull() ?: 16.5f
                    val rAngle = parts[4].toFloatOrNull() ?: 0f
                    val rHealth = parts[5].toIntOrNull() ?: 100
                    val rArmor = parts[6].toIntOrNull() ?: 50
                    val rWeapon = WeaponType.valueOf(parts[7].ifEmpty { "RIFLE" })
                    val rFiring = parts[8].toBooleanStrictOrNull() ?: false
                    val rKills = parts[10].toIntOrNull() ?: 0
                    val rDeaths = parts[11].toIntOrNull() ?: 0

                    val current = _remotePlayer.value ?: Player(
                        id = peerId,
                        name = "Friend",
                        x = rx,
                        y = ry,
                        angle = rAngle,
                        colorHex = 0xFFFF2A55
                    )

                    current.x = rx
                    current.y = ry
                    current.angle = rAngle
                    current.health = rHealth
                    current.armor = rArmor
                    current.weapon = rWeapon
                    current.isFiring = rFiring
                    current.kills = rKills
                    current.deaths = rDeaths
                    current.isAlive = rHealth > 0

                    _remotePlayer.value = current
                }
            }
            "HIT" -> {
                if (parts.size >= 5) {
                    val targetId = parts[1]
                    val dmg = parts[2].toIntOrNull() ?: 20
                    val isHead = parts[3].toBooleanStrictOrNull() ?: false
                    val attackerId = parts[4]

                    if (targetId == _localPlayer.value.id) {
                        val player = _localPlayer.value
                        player.takeDamage(dmg)
                        if (!player.isAlive) {
                            val killerName = _remotePlayer.value?.name ?: "Friend"
                            recordKill(killerName, player.name, "Rifle")
                            handleLocalPlayerDeath()
                        }
                    }
                }
            }
            "LOOT" -> {
                if (parts.size >= 3) {
                    val lootId = parts[1]
                    val pickerId = parts[2]
                    val list = _loots.value
                    list.find { it.id == lootId }?.let {
                        it.isAvailable = false
                        it.respawnTimerSec = 15f
                        _loots.value = list.toList()
                    }
                }
            }
            "RESPAWN" -> {
                if (parts.size >= 4) {
                    val peerId = parts[1]
                    val rx = parts[2].toFloatOrNull() ?: 16.5f
                    val ry = parts[3].toFloatOrNull() ?: 16.5f
                    _remotePlayer.value?.let {
                        it.x = rx
                        it.y = ry
                        it.health = 100
                        it.armor = 50
                        it.isAlive = true
                    }
                }
            }
        }
    }

    fun exitToLobby() {
        gameLoopJob?.cancel()
        networkManager.stopAll()
        _gameMode.value = GameMode.LOBBY
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        netListenJob?.cancel()
        networkManager.stopAll()
    }
}
