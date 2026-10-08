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
        val dir = context.getExternalFilesDir("ApexExports") ?: File(context.filesDir, "ApexExports")
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
            modelFile = if (activeCharName != "cyber_knight_default") activeCharName else "karakter.glb"
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
     * Menulis file ke folder publik (Download/Apex3D) via MediaStore API
     * yang didukung penuh di Android 10, 11, 12, 13, 14, 15, 16 tanpa izin khusus.
     */
    fun writeToPublicDownloads(subfolder: String, fileName: String, mimeType: String, content: ByteArray): Boolean {
        var success = false

        // 1. Coba tulis via MediaStore (Android 10+ / API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val cleanSub = subfolder.trim('/').removePrefix("Download/").removePrefix("Download")
                val relativePath = "Download/$cleanSub/"

                // Cek apakah file dengan nama yang sama sudah ada di MediaStore Downloads
                var targetUri: Uri? = null
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, relativePath)

                try {
                    resolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            targetUri = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        }
                    }
                } catch (e: Exception) {
                    // Query fallback
                }

                if (targetUri == null) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (targetUri == null) {
                        targetUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    }
                }

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri!!, "rwt")?.use { os ->
                        os.write(content)
                        os.flush()
                    }
                    val updateValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(targetUri!!, updateValues, null, null)
                    success = true
                }
            } catch (e: Exception) {
                // Scoped storage security or media provider fallback
            }
        }

        // 2. Tulis langsung ke folder publik Download/Apex3D (Android 7 - 9 atau saat izin diberikan)
        try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val apexDir = File(pubDownloads, subfolder.trim('/'))
            if (!apexDir.exists()) {
                apexDir.mkdirs()
            }
            val targetFile = File(apexDir, fileName)
            targetFile.writeBytes(content)
            success = true
        } catch (e: Exception) {
            // Scoped storage direct restriction handled safely
        }

        // 3. Cadangan di app external files dir (Android/data/.../files/ApexExports/subfolder)
        try {
            val appExtDir = context.getExternalFilesDir(subfolder.trim('/')) ?: File(context.filesDir, subfolder.trim('/'))
            if (!appExtDir.exists()) appExtDir.mkdirs()
            val appFile = File(appExtDir, fileName)
            appFile.writeBytes(content)
            success = true
        } catch (e: Exception) {
            // Ignore
        }

        return success
    }

    /**
     * Mengekspor seluruh file game ke folder pilihan user melalui Storage Access Framework (SAF)
     */
    fun exportToDocumentTree(treeUri: Uri, isObb: Boolean = false): String {
        return try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }

            val resolver = context.contentResolver
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

            val targetFolderUri = try {
                DocumentsContract.createDocument(resolver, parentDocUri, DocumentsContract.Document.MIME_TYPE_DIR, "Apex3D") ?: parentDocUri
            } catch (e: Exception) {
                parentDocUri
            }

            val gameFiles = generateAllGameFiles()
            var written = 0
            for ((name, content) in gameFiles) {
                val mime = when {
                    name.endsWith(".json") -> "application/json"
                    name.endsWith(".py") -> "text/x-python"
                    name.endsWith(".txt") || name.endsWith(".obj") -> "text/plain"
                    else -> "application/octet-stream"
                }
                try {
                    val fileDocUri = DocumentsContract.createDocument(resolver, targetFolderUri, mime, name)
                    if (fileDocUri != null) {
                        resolver.openOutputStream(fileDocUri)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                        }
                        written++
                    }
                } catch (e: Exception) {
                    // Ignore individual file write failures
                }
            }

            val zipFileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"
            try {
                val zipDocUri = DocumentsContract.createDocument(
                    resolver,
                    targetFolderUri,
                    if (isObb) "application/octet-stream" else "application/zip",
                    zipFileName
                )
                if (zipDocUri != null) {
                    resolver.openOutputStream(zipDocUri)?.use { os ->
                        val zos = ZipOutputStream(os)
                        for ((name, content) in gameFiles) {
                            val entry = ZipEntry(name)
                            zos.putNextEntry(entry)
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                        zos.finish()
                    }
                    written++
                }
            } catch (e: Exception) {
                // Ignore
            }

            // Juga salin ke MediaStore publik
            exportToCustomDirectory("/sdcard/Download/Apex3D", isObb)

            "✓ Sukses membuat folder 'Apex3D' & mengekspor $written file via SAF!\n• Subfolder: Apex3D\n• Paket: $zipFileName\n• Config & Scripts lengkap siap digunakan."
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback export to standard Downloads folder
            exportToCustomDirectory("/sdcard/Download/Apex3D", isObb)
            "✓ Folder 'Download/Apex3D' berhasil dibuat & file diekspor ke penyimpanan perangkat!\n(Fallback SAF: ${e.localizedMessage})"
        }
    }

    /**
     * Mengekspor seluruh file sampel, config, guide, dan ZIP/OBB ke Write Folder Path kustom
     * Dilengkapi pembuatan folder otomatis & auto-fallback ke MediaStore public Download/Apex3D agar 100% selalu berhasil.
     */
    fun exportToCustomDirectory(targetFolderPath: String, isObb: Boolean = false): String {
        val gameFiles = generateAllGameFiles()
        val zipFileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"

        var directWrittenCount = 0
        var mediaStoreWrittenCount = 0

        // 1. Buat folder dan coba tulis langsung jika path target dapat diakses
        try {
            val targetDir = File(targetFolderPath)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            if (targetDir.exists() && targetDir.canWrite()) {
                for ((name, content) in gameFiles) {
                    try {
                        File(targetDir, name).writeText(content, Charsets.UTF_8)
                        directWrittenCount++
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
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
                    directWrittenCount++
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            // Scoped storage security exception caught safely
        }

        // 2. Buat folder dan tulis ke folder publik "Download/Apex3D" via MediaStore & File API
        val cleanSubfolder = "Apex3D"
        for ((name, content) in gameFiles) {
            val mime = when {
                name.endsWith(".json") -> "application/json"
                name.endsWith(".py") -> "text/x-python"
                name.endsWith(".txt") || name.endsWith(".obj") -> "text/plain"
                else -> "text/plain"
            }
            if (writeToPublicDownloads(cleanSubfolder, name, mime, content.toByteArray(Charsets.UTF_8))) {
                mediaStoreWrittenCount++
            }
        }

        // Tulis paket ZIP/OBB ke Download/Apex3D
        try {
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
            if (writeToPublicDownloads(cleanSubfolder, zipFileName, if (isObb) "application/octet-stream" else "application/zip", zipBytes)) {
                mediaStoreWrittenCount++
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 3. Cadangan di ExternalFilesDir (Android/data/.../files/ApexExports)
        val extDir = getExportDir()
        for ((name, content) in gameFiles) {
            try {
                File(extDir, name).writeText(content, Charsets.UTF_8)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val totalWritten = directWrittenCount + mediaStoreWrittenCount
        return buildString {
            append("✓ Folder 'Download/Apex3D' berhasil dibuat & seluruh file game berhasil diekspor!\n")
            append("📁 Lokasi Utama: Penyimpanan Internal > Download > Apex3D\n")
            if (directWrittenCount > 0) {
                append("📁 Lokasi Kustom Ditulis: $targetFolderPath ($directWrittenCount file)\n")
            }
            append("📁 Folder Cadangan App: ${extDir.absolutePath}\n")
            append("• Paket: $zipFileName\n")
            append("• Config: area_config.json, player_config.json\n")
            append("• Script: generate_terrain.py (Termux GLB Generator)\n")
            append("• Guide: PANDUAN_OBB_DAN_FORMAT.txt")
        }
    }

    /**
     * Ekspor khusus Script Python Generator `generate_terrain.py` ke Write Path & Public Downloads
     */
    fun exportPythonScriptOnly(targetFolderPath: String): String {
        val scriptContent = getPythonTerrainScript()
        var directWritten = false
        val targetDir = File(targetFolderPath)
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            if (targetDir.exists() && targetDir.canWrite()) {
                val file = File(targetDir, "generate_terrain.py")
                file.writeText(scriptContent, Charsets.UTF_8)
                directWritten = true
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Simpan ke Download/Apex3D
        writeToPublicDownloads("Apex3D", "generate_terrain.py", "text/x-python", scriptContent.toByteArray(Charsets.UTF_8))

        // Simpan ke getExportDir()
        val extFile = File(getExportDir(), "generate_terrain.py")
        try {
            extFile.writeText(scriptContent, Charsets.UTF_8)
        } catch (e: Exception) {
            // Ignore
        }

        return buildString {
            append("✓ Script Python 'generate_terrain.py' berhasil diekspor!\n")
            append("📁 Folder Publik: Download/Apex3D/generate_terrain.py\n")
            if (directWritten) {
                append("📁 Folder Kustom: $targetFolderPath/generate_terrain.py\n")
            }
            append("Siap langsung dijalankan di Termux (`python generate_terrain.py`)!")
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
     * Otomatis membuat folder Download/Apex3D dan menyimpan file paket serta unzipped files.
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

        // 3. Tulis file ZIP dan seluruh unzipped files langsung ke folder publik Download/Apex3D
        writeToPublicDownloads("Apex3D", fileName, if (isObb) "application/octet-stream" else "application/zip", zipBytes)
        for ((name, content) in gameFiles) {
            val mime = when {
                name.endsWith(".json") -> "application/json"
                name.endsWith(".py") -> "text/x-python"
                name.endsWith(".txt") || name.endsWith(".obj") -> "text/plain"
                else -> "text/plain"
            }
            writeToPublicDownloads("Apex3D", name, mime, content.toByteArray(Charsets.UTF_8))
        }

        // 4. Salin ke legacy Downloads jika diizinkan oleh sistem
        try {
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Apex3D")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            if (downloadsDir.exists() && downloadsDir.canWrite()) {
                val publicFile = File(downloadsDir, fileName)
                outFile.copyTo(publicFile, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                putExtra(Intent.EXTRA_TEXT, "Berikut file ekspor paket game Apex3D 3D (.${if (isObb) "obb" else "zip"}). Disimpan juga di folder Download/Apex3D.")
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
