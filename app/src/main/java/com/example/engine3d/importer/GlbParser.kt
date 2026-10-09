package com.example.engine3d.importer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.engine3d.core.GlbAnimationClip
import com.example.engine3d.core.GlbNode
import com.example.engine3d.core.KeyframeChannel
import com.example.engine3d.core.Mesh
import com.example.engine3d.core.SkinDef
import com.example.engine3d.math.Mat4
import kotlin.math.pow
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbParser {
    private const val GLB_MAGIC = 0x46546C67
    private const val CHUNK_TYPE_JSON = 0x4E4F534A
    private const val CHUNK_TYPE_BIN = 0x004E4942

    private data class NodeInfo(
        val index: Int,
        val name: String,
        val meshIndex: Int,
        val localMatrix: Mat4,
        val children: List<Int>,
        val skinIndex: Int = -1
    )

    fun parse(inputStream: InputStream, modelName: String = "ImportedGLB"): Mesh? {
        try {
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            if (buffer.remaining() < 12) {
                Log.e("GlbParser", "File too small for GLB header")
                return null
            }

            val magic = buffer.int
            val version = buffer.int
            val totalLength = buffer.int

            if (magic != GLB_MAGIC) {
                Log.e("GlbParser", "Invalid GLB magic: 0x${Integer.toHexString(magic)}")
                return null
            }

            var jsonString: String? = null
            var binBuffer: ByteBuffer? = null

            while (buffer.hasRemaining() && buffer.remaining() >= 8) {
                val chunkLength = buffer.int
                val chunkType = buffer.int

                if (chunkLength < 0 || chunkLength > buffer.remaining()) {
                    break
                }

                when (chunkType) {
                    CHUNK_TYPE_JSON -> {
                        val jsonBytes = ByteArray(chunkLength)
                        buffer.get(jsonBytes)
                        jsonString = String(jsonBytes, Charsets.UTF_8)
                    }
                    CHUNK_TYPE_BIN -> {
                        val binBytes = ByteArray(chunkLength)
                        buffer.get(binBytes)
                        binBuffer = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN)
                    }
                    else -> {
                        buffer.position(buffer.position() + chunkLength)
                    }
                }
            }

            if (jsonString == null || binBuffer == null) {
                Log.e("GlbParser", "Missing JSON or BIN chunk in GLB")
                return null
            }

            val json = JSONObject(jsonString)
            return buildMeshFromJson(json, binBuffer, modelName)
        } catch (e: Exception) {
            Log.e("GlbParser", "Failed to parse GLB: ${e.message}", e)
            return null
        }
    }

    private fun buildMeshFromJson(json: JSONObject, binBuffer: ByteBuffer, name: String): Mesh? {
        val accessors = json.optJSONArray("accessors") ?: return null
        val bufferViews = json.optJSONArray("bufferViews") ?: return null
        val meshes = json.optJSONArray("meshes") ?: return null
        if (meshes.length() == 0) return null

        val materialsArray = json.optJSONArray("materials")
        val nodesArray = json.optJSONArray("nodes")
        val scenesArray = json.optJSONArray("scenes")

        val nodeList = mutableListOf<NodeInfo>()
        val nodeParents = mutableMapOf<Int, Int>()

        if (nodesArray != null) {
            for (i in 0 until nodesArray.length()) {
                val nodeObj = nodesArray.getJSONObject(i)
                val nodeName = nodeObj.optString("name", "node_$i")
                val meshIdx = nodeObj.optInt("mesh", -1)
                val childrenJson = nodeObj.optJSONArray("children")
                val children = mutableListOf<Int>()
                if (childrenJson != null) {
                    for (c in 0 until childrenJson.length()) {
                        val childIdx = childrenJson.getInt(c)
                        children.add(childIdx)
                        nodeParents[childIdx] = i
                    }
                }
                val skinIdx = nodeObj.optInt("skin", -1)
                val localMat = computeNodeLocalMatrix(nodeObj)
                nodeList.add(NodeInfo(i, nodeName, meshIdx, localMat, children, skinIdx))
            }
        }

        val rootNodeIndices = mutableListOf<Int>()
        if (scenesArray != null && scenesArray.length() > 0) {
            val scene0 = scenesArray.getJSONObject(0)
            val sceneNodes = scene0.optJSONArray("nodes")
            if (sceneNodes != null) {
                for (s in 0 until sceneNodes.length()) {
                    rootNodeIndices.add(sceneNodes.getInt(s))
                }
            }
        }
        if (rootNodeIndices.isEmpty() && nodeList.isNotEmpty()) {
            for (node in nodeList) {
                if (!nodeParents.containsKey(node.index)) {
                    rootNodeIndices.add(node.index)
                }
            }
        }

        val nodeWorldMatrices = mutableMapOf<Int, Mat4>()
        fun traverseNode(nodeIdx: Int, parentWorld: Mat4) {
            if (nodeIdx < 0 || nodeIdx >= nodeList.size) return
            val node = nodeList[nodeIdx]
            val worldMat = Mat4().set(parentWorld).multiply(node.localMatrix)
            nodeWorldMatrices[nodeIdx] = worldMat
            for (childIdx in node.children) {
                traverseNode(childIdx, worldMat)
            }
        }

        for (rootIdx in rootNodeIndices) {
            traverseNode(rootIdx, Mat4().identity())
        }

        val skinsArray = json.optJSONArray("skins")
        val parsedSkins = mutableListOf<SkinDef>()
        if (skinsArray != null) {
            for (s in 0 until skinsArray.length()) {
                val sObj = skinsArray.getJSONObject(s)
                val sJointsJson = sObj.optJSONArray("joints")
                val sIbmIdx = sObj.optInt("inverseBindMatrices", -1)
                val sJoints = if (sJointsJson != null) {
                    IntArray(sJointsJson.length()) { sJointsJson.getInt(it) }
                } else IntArray(0)

                val sIbm: Array<Mat4> = if (sJoints.isNotEmpty() && sIbmIdx >= 0) {
                    val rawIbm = extractFloatArray(sIbmIdx, accessors, bufferViews, binBuffer)
                    if (rawIbm != null && rawIbm.size >= sJoints.size * 16) {
                        Array(sJoints.size) { j ->
                            val m = Mat4()
                            val slice = FloatArray(16)
                            System.arraycopy(rawIbm, j * 16, slice, 0, 16)
                            m.set(slice)
                        }
                    } else Array(sJoints.size) { Mat4().identity() }
                } else Array(sJoints.size) { Mat4().identity() }

                parsedSkins.add(SkinDef(sJoints, sIbm))
            }
        }

        val primarySkin = parsedSkins.firstOrNull()
        val skinJoints = primarySkin?.joints
        val inverseBindMatrices = primarySkin?.inverseBindMatrices

        val allPositions = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allTexCoords = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        val allJoints = mutableListOf<Int>()
        val allWeights = mutableListOf<Float>()
        val allVertexSkinIndices = mutableListOf<Int>()
        val hasExplicitColors = BooleanArray(1) { false }
        var totalVertexCount = 0

        val nodesWithMesh = nodeList.filter { it.meshIndex >= 0 && it.meshIndex < meshes.length() }

        if (nodesWithMesh.isNotEmpty()) {
            for (node in nodesWithMesh) {
                val worldMat = nodeWorldMatrices[node.index] ?: Mat4().identity()
                val meshObj = meshes.getJSONObject(node.meshIndex)
                val nodeSkinIdx = if (node.skinIndex >= 0 && node.skinIndex < parsedSkins.size) {
                    node.skinIndex
                } else if (parsedSkins.isNotEmpty()) {
                    0
                } else {
                    -1
                }

                totalVertexCount = parseAndAppendMeshPrimitives(
                    meshObj = meshObj,
                    materialsArray = materialsArray,
                    worldMat = worldMat,
                    nodeSkinIdx = nodeSkinIdx,
                    accessors = accessors,
                    bufferViews = bufferViews,
                    binBuffer = binBuffer,
                    allPositions = allPositions,
                    allNormals = allNormals,
                    allColors = allColors,
                    allTexCoords = allTexCoords,
                    allIndices = allIndices,
                    allJoints = allJoints,
                    allWeights = allWeights,
                    allVertexSkinIndices = allVertexSkinIndices,
                    hasExplicitColors = hasExplicitColors,
                    totalVertexCount = totalVertexCount
                )
            }
        } else {
            for (m in 0 until meshes.length()) {
                val meshObj = meshes.getJSONObject(m)
                totalVertexCount = parseAndAppendMeshPrimitives(
                    meshObj = meshObj,
                    materialsArray = materialsArray,
                    worldMat = Mat4().identity(),
                    nodeSkinIdx = if (parsedSkins.isNotEmpty()) 0 else -1,
                    accessors = accessors,
                    bufferViews = bufferViews,
                    binBuffer = binBuffer,
                    allPositions = allPositions,
                    allNormals = allNormals,
                    allColors = allColors,
                    allTexCoords = allTexCoords,
                    allIndices = allIndices,
                    allJoints = allJoints,
                    allWeights = allWeights,
                    allVertexSkinIndices = allVertexSkinIndices,
                    hasExplicitColors = hasExplicitColors,
                    totalVertexCount = totalVertexCount
                )
            }
        }

        if (allPositions.isEmpty()) return null

        val animationClips = extractAnimationClips(json, accessors, bufferViews, binBuffer)
        val textureBitmap = extractTextureBitmap(json, bufferViews, binBuffer)

        val glbNodes = mutableListOf<GlbNode>()
        for (node in nodeList) {
            val parentIdx = nodeParents[node.index] ?: -1
            glbNodes.add(
                GlbNode(
                    index = node.index,
                    name = node.name,
                    parentIndex = parentIdx,
                    meshIndex = node.meshIndex,
                    defaultLocalMatrix = node.localMatrix,
                    children = node.children,
                    subMesh = null
                )
            )
        }

        val hasSkinning = parsedSkins.isNotEmpty() && allWeights.isNotEmpty()

        return Mesh(
            name = name,
            vertices = allPositions.toFloatArray(),
            normals = allNormals.toFloatArray(),
            colors = allColors.toFloatArray(),
            texCoords = if (allTexCoords.isNotEmpty()) allTexCoords.toFloatArray() else null,
            indices = if (allIndices.isNotEmpty()) allIndices.toShortArray() else null,
            animationClips = animationClips,
            nodes = glbNodes,
            rootNodes = rootNodeIndices,
            bindVertices = if (hasSkinning) allPositions.toFloatArray() else null,
            vertexJoints = if (hasSkinning) allJoints.toIntArray() else null,
            vertexWeights = if (hasSkinning) allWeights.toFloatArray() else null,
            skinJointNodes = skinJoints,
            inverseBindMatrices = inverseBindMatrices,
            skins = if (hasSkinning) parsedSkins else null,
            vertexSkinIndices = if (hasSkinning && allVertexSkinIndices.isNotEmpty()) allVertexSkinIndices.toIntArray() else null,
            hasExplicitVertexColors = hasExplicitColors[0],
            textureBitmap = textureBitmap
        )
    }

    private fun parseAndAppendMeshPrimitives(
        meshObj: JSONObject,
        materialsArray: JSONArray?,
        worldMat: Mat4,
        nodeSkinIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer,
        allPositions: MutableList<Float>,
        allNormals: MutableList<Float>,
        allColors: MutableList<Float>,
        allTexCoords: MutableList<Float>,
        allIndices: MutableList<Short>,
        allJoints: MutableList<Int>,
        allWeights: MutableList<Float>,
        allVertexSkinIndices: MutableList<Int>,
        hasExplicitColors: BooleanArray,
        totalVertexCount: Int
    ): Int {
        var currentVertexCount = totalVertexCount
        val primitives = meshObj.optJSONArray("primitives") ?: return currentVertexCount
        val m = worldMat.data
        val isSkinnedNode = nodeSkinIdx >= 0

        for (p in 0 until primitives.length()) {
            val prim = primitives.getJSONObject(p)
            val attributes = prim.optJSONObject("attributes") ?: continue

            val posAccessorIdx = attributes.optInt("POSITION", -1)
            if (posAccessorIdx < 0) continue

            val rawPositions = extractFloatVec3(posAccessorIdx, accessors, bufferViews, binBuffer) ?: continue
            val primitiveVertexCount = rawPositions.size / 3

            val normalAccessorIdx = attributes.optInt("NORMAL", -1)
            var rawNormals = if (normalAccessorIdx >= 0) {
                extractFloatVec3(normalAccessorIdx, accessors, bufferViews, binBuffer)
            } else null

            val indicesAccessorIdx = prim.optInt("indices", -1)
            val indices = if (indicesAccessorIdx >= 0) {
                extractIndices(indicesAccessorIdx, accessors, bufferViews, binBuffer)
            } else null

            if (rawNormals == null) {
                rawNormals = computeNormals(rawPositions, indices)
            }

            val jointAccessorIdx = attributes.optInt("JOINTS_0", -1)
            val rawJoints = if (jointAccessorIdx >= 0) {
                extractJointIndices(jointAccessorIdx, accessors, bufferViews, binBuffer, primitiveVertexCount)
            } else null

            val weightAccessorIdx = attributes.optInt("WEIGHTS_0", -1)
            val rawWeights = if (weightAccessorIdx >= 0) {
                extractWeightArray(weightAccessorIdx, accessors, bufferViews, binBuffer, primitiveVertexCount)
            } else null

            val matIdx = prim.optInt("material", -1)
            var matBaseColor: FloatArray? = null
            if (materialsArray != null && matIdx >= 0 && matIdx < materialsArray.length()) {
                val matObj = materialsArray.optJSONObject(matIdx)
                val pbrObj = matObj?.optJSONObject("pbrMetallicRoughness")
                val baseColorFactor = pbrObj?.optJSONArray("baseColorFactor")
                if (baseColorFactor != null && baseColorFactor.length() >= 3) {
                    val rLin = baseColorFactor.getDouble(0).toFloat()
                    val gLin = baseColorFactor.getDouble(1).toFloat()
                    val bLin = baseColorFactor.getDouble(2).toFloat()
                    val rSrgb = rLin.coerceIn(0f, 1f).pow(1f / 2.2f)
                    val gSrgb = gLin.coerceIn(0f, 1f).pow(1f / 2.2f)
                    val bSrgb = bLin.coerceIn(0f, 1f).pow(1f / 2.2f)
                    val aVal = if (baseColorFactor.length() >= 4) baseColorFactor.getDouble(3).toFloat() else 1.0f
                    matBaseColor = floatArrayOf(rSrgb, gSrgb, bSrgb, aVal)
                }
            }

            val colorAccessorIdx = attributes.optInt("COLOR_0", -1)
            val extractedColors = if (colorAccessorIdx >= 0) {
                extractColorArray(colorAccessorIdx, accessors, bufferViews, binBuffer, primitiveVertexCount)
            } else null
            if (extractedColors != null) {
                hasExplicitColors[0] = true
            }

            val texCoordAccessorIdx = attributes.optInt("TEXCOORD_0", -1)
            val rawTexCoords = if (texCoordAccessorIdx >= 0) {
                extractTexCoordArray(texCoordAccessorIdx, accessors, bufferViews, binBuffer, primitiveVertexCount)
            } else null

            for (i in 0 until primitiveVertexCount) {
                val vx = rawPositions[i * 3]
                val vy = rawPositions[i * 3 + 1]
                val vz = rawPositions[i * 3 + 2]

                // For skinned nodes, positions remain in bind space (skin matrices evaluate to world space).
                // For static unskinned nodes, multiply by worldMat.
                val tx = if (isSkinnedNode) vx else m[0] * vx + m[4] * vy + m[8] * vz + m[12]
                val ty = if (isSkinnedNode) vy else m[1] * vx + m[5] * vy + m[9] * vz + m[13]
                val tz = if (isSkinnedNode) vz else m[2] * vx + m[6] * vy + m[10] * vz + m[14]

                allPositions.add(tx)
                allPositions.add(ty)
                allPositions.add(tz)

                val nx = rawNormals[i * 3]
                val ny = rawNormals[i * 3 + 1]
                val nz = rawNormals[i * 3 + 2]

                if (isSkinnedNode) {
                    allNormals.add(nx)
                    allNormals.add(ny)
                    allNormals.add(nz)
                } else {
                    var tnx = m[0] * nx + m[4] * ny + m[8] * nz
                    var tny = m[1] * nx + m[5] * ny + m[9] * nz
                    var tnz = m[2] * nx + m[6] * ny + m[10] * nz

                    val len = kotlin.math.sqrt(tnx * tnx + tny * tny + tnz * tnz)
                    if (len > 0.0001f) {
                        tnx /= len; tny /= len; tnz /= len
                    } else {
                        tny = 1f
                    }

                    allNormals.add(tnx)
                    allNormals.add(tny)
                    allNormals.add(tnz)
                }

                if (isSkinnedNode) {
                    allVertexSkinIndices.add(nodeSkinIdx)
                } else {
                    allVertexSkinIndices.add(0)
                }

                val i2 = i * 2
                if (rawTexCoords != null && rawTexCoords.size >= i2 + 2) {
                    allTexCoords.add(rawTexCoords[i2])
                    allTexCoords.add(rawTexCoords[i2 + 1])
                } else {
                    allTexCoords.add(0f)
                    allTexCoords.add(0f)
                }

                val i4 = i * 4
                if (rawJoints != null && rawJoints.size >= i4 + 4) {
                    allJoints.add(rawJoints[i4])
                    allJoints.add(rawJoints[i4 + 1])
                    allJoints.add(rawJoints[i4 + 2])
                    allJoints.add(rawJoints[i4 + 3])
                } else {
                    allJoints.add(0); allJoints.add(0); allJoints.add(0); allJoints.add(0)
                }

                if (rawWeights != null && rawWeights.size >= i4 + 4) {
                    allWeights.add(rawWeights[i4])
                    allWeights.add(rawWeights[i4 + 1])
                    allWeights.add(rawWeights[i4 + 2])
                    allWeights.add(rawWeights[i4 + 3])
                } else {
                    allWeights.add(1f); allWeights.add(0f); allWeights.add(0f); allWeights.add(0f)
                }

                if (extractedColors != null && extractedColors.size >= i4 + 4) {
                    var r = extractedColors[i4]
                    var g = extractedColors[i4 + 1]
                    var b = extractedColors[i4 + 2]
                    val a = extractedColors[i4 + 3]
                    if (matBaseColor != null) {
                        r *= matBaseColor[0]
                        g *= matBaseColor[1]
                        b *= matBaseColor[2]
                    }
                    allColors.add(r); allColors.add(g); allColors.add(b); allColors.add(a)
                } else if (matBaseColor != null) {
                    allColors.add(matBaseColor[0])
                    allColors.add(matBaseColor[1])
                    allColors.add(matBaseColor[2])
                    allColors.add(matBaseColor[3])
                } else {
                    val defaultCol = if (rawTexCoords != null) 1.0f else 0.9f
                    allColors.add(defaultCol); allColors.add(defaultCol); allColors.add(defaultCol); allColors.add(1.0f)
                }
            }

            if (indices != null) {
                for (idx in indices) {
                    val offsetIdx = (idx.toInt() and 0xFFFF) + currentVertexCount
                    allIndices.add(offsetIdx.toShort())
                }
            } else {
                for (vIdx in 0 until primitiveVertexCount) {
                    allIndices.add((currentVertexCount + vIdx).toShort())
                }
            }

            currentVertexCount += primitiveVertexCount
        }
        return currentVertexCount
    }

    private fun extractWeightArray(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer,
        vertexCount: Int
    ): FloatArray? {
        if (accessorIdx < 0 || accessorIdx >= accessors.length()) return null
        val accessor = accessors.optJSONObject(accessorIdx) ?: return null
        val bufferViewIdx = accessor.optInt("bufferView", -1)
        if (bufferViewIdx < 0) return null
        val count = accessor.optInt("count", 0)
        val componentType = accessor.optInt("componentType", 5126)
        val byteOffset = accessor.optInt("byteOffset", 0)

        val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
        val bvByteOffset = bufferView.optInt("byteOffset", 0)
        val startOffset = bvByteOffset + byteOffset
        if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

        binBuffer.position(startOffset)
        val totalElements = count * 4
        val result = FloatArray(vertexCount * 4)

        for (i in 0 until totalElements.coerceAtMost(vertexCount * 4)) {
            when (componentType) {
                5126 -> {
                    if (binBuffer.remaining() >= 4) result[i] = binBuffer.float
                }
                5121 -> {
                    if (binBuffer.remaining() >= 1) result[i] = (binBuffer.get().toInt() and 0xFF) / 255.0f
                }
                5123 -> {
                    if (binBuffer.remaining() >= 2) result[i] = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                }
            }
        }
        return result
    }

    private fun extractJointIndices(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer,
        vertexCount: Int
    ): IntArray? {
        if (accessorIdx < 0 || accessorIdx >= accessors.length()) return null
        val accessor = accessors.optJSONObject(accessorIdx) ?: return null
        val bufferViewIdx = accessor.optInt("bufferView", -1)
        if (bufferViewIdx < 0) return null
        val count = accessor.optInt("count", 0)
        val componentType = accessor.optInt("componentType", 5121)
        val byteOffset = accessor.optInt("byteOffset", 0)

        val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
        val bvByteOffset = bufferView.optInt("byteOffset", 0)
        val startOffset = bvByteOffset + byteOffset
        if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

        binBuffer.position(startOffset)
        val totalElements = count * 4
        val result = IntArray(totalElements)
        for (i in 0 until totalElements) {
            if (componentType == 5121) {
                if (binBuffer.remaining() >= 1) {
                    result[i] = binBuffer.get().toInt() and 0xFF
                }
            } else if (componentType == 5123) {
                if (binBuffer.remaining() >= 2) {
                    result[i] = binBuffer.short.toInt() and 0xFFFF
                }
            }
        }
        return result
    }

    private fun computeNodeLocalMatrix(nodeObj: JSONObject): Mat4 {
        val mat = Mat4()
        val matrixArray = nodeObj.optJSONArray("matrix")
        if (matrixArray != null && matrixArray.length() == 16) {
            val data = FloatArray(16)
            for (i in 0 until 16) {
                data[i] = matrixArray.getDouble(i).toFloat()
            }
            return mat.set(data)
        }

        var tx = 0f; var ty = 0f; var tz = 0f
        val transArray = nodeObj.optJSONArray("translation")
        if (transArray != null && transArray.length() >= 3) {
            tx = transArray.getDouble(0).toFloat()
            ty = transArray.getDouble(1).toFloat()
            tz = transArray.getDouble(2).toFloat()
        }

        var sx = 1f; var sy = 1f; var sz = 1f
        val scaleArray = nodeObj.optJSONArray("scale")
        if (scaleArray != null && scaleArray.length() >= 3) {
            sx = scaleArray.getDouble(0).toFloat()
            sy = scaleArray.getDouble(1).toFloat()
            sz = scaleArray.getDouble(2).toFloat()
        }

        var qx = 0f; var qy = 0f; var qz = 0f; var qw = 1f
        val rotArray = nodeObj.optJSONArray("rotation")
        if (rotArray != null && rotArray.length() >= 4) {
            qx = rotArray.getDouble(0).toFloat()
            qy = rotArray.getDouble(1).toFloat()
            qz = rotArray.getDouble(2).toFloat()
            qw = rotArray.getDouble(3).toFloat()
        }

        val rotMat = quaternionToMat4(qx, qy, qz, qw)
        return Mat4().translate(tx, ty, tz).multiply(rotMat).scale(sx, sy, sz)
    }

    private fun quaternionToMat4(qx: Float, qy: Float, qz: Float, qw: Float): Mat4 {
        val m = Mat4()
        val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
        val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
        val wx = qw * qx; val wy = qw * qy; val wz = qw * qz

        val data = FloatArray(16)
        data[0] = 1f - 2f * (yy + zz)
        data[1] = 2f * (xy + wz)
        data[2] = 2f * (xz - wy)
        data[3] = 0f

        data[4] = 2f * (xy - wz)
        data[5] = 1f - 2f * (xx + zz)
        data[6] = 2f * (yz + wx)
        data[7] = 0f

        data[8] = 2f * (xz + wy)
        data[9] = 2f * (yz - wx)
        data[10] = 1f - 2f * (xx + yy)
        data[11] = 0f

        data[12] = 0f; data[13] = 0f; data[14] = 0f; data[15] = 1f
        return m.set(data)
    }

    private fun extractAnimationClips(
        json: JSONObject,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): List<GlbAnimationClip> {
        val animations = json.optJSONArray("animations") ?: return emptyList()
        val clips = mutableListOf<GlbAnimationClip>()

        for (a in 0 until animations.length()) {
            val animObj = animations.getJSONObject(a)
            val animName = animObj.optString("name", "anim_$a")
            val samplers = animObj.optJSONArray("samplers") ?: continue
            val channels = animObj.optJSONArray("channels") ?: continue

            val parsedChannels = mutableListOf<KeyframeChannel>()
            var maxDuration = 0f

            for (c in 0 until channels.length()) {
                val channelObj = channels.getJSONObject(c)
                val samplerIdx = channelObj.optInt("sampler", -1)
                val target = channelObj.optJSONObject("target") ?: continue
                val nodeIdx = target.optInt("node", -1)
                val path = target.optString("path", "")

                if (samplerIdx < 0 || samplerIdx >= samplers.length()) continue
                val samplerObj = samplers.getJSONObject(samplerIdx)
                val inputAccessorIdx = samplerObj.optInt("input", -1)
                val outputAccessorIdx = samplerObj.optInt("output", -1)

                if (inputAccessorIdx < 0 || outputAccessorIdx < 0) continue

                val times = extractFloatArray(inputAccessorIdx, accessors, bufferViews, binBuffer) ?: continue
                val values = extractFloatArray(outputAccessorIdx, accessors, bufferViews, binBuffer) ?: continue

                if (times.isNotEmpty()) {
                    val lastTime = times.last()
                    if (lastTime > maxDuration) maxDuration = lastTime
                }

                parsedChannels.add(KeyframeChannel(nodeIdx, path, times, values))
            }

            clips.add(GlbAnimationClip(animName, maxDuration, parsedChannels))
        }
        return clips
    }

    private fun extractFloatArray(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): FloatArray? {
        val accessor = accessors.optJSONObject(accessorIdx) ?: return null
        val bufferViewIdx = accessor.optInt("bufferView", -1)
        if (bufferViewIdx < 0) return null
        val count = accessor.optInt("count", 0)
        val type = accessor.optString("type", "SCALAR")
        val byteOffset = accessor.optInt("byteOffset", 0)

        val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
        val bvByteOffset = bufferView.optInt("byteOffset", 0)

        val numComponents = when (type) {
            "SCALAR" -> 1
            "VEC2" -> 2
            "VEC3" -> 3
            "VEC4" -> 4
            "MAT4" -> 16
            else -> 1
        }

        val startOffset = bvByteOffset + byteOffset
        if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

        binBuffer.position(startOffset)
        val result = FloatArray(count * numComponents)
        for (i in result.indices) {
            if (binBuffer.remaining() >= 4) {
                result[i] = binBuffer.float
            }
        }
        return result
    }

    private fun extractFloatVec3(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): FloatArray? {
        val accessor = accessors.optJSONObject(accessorIdx) ?: return null
        val bufferViewIdx = accessor.optInt("bufferView", -1)
        if (bufferViewIdx < 0) return null
        val count = accessor.optInt("count", 0)
        val byteOffset = accessor.optInt("byteOffset", 0)

        val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
        val bvByteOffset = bufferView.optInt("byteOffset", 0)

        val startOffset = bvByteOffset + byteOffset
        if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

        binBuffer.position(startOffset)

        val result = FloatArray(count * 3)
        for (i in 0 until count * 3) {
            if (binBuffer.remaining() >= 4) {
                result[i] = binBuffer.float
            } else {
                result[i] = 0f
            }
        }
        return result
    }

    private fun extractIndices(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): ShortArray? {
        val accessor = accessors.optJSONObject(accessorIdx) ?: return null
        val bufferViewIdx = accessor.optInt("bufferView", -1)
        if (bufferViewIdx < 0) return null
        val count = accessor.optInt("count", 0)
        val componentType = accessor.optInt("componentType", 5123)
        val byteOffset = accessor.optInt("byteOffset", 0)

        val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
        val bvByteOffset = bufferView.optInt("byteOffset", 0)

        val startOffset = bvByteOffset + byteOffset
        if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

        binBuffer.position(startOffset)

        val result = ShortArray(count)
        when (componentType) {
            5123 -> {
                for (i in 0 until count) {
                    if (binBuffer.remaining() >= 2) result[i] = binBuffer.short
                }
            }
            5121 -> {
                for (i in 0 until count) {
                    if (binBuffer.remaining() >= 1) result[i] = (binBuffer.get().toInt() and 0xFF).toShort()
                }
            }
            5125 -> {
                for (i in 0 until count) {
                    if (binBuffer.remaining() >= 4) result[i] = (binBuffer.int and 0xFFFF).toShort()
                }
            }
            else -> {
                for (i in 0 until count) {
                    if (binBuffer.remaining() >= 2) result[i] = binBuffer.short
                }
            }
        }
        return result
    }

    private fun computeNormals(vertices: FloatArray, indices: ShortArray?): FloatArray {
        val normals = FloatArray(vertices.size)
        val vertexCount = vertices.size / 3

        if (indices != null) {
            for (i in 0 until indices.size step 3) {
                val i0 = (indices[i].toInt() and 0xFFFF) * 3
                val i1 = (indices[i + 1].toInt() and 0xFFFF) * 3
                val i2 = (indices[i + 2].toInt() and 0xFFFF) * 3

                if (i0 + 2 < vertices.size && i1 + 2 < vertices.size && i2 + 2 < vertices.size) {
                    val ax = vertices[i1] - vertices[i0]
                    val ay = vertices[i1 + 1] - vertices[i0 + 1]
                    val az = vertices[i1 + 2] - vertices[i0 + 2]

                    val bx = vertices[i2] - vertices[i0]
                    val by = vertices[i2 + 1] - vertices[i0 + 1]
                    val bz = vertices[i2 + 2] - vertices[i0 + 2]

                    val nx = ay * bz - az * by
                    val ny = az * bx - ax * bz
                    val nz = ax * by - ay * bx

                    normals[i0] += nx; normals[i0 + 1] += ny; normals[i0 + 2] += nz
                    normals[i1] += nx; normals[i1 + 1] += ny; normals[i1 + 2] += nz
                    normals[i2] += nx; normals[i2 + 1] += ny; normals[i2 + 2] += nz
                }
            }
        } else {
            for (i in 0 until vertexCount step 3) {
                val i0 = i * 3
                val i1 = (i + 1) * 3
                val i2 = (i + 2) * 3
                if (i2 + 2 < vertices.size) {
                    val ax = vertices[i1] - vertices[i0]
                    val ay = vertices[i1 + 1] - vertices[i0 + 1]
                    val az = vertices[i1 + 2] - vertices[i0 + 2]

                    val bx = vertices[i2] - vertices[i0]
                    val by = vertices[i2 + 1] - vertices[i0 + 1]
                    val bz = vertices[i2 + 2] - vertices[i0 + 2]

                    val nx = ay * bz - az * by
                    val ny = az * bx - ax * bz
                    val nz = ax * by - ay * bx

                    normals[i0] = nx; normals[i0 + 1] = ny; normals[i0 + 2] = nz
                    normals[i1] = nx; normals[i1 + 1] = ny; normals[i1 + 2] = nz
                    normals[i2] = nx; normals[i2 + 1] = ny; normals[i2 + 2] = nz
                }
            }
        }

        for (i in 0 until vertexCount) {
            val idx = i * 3
            val x = normals[idx]
            val y = normals[idx + 1]
            val z = normals[idx + 2]
            val len = kotlin.math.sqrt(x * x + y * y + z * z)
            if (len > 0.0001f) {
                normals[idx] /= len
                normals[idx + 1] /= len
                normals[idx + 2] /= len
            } else {
                normals[idx + 1] = 1f
            }
        }

        return normals
    }

    private fun extractColorArray(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer,
        vertexCount: Int
    ): FloatArray? {
        return try {
            val accessor = accessors.optJSONObject(accessorIdx) ?: return null
            val bufferViewIdx = accessor.optInt("bufferView", -1)
            if (bufferViewIdx < 0) return null
            val count = accessor.optInt("count", 0)
            val type = accessor.optString("type", "VEC3")
            val componentType = accessor.optInt("componentType", 5126)
            val byteOffset = accessor.optInt("byteOffset", 0)

            val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
            val bvByteOffset = bufferView.optInt("byteOffset", 0)

            val startOffset = bvByteOffset + byteOffset
            if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

            binBuffer.position(startOffset)
            val numComponents = if (type == "VEC3") 3 else 4
            val result = FloatArray(vertexCount * 4)

            for (i in 0 until count.coerceAtMost(vertexCount)) {
                var r = 1f; var g = 1f; var b = 1f; var a = 1f

                when (componentType) {
                    5126 -> {
                        if (binBuffer.remaining() >= 4 * numComponents) {
                            r = binBuffer.float
                            g = binBuffer.float
                            b = binBuffer.float
                            if (numComponents == 4) a = binBuffer.float
                        }
                    }
                    5121 -> {
                        if (binBuffer.remaining() >= numComponents) {
                            r = (binBuffer.get().toInt() and 0xFF) / 255.0f
                            g = (binBuffer.get().toInt() and 0xFF) / 255.0f
                            b = (binBuffer.get().toInt() and 0xFF) / 255.0f
                            if (numComponents == 4) a = (binBuffer.get().toInt() and 0xFF) / 255.0f
                        }
                    }
                    5123 -> {
                        if (binBuffer.remaining() >= 2 * numComponents) {
                            r = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                            g = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                            b = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                            if (numComponents == 4) a = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                        }
                    }
                }

                val base = i * 4
                result[base] = r
                result[base + 1] = g
                result[base + 2] = b
                result[base + 3] = a
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTexCoordArray(
        accessorIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer,
        vertexCount: Int
    ): FloatArray? {
        return try {
            val accessor = accessors.optJSONObject(accessorIdx) ?: return null
            val bufferViewIdx = accessor.optInt("bufferView", -1)
            if (bufferViewIdx < 0 || bufferViewIdx >= bufferViews.length()) return null
            val count = accessor.optInt("count", 0)
            val componentType = accessor.optInt("componentType", 5126)
            val byteOffset = accessor.optInt("byteOffset", 0)

            val bufferView = bufferViews.optJSONObject(bufferViewIdx) ?: return null
            val bvByteOffset = bufferView.optInt("byteOffset", 0)

            val startOffset = bvByteOffset + byteOffset
            if (startOffset < 0 || startOffset >= binBuffer.capacity()) return null

            binBuffer.position(startOffset)
            val result = FloatArray(vertexCount * 2)

            for (i in 0 until count.coerceAtMost(vertexCount)) {
                var u = 0f
                var v = 0f
                when (componentType) {
                    5126 -> { // FLOAT
                        if (binBuffer.remaining() >= 8) {
                            u = binBuffer.float
                            v = binBuffer.float
                        }
                    }
                    5121 -> { // UNSIGNED_BYTE normalized
                        if (binBuffer.remaining() >= 2) {
                            u = (binBuffer.get().toInt() and 0xFF) / 255.0f
                            v = (binBuffer.get().toInt() and 0xFF) / 255.0f
                        }
                    }
                    5123 -> { // UNSIGNED_SHORT normalized
                        if (binBuffer.remaining() >= 4) {
                            u = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                            v = (binBuffer.short.toInt() and 0xFFFF) / 65535.0f
                        }
                    }
                }
                val base = i * 2
                result[base] = u
                result[base + 1] = v
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextureBitmap(
        json: JSONObject,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): Bitmap? {
        return try {
            val images = json.optJSONArray("images") ?: return null
            if (images.length() == 0) return null

            val textures = json.optJSONArray("textures")
            val materials = json.optJSONArray("materials")

            var targetImageIdx = -1

            // 1. Check materials to find baseColorTexture
            if (materials != null) {
                for (m in 0 until materials.length()) {
                    val mat = materials.optJSONObject(m) ?: continue
                    val pbr = mat.optJSONObject("pbrMetallicRoughness")
                    val baseColorTex = pbr?.optJSONObject("baseColorTexture")
                    val texIdx = baseColorTex?.optInt("index", -1) ?: -1
                    if (texIdx >= 0 && textures != null && texIdx < textures.length()) {
                        val texObj = textures.optJSONObject(texIdx)
                        val source = texObj?.optInt("source", -1) ?: -1
                        if (source in 0 until images.length()) {
                            targetImageIdx = source
                            break
                        }
                    }
                }
            }

            // 2. Fallback: check textures[0].source
            if (targetImageIdx < 0 && textures != null && textures.length() > 0) {
                val texObj = textures.optJSONObject(0)
                val source = texObj?.optInt("source", 0) ?: 0
                if (source in 0 until images.length()) {
                    targetImageIdx = source
                }
            }

            // 3. Fallback: take image 0
            if (targetImageIdx < 0) {
                targetImageIdx = 0
            }

            val imgObj = images.getJSONObject(targetImageIdx)
            val bufferViewIdx = imgObj.optInt("bufferView", -1)

            if (bufferViewIdx in 0 until bufferViews.length()) {
                val bv = bufferViews.getJSONObject(bufferViewIdx)
                val byteOffset = bv.optInt("byteOffset", 0)
                val byteLength = bv.getInt("byteLength")

                if (byteOffset >= 0 && byteOffset + byteLength <= binBuffer.capacity()) {
                    val bytes = ByteArray(byteLength)
                    val oldPos = binBuffer.position()
                    binBuffer.position(byteOffset)
                    binBuffer.get(bytes, 0, byteLength)
                    binBuffer.position(oldPos)

                    // Memory-safe decode (downsample if larger than 2048x2048)
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, byteLength, boundsOptions)

                    var sampleSize = 1
                    val maxDim = 2048
                    while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, byteLength, decodeOptions)
                    if (bmp != null) {
                        Log.i("GlbParser", "Successfully decoded texture: ${bmp.width}x${bmp.height} from bufferView $bufferViewIdx")
                        return bmp
                    }
                }
            }

            // Check if uri (e.g. data:image/png;base64,...)
            val uri = imgObj.optString("uri", "")
            if (uri.startsWith("data:")) {
                val commaIdx = uri.indexOf(',')
                if (commaIdx >= 0) {
                    val base64Data = uri.substring(commaIdx + 1)
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) return bmp
                }
            }

            null
        } catch (e: Exception) {
            Log.e("GlbParser", "Failed to extract texture bitmap: ${e.message}")
            null
        }
    }
}
