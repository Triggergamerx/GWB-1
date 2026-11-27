package com.example.engine3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.AvatarStyle
import com.example.data.DiscoveryCategory
import com.example.data.WorldCatalog
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class GwbGlRenderer(
    private val simulation: GwbWorldSimulation
) : GLSurfaceView.Renderer {

    private var mainProgram = 0
    private var waterProgram = 0

    // Main shader uniforms
    private var uMvpLoc = -1
    private var uModelLoc = -1
    private var uTintLoc = -1
    private var uSunDirLoc = -1
    private var uCamPosLoc = -1
    private var uRimStrengthLoc = -1
    private var uAlphaLoc = -1

    // Water shader uniforms
    private var wMvpLoc = -1
    private var wModelLoc = -1
    private var wTimeLoc = -1
    private var wCamPosLoc = -1

    // GPU Meshes
    private lateinit var terrainMesh: GpuMesh
    private lateinit var waterMesh: GpuMesh
    private lateinit var cubeMesh: GpuMesh
    private lateinit var sphereMesh: GpuMesh
    private lateinit var coneMesh: GpuMesh
    private lateinit var wingMesh: GpuMesh
    private lateinit var ringMesh: GpuMesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNanos = System.nanoTime()

    private data class TreeProp(
        val x: Float,
        val z: Float,
        val scale: Float,
        val style: Int // 0 = sakura pink, 1 = emerald oak, 2 = pine
    )

    private val treeProps: List<TreeProp> = buildList {
        for (i in 0 until 54) {
            val angle = i * 2.39996f // golden angle spiral
            val dist = 12f + (i * 1.18f)
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            // Keep clear of river, cottage, bridge, and treehouse
            val riverDist = kotlin.math.abs(x - GwbWorldSimulation.riverCenterX(z))
            val nearCottage = (x + 24f) * (x + 24f) + (z + 18f) * (z + 18f) < 64f
            val nearTreehouse = (x + 26f) * (x + 26f) + (z - 34f) * (z - 34f) < 81f
            val nearBridge = kotlin.math.abs(x) < 9f && kotlin.math.abs(z - 4f) < 6f
            if (riverDist > 6.8f && !nearCottage && !nearTreehouse && !nearBridge) {
                val style = when {
                    x > 8f && z < 4f -> 0 // Blossom meadow sakura
                    x > 14f && z > 14f -> if (i % 2 == 0) 0 else 2
                    else -> if (i % 3 == 0) 2 else 1
                }
                add(TreeProp(x, z, 0.85f + (i % 4) * 0.22f, style))
            }
        }
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.99f, 0.84f, 0.91f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        mainProgram = createProgram(MAIN_VERTEX_SHADER, MAIN_FRAGMENT_SHADER)
        uMvpLoc = GLES30.glGetUniformLocation(mainProgram, "uMvp")
        uModelLoc = GLES30.glGetUniformLocation(mainProgram, "uModel")
        uTintLoc = GLES30.glGetUniformLocation(mainProgram, "uTint")
        uSunDirLoc = GLES30.glGetUniformLocation(mainProgram, "uSunDir")
        uCamPosLoc = GLES30.glGetUniformLocation(mainProgram, "uCamPos")
        uRimStrengthLoc = GLES30.glGetUniformLocation(mainProgram, "uRimStrength")
        uAlphaLoc = GLES30.glGetUniformLocation(mainProgram, "uAlpha")

        waterProgram = createProgram(WATER_VERTEX_SHADER, WATER_FRAGMENT_SHADER)
        wMvpLoc = GLES30.glGetUniformLocation(waterProgram, "uMvp")
        wModelLoc = GLES30.glGetUniformLocation(waterProgram, "uModel")
        wTimeLoc = GLES30.glGetUniformLocation(waterProgram, "uTime")
        wCamPosLoc = GLES30.glGetUniformLocation(waterProgram, "uCamPos")

        terrainMesh = GwbMeshFactory.createTerrainMesh()
        waterMesh = GwbMeshFactory.createWaterMesh()
        cubeMesh = GwbMeshFactory.createUnitCube()
        sphereMesh = GwbMeshFactory.createSphere()
        coneMesh = GwbMeshFactory.createCone()
        wingMesh = GwbMeshFactory.createButterflyWing()
        ringMesh = GwbMeshFactory.createTorusRing()
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 52f, aspect, 0.3f, 220f)
    }

    override fun onDrawFrame(unused: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000.0).toFloat()
        lastFrameNanos = now

        simulation.step(dt)
        val t = simulation.elapsedTime

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Third-person GTA-style chase camera
        val camYawRad = Math.toRadians(simulation.cameraYaw.toDouble()).toFloat()
        val camPitchRad = Math.toRadians(simulation.cameraPitch.toDouble()).toFloat()
        val dist = simulation.cameraDistance
        val horizDist = dist * cos(camPitchRad)
        val camX = simulation.playerX - sin(camYawRad) * horizDist
        val camZ = simulation.playerZ - cos(camYawRad) * horizDist
        val rawCamY = simulation.playerY + 1.8f + dist * sin(camPitchRad)
        val groundUnderCam = GwbWorldSimulation.terrainHeight(camX, camZ)
        val camY = max(rawCamY, groundUnderCam + 1.3f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            simulation.playerX, simulation.playerY + 1.35f, simulation.playerZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Main Opaque World Pass
        GLES30.glUseProgram(mainProgram)
        GLES30.glUniform3f(uSunDirLoc, 0.55f, 0.78f, 0.32f)
        GLES30.glUniform3f(uCamPosLoc, camX, camY, camZ)
        GLES30.glUniform1f(uAlphaLoc, 1.0f)
        GLES30.glUniform1f(uRimStrengthLoc, 0.15f)

        // Draw Terrain
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(terrainMesh, 1f, 1f, 1f)

        // Draw Landmarks
        drawCottage(-24f, -18f, t)
        drawTreehousePlaza(-26f, 34f, t)
        drawRainbowBridge()

        // Draw Trees
        for (tree in treeProps) {
            drawTree(tree)
        }

        // Draw Interactive Flora & Fauna
        for (entry in WorldCatalog.discoveries) {
            drawDiscoveryLandmark(entry.id, entry.category, entry.worldX, entry.worldZ, entry.accentColorHex, t)
        }

        // Draw Mini-Game Portals
        GLES30.glUniform1f(uRimStrengthLoc, 0.55f)
        for (portal in WorldCatalog.portals) {
            drawPortal(portal.worldX, portal.worldZ, portal.colorHex, t)
        }

        // Draw Barnaby Bear
        GLES30.glUniform1f(uRimStrengthLoc, 0.35f)
        drawBarnabyBear(
            x = simulation.bearX,
            y = simulation.bearY,
            z = simulation.bearZ,
            yaw = simulation.bearYaw,
            walkCycle = simulation.bearWalkCycle,
            isCarrying = simulation.bearBoostTimer > 0f
        )

        // Draw Player Character
        val playerRenderY = if (simulation.bearBoostTimer > 0f) simulation.playerY + 1.55f else simulation.playerY
        val twirlSpin = if (simulation.twirlTimer > 0f) simulation.twirlTimer * 480f else 0f
        drawAvatarCharacter(
            x = simulation.playerX,
            y = playerRenderY,
            z = simulation.playerZ,
            yaw = simulation.playerYaw + twirlSpin,
            avatar = simulation.localAvatar,
            walkCycle = simulation.walkCycle,
            netSwing = simulation.netSwingTimer,
            t = t,
            emote = ""
        )

        // Draw Remote Multiplayer Friends
        for (friend in simulation.copyRenderFriends()) {
            val fy = GwbWorldSimulation.effectiveFloorHeight(friend.x, friend.z)
            drawAvatarCharacter(
                x = friend.x,
                y = fy,
                z = friend.z,
                yaw = friend.yaw,
                avatar = friend.avatar,
                walkCycle = friend.walkCycle,
                netSwing = 0f,
                t = t,
                emote = friend.emote
            )
        }

        // Draw Hero Pinky Butterfly orbiting player
        val pinkyAngle = t * 2.3f
        val pinkyX = simulation.playerX + cos(pinkyAngle) * 1.45f
        val pinkyZ = simulation.playerZ + sin(pinkyAngle * 1.3f) * 1.45f
        val pinkyY = playerRenderY + 2.05f + sin(t * 4.5f) * 0.25f
        drawButterfly(
            x = pinkyX,
            y = pinkyY,
            z = pinkyZ,
            yaw = Math.toDegrees((-pinkyAngle).toDouble()).toFloat(),
            r = 1.0f,
            g = 0.32f,
            b = 0.72f,
            scale = 0.52f,
            flapPhase = t * 18f
        )

        // Draw Wild Meadow Butterflies
        for (bf in simulation.copyButterflies()) {
            if (bf.cooldownTimer <= 0f) {
                drawButterfly(
                    x = bf.x,
                    y = bf.y,
                    z = bf.z,
                    yaw = bf.yaw,
                    r = bf.colorR,
                    g = bf.colorG,
                    b = bf.colorB,
                    scale = 0.48f,
                    flapPhase = t * 16f + bf.phase
                )
            }
        }

        // Draw Sparkle Particles
        for (p in simulation.copyParticles()) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            val s = p.scale * (p.life / p.maxLife).coerceIn(0.2f, 1f)
            Matrix.scaleM(modelMatrix, 0, s, s, s)
            drawMesh(sphereMesh, p.r, p.g, p.b)
        }

        // 2. Translucent Shimmering Water Pass
        GLES30.glUseProgram(waterProgram)
        GLES30.glUniform1f(wTimeLoc, t)
        GLES30.glUniform3f(wCamPosLoc, camX, camY, camZ)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(wMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(wModelLoc, 1, false, modelMatrix, 0)
        waterMesh.bindAndDraw()
    }

    private fun drawMesh(mesh: GpuMesh, r: Float, g: Float, b: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform3f(uTintLoc, r, g, b)
        mesh.bindAndDraw()
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMesh(cubeMesh, r, g, b)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMesh(sphereMesh, r, g, b)
    }

    private fun drawCottage(cx: Float, cz: Float, t: Float) {
        val base = GwbWorldSimulation.terrainHeight(cx, cz)
        // Cozy timber walls
        drawBox(cx, base + 1.5f, cz, 5.2f, 3.0f, 4.4f, 0.98f, 0.90f, 0.78f)
        // Door
        drawBox(cx, base + 0.95f, cz + 2.25f, 1.1f, 1.9f, 0.2f, 0.62f, 0.36f, 0.20f)
        // Warm glowing windows
        drawBox(cx - 1.5f, base + 1.6f, cz + 2.25f, 0.9f, 0.9f, 0.15f, 1.0f, 0.86f, 0.38f)
        drawBox(cx + 1.5f, base + 1.6f, cz + 2.25f, 0.9f, 0.9f, 0.15f, 1.0f, 0.86f, 0.38f)
        // Strawberry pink pitched roof
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, base + 4.1f, cz)
        Matrix.rotateM(modelMatrix, 0, 45f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 7.2f, 2.6f, 7.2f)
        drawMesh(coneMesh, 0.96f, 0.36f, 0.56f)
        // Chimney + animated smoke puffs
        drawBox(cx + 1.5f, base + 4.2f, cz - 0.9f, 0.7f, 1.8f, 0.7f, 0.68f, 0.42f, 0.38f)
        for (i in 0..2) {
            val phase = (t * 0.6f + i * 0.33f) % 1f
            drawSphere(
                cx + 1.5f + sin(phase * 4f) * 0.25f,
                base + 5.4f + phase * 2.2f,
                cz - 0.9f,
                0.35f + phase * 0.25f, 0.35f + phase * 0.25f, 0.35f + phase * 0.25f,
                0.98f, 0.95f, 0.98f
            )
        }
        // Honey picnic table outside
        drawBox(cx + 4.5f, base + 0.65f, cz + 1.5f, 2.0f, 0.2f, 1.3f, 0.78f, 0.52f, 0.28f)
        drawBox(cx + 4.5f, base + 0.95f, cz + 1.5f, 0.45f, 0.5f, 0.45f, 1.0f, 0.75f, 0.10f)
    }

    private fun drawTreehousePlaza(tx: Float, tz: Float, t: Float) {
        val base = GwbWorldSimulation.terrainHeight(tx, tz)
        // Massive ancient oak trunk
        drawBox(tx, base + 3.2f, tz, 2.4f, 6.4f, 2.4f, 0.52f, 0.32f, 0.18f)
        // Elevated wooden platform
        drawBox(tx, base + 4.0f, tz, 7.5f, 0.45f, 7.5f, 0.76f, 0.50f, 0.26f)
        // Cozy Treehouse cabin on top
        drawBox(tx - 1.2f, base + 5.4f, tz, 3.2f, 2.4f, 3.0f, 0.98f, 0.88f, 0.72f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx - 1.2f, base + 7.5f, tz)
        Matrix.rotateM(modelMatrix, 0, 45f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 4.6f, 2.1f, 4.6f)
        drawMesh(coneMesh, 0.22f, 0.76f, 0.54f)
        // Lush canopy spheres
        drawSphere(tx + 1.8f, base + 7.2f, tz + 1.2f, 3.2f, 2.6f, 3.2f, 0.28f, 0.78f, 0.42f)
        drawSphere(tx - 2.2f, base + 7.6f, tz - 1.4f, 2.8f, 2.3f, 2.8f, 1.0f, 0.55f, 0.76f)
        // Glowing colorful hanging lanterns
        val lanternColors = listOf(
            Triple(1.0f, 0.35f, 0.65f),
            Triple(1.0f, 0.82f, 0.15f),
            Triple(0.25f, 0.85f, 0.72f),
            Triple(0.68f, 0.42f, 1.0f)
        )
        for (i in 0 until 4) {
            val angle = i * (PI.toFloat() / 2f) + 0.4f
            val lx = tx + cos(angle) * 3.6f + sin(t * 2f + i) * 0.12f
            val lz = tz + sin(angle) * 3.6f
            val col = lanternColors[i]
            drawSphere(lx, base + 3.2f, lz, 0.35f, 0.45f, 0.35f, col.first, col.second, col.third)
        }
    }

    private fun drawRainbowBridge() {
        val bx = GwbWorldSimulation.riverCenterX(4f)
        val bz = 4f
        val rainbow = listOf(
            Triple(1.0f, 0.42f, 0.62f),
            Triple(1.0f, 0.72f, 0.25f),
            Triple(0.98f, 0.90f, 0.32f),
            Triple(0.32f, 0.85f, 0.58f),
            Triple(0.32f, 0.65f, 1.0f),
            Triple(0.68f, 0.42f, 0.98f)
        )
        for (i in -6..6) {
            val px = bx + i * 0.95f
            val archY = 0.28f + 1.25f * cos((i / 6.2f) * (PI.toFloat() / 2f))
            val c = rainbow[(i + 6) % rainbow.size]
            drawBox(px, archY, bz, 0.9f, 0.28f, 4.4f, c.first, c.second, c.third)
            // Side handrail posts
            if (i % 2 == 0) {
                drawBox(px, archY + 0.65f, bz + 2.0f, 0.2f, 1.1f, 0.2f, 0.95f, 0.85f, 0.68f)
                drawBox(px, archY + 0.65f, bz - 2.0f, 0.2f, 1.1f, 0.2f, 0.95f, 0.85f, 0.68f)
            }
        }
    }

    private fun drawTree(tree: TreeProp) {
        val y = GwbWorldSimulation.terrainHeight(tree.x, tree.z)
        val s = tree.scale
        // Trunk
        drawBox(tree.x, y + 1.5f * s, tree.z, 0.65f * s, 3.0f * s, 0.65f * s, 0.54f, 0.34f, 0.20f)
        when (tree.style) {
            0 -> {
                // Sakura pink blossom tree
                drawSphere(tree.x, y + 3.6f * s, tree.z, 2.2f * s, 1.8f * s, 2.2f * s, 1.0f, 0.54f, 0.76f)
                drawSphere(tree.x + 0.9f * s, y + 3.1f * s, tree.z + 0.6f * s, 1.4f * s, 1.3f * s, 1.4f * s, 1.0f, 0.72f, 0.86f)
            }
            1 -> {
                // Lush emerald oak
                drawSphere(tree.x, y + 3.6f * s, tree.z, 2.1f * s, 1.9f * s, 2.1f * s, 0.24f, 0.76f, 0.38f)
                drawSphere(tree.x - 0.8f * s, y + 3.2f * s, tree.z + 0.5f * s, 1.3f * s, 1.2f * s, 1.3f * s, 0.38f, 0.86f, 0.45f)
            }
            else -> {
                // Pine cone tiers
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tree.x, y + 3.4f * s, tree.z)
                Matrix.scaleM(modelMatrix, 0, 2.9f * s, 3.0f * s, 2.9f * s)
                drawMesh(coneMesh, 0.16f, 0.64f, 0.42f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tree.x, y + 4.9f * s, tree.z)
                Matrix.scaleM(modelMatrix, 0, 2.1f * s, 2.4f * s, 2.1f * s)
                drawMesh(coneMesh, 0.22f, 0.74f, 0.48f)
            }
        }
    }

    private fun drawDiscoveryLandmark(
        id: String,
        category: DiscoveryCategory,
        x: Float,
        z: Float,
        colorHex: Long,
        t: Float
    ) {
        val y = GwbWorldSimulation.terrainHeight(x, z)
        val r = ((colorHex shr 16) and 0xFF) / 255f
        val g = ((colorHex shr 8) and 0xFF) / 255f
        val b = (colorHex and 0xFF) / 255f

        when (category) {
            DiscoveryCategory.FLORA -> {
                // Glowing blossom / rainbow mushroom
                val bob = sin(t * 2.4f + x) * 0.08f
                drawBox(x, y + 0.45f, z, 0.3f, 0.9f, 0.3f, 0.95f, 0.92f, 0.85f)
                drawSphere(x, y + 1.0f + bob, z, 0.95f, 0.45f, 0.95f, r, g, b)
                drawSphere(x, y + 1.32f + bob, z, 0.35f, 0.35f, 0.35f, 1.0f, 0.92f, 0.35f)
            }
            DiscoveryCategory.FAUNA -> {
                if (id != "barnaby_bear") {
                    val hop = kotlin.math.abs(sin(t * 3.0f + z)) * 0.35f
                    drawSphere(x, y + 0.55f + hop, z, 0.65f, 0.55f, 0.75f, r, g, b)
                    drawSphere(x, y + 0.95f + hop, z + 0.35f, 0.35f, 0.35f, 0.35f, 1.0f, 0.95f, 0.90f)
                }
            }
            DiscoveryCategory.BUTTERFLY -> {
                // Glowing flower perch for named rare butterflies
                drawSphere(x, y + 0.35f, z, 0.7f, 0.35f, 0.7f, r, g, b)
            }
        }
    }

    private fun drawPortal(x: Float, z: Float, colorHex: Long, t: Float) {
        val y = GwbWorldSimulation.effectiveFloorHeight(x, z)
        val r = ((colorHex shr 16) and 0xFF) / 255f
        val g = ((colorHex shr 8) and 0xFF) / 255f
        val b = (colorHex and 0xFF) / 255f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y + 0.25f, z)
        Matrix.rotateM(modelMatrix, 0, t * 65f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 2.2f, 1.0f, 2.2f)
        drawMesh(ringMesh, r, g, b)

        // Floating crystal star above portal
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y + 2.1f + sin(t * 3f) * 0.3f, z)
        Matrix.rotateM(modelMatrix, 0, t * 90f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 45f, 1f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.65f, 0.65f, 0.65f)
        drawMesh(cubeMesh, 1.0f, 0.88f, 0.28f)
    }

    private fun drawBarnabyBear(x: Float, y: Float, z: Float, yaw: Float, walkCycle: Float, isCarrying: Boolean) {
        val swing = sin(walkCycle) * 28f
        val brownR = 0.56f; val brownG = 0.34f; val brownB = 0.18f
        val snoutR = 0.86f; val snoutG = 0.70f; val snoutB = 0.52f

        fun bearPart(lx: Float, ly: Float, lz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, mesh: GpuMesh, pitch: Float = 0f) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, lx, ly, lz)
            if (pitch != 0f) Matrix.rotateM(modelMatrix, 0, pitch, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
            drawMesh(mesh, r, g, b)
        }

        // Chubby cozy torso + belly patch
        bearPart(0f, 1.15f, 0f, 0.95f, 1.05f, 0.85f, brownR, brownG, brownB, sphereMesh)
        bearPart(0f, 1.10f, 0.42f, 0.62f, 0.72f, 0.48f, snoutR, snoutG, snoutB, sphereMesh)
        // Warm knitted golden scarf
        bearPart(0f, 1.88f, 0f, 0.68f, 0.24f, 0.68f, 1.0f, 0.72f, 0.14f, sphereMesh)
        // Head + round ears + cute snout
        bearPart(0f, 2.38f, 0.08f, 0.72f, 0.68f, 0.68f, brownR, brownG, brownB, sphereMesh)
        bearPart(-0.52f, 2.95f, 0.05f, 0.24f, 0.24f, 0.18f, brownR, brownG, brownB, sphereMesh)
        bearPart(0.52f, 2.95f, 0.05f, 0.24f, 0.24f, 0.18f, brownR, brownG, brownB, sphereMesh)
        bearPart(0f, 2.28f, 0.68f, 0.34f, 0.26f, 0.28f, snoutR, snoutG, snoutB, sphereMesh)
        bearPart(0f, 2.38f, 0.92f, 0.12f, 0.10f, 0.10f, 0.16f, 0.10f, 0.08f, sphereMesh)
        // Arms & legs
        val armLift = if (isCarrying) -35f else swing
        bearPart(-0.92f, 1.25f, 0f, 0.34f, 0.75f, 0.34f, brownR, brownG, brownB, cubeMesh, armLift)
        bearPart(0.92f, 1.25f, 0f, 0.34f, 0.75f, 0.34f, brownR, brownG, brownB, cubeMesh, -armLift)
        bearPart(-0.42f, 0.38f, 0f, 0.42f, 0.75f, 0.45f, brownR, brownG, brownB, cubeMesh, -swing)
        bearPart(0.42f, 0.38f, 0f, 0.42f, 0.75f, 0.45f, brownR, brownG, brownB, cubeMesh, swing)
    }

    private fun drawAvatarCharacter(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        avatar: AvatarStyle,
        walkCycle: Float,
        netSwing: Float,
        t: Float,
        emote: String
    ) {
        val pr = ((avatar.primaryHex shr 16) and 0xFF) / 255f
        val pg = ((avatar.primaryHex shr 8) and 0xFF) / 255f
        val pb = (avatar.primaryHex and 0xFF) / 255f
        val ar = ((avatar.accentHex shr 16) and 0xFF) / 255f
        val ag = ((avatar.accentHex shr 8) and 0xFF) / 255f
        val ab = (avatar.accentHex and 0xFF) / 255f

        val swing = sin(walkCycle) * 32f
        val netPitch = if (netSwing > 0f) -75f * sin(netSwing * 6f) else -swing

        fun part(lx: Float, ly: Float, lz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, mesh: GpuMesh, pitch: Float = 0f) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, lx, ly, lz)
            if (pitch != 0f) Matrix.rotateM(modelMatrix, 0, pitch, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
            drawMesh(mesh, r, g, b)
        }

        // Legs
        part(-0.18f, 0.35f, 0f, 0.22f, 0.65f, 0.22f, 0.98f, 0.92f, 0.95f, cubeMesh, swing)
        part(0.18f, 0.35f, 0f, 0.22f, 0.65f, 0.22f, 0.98f, 0.92f, 0.95f, cubeMesh, -swing)
        // Dress / Cone Tunic
        part(0f, 0.92f, 0f, 0.95f, 0.85f, 0.95f, pr, pg, pb, coneMesh)
        // Arms
        part(-0.46f, 0.95f, 0f, 0.18f, 0.56f, 0.18f, pr, pg, pb, cubeMesh, -swing)
        part(0.46f, 0.95f, 0f, 0.18f, 0.56f, 0.18f, pr, pg, pb, cubeMesh, netPitch)
        // Head + rosy cheeks + hood/hair
        part(0f, 1.62f, 0f, 0.42f, 0.42f, 0.42f, 1.0f, 0.86f, 0.78f, sphereMesh)
        part(0f, 1.72f, -0.06f, 0.46f, 0.40f, 0.44f, ar, ag, ab, sphereMesh)
        // Cute pigtails or ears
        part(-0.44f, 1.72f, -0.1f, 0.18f, 0.24f, 0.18f, ar, ag, ab, sphereMesh)
        part(0.44f, 1.72f, -0.1f, 0.18f, 0.24f, 0.18f, ar, ag, ab, sphereMesh)

        // Pinky Fairy wings on back if Pinky avatar
        if (avatar == AvatarStyle.PINKY || avatar == AvatarStyle.BLOSSOM) {
            drawButterfly(
                x = x - sin(Math.toRadians(yaw.toDouble())).toFloat() * 0.35f,
                y = y + 1.05f,
                z = z - cos(Math.toRadians(yaw.toDouble())).toFloat() * 0.35f,
                yaw = yaw,
                r = ar, g = ag, b = ab,
                scale = 0.55f,
                flapPhase = t * 10f
            )
        }

        // Floating Emote Halo for Multiplayer Friends
        if (emote.isNotEmpty()) {
            val pulse = 1.0f + 0.18f * sin(t * 6f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y + 2.45f + sin(t * 4f) * 0.12f, z)
            Matrix.rotateM(modelMatrix, 0, t * 120f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.55f * pulse, 0.55f * pulse, 0.55f * pulse)
            drawMesh(ringMesh, 1.0f, 0.85f, 0.22f)
        }
    }

    private fun drawButterfly(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        r: Float,
        g: Float,
        b: Float,
        scale: Float,
        flapPhase: Float
    ) {
        val flapAngle = sin(flapPhase) * 58f
        // Body
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.12f * scale, 0.12f * scale, 0.65f * scale)
        drawMesh(sphereMesh, 0.25f, 0.12f, 0.28f)

        // Right Wing
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, flapAngle, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        drawMesh(wingMesh, r, g, b)

        // Left Wing (mirrored along X)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, -flapAngle, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, -scale, scale, scale)
        drawMesh(wingMesh, r, g, b)
    }

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val MAIN_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;

uniform mat4 uMvp;
uniform mat4 uModel;

out vec3 vWorldPos;
out vec3 vNormal;
out vec3 vColor;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vColor = aColor;
    gl_Position = uMvp * vec4(aPos, 1.0);
}
"""

        private const val MAIN_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec3 vColor;

uniform vec3 uTint;
uniform vec3 uSunDir;
uniform vec3 uCamPos;
uniform float uRimStrength;
uniform float uAlpha;

out vec4 fragColor;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uSunDir);
    vec3 V = normalize(uCamPos - vWorldPos);

    float diff = max(dot(N, L), 0.0);
    float hemi = 0.5 + 0.5 * N.y;
    vec3 ambient = mix(vec3(0.45, 0.36, 0.48), vec3(0.95, 0.92, 0.98), hemi) * 0.52;
    vec3 sunLight = vec3(1.05, 0.98, 0.88) * diff * 0.72;

    float rim = pow(1.0 - max(dot(N, V), 0.0), 2.5) * uRimStrength;
    vec3 rimGlow = vec3(1.0, 0.85, 0.95) * rim;

    vec3 baseCol = vColor * uTint;
    vec3 lit = baseCol * (ambient + sunLight) + rimGlow;

    float dist = length(uCamPos - vWorldPos);
    float fog = clamp((dist - 55.0) / 70.0, 0.0, 1.0);
    vec3 skyFog = vec3(0.99, 0.84, 0.91);
    vec3 finalCol = mix(lit, skyFog, fog);

    fragColor = vec4(finalCol, uAlpha);
}
"""

        private const val WATER_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;

uniform mat4 uMvp;
uniform mat4 uModel;
uniform float uTime;

out vec3 vWorldPos;
out vec3 vColor;

void main() {
    vec3 pos = aPos;
    pos.y += sin(pos.x * 1.4 + uTime * 2.6) * 0.06 + cos(pos.z * 0.9 + uTime * 2.1) * 0.06;
    vec4 worldPos = uModel * vec4(pos, 1.0);
    vWorldPos = worldPos.xyz;
    vColor = aColor;
    gl_Position = uMvp * vec4(pos, 1.0);
}
"""

        private const val WATER_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in vec3 vWorldPos;
in vec3 vColor;

uniform float uTime;
uniform vec3 uCamPos;

out vec4 fragColor;

void main() {
    float ripple = 0.5 + 0.5 * sin(vWorldPos.x * 2.5 + vWorldPos.z * 1.8 - uTime * 3.4);
    vec3 waterCol = mix(vec3(0.18, 0.72, 0.96), vec3(0.55, 0.92, 1.0), ripple * 0.55);
    float sparkle = pow(ripple, 6.0) * 0.35;
    float dist = length(uCamPos - vWorldPos);
    float fog = clamp((dist - 55.0) / 70.0, 0.0, 1.0);
    vec3 finalCol = mix(waterCol + vec3(sparkle), vec3(0.99, 0.84, 0.91), fog);
    fragColor = vec4(finalCol, 0.78);
}
"""
    }
}
