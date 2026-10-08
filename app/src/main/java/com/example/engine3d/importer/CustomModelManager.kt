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
    var activeTerrainFileName: String? = null
    var activeCharacterFileName: String? = null
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
        // Ensure karakter.glb is copied from assets if available
        try {
            val targetKarFile = File(dir, "karakter.glb")
            if (!targetKarFile.exists()) {
                context.assets.open("karakter.glb").use { input ->
                    FileOutputStream(targetKarFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            // asset might not be present or error
        }

        val files = dir.listFiles() ?: return
        for (file in files) {
            try {
                if (file.name == "player_config.json" || file.name == "active_scene.json") continue
                FileInputStream(file).use { fis ->
                    val mesh = if (file.name.endsWith(".glb", ignoreCase = true)) {
                        GlbParser.parse(fis, file.name)
                    } else if (file.name.endsWith(".obj", ignoreCase = true)) {
                        ObjParser.parse(fis, file.name)
                    } else null

                    if (mesh != null) {
                        val target = when {
                            file.name.equals("karakter.glb", ignoreCase = true) -> ModelTarget.CHARACTER
                            file.name.contains("terrain", ignoreCase = true) || file.name.contains("map", ignoreCase = true) -> ModelTarget.TERRAIN
                            file.name.contains("char", ignoreCase = true) || file.name.contains("hero", ignoreCase = true) -> ModelTarget.CHARACTER
                            else -> ModelTarget.WORLD_PROP
                        }
                        importedModels.removeAll { it.fileName.equals(file.name, ignoreCase = true) }
                        importedModels.add(ImportedModelEntry(file.name, file.extension.uppercase(), target, mesh))
                    }
                }
            } catch (e: Exception) {
                // Ignore corrupt cache
            }
        }

        try {
            val cfgFile = File(dir, "player_config.json")
            if (cfgFile.exists()) {
                val loadedConfig = PlayerConfig.fromJson(cfgFile.readText())
                val normalizedConfig = if (loadedConfig.modelFile.equals("karakter.glb", ignoreCase = true) && loadedConfig.scaleX > 0.8f) {
                    loadedConfig.copy(scaleX = 0.4f, scaleY = 0.4f, scaleZ = 0.4f, heightOffset = 0f)
                } else loadedConfig
                playerConfig = normalizedConfig
                val matchingModel = importedModels.firstOrNull { it.fileName.equals(normalizedConfig.modelFile, ignoreCase = true) }
                if (matchingModel != null) {
                    activeCustomCharacterMesh = matchingModel.mesh
                    activeCharacterFileName = matchingModel.fileName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Restore active scene model settings if present
        try {
            val sceneFile = File(dir, "active_scene.json")
            if (sceneFile.exists()) {
                val json = org.json.JSONObject(sceneFile.readText())
                val savedTerrain = json.optString("activeTerrain", "")
                val savedChar = json.optString("activeCharacter", "")
                if (savedTerrain.isNotEmpty()) {
                    val matchT = importedModels.firstOrNull { it.fileName.equals(savedTerrain, ignoreCase = true) }
                    if (matchT != null) {
                        activeCustomTerrainMesh = matchT.mesh
                        activeTerrainFileName = matchT.fileName
                    }
                }
                if (savedChar.isNotEmpty()) {
                    val matchC = importedModels.firstOrNull { it.fileName.equals(savedChar, ignoreCase = true) }
                    if (matchC != null) {
                        activeCustomCharacterMesh = matchC.mesh
                        activeCharacterFileName = matchC.fileName
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no character is active yet, default to karakter.glb or first CHARACTER model
        if (activeCustomCharacterMesh == null && importedModels.isNotEmpty()) {
            val charModel = importedModels.firstOrNull { it.fileName.equals("karakter.glb", ignoreCase = true) }
                ?: importedModels.firstOrNull { it.target == ModelTarget.CHARACTER }
                ?: importedModels.first()
            if (charModel != null) {
                setActiveCharacter(charModel, updatePlayerConfig = true)
            }
        }

        // Ensure karakter.glb has its 11 animations mapped if current config has placeholder/empty anims
        val currentKar = importedModels.firstOrNull { it.fileName.equals("karakter.glb", ignoreCase = true) }
        if (currentKar != null && (activeCharacterFileName.equals("karakter.glb", ignoreCase = true) || activeCustomCharacterMesh == null)) {
            if (activeCustomCharacterMesh == null) {
                setActiveCharacter(currentKar, updatePlayerConfig = true)
            } else if (playerConfig.animIdleName.isBlank() || playerConfig.animIdleName.startsWith("anim_")) {
                setActiveCharacter(currentKar, updatePlayerConfig = true)
            }
        }

        // If no terrain is active yet, but models exist with TERRAIN target
        if (activeCustomTerrainMesh == null && importedModels.isNotEmpty()) {
            val terrainModel = importedModels.firstOrNull { it.target == ModelTarget.TERRAIN }
            if (terrainModel != null) {
                activeCustomTerrainMesh = terrainModel.mesh
                activeTerrainFileName = terrainModel.fileName
            }
        }
    }

    private fun saveActiveScene() {
        try {
            val sceneFile = File(getModelsDir(), "active_scene.json")
            val json = org.json.JSONObject().apply {
                put("activeTerrain", activeTerrainFileName ?: "")
                put("activeCharacter", activeCharacterFileName ?: "")
            }
            sceneFile.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mengganti penuh terrain/permukaan default dengan model kustom (overwrite tanpa konflik)
     */
    fun setActiveTerrain(entry: ImportedModelEntry?, terrainMesh: com.example.engine3d.terrain.TerrainMesh? = null) {
        activeCustomTerrainMesh = entry?.mesh
        activeTerrainFileName = entry?.fileName
        if (entry != null && terrainMesh != null) {
            terrainMesh.setCustomMesh(entry.mesh)
        } else if (entry == null && terrainMesh != null) {
            terrainMesh.setPreset(com.example.engine3d.terrain.TerrainPreset.HIGHLAND_HILLS)
        }
        saveActiveScene()
    }

    /**
     * Mengganti penuh karakter default dengan model kustom (overwrite tanpa konflik)
     */
    fun setActiveCharacter(entry: ImportedModelEntry?, updatePlayerConfig: Boolean = true) {
        activeCustomCharacterMesh = entry?.mesh
        activeCharacterFileName = entry?.fileName
        if (entry != null && updatePlayerConfig) {
            val clips = entry.mesh.animationClips.map { it.name }
            val clean = { str: String -> str.substringAfterLast("|").substringAfterLast(":").lowercase().replace("_", "").replace("-", "") }

            val autoIdle = clips.firstOrNull {
                val c = clean(it)
                c.contains("idle") || c.contains("stand") || c.contains("breath") || c.contains("stay") || c.contains("wait") || c.contains("loop")
            } ?: clips.firstOrNull() ?: "idle"

            val autoWalk = clips.firstOrNull {
                val c = clean(it)
                c.contains("walk") || c.contains("move") || c.contains("jalan") || c.contains("step") || c.contains("stride") || c.contains("forward")
            } ?: clips.firstOrNull { clean(it).contains("run") } ?: autoIdle

            val autoRun = clips.firstOrNull {
                val c = clean(it)
                c.contains("run") || c.contains("sprint") || c.contains("lari") || c.contains("dash") || c.contains("fast") || c.contains("jog")
            } ?: autoWalk

            val autoJump = clips.firstOrNull {
                val c = clean(it)
                c.contains("jump") || c.contains("leap") || c.contains("lompat") || c.contains("air") || c.contains("fall")
            } ?: autoIdle

            val autoSlash = clips.firstOrNull {
                val c = clean(it)
                c.contains("slash") || c.contains("attack") || c.contains("serang") || c.contains("hit") || c.contains("strike") || c.contains("punch") || c.contains("sword")
            } ?: autoIdle

            val shouldAutoBind = playerConfig.animIdleName.startsWith("anim_") ||
                clips.none { it.equals(playerConfig.animIdleName, ignoreCase = true) }

            val isKar = entry.fileName.equals("karakter.glb", ignoreCase = true)
            val rawHeight = entry.mesh.aabb.max.y - entry.mesh.aabb.min.y
            
            // Standard humanoid character target height in world space is ~1.8 units
            val autoTargetScale = if (isKar) {
                0.4f
            } else if (rawHeight > 0.05f) {
                // Automatically adapt models exported in centimeters (Mixamo ~180cm -> 0.01f),
                // meters (Blender ~1.8m -> 1.0f), or custom scale (~4.4 units -> 0.41f).
                (1.8f / rawHeight).coerceIn(0.005f, 20f)
            } else {
                1.0f
            }
            
            val autoHeightOffset = if (isKar) {
                0f
            } else if (rawHeight > 0.05f) {
                // Align lowest vertex (feet) cleanly with ground surface
                (-entry.mesh.aabb.min.y * autoTargetScale).coerceIn(-10f, 10f)
            } else {
                0f
            }

            val isSameModel = playerConfig.modelFile.equals(entry.fileName, ignoreCase = true)
            val finalScale = if (isKar) 0.4f else if (isSameModel) playerConfig.scaleX else autoTargetScale
            val finalHeightOffset = if (isKar) 0f else if (isSameModel) playerConfig.heightOffset else autoHeightOffset

            playerConfig = playerConfig.copy(
                modelFile = entry.fileName,
                characterName = entry.fileName.substringBeforeLast("."),
                scaleX = finalScale,
                scaleY = finalScale,
                scaleZ = finalScale,
                heightOffset = finalHeightOffset,
                animIdleName = if (clips.isNotEmpty() && shouldAutoBind) autoIdle else playerConfig.animIdleName,
                animWalkName = if (clips.isNotEmpty() && shouldAutoBind) autoWalk else playerConfig.animWalkName,
                animRunName = if (clips.isNotEmpty() && shouldAutoBind) autoRun else playerConfig.animRunName,
                animJumpName = if (clips.isNotEmpty() && shouldAutoBind) autoJump else playerConfig.animJumpName,
                animSlashName = if (clips.isNotEmpty() && shouldAutoBind) autoSlash else playerConfig.animSlashName
            )
            savePlayerConfig(playerConfig)
        }
        saveActiveScene()
    }

    fun clearActiveTerrain(terrainMesh: com.example.engine3d.terrain.TerrainMesh? = null) {
        setActiveTerrain(null, terrainMesh)
    }

    fun clearActiveCharacter() {
        setActiveCharacter(null, false)
    }

    fun savePlayerConfig(config: PlayerConfig) {
        playerConfig = config
        try {
            val file = File(getModelsDir(), "player_config.json")
            FileOutputStream(file).use { fos ->
                fos.write(config.toJson().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                setActiveTerrain(entry)
            } else if (target == ModelTarget.CHARACTER) {
                setActiveCharacter(entry, updatePlayerConfig = true)
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
