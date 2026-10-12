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
    fun getRootDirectory(context: Context): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val root = File(docs, "Apex3D_Projects")
        if (!root.exists()) {
            root.mkdirs()
        }
        return if (root.canWrite()) root else File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Apex3D_Projects").apply { mkdirs() }
    }

    var activeProject: ProjectInfo? = null
    var onProjectActivatedListener: ((ProjectInfo) -> Unit)? = null

    // Simpan status proyek aktif secara permanen ke file active_project.txt
    fun setActive(context: Context, project: ProjectInfo) {
        activeProject = project
        try {
            val configFile = File(getRootDirectory(context), "active_project.txt")
            configFile.writeText(project.folder.name)
        } catch (_: Exception) {}
        onProjectActivatedListener?.invoke(project)
    }

    // Mengambil file script aktif dari folder proyek
    fun getActiveScriptFile(context: Context? = null): File? {
        val root = if (context != null) getRootDirectory(context) else null
        val p = activeProject
        val targetFolder = p?.folder ?: if (root != null) File(root, "Proyek_Harvest_3D") else null
        if (targetFolder == null) return null

        val scriptsDir = File(targetFolder, "scripts").apply { if (!exists()) mkdirs() }
        return File(scriptsDir, "player_controller.lua")
    }

    fun readActiveScript(context: Context? = null): String {
        val file = getActiveScriptFile(context)
        return if (file != null && file.exists()) {
            file.readText()
        } else {
            """-- Controller Proyek Aktif
function _ready()
    print("[Engine] Script proyek aktif dimulai!")
end

function _process(delta)
    -- Logika frame loop 60 FPS
end
""".trimIndent()
        }
    }

    fun saveActiveScript(code: String, context: Context? = null): Boolean {
        val file = getActiveScriptFile(context) ?: return false
        return try {
            file.writeText(code)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun listProjects(context: Context): List<ProjectInfo> {
        val root = getRootDirectory(context)
        ensureDefaultProject(context, root)

        // Baca file active_project.txt jika ada
        val savedActiveName = try {
            File(root, "active_project.txt").takeIf { it.exists() }?.readText()?.trim()
        } catch (_: Exception) { null }

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

            val modelCount = assetsDir.listFiles()?.count { it.name.endsWith(".glb", true) || it.name.endsWith(".obj", true) } ?: 0
            val scriptCount = scriptsDir.listFiles()?.count { it.name.endsWith(".lua", true) } ?: 0

            val pInfo = ProjectInfo(projName, dir, dir.lastModified(), modelCount, scriptCount, desc)
            list.add(pInfo)

            if (savedActiveName != null && dir.name == savedActiveName) {
                activeProject = pInfo
            }
        }

        if (activeProject == null && list.isNotEmpty()) {
            activeProject = list.first()
        }

        return list.sortedByDescending { it.lastModified }
    }

    fun ensureDefaultProject(context: Context, root: File) {
        val defaultDir = File(root, "Proyek_Harvest_3D")
        if (!defaultDir.exists()) {
            createProject(context, "Proyek_Harvest_3D", "Template Proyek Petani Harvest Moon 3D")
        }
    }

    fun createProject(context: Context, name: String, description: String = "Proyek Baru"): Boolean {
        val root = getRootDirectory(context)
        val cleanName = name.trim().replace(" ", "_")
        val dir = File(root, cleanName)
        if (dir.exists()) return false

        dir.mkdirs()
        val assetsDir = File(dir, "assets").apply { mkdirs() }
        val scriptsDir = File(dir, "scripts").apply { mkdirs() }
        File(dir, "scenes").apply { mkdirs() }

        val json = JSONObject().apply {
            put("name", name)
            put("version", "1.0.0")
            put("description", description)
            put("engine", "Godot/Apex3D")
        }
        File(dir, "project.json").writeText(json.toString(2))

        val sampleScript = """-- Controller Karakter: $name
-- Lokasi: Documents/Apex3D_Projects/$cleanName/scripts/player_controller.lua

local timer = 0.0

function _ready()
    print("[$name] Siap! Dijalankan dalam frame loop 60 FPS.")
end

function _process(delta)
    -- 1. Berputar otomatis perlahan
    player:rotate_y(25.0 * delta)

    -- 2. Lompat otomatis tiap 2 detik
    timer = timer + delta
    if timer >= 2.0 then
        player:jump()
        timer = 0.0
    end
end
""".trimIndent()
        File(scriptsDir, "player_controller.lua").writeText(sampleScript)

        try {
            context.assets.open("farmer_harvest_moon.glb").use { input ->
                FileOutputStream(File(assetsDir, "farmer_harvest_moon.glb")).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}

        return true
    }

    fun deleteProject(folder: File): Boolean {
        if (activeProject?.folder?.path == folder.path) {
            activeProject = null
        }
        return folder.deleteRecursively()
    }
}
