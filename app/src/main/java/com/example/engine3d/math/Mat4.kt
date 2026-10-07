package com.example.engine3d.math

import android.opengl.Matrix

class Mat4 {
    val data = FloatArray(16)

    init {
        identity()
    }

    fun identity(): Mat4 {
        Matrix.setIdentityM(data, 0)
        return this
    }

    fun set(source: FloatArray): Mat4 {
        System.arraycopy(source, 0, data, 0, 16)
        return this
    }

    fun set(other: Mat4): Mat4 {
        System.arraycopy(other.data, 0, data, 0, 16)
        return this
    }

    fun translate(x: Float, y: Float, z: Float): Mat4 {
        Matrix.translateM(data, 0, x, y, z)
        return this
    }

    fun rotate(angleDeg: Float, ax: Float, ay: Float, az: Float): Mat4 {
        Matrix.rotateM(data, 0, angleDeg, ax, ay, az)
        return this
    }

    fun scale(sx: Float, sy: Float, sz: Float): Mat4 {
        Matrix.scaleM(data, 0, sx, sy, sz)
        return this
    }

    fun multiply(rhs: Mat4): Mat4 {
        val result = FloatArray(16)
        Matrix.multiplyMM(result, 0, data, 0, rhs.data, 0)
        System.arraycopy(result, 0, data, 0, 16)
        return this
    }

    fun perspective(fovyDeg: Float, aspect: Float, near: Float, far: Float): Mat4 {
        Matrix.perspectiveM(data, 0, fovyDeg, aspect, near, far)
        return this
    }

    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        Matrix.setLookAtM(
            data, 0,
            eye.x, eye.y, eye.z,
            center.x, center.y, center.z,
            up.x, up.y, up.z
        )
        return this
    }

    fun invert(): Mat4 {
        val inv = FloatArray(16)
        Matrix.invertM(inv, 0, data, 0)
        System.arraycopy(inv, 0, data, 0, 16)
        return this
    }

    fun transformPoint(p: Vec3): Vec3 {
        val inVec = floatArrayOf(p.x, p.y, p.z, 1.0f)
        val outVec = FloatArray(4)
        Matrix.multiplyMV(outVec, 0, data, 0, inVec, 0)
        val w = if (outVec[3] != 0f) outVec[3] else 1.0f
        return Vec3(outVec[0] / w, outVec[1] / w, outVec[2] / w)
    }

    fun copy(): Mat4 {
        val m = Mat4()
        m.set(this)
        return m
    }

    companion object {
        fun identity(): Mat4 = Mat4()

        fun perspective(fovyDeg: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val m = Mat4()
            m.perspective(fovyDeg, aspect, near, far)
            return m
        }

        fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
            val m = Mat4()
            m.lookAt(eye, center, up)
            return m
        }
    }
}
