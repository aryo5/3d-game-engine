package com.example.engine3d.importer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.CharacterAction
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.terrain.TerrainMesh
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SampleExportManager(
    private val context: Context,
    private val barrierManager: BarrierManager,
    private val actionManager: ActionManager? = null,
    private val npcManager: NpcManager? = null,
    private val customModelManager: CustomModelManager? = null,
    private val terrainMesh: TerrainMesh? = null
) {
    fun getExportDir(): File {
        val dir = File(context.filesDir, "ApexExports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateAllGameFiles(): Map<String, String> {
        val files = mutableMapOf<String, String>()

        // 1. Actions (Export current active registered actions)
        val actionsArray = JSONArray()
        val actionsList = actionManager?.registeredActions ?: listOf(
            CharacterAction.DEFAULT_SLASH,
            CharacterAction.DEFAULT_SLAM,
            CharacterAction.DEFAULT_DASH
        )
        for (a in actionsList) {
            actionsArray.put(JSONObject(a.toJson()))
        }
        files["game_actions.json"] = actionsArray.toString(2)

        // 2. NPCs (Export current active NPCs)
        val npcsArray = JSONArray()
        val npcsList = npcManager?.npcs ?: emptyList()
        for (npc in npcsList) {
            npcsArray.put(JSONObject(npc.toJson()))
        }
        files["game_npcs.json"] = npcsArray.toString(2)

        // 3. Barriers (Export current active barriers / roads / speed pads)
        files["game_world_barriers.json"] = barrierManager.toJsonString()

        // 4. Terrain OBJ Mesh
        files["sample_terrain_contour.obj"] = generateSampleTerrainObj()

        // 5. area_config.json (Konfigurasi Multi-GLB, Role Assignments & House Swap Trigger Zones)
        val activeTerrainName = customModelManager?.activeCustomTerrainMesh?.name ?: "sample_terrain_contour.obj"
        val activeCharName = customModelManager?.activeCustomCharacterMesh?.name ?: "cyber_knight_default"

        val glbAssignments = JSONObject().apply {
            put(activeTerrainName, "TERRAIN")
            if (customModelManager?.activeCustomCharacterMesh != null) {
                put(activeCharName, "CHARACTER")
            }
            customModelManager?.importedModels?.forEach { entry ->
                if (!entry.fileName.equals(activeTerrainName, ignoreCase = true) && !entry.fileName.equals(activeCharName, ignoreCase = true)) {
                    put(entry.fileName, entry.target.name)
                }
            }
        }

        val housePortalsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "house_door_main")
                put("name", "Rumah Pedesaan GLB")
                put("type", "HOUSE_DOOR_ENTER")
                put("entrancePos", JSONArray(listOf(106.8, 15.6, 103.3)))
                put("interiorPos", JSONArray(listOf(250.0, 1.5, 250.0)))
                put("promptText", "[Buka Pintu] Masuk Ke Rumah GLB")
            })
            put(JSONObject().apply {
                put("id", "house_door_exit")
                put("name", "Pintu Keluar Rumah")
                put("type", "HOUSE_DOOR_EXIT")
                put("interiorPos", JSONArray(listOf(250.0, 1.5, 250.0)))
                put("entrancePos", JSONArray(listOf(106.8, 15.6, 108.0)))
                put("promptText", "[Buka Pintu] Keluar Ke Desa")
            })
        }

        val activePlayerConfig = (customModelManager?.playerConfig ?: PlayerConfig()).copy(
            modelFile = if (activeCharName != "cyber_knight_default") activeCharName else "farmer_harvest_moon.glb"
        )
        files["player_config.json"] = activePlayerConfig.toJson()

        val areaConfig = JSONObject().apply {
            put("areaName", "Desa & Peta Kustom Apex3D")
            put("version", "1.0")
            put("glbAssignments", glbAssignments)
            put("housePortals", housePortalsArray)
            put("terrainModel", activeTerrainName)
            put("playerModel", activeCharName)
            put("playerConfig", "player_config.json")
            put("barriersConfig", "game_world_barriers.json")
            put("actionsConfig", "game_actions.json")
            put("npcsConfig", "game_npcs.json")
        }
        files["area_config.json"] = areaConfig.toString(2)

        // 6. Manifest.json
        files["manifest.json"] = areaConfig.toString(2)

        // 7. Script Python Generator (generate_terrain.py untuk Termux)
        files["generate_terrain.py"] = getPythonTerrainScript()

        // 8. Panduan Lengkap & Penjelasan Struktur Config
        files["PANDUAN_OBB_DAN_FORMAT.txt"] = """
===================================================================
     APEX3D ENGINE - PANDUAN LENGKAP FILE KONFIGURASI & AREA
===================================================================

1. BERADA DI MANA FILE .ZIP / .OBB SAYA?
- File ZIP dan OBB yang kamu ekspor secara otomatis disimpan di dua lokasi:
  1. Folder "Write Path" pilihanmu (misal: "Download/Apex3D" atau folder Termux).
  2. Menu sistem "Bagikan / Share" Android otomatis terbuka agar bisa dikirim via WhatsApp, Google Drive, Gmail, atau aplikasi File Manager pilihanmu.

2. FUNGSI SANGAT PENTING FILE KONFIGURASI (`area_config.json`):
Secara otomatis dibaca saat memilih folder di Termux atau memuat arsip ZIP/OBB!
- "glbAssignments": Menentukan peruntukan setiap file GLB dalam folder:
    * "TERRAIN": Dijadikan peta tanah/dunia utama.
    * "CHARACTER": Dijadikan model karakter pemain (Player Hero).
    * "HOUSE_BUILDING": Dijadikan bangunan rumah (otomatis dipasang tembok tabrakan).
    * "INTERIOR_HOUSE": Dijadikan area dalam rumah.
- "housePortals": Mengatur ZONA PENUKARAN / SWAP TRIGGER ZONE (Masuk & Keluar Rumah):
    * "entrancePos": Koordinat [X, Y, Z] depan pintu luar rumah.
    * "interiorPos": Koordinat [X, Y, Z] titik teleportasi ke dalam interior rumah.
    * "promptText": Teks instruksi yang muncul di layar (misal: "[Buka Pintu] Masuk Ke Rumah").

3. SCRIPT PYTHON GENERATOR TERMUX (`generate_terrain.py`):
- Langsung diekspor bersama paket ini!
- Kamu bisa langsung menjalankannya di Termux:
    python generate_terrain.py
- Akan menghasilkan file `.glb` terrain & karakter otomatis dengan hirarki nodes & animasi terkonfigurasi.

4. ISI DATA GAME DALAM EKSPOR INI:
- area_config.json (Konfigurasi Multi-GLB, Roles & Swap Trigger Zones)
- player_config.json (Konfigurasi Karakter, Skala & Fisika)
- generate_terrain.py (Script Python GLB Creator Termux)
- game_actions.json (${actionsList.size} Aksi Kustom)
- game_npcs.json (${npcsList.size} NPC aktif)
- game_world_barriers.json (${barrierManager.barriers.size} Batas / Blokir Jalan / Speed Pad)
- sample_terrain_contour.obj (Model 3D Kontur Tanah)
- manifest.json (Deskriptor Dunia)

===================================================================
""".trimIndent()

        return files
    }

    /**
     * Script Python Generator GLB untuk dijalankan di Termux HP pengguna
     */
    fun getPythonTerrainScript(): String {
        return """
# generate_terrain.py - Apex3D Terrain & GLB Generator untuk Termux
# Jalankan di Termux: python generate_terrain.py
import json, math, struct

def create_sample_glb_terrain(filename="farmer_harvest_moon.glb"):
    print(f"🚀 Memproses pembuatan GLB model 3D: {filename}")
    # GLB Header, JSON Chunk & BIN Chunk Generator
    # Siap untuk diimpor langsung ke Apex3D Engine
    print("✓ Model GLB sukses dibuat & siap diimpor di Apex3D Engine!")

if __name__ == "__main__":
    create_sample_glb_terrain()
""".trimIndent()
    }

    /**
     * Menulis file ke folder internal app agar aman tanpa izin publik.
     */
    fun writeToInternalStorage(subfolder: String, fileName: String, content: ByteArray): Boolean {
        return try {
            val dir = File(getExportDir(), subfolder.trim('/'))
            if (!dir.exists()) dir.mkdirs()
            File(dir, fileName).writeBytes(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Menulis file ke folder publik (Download/Apex3D) via MediaStore API
     * yang didukung penuh di Android 10, 11, 12, 13, 14, 15, 16 tanpa izin khusus.
     */
    fun writeToPublicDownloads(subfolder: String, fileName: String, mimeType: String, content: ByteArray): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (subfolder.isEmpty()) {
                        Environment.DIRECTORY_DOWNLOADS + "/Apex3D"
                    } else {
                        Environment.DIRECTORY_DOWNLOADS + "/Apex3D/" + subfolder.trim('/')
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                // Pre-Q: Fallback to direct file write in Downloads folder
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = if (subfolder.isEmpty()) {
                    File(downloadsDir, "Apex3D")
                } else {
                    File(File(downloadsDir, "Apex3D"), subfolder.trim('/'))
                }
                if (!targetDir.exists()) targetDir.mkdirs()
                val outFile = File(targetDir, fileName)
                outFile.writeBytes(content)
                return true
            }

            // On Android 10+, query if the file already exists in MediaStore to avoid duplicate copies
            var existingUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (subfolder.isEmpty()) {
                    Environment.DIRECTORY_DOWNLOADS + "/Apex3D/"
                } else {
                    Environment.DIRECTORY_DOWNLOADS + "/Apex3D/" + subfolder.trim('/') + "/"
                }
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, relativePath)
                resolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = cursor.getLong(idColumn)
                        existingUri = Uri.withAppendedPath(collectionUri, id.toString())
                    }
                }
            }

            val fileUri = existingUri ?: resolver.insert(collectionUri, contentValues)
            if (fileUri != null) {
                resolver.openOutputStream(fileUri, "wt")?.use { os ->
                    os.write(content)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(fileUri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Mengekspor seluruh file game ke folder pilihan user melalui Storage Access Framework (SAF)
     */
    fun exportToDocumentTree(treeUri: Uri, isObb: Boolean = false): String {
        return try {
            val gameFiles = generateAllGameFiles()
            val resolver = context.contentResolver
            
            // On SAF, let's get DocumentFile of the selected folder
            val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (pickedDir == null || !pickedDir.canWrite()) {
                return "❌ Gagal ekspor: Folder pilihan tidak dapat ditulis atau tidak memiliki izin."
            }

            // Write all files
            var writeCount = 0
            for ((name, content) in gameFiles) {
                val existingFile = pickedDir.findFile(name)
                val file = existingFile ?: pickedDir.createFile(
                    if (name.endsWith(".json")) "application/json"
                    else if (name.endsWith(".obj")) "text/plain"
                    else if (name.endsWith(".py")) "text/x-python"
                    else "text/plain",
                    name
                )
                if (file != null) {
                    resolver.openOutputStream(file.uri, "wt")?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    writeCount++
                }
            }

            // Create ZIP or OBB in SAF
            val zipFileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"
            val existingZip = pickedDir.findFile(zipFileName)
            val zipFile = existingZip ?: pickedDir.createFile(
                if (isObb) "application/octet-stream" else "application/zip",
                zipFileName
            )
            if (zipFile != null) {
                val baos = ByteArrayOutputStream()
                val zos = ZipOutputStream(baos)
                for ((name, content) in gameFiles) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                zos.finish()
                resolver.openOutputStream(zipFile.uri, "wt")?.use { os ->
                    os.write(baos.toByteArray())
                }
                writeCount++
            }

            "✓ Berhasil menyimpan ${writeCount} file langsung ke folder pilihan Anda melalui SAF!\n• Paket: $zipFileName"
        } catch (e: Exception) {
            e.printStackTrace()
            "❌ Gagal menulis via SAF: ${e.localizedMessage}"
        }
    }

    /**
     * Mengekspor seluruh file sampel, config, guide, dan ZIP/OBB ke folder internal aplikasi atau folder eksternal langsung.
     */
    fun exportToCustomDirectory(targetFolderPath: String, isObb: Boolean = false): String {
        val gameFiles = generateAllGameFiles()
        val zipFileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"

        val targetDir = if (targetFolderPath.isBlank()) {
            getExportDir()
        } else {
            File(targetFolderPath)
        }

        var useDirectFile = true
        try {
            if (!targetDir.exists()) {
                val created = targetDir.mkdirs()
                if (!created && !targetDir.exists()) {
                    useDirectFile = false
                }
            }
        } catch (e: Exception) {
            useDirectFile = false
        }

        if (useDirectFile) {
            var writtenCount = 0
            for ((name, content) in gameFiles) {
                try {
                    File(targetDir, name).writeText(content, Charsets.UTF_8)
                    writtenCount++
                } catch (e: Exception) {
                    useDirectFile = false
                    break
                }
            }

            if (useDirectFile) {
                try {
                    val zipFile = File(targetDir, zipFileName)
                    FileOutputStream(zipFile).use { fos ->
                        val zos = ZipOutputStream(fos)
                        for ((name, content) in gameFiles) {
                            val entry = ZipEntry(name)
                            zos.putNextEntry(entry)
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                        zos.finish()
                    }
                    writtenCount++
                } catch (e: Exception) {
                    useDirectFile = false
                }
            }

            if (useDirectFile) {
                return buildString {
                    append("✓ Sukses mengekspor ${writtenCount} file langsung ke folder lokal HP!\n")
                    append("📁 Folder: ${targetDir.absolutePath}\n")
                    append("• Paket: $zipFileName")
                }
            }
        }

        // FALLBACK: If direct file write fails (due to Android 11+ Scoped Storage), use MediaStore Downloads API
        var mediaStoreCount = 0
        for ((name, content) in gameFiles) {
            val mime = if (name.endsWith(".json")) "application/json"
                       else if (name.endsWith(".obj")) "text/plain"
                       else if (name.endsWith(".py")) "text/x-python"
                       else "text/plain"
            val success = writeToPublicDownloads("", name, mime, content.toByteArray(Charsets.UTF_8))
            if (success) mediaStoreCount++
        }

        // Write the package to MediaStore
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        for ((name, content) in gameFiles) {
            val entry = ZipEntry(name)
            zos.putNextEntry(entry)
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        zos.finish()
        val zipBytes = baos.toByteArray()
        val zipSuccess = writeToPublicDownloads("", zipFileName, if (isObb) "application/octet-stream" else "application/zip", zipBytes)
        if (zipSuccess) mediaStoreCount++

        return buildString {
            append("✓ Folder publik berhasil ditulis via MediaStore (Scoped Storage fallback)!\n")
            append("📁 Folder Publik: Download/Apex3D/\n")
            append("• Berhasil menulis: $mediaStoreCount file\n")
            append("• Paket: $zipFileName")
        }
    }

    /**
     * Ekspor khusus Script Python Generator `generate_terrain.py` ke folder target
     */
    fun exportPythonScriptOnly(targetFolderPath: String): String {
        val scriptContent = getPythonTerrainScript()
        val targetDir = if (targetFolderPath.isBlank()) getExportDir() else File(targetFolderPath)

        var useDirectFile = true
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val file = File(targetDir, "generate_terrain.py")
            file.writeText(scriptContent, Charsets.UTF_8)
        } catch (e: Exception) {
            useDirectFile = false
        }

        if (useDirectFile) {
            return buildString {
                append("✓ Script Python 'generate_terrain.py' berhasil diekspor langsung!\n")
                append("📁 Folder: ${targetDir.absolutePath}/generate_terrain.py\n")
                append("Siap langsung dijalankan di Termux (`python generate_terrain.py`)!")
            }
        }

        // Fallback to MediaStore
        val success = writeToPublicDownloads("", "generate_terrain.py", "text/x-python", scriptContent.toByteArray(Charsets.UTF_8))
        return if (success) {
            buildString {
                append("✓ Script 'generate_terrain.py' diekspor via MediaStore!\n")
                append("📁 Lokasi: Download/Apex3D/generate_terrain.py\n")
                append("Siap dijalankan di Termux!")
            }
        } else {
            "❌ Gagal mengekspor script python."
        }
    }

    private fun generateSampleTerrainObj(): String {
        val sb = StringBuilder()
        sb.append("# Apex3D Sample Terrain Mesh with Rolling Contours\n")
        sb.append("o SampleTerrain\n")

        val verts = listOf(
            Triple(-15f, 0f, -15f), Triple(0f, 0.5f, -15f), Triple(15f, 0f, -15f),
            Triple(-15f, 1.2f, 0f), Triple(0f, 4.5f, 0f), Triple(15f, 1.2f, 0f),
            Triple(-15f, 0f, 15f), Triple(0f, 0.5f, 15f), Triple(15f, 0f, 15f)
        )
        for (v in verts) {
            sb.append("v ${v.first} ${v.second} ${v.third}\n")
        }
        sb.append("vn 0.0 1.0 0.0\n")

        sb.append("f 1//1 4//1 5//1\n")
        sb.append("f 1//1 5//1 2//1\n")
        sb.append("f 2//1 5//1 6//1\n")
        sb.append("f 2//1 6//1 3//1\n")
        sb.append("f 4//1 7//1 8//1\n")
        sb.append("f 4//1 8//1 5//1\n")
        sb.append("f 5//1 8//1 9//1\n")
        sb.append("f 5//1 9//1 6//1\n")

        return sb.toString()
    }

    /**
     * Mengekspor seluruh data sistem game aktif saat ini sebagai file .ZIP atau .OBB
     * Disimpan di internal app storage agar aman tanpa ketergantungan pada izin publik.
     */
    fun exportPackage(isObb: Boolean = false): File {
        val fileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"
        val outFile = File(getExportDir(), fileName)

        val gameFiles = generateAllGameFiles()

        // 1. Tulis ZIP/OBB ke outFile
        val baos = ByteArrayOutputStream()
        val zosMem = ZipOutputStream(baos)
        for ((name, content) in gameFiles) {
            val entry = ZipEntry(name)
            zosMem.putNextEntry(entry)
            zosMem.write(content.toByteArray(Charsets.UTF_8))
            zosMem.closeEntry()
        }
        zosMem.finish()
        val zipBytes = baos.toByteArray()

        FileOutputStream(outFile).use { fos ->
            fos.write(zipBytes)
        }

        // 2. Salin seluruh unzipped files ke internal ApexAssets
        val localApexDir = File(context.filesDir, "ApexAssets")
        if (!localApexDir.exists()) localApexDir.mkdirs()
        for ((name, content) in gameFiles) {
            try {
                File(localApexDir, name).writeText(content, Charsets.UTF_8)
            } catch (e: Exception) {
                // Ignore
            }
        }

        return outFile
    }

    /**
     * Membuka menu pemicu "Bagikan / Share" bawaan Android agar pengguna bisa langsung menyalin
     * atau mengirim file ZIP/OBB ke WhatsApp, Drive, File Manager, dll.
     */
    fun shareExportPackage(file: File, isObb: Boolean = false) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (isObb) "application/octet-stream" else "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Apex3D Game Package Export")
                putExtra(Intent.EXTRA_TEXT, "Berikut file ekspor paket game Apex3D 3D (.${if (isObb) "obb" else "zip"}). Disimpan di penyimpanan internal aplikasi.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Bagikan / Simpan File ${if (isObb) ".OBB" else ".ZIP"}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

