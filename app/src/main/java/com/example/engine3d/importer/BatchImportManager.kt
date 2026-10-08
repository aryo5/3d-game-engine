package com.example.engine3d.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.CharacterAction
import com.example.engine3d.actions.InteractableType
import com.example.engine3d.actions.WorldInteractable
import com.example.engine3d.math.Vec3
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.physics.BarrierType
import com.example.engine3d.physics.WorldBarrier
import com.example.engine3d.terrain.TerrainMesh
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class BatchImportReport(
    var glbCount: Int = 0,
    var objCount: Int = 0,
    var actionCount: Int = 0,
    var npcCount: Int = 0,
    var barrierCount: Int = 0,
    var obbCount: Int = 0,
    var manifestFound: Boolean = false,
    var details: MutableList<String> = mutableListOf()
)

class BatchImportManager(
    private val context: Context,
    private val customModelManager: CustomModelManager,
    private val terrainMesh: TerrainMesh,
    private val actionManager: ActionManager,
    private val npcManager: NpcManager,
    private val barrierManager: BarrierManager
) {
    fun getLocalAssetDir(): File {
        val dir = File(context.filesDir, "ApexAssets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Solusi Arsitektur 1: Batch Scan Folder menggunakan DocumentsContract Native Android
     */
    fun importFolderFromTreeUri(treeUri: Uri): BatchImportReport {
        val report = BatchImportReport()
        val targetDir = getLocalAssetDir()

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            scanTreeDocumentsRecursive(treeUri, rootDocId, targetDir, report)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return report
    }

    private fun scanTreeDocumentsRecursive(
        treeUri: Uri,
        parentDocId: String,
        localTargetDir: File,
        report: BatchImportReport
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        val cursor = context.contentResolver.query(childrenUri, projection, null, null, null) ?: return
        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idCol)
                val displayName = cursor.getString(nameCol)
                val mimeType = cursor.getString(mimeCol)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subFolder = File(localTargetDir, displayName)
                    if (!subFolder.exists()) subFolder.mkdirs()
                    scanTreeDocumentsRecursive(treeUri, docId, subFolder, report)
                } else {
                    try {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        context.contentResolver.openInputStream(fileDocUri)?.use { input ->
                            val localFile = File(localTargetDir, displayName)
                            FileOutputStream(localFile).use { out ->
                                input.copyTo(out)
                            }
                            processLocalAssetFile(localFile, report)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Solusi Arsitektur 2: Batch Import Arsip ZIP atau OBB
     * Mengimpor arsip game bundle (.zip atau .obb) dan membongkar semua aset ke memori offline.
     */
    fun importZipOrObbArchive(archiveUri: Uri, isObb: Boolean = false): BatchImportReport {
        val report = BatchImportReport()
        val targetDir = getLocalAssetDir()

        try {
            val cr = context.contentResolver
            cr.openInputStream(archiveUri)?.use { input ->
                val zis = ZipInputStream(input)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val outFile = File(targetDir, fileName)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        processLocalAssetFile(outFile, report)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                if (isObb) {
                    report.obbCount++
                    report.details.add("Paket .OBB sukses dimuat ke sistem!")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return report
    }

    /**
     * Solusi Arsitektur 3: Pindai Folder Lokal (ApexAssets)
     */
    fun scanLocalAssetFolder(): BatchImportReport {
        val report = BatchImportReport()
        val dir = getLocalAssetDir()
        val files = dir.listFiles() ?: return report

        for (file in files) {
            if (file.isFile) {
                processLocalAssetFile(file, report)
            }
        }

        return report
    }

    /**
     * Solusi Arsitektur File Access Focus: Memindai Folder Langsung Berdasarkan Path Kustom
     * Membaca file .glb, .obj, player_config.json, area_config.json dari path yang ditentukan (misal Termux / Download).
     */
    fun scanDirectFolderPath(folderPath: String): BatchImportReport {
        val report = BatchImportReport()
        val targetDir = File(folderPath)

        if (!targetDir.exists()) {
            report.details.add("❌ Folder tidak ditemukan: $folderPath (Otomatis dibuat folder baru)")
            targetDir.mkdirs()
            return report
        }

        val files = targetDir.listFiles()
        if (files == null || files.isEmpty()) {
            report.details.add("ℹ️ Folder '$folderPath' ada tetapi masih kosong.")
            return report
        }

        for (file in files) {
            if (file.isFile) {
                // Salin ke folder kerja lokal untuk stabilitas rendering
                try {
                    val localCopy = File(getLocalAssetDir(), file.name)
                    file.copyTo(localCopy, overwrite = true)
                    processLocalAssetFile(localCopy, report)
                } catch (e: Exception) {
                    processLocalAssetFile(file, report)
                }
            } else if (file.isDirectory) {
                // Pindai subfolder 1 tingkat
                file.listFiles()?.forEach { sub ->
                    if (sub.isFile) processLocalAssetFile(sub, report)
                }
            }
        }

        return report
    }

    private fun processLocalAssetFile(file: File, report: BatchImportReport) {
        val name = file.name
        val lower = name.lowercase()

        try {
            if (lower.endsWith(".glb")) {
                FileInputStream(file).use { fis ->
                    val mesh = GlbParser.parse(fis, name)
                    if (mesh != null) {
                        report.glbCount++
                        val target = when {
                            lower.contains("terrain") || lower.contains("ground") || lower.contains("land") -> {
                                customModelManager.activeCustomTerrainMesh = mesh
                                terrainMesh.setCustomMesh(mesh)
                                report.details.add("Terrain Kontur diaktifkan: $name")
                                ModelTarget.TERRAIN
                            }
                            lower.contains("char") || lower.contains("player") || lower.contains("hero") -> {
                                customModelManager.activeCustomCharacterMesh = mesh
                                report.details.add("Karakter Player diaktifkan: $name")
                                ModelTarget.CHARACTER
                            }
                            lower.contains("npc") -> {
                                val cleanName = name.substringBeforeLast(".").replace("_", " ")
                                val newNpc = NpcEntity(
                                    id = "npc_${name.substringBeforeLast(".")}",
                                    name = cleanName,
                                    customMeshName = name,
                                    customMesh = mesh
                                )
                                npcManager.addNpc(newNpc)
                                report.details.add("NPC baru ditambahkan: ${newNpc.name}")
                                ModelTarget.WORLD_PROP
                            }
                            else -> ModelTarget.WORLD_PROP
                        }

                        val entry = ImportedModelEntry(name, "GLB", target, mesh)
                        customModelManager.importedModels.removeAll { it.fileName == name }
                        customModelManager.importedModels.add(entry)
                        if (target == ModelTarget.CHARACTER) {
                            customModelManager.setActiveCharacter(entry, updatePlayerConfig = true)
                        }
                    }
                }
            } else if (lower.endsWith(".obj")) {
                FileInputStream(file).use { fis ->
                    val mesh = ObjParser.parse(fis, name)
                    if (mesh != null) {
                        report.objCount++
                        if (lower.contains("terrain") || lower.contains("ground")) {
                            terrainMesh.setCustomMesh(mesh)
                            customModelManager.activeCustomTerrainMesh = mesh
                            report.details.add("Terrain Kontur diaktifkan dari OBJ: $name")
                        }
                        customModelManager.importedModels.removeAll { it.fileName == name }
                        customModelManager.importedModels.add(
                            ImportedModelEntry(name, "OBJ", ModelTarget.WORLD_PROP, mesh)
                        )
                        report.details.add("Model OBJ terbaca: $name")
                    }
                }
            } else if (lower.endsWith(".action.json") || (lower.endsWith(".json") && lower.contains("action"))) {
                val jsonStr = file.readText().trim()
                if (jsonStr.startsWith("[")) {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val action = CharacterAction.fromJson(array.getJSONObject(i).toString())
                        if (action != null) {
                            actionManager.addCustomAction(action)
                            report.actionCount++
                            report.details.add("Aksi Kustom terdaftar: ${action.name}")
                        }
                    }
                } else {
                    val action = CharacterAction.fromJson(jsonStr)
                    if (action != null) {
                        actionManager.addCustomAction(action)
                        report.actionCount++
                        report.details.add("Aksi Kustom terdaftar: ${action.name}")
                    }
                }
            } else if (lower.endsWith(".npc.json") || (lower.endsWith(".json") && lower.contains("npc"))) {
                val jsonStr = file.readText().trim()
                if (jsonStr.startsWith("[")) {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val npc = NpcEntity.fromJson(array.getJSONObject(i).toString())
                        if (npc != null) {
                            val matchMesh = customModelManager.importedModels.firstOrNull { it.fileName == npc.customMeshName }?.mesh
                            if (matchMesh != null) {
                                npc.customMesh = matchMesh
                            }
                            npcManager.addNpc(npc)
                            report.npcCount++
                            report.details.add("NPC terdaftar: ${npc.name} (${npc.role})")
                        }
                    }
                } else {
                    val npc = NpcEntity.fromJson(jsonStr)
                    if (npc != null) {
                        val matchMesh = customModelManager.importedModels.firstOrNull { it.fileName == npc.customMeshName }?.mesh
                        if (matchMesh != null) {
                            npc.customMesh = matchMesh
                        }
                        npcManager.addNpc(npc)
                        report.npcCount++
                        report.details.add("NPC terdaftar: ${npc.name} (${npc.role})")
                    }
                }
            } else if (lower.contains("barrier") || lower.contains("road") || lower.contains("batas")) {
                val jsonStr = file.readText()
                if (barrierManager.loadFromJson(jsonStr)) {
                    report.barrierCount += barrierManager.barriers.size
                    report.details.add("Batas & Blokir Jalan dimuat: ${barrierManager.barriers.size} zona")
                }
            } else if (lower == "player_config.json" || lower.contains("player_config")) {
                val jsonStr = file.readText().trim()
                val config = PlayerConfig.fromJson(jsonStr)
                customModelManager.playerConfig = config
                if (config.modelFile.isNotEmpty()) {
                    val found = customModelManager.importedModels.firstOrNull { it.fileName.equals(config.modelFile, ignoreCase = true) }
                    if (found != null) {
                        customModelManager.activeCustomCharacterMesh = found.mesh
                    }
                }
                report.details.add("🧍 Player Config Karakter dimuat: ${config.characterName} (${config.walkSpeed}m/s)")
            } else if (lower.contains("config") || lower == "manifest.json" || lower == "scene.json") {
                parseAreaConfigFile(file, report)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseAreaConfigFile(configFile: File, report: BatchImportReport) {
        try {
            val jsonStr = configFile.readText()
            val json = JSONObject(jsonStr)
            report.manifestFound = true
            report.details.add("📜 Membaca Config Area: ${configFile.name}")

            // 1. GLB Role Assignments
            val assignments = json.optJSONObject("glbAssignments") ?: json.optJSONObject("assignments")
            if (assignments != null) {
                val keys = assignments.keys()
                while (keys.hasNext()) {
                    val glbName = keys.next()
                    val roleStr = assignments.getString(glbName).uppercase()
                    val matchedEntry = customModelManager.importedModels.firstOrNull { it.fileName.equals(glbName, ignoreCase = true) }
                    if (matchedEntry != null) {
                        when (roleStr) {
                            "TERRAIN", "MAP" -> {
                                customModelManager.activeCustomTerrainMesh = matchedEntry.mesh
                                terrainMesh.setCustomMesh(matchedEntry.mesh)
                                report.details.add("  -> Role GLB '$glbName' set ke TERRAIN MAP")
                            }
                            "CHARACTER", "PLAYER", "HERO" -> {
                                customModelManager.activeCustomCharacterMesh = matchedEntry.mesh
                                report.details.add("  -> Role GLB '$glbName' set ke KARAKTER PLAYER")
                            }
                        }
                    }
                }
            }

            // 2. Direct Terrain / Player Model options
            val terrainFileName = json.optString("terrain", json.optString("terrainModel", ""))
            if (terrainFileName.isNotEmpty()) {
                val found = customModelManager.importedModels.firstOrNull { it.fileName.equals(terrainFileName, ignoreCase = true) }
                if (found != null) {
                    customModelManager.activeCustomTerrainMesh = found.mesh
                    terrainMesh.setCustomMesh(found.mesh)
                    report.details.add("  -> Terrain Model diatur ke: $terrainFileName")
                }
            }

            val playerFileName = json.optString("player", json.optString("playerModel", ""))
            if (playerFileName.isNotEmpty()) {
                val found = customModelManager.importedModels.firstOrNull { it.fileName.equals(playerFileName, ignoreCase = true) }
                if (found != null) {
                    customModelManager.activeCustomCharacterMesh = found.mesh
                    report.details.add("  -> Player Hero Model diatur ke: $playerFileName")
                }
            }

            // 3. House Swap Trigger Zones (Portals Enter/Exit)
            val housePortals = json.optJSONArray("housePortals") ?: json.optJSONArray("portals")
            if (housePortals != null) {
                for (i in 0 until housePortals.length()) {
                    val pObj = housePortals.getJSONObject(i)
                    val pName = pObj.optString("name", "Pintu Rumah ${i + 1}")
                    val entArr = pObj.optJSONArray("entrancePos")
                    val intArr = pObj.optJSONArray("interiorPos")

                    val entPos = if (entArr != null && entArr.length() >= 3) {
                        Vec3(entArr.getDouble(0).toFloat(), entArr.getDouble(1).toFloat(), entArr.getDouble(2).toFloat())
                    } else Vec3(106.8f, 15.6f, 103.3f)

                    val intPos = if (intArr != null && intArr.length() >= 3) {
                        Vec3(intArr.getDouble(0).toFloat(), intArr.getDouble(1).toFloat(), intArr.getDouble(2).toFloat())
                    } else Vec3(250.0f, 1.5f, 250.0f)

                    val enterPortal = WorldInteractable(
                        id = "portal_enter_${System.currentTimeMillis()}_$i",
                        name = pName,
                        type = InteractableType.HOUSE_DOOR_ENTER,
                        position = entPos,
                        targetTeleportPos = intPos,
                        promptText = pObj.optString("promptText", "[Buka Pintu] Masuk Ke Rumah GLB")
                    )
                    val exitPortal = WorldInteractable(
                        id = "portal_exit_${System.currentTimeMillis()}_$i",
                        name = "Pintu Keluar $pName",
                        type = InteractableType.HOUSE_DOOR_EXIT,
                        position = intPos,
                        targetTeleportPos = Vec3(entPos.x, entPos.y, entPos.z + 4f),
                        promptText = "[Buka Pintu] Keluar Ke Desa"
                    )
                    val houseWall = WorldBarrier(
                        id = "house_wall_$i",
                        name = "Dinding $pName",
                        type = BarrierType.WALL_BARRIER,
                        position = entPos,
                        size = Vec3(8f, 5f, 8f),
                        isPassable = false
                    )
                    barrierManager.barriers.add(houseWall)
                    report.details.add("  -> Swap Trigger Zone Pintu '$pName' terpasang!")
                }
            }

            // 4. Barriers array
            val barriersArr = json.optJSONArray("barriers")
            if (barriersArr != null) {
                barrierManager.loadFromJson(barriersArr.toString())
                report.barrierCount += barriersArr.length()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Pindai dan daftarkan seluruh file GLB, OBJ, dan JSON yang ada di folder path aktif
     */
    fun listFilesInFolderPath(folderPath: String): List<File> {
        val dir = File(folderPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val result = mutableListOf<File>()
        dir.listFiles()?.forEach { f ->
            if (f.isFile) {
                val ext = f.extension.lowercase()
                if (ext == "glb" || ext == "obj" || ext == "json" || ext == "txt") {
                    result.add(f)
                }
            }
        }
        return result
    }

    /**
     * Memuat file Player Config JSON pilihan manual pengguna (misal nama file kustom seperti `hero_v1_config.json`)
     */
    fun applyCustomPlayerConfigFile(file: File): String {
        return try {
            val jsonStr = file.readText().trim()
            val config = PlayerConfig.fromJson(jsonStr)
            customModelManager.playerConfig = config
            if (config.modelFile.isNotEmpty()) {
                val found = customModelManager.importedModels.firstOrNull {
                    it.fileName.equals(config.modelFile, ignoreCase = true)
                }
                if (found != null) {
                    customModelManager.activeCustomCharacterMesh = found.mesh
                }
            }
            "✓ Player Config Karakter dimuat dari '${file.name}': Nama=${config.characterName}, WalkSpeed=${config.walkSpeed}"
        } catch (e: Exception) {
            "❌ Gagal membaca config karakter dari '${file.name}': ${e.localizedMessage}"
        }
    }

    /**
     * Memuat file Area Config JSON pilihan manual pengguna (misal nama file kustom seperti `map_desa_manifest.json`)
     */
    fun applyCustomAreaConfigFile(file: File): String {
        val report = BatchImportReport()
        return try {
            parseAreaConfigFile(file, report)
            "✓ Area Config dimuat dari '${file.name}' (${report.details.size} elemen terkonfigurasi)"
        } catch (e: Exception) {
            "❌ Gagal membaca config area dari '${file.name}': ${e.localizedMessage}"
        }
    }
}
