package com.example.engine3d.core

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

class Mesh(
    val name: String = "Mesh",
    val vertices: FloatArray,   // x, y, z
    val normals: FloatArray? = null,    // nx, ny, nz
    val colors: FloatArray? = null,     // r, g, b, a
    val texCoords: FloatArray? = null,  // u, v
    val indices: ShortArray? = null,
    var animationClips: List<GlbAnimationClip> = emptyList(),
    var nodes: List<GlbNode> = emptyList(),
    var rootNodes: List<Int> = emptyList()
) {
    val vertexCount: Int = vertices.size / 3
    val triangleCount: Int = if (indices != null) indices.size / 3 else vertexCount / 3
    val aabb = AABB()

    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null

    init {
        // Calculate AABB
        for (i in 0 until vertexCount) {
            val idx = i * 3
            aabb.encircle(Vec3(vertices[idx], vertices[idx + 1], vertices[idx + 2]))
        }

        // Setup vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertices)
                position(0)
            }

        // Setup normals
        if (normals != null) {
            normalBuffer = ByteBuffer.allocateDirect(normals.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(normals)
                    position(0)
                }
        }

        // Setup colors
        if (colors != null) {
            colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(colors)
                    position(0)
                }
        }

        // Setup indices
        if (indices != null) {
            indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer().apply {
                    put(indices)
                    position(0)
                }
        }
    }

    fun render(
        positionHandle: Int,
        normalHandle: Int,
        colorHandle: Int,
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
    }
}
