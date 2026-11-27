package com.example.engine3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Interleaved vertex format: x, y, z, nx, ny, nz, r, g, b (9 floats = 36 bytes)
class GpuMesh(
    val vboId: Int,
    val vertexCount: Int
) {
    fun bindAndDraw() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        val stride = 9 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }
}

object GwbMeshFactory {

    fun uploadMesh(vertices: FloatArray): GpuMesh {
        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        val vbo = buffers[0]
        val fb: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        fb.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, fb, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        return GpuMesh(vbo, vertices.size / 9)
    }

    fun createTerrainMesh(gridSize: Int = 96, worldExtent: Float = 82f): GpuMesh {
        val verts = ArrayList<Float>(gridSize * gridSize * 6 * 9)
        val step = (worldExtent * 2f) / gridSize

        fun colorAt(x: Float, z: Float, h: Float): Triple<Float, Float, Float> {
            val riverDist = abs(x - GwbWorldSimulation.riverCenterX(z))
            if (riverDist < 5.2f) {
                // Warm golden-sandy riverbed & shore
                return Triple(0.95f, 0.85f, 0.62f)
            }
            // Dirt & blossom footpath connecting Cottage -> Bridge -> Meadow -> Treehouse
            val pathDist1 = distanceToSegment(x, z, -24f, -18f, 0f, 4f)
            val pathDist2 = distanceToSegment(x, z, 0f, 4f, 22f, -14f)
            val pathDist3 = distanceToSegment(x, z, 0f, 4f, -26f, 34f)
            val minPath = minOf(pathDist1, pathDist2, pathDist3)
            if (minPath < 2.1f) {
                return Triple(0.92f, 0.78f, 0.64f)
            }

            // Starlight Lavender Ridge
            if (x > 14f && z > 16f) {
                val blend = ((h - 0.5f) / 3.5f).coerceIn(0f, 1f)
                return Triple(
                    0.46f + 0.34f * blend,
                    0.78f - 0.25f * blend,
                    0.54f + 0.38f * blend
                )
            }
            // Pinky Blossom Meadow
            if (x > 8f && z < 0f) {
                val petal = 0.5f + 0.5f * sin(x * 0.45f) * cos(z * 0.45f)
                return Triple(
                    0.42f + 0.42f * petal,
                    0.82f - 0.24f * petal,
                    0.46f + 0.26f * petal
                )
            }
            // Lush sunlit emerald grass
            val grassVar = 0.5f + 0.5f * sin(x * 0.25f + z * 0.2f)
            return Triple(
                0.28f + 0.12f * grassVar,
                0.76f + 0.12f * grassVar,
                0.38f + 0.08f * grassVar
            )
        }

        fun normalAt(x: Float, z: Float): Triple<Float, Float, Float> {
            val eps = 0.6f
            val hL = GwbWorldSimulation.terrainHeight(x - eps, z)
            val hR = GwbWorldSimulation.terrainHeight(x + eps, z)
            val hD = GwbWorldSimulation.terrainHeight(x, z - eps)
            val hU = GwbWorldSimulation.terrainHeight(x, z + eps)
            val nx = hL - hR
            val ny = 2f * eps
            val nz = hD - hU
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
            return Triple(nx / len, ny / len, nz / len)
        }

        fun pushVertex(x: Float, z: Float) {
            val y = GwbWorldSimulation.terrainHeight(x, z)
            val (nx, ny, nz) = normalAt(x, z)
            val (r, g, b) = colorAt(x, z, y)
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(nx); verts.add(ny); verts.add(nz)
            verts.add(r); verts.add(g); verts.add(b)
        }

        for (ix in 0 until gridSize) {
            val x0 = -worldExtent + ix * step
            val x1 = x0 + step
            for (iz in 0 until gridSize) {
                val z0 = -worldExtent + iz * step
                val z1 = z0 + step

                pushVertex(x0, z0)
                pushVertex(x0, z1)
                pushVertex(x1, z1)

                pushVertex(x0, z0)
                pushVertex(x1, z1)
                pushVertex(x1, z0)
            }
        }
        return uploadMesh(verts.toFloatArray())
    }

    fun createWaterMesh(lengthSegs: Int = 64, widthSegs: Int = 8): GpuMesh {
        val verts = ArrayList<Float>(lengthSegs * widthSegs * 6 * 9)
        val zMin = -82f
        val zMax = 82f
        val dz = (zMax - zMin) / lengthSegs
        val halfWidth = 6.2f

        for (iz in 0 until lengthSegs) {
            val z0 = zMin + iz * dz
            val z1 = z0 + dz
            val cx0 = GwbWorldSimulation.riverCenterX(z0)
            val cx1 = GwbWorldSimulation.riverCenterX(z1)

            for (iw in 0 until widthSegs) {
                val u0 = -1f + (2f * iw) / widthSegs
                val u1 = -1f + (2f * (iw + 1)) / widthSegs
                val x00 = cx0 + u0 * halfWidth
                val x10 = cx0 + u1 * halfWidth
                val x01 = cx1 + u0 * halfWidth
                val x11 = cx1 + u1 * halfWidth

                val y = -0.35f
                fun push(x: Float, z: Float) {
                    verts.add(x); verts.add(y); verts.add(z)
                    verts.add(0f); verts.add(1f); verts.add(0f)
                    verts.add(0.24f); verts.add(0.76f); verts.add(0.98f)
                }
                push(x00, z0); push(x01, z1); push(x11, z1)
                push(x00, z0); push(x11, z1); push(x10, z0)
            }
        }
        return uploadMesh(verts.toFloatArray())
    }

    fun createUnitCube(): GpuMesh {
        val s = 0.5f
        val faces = listOf(
            // Front (+Z)
            Triple(0f, 0f, 1f) to listOf(
                Triple(-s, -s, s), Triple(s, -s, s), Triple(s, s, s),
                Triple(-s, -s, s), Triple(s, s, s), Triple(-s, s, s)
            ),
            // Back (-Z)
            Triple(0f, 0f, -1f) to listOf(
                Triple(s, -s, -s), Triple(-s, -s, -s), Triple(-s, s, -s),
                Triple(s, -s, -s), Triple(-s, s, -s), Triple(s, s, -s)
            ),
            // Left (-X)
            Triple(-1f, 0f, 0f) to listOf(
                Triple(-s, -s, -s), Triple(-s, -s, s), Triple(-s, s, s),
                Triple(-s, -s, -s), Triple(-s, s, s), Triple(-s, s, -s)
            ),
            // Right (+X)
            Triple(1f, 0f, 0f) to listOf(
                Triple(s, -s, s), Triple(s, -s, -s), Triple(s, s, -s),
                Triple(s, -s, s), Triple(s, s, -s), Triple(s, s, s)
            ),
            // Top (+Y)
            Triple(0f, 1f, 0f) to listOf(
                Triple(-s, s, s), Triple(s, s, s), Triple(s, s, -s),
                Triple(-s, s, s), Triple(s, s, -s), Triple(-s, s, -s)
            ),
            // Bottom (-Y)
            Triple(0f, -1f, 0f) to listOf(
                Triple(-s, -s, -s), Triple(s, -s, -s), Triple(s, -s, s),
                Triple(-s, -s, -s), Triple(s, -s, s), Triple(-s, -s, s)
            )
        )

        val verts = ArrayList<Float>(36 * 9)
        for ((norm, pts) in faces) {
            for (p in pts) {
                verts.add(p.first); verts.add(p.second); verts.add(p.third)
                verts.add(norm.first); verts.add(norm.second); verts.add(norm.third)
                verts.add(1f); verts.add(1f); verts.add(1f)
            }
        }
        return uploadMesh(verts.toFloatArray())
    }

    fun createSphere(stacks: Int = 12, slices: Int = 16): GpuMesh {
        val verts = ArrayList<Float>(stacks * slices * 6 * 9)
        fun pushSpherePt(theta: Float, phi: Float) {
            val x = sin(theta) * cos(phi)
            val y = cos(theta)
            val z = sin(theta) * sin(phi)
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(x); verts.add(y); verts.add(z)
            verts.add(1f); verts.add(1f); verts.add(1f)
        }

        for (i in 0 until stacks) {
            val t0 = PI.toFloat() * i / stacks
            val t1 = PI.toFloat() * (i + 1) / stacks
            for (j in 0 until slices) {
                val p0 = 2f * PI.toFloat() * j / slices
                val p1 = 2f * PI.toFloat() * (j + 1) / slices

                pushSpherePt(t0, p0); pushSpherePt(t1, p0); pushSpherePt(t1, p1)
                pushSpherePt(t0, p0); pushSpherePt(t1, p1); pushSpherePt(t0, p1)
            }
        }
        return uploadMesh(verts.toFloatArray())
    }

    fun createCone(slices: Int = 16): GpuMesh {
        val verts = ArrayList<Float>(slices * 6 * 9)
        for (i in 0 until slices) {
            val a0 = 2f * PI.toFloat() * i / slices
            val a1 = 2f * PI.toFloat() * (i + 1) / slices
            val x0 = cos(a0) * 0.5f; val z0 = sin(a0) * 0.5f
            val x1 = cos(a1) * 0.5f; val z1 = sin(a1) * 0.5f
            val nx = cos((a0 + a1) * 0.5f) * 0.89f
            val nz = sin((a0 + a1) * 0.5f) * 0.89f
            val ny = 0.45f

            // Side triangle
            verts.add(0f); verts.add(0.5f); verts.add(0f); verts.add(nx); verts.add(ny); verts.add(nz); verts.add(1f); verts.add(1f); verts.add(1f)
            verts.add(x0); verts.add(-0.5f); verts.add(z0); verts.add(nx); verts.add(ny); verts.add(nz); verts.add(1f); verts.add(1f); verts.add(1f)
            verts.add(x1); verts.add(-0.5f); verts.add(z1); verts.add(nx); verts.add(ny); verts.add(nz); verts.add(1f); verts.add(1f); verts.add(1f)

            // Bottom cap
            verts.add(0f); verts.add(-0.5f); verts.add(0f); verts.add(0f); verts.add(-1f); verts.add(0f); verts.add(1f); verts.add(1f); verts.add(1f)
            verts.add(x1); verts.add(-0.5f); verts.add(z1); verts.add(0f); verts.add(-1f); verts.add(0f); verts.add(1f); verts.add(1f); verts.add(1f)
            verts.add(x0); verts.add(-0.5f); verts.add(z0); verts.add(0f); verts.add(-1f); verts.add(0f); verts.add(1f); verts.add(1f); verts.add(1f)
        }
        return uploadMesh(verts.toFloatArray())
    }

    // Articulated butterfly wing anchored along x = 0 hinge, extending along +x
    fun createButterflyWing(): GpuMesh {
        val pts = listOf(
            // Upper wing triangle 1
            Triple(0f, 0f, 0.12f), Triple(1.0f, 0f, 0.55f), Triple(1.05f, 0f, -0.05f),
            // Upper wing triangle 2
            Triple(0f, 0f, 0.12f), Triple(1.05f, 0f, -0.05f), Triple(0f, 0f, -0.05f),
            // Lower wing triangle
            Triple(0f, 0f, -0.05f), Triple(0.82f, 0f, -0.15f), Triple(0.55f, 0f, -0.62f),
            Triple(0f, 0f, -0.05f), Triple(0.55f, 0f, -0.62f), Triple(0f, 0f, -0.35f)
        )
        val verts = ArrayList<Float>(pts.size * 9)
        for (p in pts) {
            verts.add(p.first); verts.add(p.second); verts.add(p.third)
            verts.add(0f); verts.add(1f); verts.add(0f)
            verts.add(1f); verts.add(1f); verts.add(1f)
        }
        return uploadMesh(verts.toFloatArray())
    }

    fun createTorusRing(segments: Int = 24, innerR: Float = 0.78f, outerR: Float = 1.0f): GpuMesh {
        val verts = ArrayList<Float>(segments * 12 * 9)
        val h = 0.14f
        for (i in 0 until segments) {
            val a0 = 2f * PI.toFloat() * i / segments
            val a1 = 2f * PI.toFloat() * (i + 1) / segments
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
                verts.add(x); verts.add(y); verts.add(z)
                verts.add(nx); verts.add(ny); verts.add(nz)
                verts.add(1f); verts.add(1f); verts.add(1f)
            }

            // Top face
            push(c0 * innerR, h, s0 * innerR, 0f, 1f, 0f)
            push(c0 * outerR, h, s0 * outerR, 0f, 1f, 0f)
            push(c1 * outerR, h, s1 * outerR, 0f, 1f, 0f)
            push(c0 * innerR, h, s0 * innerR, 0f, 1f, 0f)
            push(c1 * outerR, h, s1 * outerR, 0f, 1f, 0f)
            push(c1 * innerR, h, s1 * innerR, 0f, 1f, 0f)

            // Outer rim
            push(c0 * outerR, -h, s0 * outerR, c0, 0f, s0)
            push(c1 * outerR, -h, s1 * outerR, c1, 0f, s1)
            push(c1 * outerR, h, s1 * outerR, c1, 0f, s1)
            push(c0 * outerR, -h, s0 * outerR, c0, 0f, s0)
            push(c1 * outerR, h, s1 * outerR, c1, 0f, s1)
            push(c0 * outerR, h, s0 * outerR, c0, 0f, s0)
        }
        return uploadMesh(verts.toFloatArray())
    }

    private fun distanceToSegment(px: Float, pz: Float, x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dz = z2 - z1
        val lenSq = dx * dx + dz * dz
        if (lenSq <= 0.0001f) return sqrt((px - x1) * (px - x1) + (pz - z1) * (pz - z1))
        val t = (((px - x1) * dx + (pz - z1) * dz) / lenSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projZ = z1 + t * dz
        return sqrt((px - projX) * (px - projX) + (pz - projZ) * (pz - projZ))
    }
}
