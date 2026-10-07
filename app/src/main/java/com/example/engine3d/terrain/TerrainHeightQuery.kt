package com.example.engine3d.terrain

import com.example.engine3d.core.Mesh
import com.example.engine3d.math.Vec3
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class SurfacePoint(
    val height: Float,
    val normal: Vec3,
    val slopeAngleDeg: Float
)

class TerrainHeightQuery {
    private class Tri(
        val p0: Vec3,
        val p1: Vec3,
        val p2: Vec3,
        val normal: Vec3
    )

    private val triangles = mutableListOf<Tri>()
    private var minX = -100f
    private var maxX = 100f
    private var minZ = -100f
    private var maxZ = 100f

    private var gridCols = 32
    private var gridRows = 32
    private var cellWidth = 10f
    private var cellHeight = 10f
    private var grid: Array<MutableList<Tri>>? = null

    fun buildSpatialIndex(mesh: Mesh) {
        triangles.clear()
        val verts = mesh.vertices
        val indices = mesh.indices

        var bMinX = Float.MAX_VALUE
        var bMaxX = -Float.MAX_VALUE
        var bMinZ = Float.MAX_VALUE
        var bMaxZ = -Float.MAX_VALUE

        fun addTri(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, cx: Float, cy: Float, cz: Float) {
            val p0 = Vec3(ax, ay, az)
            val p1 = Vec3(bx, by, bz)
            val p2 = Vec3(cx, cy, cz)

            bMinX = min(bMinX, min(ax, min(bx, cx)))
            bMaxX = max(bMaxX, max(ax, max(bx, cx)))
            bMinZ = min(bMinZ, min(az, min(bz, cz)))
            bMaxZ = max(bMaxZ, max(az, max(bz, cz)))

            // Face normal
            val e1x = bx - ax; val e1y = by - ay; val e1z = bz - az
            val e2x = cx - ax; val e2y = cy - ay; val e2z = cz - az
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 0.0001f) {
                nx /= len; ny /= len; nz /= len
            } else {
                ny = 1f
            }

            triangles.add(Tri(p0, p1, p2, Vec3(nx, ny, nz)))
        }

        if (indices != null) {
            for (i in 0 until indices.size step 3) {
                val i0 = (indices[i].toInt() and 0xFFFF) * 3
                val i1 = (indices[i + 1].toInt() and 0xFFFF) * 3
                val i2 = (indices[i + 2].toInt() and 0xFFFF) * 3

                if (i0 + 2 < verts.size && i1 + 2 < verts.size && i2 + 2 < verts.size) {
                    addTri(
                        verts[i0], verts[i0 + 1], verts[i0 + 2],
                        verts[i1], verts[i1 + 1], verts[i1 + 2],
                        verts[i2], verts[i2 + 1], verts[i2 + 2]
                    )
                }
            }
        } else {
            for (i in 0 until verts.size / 3 step 3) {
                val i0 = i * 3
                val i1 = (i + 1) * 3
                val i2 = (i + 2) * 3
                if (i2 + 2 < verts.size) {
                    addTri(
                        verts[i0], verts[i0 + 1], verts[i0 + 2],
                        verts[i1], verts[i1 + 1], verts[i1 + 2],
                        verts[i2], verts[i2 + 1], verts[i2 + 2]
                    )
                }
            }
        }

        minX = bMinX.coerceAtLeast(-500f)
        maxX = bMaxX.coerceAtMost(500f)
        minZ = bMinZ.coerceAtLeast(-500f)
        maxZ = bMaxZ.coerceAtMost(500f)

        // Partition into spatial grid
        gridCols = 32
        gridRows = 32
        val width = max(1f, maxX - minX)
        val depth = max(1f, maxZ - minZ)
        cellWidth = width / gridCols
        cellHeight = depth / gridRows

        val newGrid = Array(gridCols * gridRows) { mutableListOf<Tri>() }
        for (tri in triangles) {
            val tMinX = min(tri.p0.x, min(tri.p1.x, tri.p2.x))
            val tMaxX = max(tri.p0.x, max(tri.p1.x, tri.p2.x))
            val tMinZ = min(tri.p0.z, min(tri.p1.z, tri.p2.z))
            val tMaxZ = max(tri.p0.z, max(tri.p1.z, tri.p2.z))

            val c0 = ((tMinX - minX) / cellWidth).toInt().coerceIn(0, gridCols - 1)
            val c1 = ((tMaxX - minX) / cellWidth).toInt().coerceIn(0, gridCols - 1)
            val r0 = ((tMinZ - minZ) / cellHeight).toInt().coerceIn(0, gridRows - 1)
            val r1 = ((tMaxZ - minZ) / cellHeight).toInt().coerceIn(0, gridRows - 1)

            for (r in r0..r1) {
                for (c in c0..c1) {
                    newGrid[r * gridCols + c].add(tri)
                }
            }
        }
        grid = newGrid
    }

    fun sampleSurface(x: Float, z: Float): SurfacePoint {
        val currentGrid = grid
        if (currentGrid == null || triangles.isEmpty()) {
            return SurfacePoint(0f, Vec3.UP, 0f)
        }

        val c = ((x - minX) / cellWidth).toInt().coerceIn(0, gridCols - 1)
        val r = ((z - minZ) / cellHeight).toInt().coerceIn(0, gridRows - 1)
        val cellTris = currentGrid[r * gridCols + c]

        var bestY = -Float.MAX_VALUE
        var bestNormal = Vec3.UP
        var found = false

        for (tri in cellTris) {
            // Barycentric coordinates 2D projection
            val x1 = tri.p0.x; val z1 = tri.p0.z
            val x2 = tri.p1.x; val z2 = tri.p1.z
            val x3 = tri.p2.x; val z3 = tri.p2.z

            val det = (z2 - z3) * (x1 - x3) + (x3 - x2) * (z1 - z3)
            if (kotlin.math.abs(det) < 0.00001f) continue

            val w1 = ((z2 - z3) * (x - x3) + (x3 - x2) * (z - z3)) / det
            val w2 = ((z3 - z1) * (x - x3) + (x1 - x3) * (z - z3)) / det
            val w3 = 1.0f - w1 - w2

            val epsilon = -0.01f
            if (w1 >= epsilon && w2 >= epsilon && w3 >= epsilon) {
                val y = w1 * tri.p0.y + w2 * tri.p1.y + w3 * tri.p2.y
                if (y > bestY) {
                    bestY = y
                    bestNormal = tri.normal
                    found = true
                }
            }
        }

        if (!found) {
            return SurfacePoint(0f, Vec3.UP, 0f)
        }

        // Calculate slope angle relative to UP vector
        val dotUp = bestNormal.y.coerceIn(-1.0f, 1.0f)
        val slopeAngleDeg = Math.toDegrees(acos(dotUp.toDouble())).toFloat()

        return SurfacePoint(bestY, bestNormal, slopeAngleDeg)
    }
}
