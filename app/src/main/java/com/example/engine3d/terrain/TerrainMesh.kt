package com.example.engine3d.terrain

import com.example.engine3d.core.Mesh
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class TerrainPreset {
    HIGHLAND_HILLS,
    CYBER_DUNES,
    ACTION_ARENA,
    CUSTOM_GLB
}

class TerrainMesh {
    var currentPreset: TerrainPreset = TerrainPreset.HIGHLAND_HILLS
    var mesh: Mesh = generateTerrain(TerrainPreset.HIGHLAND_HILLS)
        private set
    val heightQuery = TerrainHeightQuery()

    init {
        heightQuery.buildSpatialIndex(mesh)
    }

    fun setCustomMesh(importedMesh: Mesh) {
        currentPreset = TerrainPreset.CUSTOM_GLB
        mesh = importedMesh
        heightQuery.buildSpatialIndex(importedMesh)
    }

    fun setPreset(preset: TerrainPreset) {
        currentPreset = preset
        mesh = generateTerrain(preset)
        heightQuery.buildSpatialIndex(mesh)
    }

    companion object {
        fun generateTerrain(preset: TerrainPreset): Mesh {
            val size = 260f // Peta diperluas (260x260m) agar tidak ada potongan draw / void saat karakter bergerak
            val segments = 64 // grid halus dan tetap super ringan di mobile
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            val colors = mutableListOf<Float>()
            val indices = mutableListOf<Short>()

            fun heightFunc(x: Float, z: Float): Float {
                return when (preset) {
                    TerrainPreset.HIGHLAND_HILLS -> {
                        val d = sqrt(x * x + z * z)
                        // Dataran tengah lapang, perbukitan di sekeliling
                        val hill1 = sin(x * 0.05f) * cos(z * 0.05f) * 4.5f
                        val hill2 = sin(x * 0.02f + 1.2f) * cos(z * 0.025f + 0.8f) * 6.5f
                        val centerFlatten = (d / 22f).coerceIn(0f, 1f)
                        (hill1 + hill2) * centerFlatten
                    }
                    TerrainPreset.CYBER_DUNES -> {
                        val wave = sin((x + z) * 0.04f) * 4.5f + cos((x - z) * 0.035f) * 3.0f
                        wave
                    }
                    TerrainPreset.ACTION_ARENA -> {
                        if (x in -16f..16f && z in -16f..16f) {
                            0f
                        } else if (x in 20f..45f && z in -14f..14f) {
                            3.5f
                        } else if (x in 16f..20f && z in -14f..14f) {
                            ((x - 16f) / 4f) * 3.5f
                        } else if (z in 20f..45f && x in -14f..14f) {
                            val step = ((z - 20f) / 5f).toInt().coerceIn(0, 4)
                            step * 1.2f
                        } else {
                            sin(x * 0.04f) * cos(z * 0.04f) * 3f
                        }
                    }
                    TerrainPreset.CUSTOM_GLB -> 0f
                }
            }

            val step = size / segments
            val start = -size / 2f

            // Generate grid vertices
            for (iz in 0..segments) {
                val z = start + iz * step
                for (ix in 0..segments) {
                    val x = start + ix * step
                    val y = heightFunc(x, z)

                    vertices.add(x)
                    vertices.add(y)
                    vertices.add(z)

                    // Normal estimation
                    val delta = 0.5f
                    val hL = heightFunc(x - delta, z)
                    val hR = heightFunc(x + delta, z)
                    val hD = heightFunc(x, z - delta)
                    val hU = heightFunc(x, z + delta)

                    var nx = hL - hR
                    val ny = 2f * delta
                    var nz = hD - hU
                    val len = sqrt(nx * nx + ny * ny + nz * nz)
                    if (len > 0.0001f) {
                        nx /= len
                        nz /= len
                    }
                    normals.add(nx)
                    normals.add(ny / len)
                    normals.add(nz)

                    // Color mapping
                    val slope = 1.0f - (ny / len).coerceIn(0f, 1f)
                    when (preset) {
                        TerrainPreset.HIGHLAND_HILLS -> {
                            if (slope > 0.35f) {
                                colors.add(0.48f); colors.add(0.45f); colors.add(0.42f); colors.add(1.0f)
                            } else if (y > 6.0f) {
                                colors.add(0.65f); colors.add(0.70f); colors.add(0.55f); colors.add(1.0f)
                            } else {
                                colors.add(0.24f); colors.add(0.55f); colors.add(0.28f); colors.add(1.0f)
                            }
                        }
                        TerrainPreset.CYBER_DUNES -> {
                            if (slope > 0.3f) {
                                colors.add(0.75f); colors.add(0.50f); colors.add(0.25f); colors.add(1.0f)
                            } else {
                                colors.add(0.85f); colors.add(0.68f); colors.add(0.42f); colors.add(1.0f)
                            }
                        }
                        TerrainPreset.ACTION_ARENA -> {
                            if (x in -16f..16f && z in -16f..16f) {
                                colors.add(0.18f); colors.add(0.22f); colors.add(0.28f); colors.add(1.0f)
                            } else {
                                colors.add(0.35f); colors.add(0.40f); colors.add(0.48f); colors.add(1.0f)
                            }
                        }
                        else -> {
                            colors.add(0.5f); colors.add(0.5f); colors.add(0.5f); colors.add(1f)
                        }
                    }
                }
            }

            // Generate indices
            val stride = segments + 1
            for (iz in 0 until segments) {
                for (ix in 0 until segments) {
                    val topLeft = (iz * stride + ix).toShort()
                    val topRight = (topLeft + 1).toShort()
                    val bottomLeft = ((iz + 1) * stride + ix).toShort()
                    val bottomRight = (bottomLeft + 1).toShort()

                    // Triangle 1
                    indices.add(topLeft)
                    indices.add(bottomLeft)
                    indices.add(topRight)

                    // Triangle 2
                    indices.add(topRight)
                    indices.add(bottomLeft)
                    indices.add(bottomRight)
                }
            }

            return Mesh(
                name = preset.name,
                vertices = vertices.toFloatArray(),
                normals = normals.toFloatArray(),
                colors = colors.toFloatArray(),
                indices = indices.toShortArray()
            )
        }
    }
}
