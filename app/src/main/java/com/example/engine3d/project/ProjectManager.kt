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

    fun setActive(project: ProjectInfo) {
        activeProject = project
        onProjectActivatedListener?.invoke(project)
    }

    // Mendapatkan file script utama dari proyek aktif
    fun getActiveScriptFile(): File? {
        val p = activeProject ?: return null
        val scriptsDir = File(p.folder, "scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()
        return File(scriptsDir, "player_controller.lua")
    }

    // Membaca isi script Lua langsung dari folder Documents
    fun readActiveScript(): String {
        val file = getActiveScriptFile()
        return if (file != null && file.exists()) {
            file.readText()
        } else {
            """-- Controller Proyek (Documents)
function _ready()
    print("[Engine] Proyek aktif dimuat dari Documents!")
    player:set_position(0.0, 0.0, 0.0)
end

function _process(delta)
    -- Putar karakter perlahan
    player:rotate_y(15.0 * delta)
end
""".trimIndent()
        }
    }

    // Menyimpan perubahan script langsung ke file fisik di Documents
    fun saveActiveScript(code: String): Boolean {
        val file = getActiveScriptFile() ?: return false
        return try {
            file.writeText(code)
            true
        } catch (_: Exception) {
            false
        }
    }

    // Mendapatkan daftar file model 3D di folder assets proyek
    fun getActiveProjectModels(): List<File> {
        val p = activeProject ?: return emptyList()
        val assetsDir = File(p.folder, "assets")
        if (!assetsDir.exists()) return emptyList()
        return assetsDir.listFiles()?.filter {
            it.name.endsWith(".glb", true) || it.name.endsWith(".obj", true)
        } ?: emptyList()
    }

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

            val modelCount = assetsDir.listFiles()?.count { it.name.endsWith(".glb", true) || it.name.endsWith(".obj", true) } ?: 0
            val scriptCount = scriptsDir.listFiles()?.count { it.name.endsWith(".lua", true) } ?: 0

            val pInfo = ProjectInfo(projName, dir, dir.lastModified(), modelCount, scriptCount, desc)
            list.add(pInfo)

            // Jadikan proyek pertama aktif jika belum ada yang aktif
            if (activeProject == null && dir.name == "Proyek_Harvest_3D") {
                activeProject = pInfo
            }
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
            put("main_scene", "main.scene")
            put("engine", "Godot/Apex3D")
        }
        File(dir, "project.json").writeText(json.toString(2))

        val sampleScript = """-- Controller Karakter Proyek: $name
-- Lokasi: Documents/Apex3D_Projects/$cleanName/scripts/player_controller.lua

function _ready()
    print("[$name] Karakter siap! Dimuat langsung dari folder Documents.")
    player:set_position(0.0, 0.0, 0.0)
end

function _process(delta)
    -- Pergerakan / rotasi per frame
    player:rotate_y(20.0 * delta)
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
