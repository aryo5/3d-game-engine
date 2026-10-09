package com.example.engine3d.core

import android.graphics.Bitmap
import android.opengl.GLES20
import com.example.engine3d.math.AABB
import com.example.engine3d.math.Mat4
import com.example.engine3d.math.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

data class KeyframeChannel(
    val nodeIndex: Int,
    val path: String,
    val times: FloatArray,
    val values: FloatArray
)

data class GlbAnimationClip(
    val name: String,
    val duration: Float,
    val channels: List<KeyframeChannel> = emptyList()
)

class GlbNode(
    val index: Int,
    val name: String,
    val parentIndex: Int,
    val meshIndex: Int,
    val defaultLocalMatrix: Mat4,
    var animatedLocalMatrix: Mat4 = Mat4().set(defaultLocalMatrix),
    var animatedWorldMatrix: Mat4 = Mat4(),
    val children: List<Int> = emptyList(),
    val subMesh: Mesh? = null
)

data class SkinDef(
    val joints: IntArray,
    val inverseBindMatrices: Array<Mat4>
)

class Mesh(
    val name: String = "Mesh",
    val vertices: FloatArray,
    val normals: FloatArray? = null,
    val colors: FloatArray? = null,
    val texCoords: FloatArray? = null,
    val indices: ShortArray? = null,
    var animationClips: List<GlbAnimationClip> = emptyList(),
    var nodes: List<GlbNode> = emptyList(),
    var rootNodes: List<Int> = emptyList(),
    var bindVertices: FloatArray? = null,
    var vertexJoints: IntArray? = null,
    var vertexWeights: FloatArray? = null,
    var skinJointNodes: IntArray? = null,
    var inverseBindMatrices: Array<Mat4>? = null,
    var skins: List<SkinDef>? = null,
    var vertexSkinIndices: IntArray? = null,
    val hasExplicitVertexColors: Boolean = false,
    var textureBitmap: Bitmap? = null,
    var textureId: Int = 0
) {
    val vertexCount: Int = vertices.size / 3
    val triangleCount: Int = if (indices != null) indices.size / 3 else vertexCount / 3
    val aabb = AABB()

    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    var texCoordBuffer: FloatBuffer? = null
        private set
    private var indexBuffer: ShortBuffer? = null

    private var skinnedVertices: FloatArray? = null
    private var cachedSkinMatrices: Array<Mat4>? = null
    private var cachedMultiSkinMatrices: Array<Array<Mat4>>? = null

    init {
        for (i in 0 until vertexCount) {
            val idx = i * 3
            aabb.encircle(Vec3(vertices[idx], vertices[idx + 1], vertices[idx + 2]))
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertices)
                position(0)
            }

        if (normals != null) {
            normalBuffer = ByteBuffer.allocateDirect(normals.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(normals)
                    position(0)
                }
        }

        if (colors != null) {
            colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(colors)
                    position(0)
                }
        }

        if (texCoords != null) {
            texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(texCoords)
                    position(0)
                }
        }

        if (indices != null) {
            indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer().apply {
                    put(indices)
                    position(0)
                }
        }
    }

    fun updateVertices(newVertices: FloatArray) {
        vertexBuffer.position(0)
        vertexBuffer.put(newVertices)
        vertexBuffer.position(0)
    }

    fun applySkinning() {
        val bVerts = bindVertices ?: return
        val joints = vertexJoints ?: return
        val weights = vertexWeights ?: return
        if (nodes.isEmpty()) return

        val multiSkins = skins
        val skinIndices = vertexSkinIndices

        if (multiSkins != null && multiSkins.isNotEmpty() && skinIndices != null) {
            // MULTI-SKIN EVALUATION (Supports models with multiple meshes/skins like FBX2glTF, Quaternius, Mixamo)
            val skinCount = multiSkins.size
            if (cachedMultiSkinMatrices == null || cachedMultiSkinMatrices!!.size != skinCount) {
                cachedMultiSkinMatrices = Array(skinCount) { s ->
                    Array(multiSkins[s].joints.size) { Mat4() }
                }
            }

            val allSMats = cachedMultiSkinMatrices!!
            for (s in 0 until skinCount) {
                val skinDef = multiSkins[s]
                val sMatArr = allSMats[s]
                val jCount = skinDef.joints.size
                for (k in 0 until jCount) {
                    val nodeIdx = skinDef.joints[k]
                    val jointNode = nodes.getOrNull(nodeIdx)
                    if (jointNode != null && k < skinDef.inverseBindMatrices.size) {
                        sMatArr[k].set(jointNode.animatedWorldMatrix).multiply(skinDef.inverseBindMatrices[k])
                    } else {
                        sMatArr[k].identity()
                    }
                }
            }

            if (skinnedVertices == null || skinnedVertices!!.size != bVerts.size) {
                skinnedVertices = FloatArray(bVerts.size)
            }
            val outVerts = skinnedVertices!!

            val totalVerts = vertexCount
            for (v in 0 until totalVerts) {
                val v3 = v * 3
                val vx = bVerts[v3]
                val vy = bVerts[v3 + 1]
                val vz = bVerts[v3 + 2]

                val sIdx = skinIndices[v].coerceIn(0, skinCount - 1)
                val sMats = allSMats[sIdx]
                val maxJoints = multiSkins[sIdx].joints.size

                val v4 = v * 4
                var ox = 0f
                var oy = 0f
                var oz = 0f

                for (k in 0 until 4) {
                    val w = weights[v4 + k]
                    if (w > 0.0001f) {
                        val jointIdx = joints[v4 + k]
                        if (jointIdx in 0 until maxJoints) {
                            val m = sMats[jointIdx].data
                            val tx = m[0] * vx + m[4] * vy + m[8] * vz + m[12]
                            val ty = m[1] * vx + m[5] * vy + m[9] * vz + m[13]
                            val tz = m[2] * vx + m[6] * vy + m[10] * vz + m[14]
                            ox += tx * w
                            oy += ty * w
                            oz += tz * w
                        }
                    }
                }
                outVerts[v3] = ox
                outVerts[v3 + 1] = oy
                outVerts[v3 + 2] = oz
            }

            updateVertices(outVerts)
            return
        }

        // Single skin fallback
        val skinNodes = skinJointNodes ?: return
        val ibmList = inverseBindMatrices ?: return
        val skinCount = skinNodes.size
        if (cachedSkinMatrices == null || cachedSkinMatrices!!.size != skinCount) {
            cachedSkinMatrices = Array(skinCount) { Mat4() }
        }

        val sMatrices = cachedSkinMatrices!!
        for (k in 0 until skinCount) {
            val nodeIdx = skinNodes[k]
            val jointNode = nodes.getOrNull(nodeIdx)
            if (jointNode != null && k < ibmList.size) {
                sMatrices[k].set(jointNode.animatedWorldMatrix).multiply(ibmList[k])
            } else {
                sMatrices[k].identity()
            }
        }

        if (skinnedVertices == null || skinnedVertices!!.size != bVerts.size) {
            skinnedVertices = FloatArray(bVerts.size)
        }
        val outVerts = skinnedVertices!!

        val totalVerts = vertexCount
        for (v in 0 until totalVerts) {
            val v3 = v * 3
            val vx = bVerts[v3]
            val vy = bVerts[v3 + 1]
            val vz = bVerts[v3 + 2]

            val v4 = v * 4
            var ox = 0f
            var oy = 0f
            var oz = 0f

            for (k in 0 until 4) {
                val w = weights[v4 + k]
                if (w > 0.0001f) {
                    val jointIdx = joints[v4 + k]
                    if (jointIdx in 0 until skinCount) {
                        val m = sMatrices[jointIdx].data
                        val tx = m[0] * vx + m[4] * vy + m[8] * vz + m[12]
                        val ty = m[1] * vx + m[5] * vy + m[9] * vz + m[13]
                        val tz = m[2] * vx + m[6] * vy + m[10] * vz + m[14]
                        ox += tx * w
                        oy += ty * w
                        oz += tz * w
                    }
                }
            }
            outVerts[v3] = ox
            outVerts[v3 + 1] = oy
            outVerts[v3 + 2] = oz
        }

        updateVertices(outVerts)
    }

    fun render(
        positionHandle: Int,
        normalHandle: Int,
        colorHandle: Int,
        wireframe: Boolean = false
    ) {
        render(positionHandle, normalHandle, colorHandle, -1, wireframe)
    }

    fun render(
        positionHandle: Int,
        normalHandle: Int,
        colorHandle: Int,
        texCoordHandle: Int,
        wireframe: Boolean = false
    ) {
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false,
            0, vertexBuffer
        )

        if (normalHandle >= 0 && normalBuffer != null) {
            GLES20.glEnableVertexAttribArray(normalHandle)
            GLES20.glVertexAttribPointer(
                normalHandle, 3, GLES20.GL_FLOAT, false,
                0, normalBuffer
            )
        }

        if (colorHandle >= 0 && colorBuffer != null) {
            GLES20.glEnableVertexAttribArray(colorHandle)
            GLES20.glVertexAttribPointer(
                colorHandle, 4, GLES20.GL_FLOAT, false,
                0, colorBuffer
            )
        }

        if (texCoordHandle >= 0 && texCoordBuffer != null) {
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(
                texCoordHandle, 2, GLES20.GL_FLOAT, false,
                0, texCoordBuffer
            )
        }

        val renderMode = if (wireframe) GLES20.GL_LINES else GLES20.GL_TRIANGLES

        if (indexBuffer != null) {
            GLES20.glDrawElements(
                renderMode,
                indices!!.size,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
            )
        } else {
            GLES20.glDrawArrays(renderMode, 0, vertexCount)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        if (normalHandle >= 0 && normalBuffer != null) {
            GLES20.glDisableVertexAttribArray(normalHandle)
        }
        if (colorHandle >= 0 && colorBuffer != null) {
            GLES20.glDisableVertexAttribArray(colorHandle)
        }
        if (texCoordHandle >= 0 && texCoordBuffer != null) {
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }
    }
}
