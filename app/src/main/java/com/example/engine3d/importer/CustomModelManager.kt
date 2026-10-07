package com.example.engine3d.importer

import android.content.Context
import android.net.Uri
import com.example.engine3d.core.Mesh
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ModelTarget {
    TERRAIN,
    CHARACTER,
    WORLD_PROP
}

data class ImportedModelEntry(
    val fileName: String,
    val format: String,
    val target: ModelTarget,
    val mesh: Mesh
)

class CustomModelManager(private val context: Context) {
    val importedModels = mutableListOf<ImportedModelEntry>()

    var activeCustomTerrainMesh: Mesh? = null
    var activeCustomCharacterMesh: Mesh? = null
    var playerConfig: PlayerConfig = PlayerConfig()

    init {
        // Load any previously imported files from app internal directory
        loadSavedModels()
    }

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "custom_3d_models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadSavedModels() {
        val dir = getModelsDir()
        val files = dir.listFiles() ?: return
        for (file in files) {
            try {
                FileInputStream(file).use { fis ->
                    val mesh = if (file.name.endsWith(".glb", ignoreCase = true)) {
                        GlbParser.parse(fis, file.name)
                    } else if (file.name.endsWith(".obj", ignoreCase = true)) {
                        ObjParser.parse(fis, file.name)
                    } else null

                    if (mesh != null) {
                        val target = when {
                            file.name.contains("terrain", ignoreCase = true) -> ModelTarget.TERRAIN
                            file.name.contains("char", ignoreCase = true) -> ModelTarget.CHARACTER
                            else -> ModelTarget.WORLD_PROP
                        }
                        importedModels.add(ImportedModelEntry(file.name, file.extension.uppercase(), target, mesh))
                    }
                }
            } catch (e: Exception) {
                // Ignore corrupt cache
            }
        }
    }

    fun importFromUri(uri: Uri, target: ModelTarget, customName: String? = null): ImportedModelEntry? {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val originalName = customName ?: (uri.lastPathSegment ?: "imported_model.glb")
            val cleanName = originalName.substringAfterLast("/")
            val isGlb = cleanName.endsWith(".glb", ignoreCase = true) || cleanName.contains("glb", ignoreCase = true)
            val isObj = cleanName.endsWith(".obj", ignoreCase = true) || cleanName.contains("obj", ignoreCase = true)

            val dir = getModelsDir()
            val targetFile = File(dir, cleanName)

            // Save copy locally for offline access
            FileOutputStream(targetFile).use { fos ->
                inputStream.copyTo(fos)
            }
            inputStream.close()

            // Parse saved copy
            val mesh = FileInputStream(targetFile).use { fis ->
                if (isGlb) {
                    GlbParser.parse(fis, cleanName)
                } else if (isObj) {
                    ObjParser.parse(fis, cleanName)
                } else {
                    // Try GLB first, then OBJ
                    GlbParser.parse(fis, cleanName)
                }
            } ?: return null

            val entry = ImportedModelEntry(
                fileName = cleanName,
                format = if (isGlb) "GLB" else "OBJ",
                target = target,
                mesh = mesh
            )

            importedModels.removeAll { it.fileName == cleanName }
            importedModels.add(entry)

            if (target == ModelTarget.TERRAIN) {
                activeCustomTerrainMesh = mesh
            } else if (target == ModelTarget.CHARACTER) {
                activeCustomCharacterMesh = mesh
            }

            return entry
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteModel(entry: ImportedModelEntry) {
        importedModels.remove(entry)
        val file = File(getModelsDir(), entry.fileName)
        if (file.exists()) file.delete()

        if (activeCustomTerrainMesh == entry.mesh) {
            activeCustomTerrainMesh = null
        }
        if (activeCustomCharacterMesh == entry.mesh) {
            activeCustomCharacterMesh = null
        }
    }
}
