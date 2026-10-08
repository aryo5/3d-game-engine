package com.example.engine3d.importer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper untuk mengelola perizinan penyimpanan Android dari versi 7 (API 24)
 * hingga versi terbaru (Android 14/15/16 - API 34+).
 */
object StoragePermissionHelper {

    /**
     * Mengecek apakah izin runtime penyimpanan biasa telah diberikan.
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Mengecek apakah aplikasi memiliki akses penuh terhadap manajemen seluruh file (All Files Access)
     * yang diperlukan untuk Custom File Manager / Explorer di Android 11+.
     */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            isStoragePermissionGranted(context)
        }
    }

    /**
     * Mengarahkan pengguna ke pengaturan sistem untuk mengaktifkan izin "Akses Semua File".
     */
    fun launchAllFilesAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    /**
     * Menentukan apakah perangkat membutuhkan dialog izin runtime sebelum menulis ke filesystem publik langsung.
     */
    fun needsRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    }

    /**
     * Status deskripsi perizinan penyimpanan untuk ditampilkan di antarmuka pengguna.
     */
    fun getPermissionStatusDescription(context: Context): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (hasAllFilesAccess(context)) {
                    "Android 11+ (API ${Build.VERSION.SDK_INT}): Akses Manajemen Semua File Aktif! Penjelajah Folder Kustom dapat mengakses semua folder di HP Anda."
                } else {
                    "Android 11+ (API ${Build.VERSION.SDK_INT}): Memerlukan izin 'Akses Semua File' agar Penjelajah Folder Kustom bawaan aplikasi dapat membaca semua folder di luar folder Download/Apex3D secara bebas."
                }
            }
            isStoragePermissionGranted(context) ->
                "Android 7-10 (API ${Build.VERSION.SDK_INT}): Izin WRITE_EXTERNAL_STORAGE aktif. Penjelajah Folder Kustom dapat membaca semua folder publik!"
            else ->
                "Android 7-10 (API ${Build.VERSION.SDK_INT}): Memerlukan izin WRITE_EXTERNAL_STORAGE agar Penjelajah Folder Kustom dapat membaca berkas."
        }
    }
}
