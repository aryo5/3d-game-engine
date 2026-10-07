package com.example.engine3d.importer

import com.example.engine3d.core.Mesh
import kotlin.math.cos
import kotlin.math.sin

object ProceduralMeshGenerator {
    fun createBox(name: String, width: Float, height: Float, depth: Float, r: Float, g: Float, b: Float): Mesh {
        val w = width * 0.5f
        val h = height * 0.5f
        val d = depth * 0.5f

        val vertices = floatArrayOf(
            // Front face
            -w, -h,  d,   w, -h,  d,   w,  h,  d,  -w,  h,  d,
            // Back face
            -w, -h, -d,  -w,  h, -d,   w,  h, -d,   w, -h, -d,
            // Top face
            -w,  h, -d,  -w,  h,  d,   w,  h,  d,   w,  h, -d,
            // Bottom face
            -w, -h, -d,   w, -h, -d,   w, -h,  d,  -w, -h,  d,
            // Right face
             w, -h, -d,   w,  h, -d,   w,  h,  d,   w, -h,  d,
            // Left face
            -w, -h, -d,  -w, -h,  d,  -w,  h,  d,  -w,  h, -d
        )

        val normals = floatArrayOf(
            // Front
             0f,  0f,  1f,   0f,  0f,  1f,   0f,  0f,  1f,   0f,  0f,  1f,
            // Back
             0f,  0f, -1f,   0f,  0f, -1f,   0f,  0f, -1f,   0f,  0f, -1f,
            // Top
             0f,  1f,  0f,   0f,  1f,  0f,   0f,  1f,  0f,   0f,  1f,  0f,
            // Bottom
             0f, -1f,  0f,   0f, -1f,  0f,   0f, -1f,  0f,   0f, -1f,  0f,
            // Right
             1f,  0f,  0f,   1f,  0f,  0f,   1f,  0f,  0f,   1f,  0f,  0f,
            // Left
            -1f,  0f,  0f,  -1f,  0f,  0f,  -1f,  0f,  0f,  -1f,  0f,  0f
        )

        val colors = FloatArray(24 * 4)
        for (i in 0 until 24) {
            colors[i * 4] = r
            colors[i * 4 + 1] = g
            colors[i * 4 + 2] = b
            colors[i * 4 + 3] = 1.0f
        }

        val indices = shortArrayOf(
             0,  1,  2,   0,  2,  3, // Front
             4,  5,  6,   4,  6,  7, // Back
             8,  9, 10,   8, 10, 11, // Top
            12, 13, 14,  12, 14, 15, // Bottom
            16, 17, 18,  16, 18, 19, // Right
            20, 21, 22,  20, 22, 23  // Left
        )

        return Mesh(name, vertices, normals, colors, null, indices)
    }

    fun createCylinder(name: String, radius: Float, height: Float, segments: Int = 12, r: Float, g: Float, b: Float): Mesh {
        val hHalf = height * 0.5f
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        // Side vertices
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top ring
            vertices.add(cosA * radius); vertices.add(hHalf); vertices.add(sinA * radius)
            normals.add(cosA); normals.add(0f); normals.add(sinA)
            colors.add(r); colors.add(g); colors.add(b); colors.add(1f)

            // Bottom ring
            vertices.add(cosA * radius); vertices.add(-hHalf); vertices.add(sinA * radius)
            normals.add(cosA); normals.add(0f); normals.add(sinA)
            colors.add(r * 0.85f); colors.add(g * 0.85f); colors.add(b * 0.85f); colors.add(1f)
        }

        for (i in 0 until segments) {
            val top1 = (i * 2).toShort()
            val bot1 = (i * 2 + 1).toShort()
            val top2 = ((i + 1) * 2).toShort()
            val bot2 = ((i + 1) * 2 + 1).toShort()

            indices.add(top1); indices.add(bot1); indices.add(top2)
            indices.add(top2); indices.add(bot1); indices.add(bot2)
        }

        return Mesh(name, vertices.toFloatArray(), normals.toFloatArray(), colors.toFloatArray(), null, indices.toShortArray())
    }

    fun createEnergyBlade(): Mesh {
        val vertices = floatArrayOf(
            // Guard
            -0.12f, 0f, 0f,   0.12f, 0f, 0f,   0f, 0.08f, 0f,
            // Blade Tip
            0f, 1.2f, 0f,    -0.08f, 0.1f, 0f,  0.08f, 0.1f, 0f
        )
        val normals = floatArrayOf(
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f
        )
        val colors = floatArrayOf(
            0.1f, 0.9f, 1.0f, 1f,  0.1f, 0.9f, 1.0f, 1f,  0.1f, 0.9f, 1.0f, 1f,
            1.0f, 1.0f, 1.0f, 1f,  0.2f, 0.8f, 1.0f, 1f,  0.2f, 0.8f, 1.0f, 1f
        )
        val indices = shortArrayOf(0, 1, 2,  3, 4, 5)
        return Mesh("EnergyBlade", vertices, normals, colors, null, indices)
    }
}
