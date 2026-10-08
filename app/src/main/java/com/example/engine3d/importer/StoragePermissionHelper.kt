package com.example.engine3d.importer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Helper untuk mengelola perizinan penyimpanan Android dari versi 7 (API 24)
 * hingga versi terbaru (Android 14/15/16 - API 34+).
 *
 * Kebijakan Android:
 * - Android 7 s/d Android 9 (API 24 - 28) & Android 10 (API 29 legacy):
 *   Membutuhkan izin runtime berbahaya `WRITE_EXTERNAL_STORAGE` untuk menulis ke direktori publik (/sdcard/Download).
 * - Android 10+ (API 29+):
 *   Mendukung Scoped Storage & MediaStore.Downloads API sehingga penulisan ke folder publik
 *   Download/Apex3D dapat dilakukan langsung tanpa memerlukan izin berbahaya.
 * - Storage Access Framework (SAF / DocumentTree) didukung di seluruh versi Android
 *   tanpa memerlukan izin WRITE_EXTERNAL_STORAGE.
 */
object StoragePermissionHelper {

    /**
     * Mengecek apakah izin runtime penyimpanan telah diberikan pada perangkat saat ini.
     * Mengembalikan true jika izin sudah diberikan ATAU jika perangkat berjalan di Android 11+
     * di mana penulisan MediaStore tidak membutuhkan izin berbahaya.
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11+ menggunakan Scoped Storage (MediaStore & SAF), tidak memerlukan WRITE_EXTERNAL_STORAGE
            true
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                "Android 11+ (API ${Build.VERSION.SDK_INT}): Menggunakan Scoped Storage & MediaStore. Ekspor ke folder publik (Download/Apex3D) otomatis aktif tanpa perlu izin manual!"
            isStoragePermissionGranted(context) ->
                "Android 7-10 (API ${Build.VERSION.SDK_INT}): Izin WRITE_EXTERNAL_STORAGE aktif. Ekspor langsung ke /sdcard/Download/Apex3D diizinkan!"
            else ->
                "Android 7-10 (API ${Build.VERSION.SDK_INT}): Memerlukan izin WRITE_EXTERNAL_STORAGE agar ekspor dapat menulis langsung ke folder Download publik."
        }
    }
}

