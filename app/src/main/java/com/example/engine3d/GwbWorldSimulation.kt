package com.example.engine3d

import com.example.data.AvatarStyle
import com.example.data.FloraFaunaEntry
import com.example.data.MiniGamePortal
import com.example.data.RoomPlayer
import com.example.data.WorldBiome
import com.example.data.WorldCatalog
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class WildButterfly(
    val id: Int,
    val centerX: Float,
    val centerZ: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    var x: Float = centerX,
    var y: Float = 2f,
    var z: Float = centerZ,
    var yaw: Float = 0f,
    var cooldownTimer: Float = 0f
)

data class SparkleParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    val maxLife: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val scale: Float
)

data class RenderFriend(
    val uid: String,
    val displayName: String,
    val avatar: AvatarStyle,
    var x: Float,
    var z: Float,
    var yaw: Float,
    var targetX: Float,
    var targetZ: Float,
    var targetYaw: Float,
    val emote: String,
    var walkCycle: Float = 0f
)

data class WorldHudSnapshot(
    val playerX: Float = 0f,
    val playerZ: Float = -6f,
    val playerYaw: Float = 0f,
    val currentBiome: WorldBiome = WorldBiome.RIVER,
    val nearbyDiscovery: FloraFaunaEntry? = null,
    val nearbyPortal: MiniGamePortal? = null,
    val bearBoostActive: Boolean = false,
    val bearBoostCooldown: Float = 0f,
    val friendPositions: List<Pair<Float, Float>> = emptyList()
)

class GwbWorldSimulation {
    private val lock = Any()

    // Input controls written from Compose UI thread
    @Volatile var joyX: Float = 0f
    @Volatile var joyY: Float = 0f
    @Volatile var localAvatar: AvatarStyle = AvatarStyle.MAYA

    // Player physics state
    var playerX: Float = -12f
        private set
    var playerY: Float = 0f
        private set
    var playerZ: Float = -14f
        private set
    var playerYaw: Float = 35f
        private set
    var playerVelY: Float = 0f
        private set
    var playerSpeed: Float = 0f
        private set
    var walkCycle: Float = 0f
        private set
    var twirlTimer: Float = 0f
        private set
    var netSwingTimer: Float = 0f
        private set
    var bearBoostTimer: Float = 0f
        private set
    var bearBoostCooldown: Float = 0f
        private set

    // Barnaby Bear companion state
    var bearX: Float = -15f
        private set
    var bearY: Float = 0f
        private set
    var bearZ: Float = -16f
        private set
    var bearYaw: Float = 35f
        private set
    var bearWalkCycle: Float = 0f
        private set

    // Camera state (3rd person GTA-style chase & orbit)
    var cameraYaw: Float = 35f
        private set
    var cameraPitch: Float = 24f
        private set
    var cameraDistance: Float = 13.5f
        private set

    var elapsedTime: Float = 0f
        private set

    // Callback events emitted to ViewModel
    var onButterflyCaught: ((WildButterfly) -> Unit)? = null
    var onPortalEntered: ((MiniGamePortal) -> Unit)? = null
    var onBiomeVisited: ((WorldBiome) -> Unit)? = null

    private var lastVisitedBiome: WorldBiome = WorldBiome.COTTAGE
    private var portalTriggerCooldown: Float = 0f

    val wildButterflies = mutableListOf<WildButterfly>()
    val particles = mutableListOf<SparkleParticle>()
    val remoteFriends = mutableMapOf<String, RenderFriend>()

    init {
        val palette = listOf(
            Triple(1.0f, 0.35f, 0.65f), // Pinky
            Triple(0.25f, 0.58f, 1.0f), // Sapphire
            Triple(1.0f, 0.78f, 0.12f), // Sunbeam Gold
            Triple(0.70f, 0.42f, 1.0f)  // Starlight Lavender
        )
        for (i in 0 until 16) {
            val angle = i * (2f * PI.toFloat() / 16f)
            val dist = 10f + (i % 4) * 11f
            val cx = cos(angle) * dist
            val cz = sin(angle) * dist
            val col = palette[i % palette.size]
            wildButterflies.add(
                WildButterfly(
                    id = i,
                    centerX = cx,
                    centerZ = cz,
                    radius = 3.5f + (i % 3) * 1.8f,
                    speed = 0.7f + (i % 4) * 0.22f,
                    phase = i * 0.9f,
                    colorR = col.first,
                    colorG = col.second,
                    colorB = col.third
                )
            )
        }
        playerY = terrainHeight(playerX, playerZ)
        bearY = terrainHeight(bearX, bearZ)
    }

    fun orbitCamera(deltaYaw: Float, deltaPitch: Float) {
        synchronized(lock) {
            cameraYaw = (cameraYaw + deltaYaw) % 360f
            cameraPitch = (cameraPitch + deltaPitch).coerceIn(10f, 52f)
        }
    }

    fun snapCameraBehindPlayer() {
        synchronized(lock) {
            cameraYaw = playerYaw
            cameraPitch = 24f
        }
    }

    fun triggerJump() {
        synchronized(lock) {
            val ground = effectiveFloorHeight(playerX, playerZ)
            if (playerY <= ground + 0.15f) {
                playerVelY = 10.5f
                twirlTimer = 0.75f
                spawnBurst(playerX, playerY + 0.6f, playerZ, 1f, 0.75f, 0.9f, 14)
            }
        }
    }

    fun triggerBearBoost() {
        synchronized(lock) {
            if (bearBoostCooldown <= 0f) {
                bearBoostTimer = 5.0f
                bearBoostCooldown = 8.0f
                spawnBurst(playerX, playerY + 1.1f, playerZ, 1f, 0.78f, 0.2f, 18)
            }
        }
    }

    fun triggerSparkleNet(): List<WildButterfly> {
        val caught = mutableListOf<WildButterfly>()
        synchronized(lock) {
            netSwingTimer = 0.55f
            val forwardX = sin(Math.toRadians(playerYaw.toDouble())).toFloat()
            val forwardZ = cos(Math.toRadians(playerYaw.toDouble())).toFloat()
            val reachX = playerX + forwardX * 2.2f
            val reachZ = playerZ + forwardZ * 2.2f
            spawnBurst(reachX, playerY + 1.3f, reachZ, 1.0f, 0.45f, 0.8f, 20)

            for (bf in wildButterflies) {
                if (bf.cooldownTimer <= 0f) {
                    val dx = bf.x - playerX
                    val dz = bf.z - playerZ
                    val distSq = dx * dx + dz * dz
                    if (distSq <= 32f) {
                        bf.cooldownTimer = 10f
                        caught.add(bf)
                        spawnBurst(bf.x, bf.y, bf.z, bf.colorR, bf.colorG, bf.colorB, 22)
                    }
                }
            }
        }
        caught.forEach { onButterflyCaught?.invoke(it) }
        return caught
    }

    fun teleportTo(x: Float, z: Float) {
        synchronized(lock) {
            playerX = x.coerceIn(-74f, 74f)
            playerZ = z.coerceIn(-74f, 74f)
            playerY = effectiveFloorHeight(playerX, playerZ) + 0.5f
            playerVelY = 0f
            bearX = (playerX - 2.6f).coerceIn(-74f, 74f)
            bearZ = (playerZ - 2.6f).coerceIn(-74f, 74f)
            bearY = effectiveFloorHeight(bearX, bearZ)
            spawnBurst(playerX, playerY + 1f, playerZ, 1f, 0.6f, 0.85f, 24)
        }
    }

    fun updateRemoteFriends(players: List<RoomPlayer>, localUid: String) {
        synchronized(lock) {
            val activeIds = players.map { it.playerUid }.filter { it != localUid }.toSet()
            remoteFriends.keys.retainAll(activeIds)
            for (p in players) {
                if (p.playerUid == localUid) continue
                val existing = remoteFriends[p.playerUid]
                if (existing == null) {
                    remoteFriends[p.playerUid] = RenderFriend(
                        uid = p.playerUid,
                        displayName = p.displayName,
                        avatar = AvatarStyle.fromId(p.avatarId),
                        x = p.posX.toFloat(),
                        z = p.posZ.toFloat(),
                        yaw = p.yaw.toFloat(),
                        targetX = p.posX.toFloat(),
                        targetZ = p.posZ.toFloat(),
                        targetYaw = p.yaw.toFloat(),
                        emote = p.emote
                    )
                } else {
                    remoteFriends[p.playerUid] = existing.copy(
                        displayName = p.displayName,
                        avatar = AvatarStyle.fromId(p.avatarId),
                        targetX = p.posX.toFloat(),
                        targetZ = p.posZ.toFloat(),
                        targetYaw = p.yaw.toFloat(),
                        emote = p.emote
                    )
                }
            }
        }
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        var portalToNotify: MiniGamePortal? = null
        var biomeToNotify: WorldBiome? = null

        synchronized(lock) {
            elapsedTime += dt
            if (twirlTimer > 0f) twirlTimer = max(0f, twirlTimer - dt)
            if (netSwingTimer > 0f) netSwingTimer = max(0f, netSwingTimer - dt)
            if (bearBoostTimer > 0f) bearBoostTimer = max(0f, bearBoostTimer - dt)
            if (bearBoostCooldown > 0f) bearBoostCooldown = max(0f, bearBoostCooldown - dt)
            if (portalTriggerCooldown > 0f) portalTriggerCooldown = max(0f, portalTriggerCooldown - dt)

            // Movement relative to camera yaw
            val jx = joyX.coerceIn(-1f, 1f)
            val jy = joyY.coerceIn(-1f, 1f)
            val mag = sqrt(jx * jx + jy * jy)

            val camRad = Math.toRadians(cameraYaw.toDouble()).toFloat()
            val forwardX = sin(camRad)
            val forwardZ = cos(camRad)
            val rightX = cos(camRad)
            val rightZ = -sin(camRad)

            if (mag > 0.08f) {
                val moveDirX = rightX * jx + forwardX * (-jy)
                val moveDirZ = rightZ * jx + forwardZ * (-jy)
                val norm = sqrt(moveDirX * moveDirX + moveDirZ * moveDirZ).coerceAtLeast(0.001f)
                val dirX = moveDirX / norm
                val dirZ = moveDirZ / norm

                val maxSpeed = if (bearBoostTimer > 0f) 14.5f else 8.8f
                val targetSpeed = maxSpeed * min(1f, mag)
                playerSpeed += (targetSpeed - playerSpeed) * min(1f, dt * 10f)

                playerX = (playerX + dirX * playerSpeed * dt).coerceIn(-75f, 75f)
                playerZ = (playerZ + dirZ * playerSpeed * dt).coerceAtLeast(-75f).coerceAtMost(75f)

                val targetYaw = Math.toDegrees(atan2(dirX.toDouble(), dirZ.toDouble())).toFloat()
                playerYaw = lerpAngle(playerYaw, targetYaw, min(1f, dt * 11f))
                walkCycle += dt * playerSpeed * 1.4f
            } else {
                playerSpeed *= max(0f, 1f - dt * 10f)
            }

            // Gravity & ground clamping (includes Rainbow Bridge)
            val floorY = effectiveFloorHeight(playerX, playerZ)
            playerVelY -= 24f * dt
            playerY += playerVelY * dt
            if (playerY <= floorY) {
                playerY = floorY
                playerVelY = 0f
            }

            // Barnaby Bear companion follows smoothly
            val targetBearX = if (bearBoostTimer > 0f) {
                playerX
            } else {
                playerX - sin(Math.toRadians(playerYaw.toDouble())).toFloat() * 2.5f
            }
            val targetBearZ = if (bearBoostTimer > 0f) {
                playerZ
            } else {
                playerZ - cos(Math.toRadians(playerYaw.toDouble())).toFloat() * 2.5f
            }
            val bdx = targetBearX - bearX
            val bdz = targetBearZ - bearZ
            val bearDist = sqrt(bdx * bdx + bdz * bdz)
            if (bearBoostTimer > 0f) {
                bearX = playerX
                bearZ = playerZ
                bearY = playerY
                bearYaw = playerYaw
                bearWalkCycle = walkCycle
            } else if (bearDist > 0.35f) {
                val followSpeed = (bearDist * 3.8f).coerceIn(2f, 12f)
                bearX += (bdx / bearDist) * followSpeed * dt
                bearZ += (bdz / bearDist) * followSpeed * dt
                val bTargetYaw = Math.toDegrees(atan2(bdx.toDouble(), bdz.toDouble())).toFloat()
                bearYaw = lerpAngle(bearYaw, bTargetYaw, min(1f, dt * 8f))
                bearWalkCycle += dt * followSpeed * 1.1f
                bearY = effectiveFloorHeight(bearX, bearZ)
            } else {
                bearY = effectiveFloorHeight(bearX, bearZ)
            }

            // Animate wild butterflies
            for (bf in wildButterflies) {
                if (bf.cooldownTimer > 0f) {
                    bf.cooldownTimer = max(0f, bf.cooldownTimer - dt)
                }
                val t = elapsedTime * bf.speed + bf.phase
                val nextX = bf.centerX + cos(t) * bf.radius
                val nextZ = bf.centerZ + sin(t * 1.3f) * bf.radius
                val dx = nextX - bf.x
                val dz = nextZ - bf.z
                bf.yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
                bf.x = nextX
                bf.z = nextZ
                bf.y = effectiveFloorHeight(nextX, nextZ) + 1.6f + sin(t * 3.2f) * 0.45f
            }

            // Smoothly interpolate remote multiplayer friends
            for ((uid, rf) in remoteFriends) {
                val fdx = rf.targetX - rf.x
                val fdz = rf.targetZ - rf.z
                val dist = sqrt(fdx * fdx + fdz * fdz)
                val blend = min(1f, dt * 6f)
                rf.x += fdx * blend
                rf.z += fdz * blend
                rf.yaw = lerpAngle(rf.yaw, rf.targetYaw, blend)
                if (dist > 0.08f) {
                    rf.walkCycle += dt * 8f
                }
            }

            // Update sparkle particles
            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.life -= dt
                if (p.life <= 0f) {
                    iter.remove()
                } else {
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.z += p.vz * dt
                    p.vy -= 3.5f * dt
                }
            }

            // Biome visit check
            val currentBiome = nearestBiome(playerX, playerZ)
            if (currentBiome != lastVisitedBiome) {
                lastVisitedBiome = currentBiome
                biomeToNotify = currentBiome
            }

            // Portal proximity auto-hint (actual launch happens cleanly when walking directly into ring center)
            if (portalTriggerCooldown <= 0f) {
                for (portal in WorldCatalog.portals) {
                    val pdx = portal.worldX - playerX
                    val pdz = portal.worldZ - playerZ
                    if (pdx * pdx + pdz * pdz < 4.8f) {
                        portalTriggerCooldown = 6f
                        portalToNotify = portal
                        break
                    }
                }
            }
        }

        biomeToNotify?.let { onBiomeVisited?.invoke(it) }
        portalToNotify?.let { onPortalEntered?.invoke(it) }
    }

    fun snapshotHud(): WorldHudSnapshot {
        synchronized(lock) {
            val biome = nearestBiome(playerX, playerZ)
            var nearestDisc: FloraFaunaEntry? = null
            var bestDiscDist = 42f
            for (d in WorldCatalog.discoveries) {
                val dx = d.worldX - playerX
                val dz = d.worldZ - playerZ
                val dSq = dx * dx + dz * dz
                if (dSq < bestDiscDist) {
                    bestDiscDist = dSq
                    nearestDisc = d
                }
            }

            var nearestPortal: MiniGamePortal? = null
            var bestPortalDist = 52f
            for (p in WorldCatalog.portals) {
                val dx = p.worldX - playerX
                val dz = p.worldZ - playerZ
                val dSq = dx * dx + dz * dz
                if (dSq < bestPortalDist) {
                    bestPortalDist = dSq
                    nearestPortal = p
                }
            }

            return WorldHudSnapshot(
                playerX = playerX,
                playerZ = playerZ,
                playerYaw = playerYaw,
                currentBiome = biome,
                nearbyDiscovery = nearestDisc,
                nearbyPortal = nearestPortal,
                bearBoostActive = bearBoostTimer > 0f,
                bearBoostCooldown = bearBoostCooldown,
                friendPositions = remoteFriends.values.map { it.x to it.z }
            )
        }
    }

    fun copyRenderFriends(): List<RenderFriend> = synchronized(lock) {
        remoteFriends.values.map { it.copy() }
    }

    fun copyButterflies(): List<WildButterfly> = synchronized(lock) {
        wildButterflies.map { it.copy() }
    }

    fun copyParticles(): List<SparkleParticle> = synchronized(lock) {
        particles.map { it.copy() }
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int) {
        if (particles.size > 140) return
        for (i in 0 until count) {
            val angle = (i * 2f * PI.toFloat() / count) + (elapsedTime % 1f)
            val speed = 1.6f + (i % 4) * 0.7f
            particles.add(
                SparkleParticle(
                    x = x,
                    y = y,
                    z = z,
                    vx = cos(angle) * speed,
                    vy = 2.2f + (i % 3) * 1.1f,
                    vz = sin(angle) * speed,
                    life = 0.75f,
                    maxLife = 0.75f,
                    r = r,
                    g = g,
                    b = b,
                    scale = 0.14f + (i % 3) * 0.05f
                )
            )
        }
    }

    companion object {
        fun riverCenterX(z: Float): Float = 6.5f * sin(z * 0.055f)

        fun terrainHeight(x: Float, z: Float): Float {
            val rcx = riverCenterX(z)
            val distRiver = abs(x - rcx)
            val riverDip = if (distRiver < 6.5f) {
                val t = 1f - distRiver / 6.5f
                -1.35f * (t * t)
            } else {
                0f
            }

            // Gentle rolling storybook hills around outer perimeter and Starlight ridge
            val gentleHills =
                0.85f * sin(x * 0.06f) * cos(z * 0.05f) +
                0.45f * cos((x + z) * 0.09f)

            val starlightSlope = if (x > 16f && z > 18f) {
                val dx = (x - 16f) * 0.08f
                val dz = (z - 18f) * 0.08f
                min(4.2f, (dx * dx + dz * dz) * 0.6f)
            } else {
                0f
            }

            // Flatten around Cottage (-24, -18) and Treehouse (-26, 34) for clean architecture placement
            val cottageDist = sqrt((x + 24f) * (x + 24f) + (z + 18f) * (z + 18f))
            val treehouseDist = sqrt((x + 26f) * (x + 26f) + (z - 34f) * (z - 34f))
            val flatFactor = min(
                (cottageDist / 10f).coerceIn(0.15f, 1f),
                (treehouseDist / 11f).coerceIn(0.15f, 1f)
            )

            return (gentleHills * flatFactor + starlightSlope + riverDip)
        }

        fun effectiveFloorHeight(x: Float, z: Float): Float {
            val base = terrainHeight(x, z)
            // Rainbow arch bridge at z in [1.5, 6.5], x in [-6.5, 6.5]
            val bridgeCenterX = riverCenterX(4f)
            val relX = abs(x - bridgeCenterX)
            val relZ = abs(z - 4f)
            if (relX <= 6.2f && relZ <= 2.4f) {
                val arch = 1.25f * cos((relX / 6.2f) * (PI.toFloat() / 2f))
                return max(base, 0.35f + arch)
            }
            return base
        }

        fun nearestBiome(x: Float, z: Float): WorldBiome {
            return WorldBiome.entries.minByOrNull { biome ->
                val dx = biome.centerX - x
                val dz = biome.centerZ - z
                dx * dx + dz * dz
            } ?: WorldBiome.MEADOW
        }

        private fun lerpAngle(current: Float, target: Float, t: Float): Float {
            var diff = (target - current) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            return current + diff * t
        }
    }
}
