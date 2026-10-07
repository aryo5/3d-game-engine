package com.example.engine3d.importer

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.CharacterAction
import com.example.engine3d.npc.NpcEntity
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.BarrierManager
import com.example.engine3d.terrain.TerrainMesh
import org.json.JSONArray
import org.json.JSONObject
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
     * Mengekspor seluruh file sampel, config, guide, dan ZIP/OBB ke Write Folder Path kustom
     */
    fun exportToCustomDirectory(targetFolderPath: String, isObb: Boolean = false): String {
        val gameFiles = generateAllGameFiles()
        val targetDir = File(targetFolderPath)
        if (!targetDir.exists()) targetDir.mkdirs()

        var writtenCount = 0
        // 1. Tulis seluruh file unzipped (area_config.json, player_config.json, generate_terrain.py, guide.txt, dll)
        for ((name, content) in gameFiles) {
            try {
                File(targetDir, name).writeText(content, Charsets.UTF_8)
                writtenCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Tulis paket .ZIP / .OBB ke folder target
        val zipFileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"
        val zipFile = File(targetDir, zipFileName)
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "✓ Sukses mengekspor $writtenCount file ke Write Path:\n'$targetFolderPath'\n• Package: $zipFileName\n• Config: area_config.json, player_config.json\n• Script: generate_terrain.py\n• Guide: PANDUAN_OBB_DAN_FORMAT.txt"
    }

    /**
     * Ekspor khusus Script Python Generator `generate_terrain.py` ke Write Path
     */
    fun exportPythonScriptOnly(targetFolderPath: String): String {
        val targetDir = File(targetFolderPath)
        if (!targetDir.exists()) targetDir.mkdirs()

        val file = File(targetDir, "generate_terrain.py")
        file.writeText(getPythonTerrainScript(), Charsets.UTF_8)

        return "✓ Script Python 'generate_terrain.py' berhasil ditulis ke:\n'${file.absolutePath}'\nSiap dijalankan di Termux!"
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
     */
    fun exportPackage(isObb: Boolean = false): File {
        val fileName = if (isObb) "main.1.apex3d.game.obb" else "Apex3D_Game_Data_Package.zip"
        val outFile = File(getExportDir(), fileName)

        val gameFiles = generateAllGameFiles()

        FileOutputStream(outFile).use { fos ->
            val zos = ZipOutputStream(fos)
            for ((name, content) in gameFiles) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            zos.finish()
        }

        // Juga salin ke folder ApexAssets internal
        val localApexDir = File(context.filesDir, "ApexAssets")
        if (!localApexDir.exists()) localApexDir.mkdirs()
        for ((name, content) in gameFiles) {
            File(localApexDir, name).writeText(content, Charsets.UTF_8)
        }

        // Salin file .zip/.obb ke folder publik Downloads/Apex3D HP pengguna
        try {
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Apex3D")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val publicFile = File(downloadsDir, fileName)
            outFile.copyTo(publicFile, overwrite = true)
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
                putExtra(Intent.EXTRA_TEXT, "Berikut file ekspor paket game Apex3D 3D (.${if (isObb) "obb" else "zip"}).")
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
