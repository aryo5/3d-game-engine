package com.example.engine3d.project

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

data class ProjectInfo(
    val name: String,
    val folder: File,
    val lastModified: Long,
    val modelCount: Int,
    val scriptCount: Int,
    val description: String = "Proyek Apex3D Engine"
)

object ProjectManager {
    // Mendapatkan folder induk di Documents/Apex3D_Projects
    fun getRootDirectory(context: Context): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val root = File(docs, "Apex3D_Projects")
        if (!root.exists()) {
            root.mkdirs()
        }
        return if (root.canWrite()) root else File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Apex3D_Projects").apply { mkdirs() }
    }

    var activeProject: ProjectInfo? = null

    // Membaca semua proyek yang ada di folder Documents
    fun listProjects(context: Context): List<ProjectInfo> {
        val root = getRootDirectory(context)
        ensureDefaultProject(context, root)

        val list = mutableListOf<ProjectInfo>()
        root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val projFile = File(dir, "project.json")
            var desc = "Proyek Game Godot"
            var projName = dir.name
            if (projFile.exists()) {
                try {
                    val json = JSONObject(projFile.readText())
                    projName = json.optString("name", dir.name)
                    desc = json.optString("description", desc)
                } catch (_: Exception) {}
            }

            val assetsDir = File(dir, "assets")
            val scriptsDir = File(dir, "scripts")

            val modelCount = assetsDir.listFiles()?.count { it.name.endsWith(".glb") || it.name.endsWith(".obj") } ?: 0
            val scriptCount = scriptsDir.listFiles()?.count { it.name.endsWith(".lua") } ?: 0

            list.add(ProjectInfo(projName, dir, dir.lastModified(), modelCount, scriptCount, desc))
        }
        return list.sortedByDescending { it.lastModified }
    }

    // Pastikan proyek template Harvest Moon selalu ada di awal
    fun ensureDefaultProject(context: Context, root: File) {
        val defaultDir = File(root, "Proyek_Harvest_3D")
        if (!defaultDir.exists()) {
            createProject(context, "Proyek_Harvest_3D", "Template Proyek Petani Harvest Moon 3D")
        }
    }

    // Membuat proyek baru beserta struktur folder lengkap
    fun createProject(context: Context, name: String, description: String = "Proyek Baru"): Boolean {
        val root = getRootDirectory(context)
        val cleanName = name.trim().replace(" ", "_")
        val dir = File(root, cleanName)
        if (dir.exists()) return false

        dir.mkdirs()
        val assetsDir = File(dir, "assets").apply { mkdirs() }
        val scriptsDir = File(dir, "scripts").apply { mkdirs() }
        File(dir, "scenes").apply { mkdirs() }

        // Tulis project.json
        val json = JSONObject().apply {
            put("name", name)
            put("version", "1.0.0")
            put("description", description)
            put("main_scene", "main.scene")
            put("engine", "Godot/Apex3D")
        }
        File(dir, "project.json").writeText(json.toString(2))

        // Contoh script awal Lua di dalam folder scripts/ proyek tersebut
        val sampleScript = """-- Controller Proyek $name (Tersimpan di Documents)
function _ready()
    print("[$name] Siap! Dimuat dari folder Documents HP.")
    player:set_position(0.0, 0.0, 0.0)
end

function _process(delta)
    -- Logika frame game
end
""".trimIndent()
        File(scriptsDir, "player_controller.lua").writeText(sampleScript)

        // Salin model glb default ke folder assets proyek baru jika ada
        try {
            context.assets.open("farmer_harvest_moon.glb").use { input ->
                FileOutputStream(File(assetsDir, "farmer_harvest_moon.glb")).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}

        return true
    }

    fun deleteProject(folder: File): Boolean = folder.deleteRecursively()

    fun renameProject(folder: File, newName: String): Boolean {
        val jsonFile = File(folder, "project.json")
        if (jsonFile.exists()) {
            try {
                val json = JSONObject(jsonFile.readText())
                json.put("name", newName)
                jsonFile.writeText(json.toString(2))
                return true
            } catch (_: Exception) {}
        }
        return false
    }
}
