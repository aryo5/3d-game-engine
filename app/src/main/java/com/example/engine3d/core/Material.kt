package com.example.engine3d.core

data class Material(
    var name: String = "DefaultMaterial",
    var diffuseColor: FloatArray = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f),
    var specularColor: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
    var roughness: Float = 0.4f,
    var shininess: Float = 32.0f,
    var unlit: Boolean = false,
    var wireframe: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Material

        if (name != other.name) return false
        if (!diffuseColor.contentEquals(other.diffuseColor)) return false
        if (!specularColor.contentEquals(other.specularColor)) return false
        if (roughness != other.roughness) return false
        if (shininess != other.shininess) return false
        if (unlit != other.unlit) return false
        if (wireframe != other.wireframe) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + diffuseColor.contentHashCode()
        result = 31 * result + specularColor.contentHashCode()
        result = 31 * result + roughness.hashCode()
        result = 31 * result + shininess.hashCode()
        result = 31 * result + unlit.hashCode()
        result = 31 * result + wireframe.hashCode()
        return result
    }
}
