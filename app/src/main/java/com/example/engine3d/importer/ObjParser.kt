package com.example.engine3d.importer

import android.util.Log
import com.example.engine3d.core.Mesh
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object ObjParser {
    fun parse(inputStream: InputStream, modelName: String = "ImportedOBJ"): Mesh? {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val rawPositions = mutableListOf<Float>()
            val rawNormals = mutableListOf<Float>()

            val outVertices = mutableListOf<Float>()
            val outNormals = mutableListOf<Float>()

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("v ")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        rawPositions.add(parts[1].toFloat())
                        rawPositions.add(parts[2].toFloat())
                        rawPositions.add(parts[3].toFloat())
                    }
                } else if (trimmed.startsWith("vn ")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        rawNormals.add(parts[1].toFloat())
                        rawNormals.add(parts[2].toFloat())
                        rawNormals.add(parts[3].toFloat())
                    }
                } else if (trimmed.startsWith("f ")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        // Triangulate if polygon has 3 or 4 vertices
                        val vIndices = mutableListOf<Int>()
                        val vnIndices = mutableListOf<Int>()
                        for (i in 1 until parts.size) {
                            val sub = parts[i].split("/")
                            val vIdx = sub[0].toIntOrNull() ?: 1
                            vIndices.add(if (vIdx > 0) vIdx - 1 else rawPositions.size / 3 + vIdx)
                            if (sub.size >= 3 && sub[2].isNotEmpty()) {
                                val vnIdx = sub[2].toIntOrNull() ?: 1
                                vnIndices.add(if (vnIdx > 0) vnIdx - 1 else rawNormals.size / 3 + vnIdx)
                            }
                        }

                        // Fan triangulation (0, 1, 2) and (0, 2, 3)
                        for (i in 1 until vIndices.size - 1) {
                            val triV = intArrayOf(vIndices[0], vIndices[i], vIndices[i + 1])
                            for (v in triV) {
                                val pBase = v * 3
                                if (pBase + 2 < rawPositions.size) {
                                    outVertices.add(rawPositions[pBase])
                                    outVertices.add(rawPositions[pBase + 1])
                                    outVertices.add(rawPositions[pBase + 2])
                                }
                            }

                            if (vnIndices.size >= vIndices.size) {
                                val triVN = intArrayOf(vnIndices[0], vnIndices[i], vnIndices[i + 1])
                                for (vn in triVN) {
                                    val nBase = vn * 3
                                    if (nBase + 2 < rawNormals.size) {
                                        outNormals.add(rawNormals[nBase])
                                        outNormals.add(rawNormals[nBase + 1])
                                        outNormals.add(rawNormals[nBase + 2])
                                    }
                                }
                            }
                        }
                    }
                }
                line = reader.readLine()
            }

            if (outVertices.isEmpty()) return null

            val vertArray = outVertices.toFloatArray()
            val normArray = if (outNormals.size == outVertices.size) outNormals.toFloatArray() else null

            // Build nice default colors
            val vertexCount = vertArray.size / 3
            val colors = FloatArray(vertexCount * 4)
            for (i in 0 until vertexCount) {
                colors[i * 4] = 0.85f
                colors[i * 4 + 1] = 0.85f
                colors[i * 4 + 2] = 0.88f
                colors[i * 4 + 3] = 1.0f
            }

            return Mesh(
                name = modelName,
                vertices = vertArray,
                normals = normArray,
                colors = colors,
                indices = null
            )
        } catch (e: Exception) {
            Log.e("ObjParser", "Error parsing OBJ: ${e.message}", e)
            return null
        }
    }
}
