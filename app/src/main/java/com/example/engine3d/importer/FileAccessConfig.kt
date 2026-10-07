package com.example.engine3d.importer

import android.content.Context
import android.content.SharedPreferences

class FileAccessConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex3d_file_access", Context.MODE_PRIVATE)

    var focusFolderPath: String
        get() = prefs.getString("focus_folder_path", "/sdcard/Download/Apex3D") ?: "/sdcard/Download/Apex3D"
        set(value) {
            prefs.edit().putString("focus_folder_path", value).apply()
        }

    var writeFolderPath: String
        get() = prefs.getString("write_folder_path", "/sdcard/Download/Apex3D") ?: "/sdcard/Download/Apex3D"
        set(value) {
            prefs.edit().putString("write_folder_path", value).apply()
        }

    var lastScanSummary: String
        get() = prefs.getString("last_scan_summary", "Belum ada pemindaian") ?: "Belum ada pemindaian"
        set(value) {
            prefs.edit().putString("last_scan_summary", value).apply()
        }

    var lastWriteSummary: String
        get() = prefs.getString("last_write_summary", "Belum ada ekspor") ?: "Belum ada ekspor"
        set(value) {
            prefs.edit().putString("last_write_summary", value).apply()
        }
}
