# Perapihan Gameplay HUD (Mode Bersih) & Standardisasi Tab UI Menu

Rencana komprehensif untuk merampingkan tampilan kontrol saat gameplay berjalan (mengelompokkan aksi sekunder ke dalam menu lipat / *collapsible tray*) serta membakukan sistem tab terstruktur pada Pusat Impor Aset dan Pengaturan Grafis agar tidak ada teks terpotong atau scroll berlebih.

### User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan konfirmasi pada sesi tanya-jawab sebelumnya:
> - **Gameplay HUD**: Dipilih **Mode Bersih** di mana tombol aksi sekunder (*Dash, Slam, Crouch, Flashlight, Reset Pos, Camera Switch*) disatukan ke dalam **Menu Lipat Cepat / Collapsible Action Tray**, sehingga layar permainan tetap luas dan fokus pada tombol esensial (*Joystick, Look Pad, Serang, Lompat, Lari, Interaksi*).
> - **Menu Impor & Grafis**: Dipilih **Standardisasi Tab Terstruktur** serupa dengan Central Studio Hub, dengan pembagian tab mandiri, baris chip yang dapat digeser ke samping (*horizontal scroll*), dan pembatasan area scroll (*bounded height*) untuk mencegah teks terpotong atau ruang kosong berlebih.

---

### 1. Overview & Core Concept

- **Problem**: Layar permainan saat ini terlalu padat (*crowded*) karena menampilkan lebih dari 12 tombol virtual, monitor performa penuh, dan deretan tombol bilah atas secara bersamaan, menghalangi pandangan dunia 3D. Selain itu, menu Pusat Impor Aset masih berupa dialog panjang yang rentan mengalami teks keluar batas layar (*overflow*) dan scroll terlalu jauh.
- **Solution**:
  1. **Gameplay HUD**: Memisahkan kontrol menjadi **Kontrol Utama (Selalu Tampak)** dan **Menu Lipat Sekunder (Collapsible Quick Tray)**. Indikator performa FPS dirampingkan menjadi pill mini yang dapat diperluas jika ditekan.
  2. **Pusat Impor Aset (`AssetManagerSheet`)**: Menerapkan arsitektur 4 Tab Mandiri (*File & Storage*, *Daftar Model*, *Terrain & Lanskap*, *NPC & Interaksi*) dengan scroll bounded dan chip scrollable.
  3. **Pengaturan Grafis (`GraphicsSettingsSheet`)**: Mempercantik tata letak kartu preset FPS dan toggle pencahayaan/kabut agar serasi dengan tema Apex3D Dark Sci-Fi.

---

### 2. User Experience & Visual Design

#### A. Tampilan Gameplay (Mode Bersih)
```
┌────────────────────────────────────────────────────────────────────────┐
│ [⚙️ Studio]  [🗺️ Peta]  [FPS: 60▾]                    [🚪 Keluar Lobby]│  <- Top Bar Ramping
│                                                                        │
│                                                                        │
│        [+ Crosshair]                                                   │
│                                              ┌───────────────────────┐ │
│                                              │ ⚡ Menu Lipat (Buka)  │ │  <- Collapsible Action Tray
│                                              │ [Dash] [Slam] [Crouch]│ │
│                                              │ [Kamera] [Senter] [Pos│ │
│                                              └───────────────────────┘ │
│                                                                        │
│   (🕹️ Joystick Gerak)                                  (⚔️ SERANG)    │
│   [HP/Stamina Bar]                             (🏃 Lari)    (🦘 Lompat)│  <- Kluster Aksi Utama
│                                                (💬 Aksi Interaksi)     │
└────────────────────────────────────────────────────────────────────────┘
```

- **Kontrol Utama**:
  - Sisi Kiri: Joystick analog virtual & status HP/Stamina tipis di bawah joystick.
  - Sisi Kanan: Tombol utama Serang (*Attack/Slash*), Lompat (*Jump*), Kunci Lari (*Sprint Lock*), dan tombol Interaksi (*Bicara/Pintu*) yang hanya menyala saat ada objek dekat.
- **Menu Lipat Cepat (Collapsible Action Tray)**:
  - Tombol toggle berdesain neon amber/cyan `[⚡ Aksi Tambahan ▾]`.
  - Saat ditekan, membuka tray mengambang semi-transparan berisi tombol: *Dash*, *Ground Slam*, *Crouch*, *Ganti Sudut Kamera*, *Senter*, dan *Reset Posisi*.
  - Menutup otomatis setelah aksi ditekan atau saat pemain bergerak aktif, menjaga layar tetap bersih.
- **Top Bar Ringkas**:
  - Hanya menampilkan 3 tombol fungsional utama: `Studio Menu`, `Buka Peta Dunia`, dan `Keluar ke Lobby`.
  - Tombol kamera & senter dipindahkan ke tray aksi sekunder.
  - Monitor performa disederhanakan menjadi badge mini `60 FPS` di bagian tengah atas yang dapat di-tap untuk melihat rincian Draw Calls / Triangles.

#### B. Standardisasi Tab Pusat Impor Aset (`AssetManagerSheet`)
- **Tab 1: 📂 File & Impor**: Storage permission, pemilih folder in-app, impor file `.GLB`, `.OBJ`, `.ZIP`, `.OBB`, dan unduh aset contoh.
- **Tab 2: 📦 Manajemen Aset**: Daftar model kustom yang sudah terpasang dengan filter chip horizontal scrollable, indikator vertex, dan aksi hapus/pasang.
- **Tab 3: 🗺️ Peta & Terrain**: Pengaturan seed terrain, ketinggian bukit, dan penggantian mesh medan.
- **Tab 4: 👥 NPC & Rintangan**: Spawner NPC dialog, penempatan rintangan pembatas, dan trigger quest.

---

### 3. Key Product Decisions & Trade-Offs

1. **Menu Lipat Sekunder vs Menghapus Tombol**:
   - *Keputusan*: Mengelompokkan tombol aksi tambahan ke dalam menu lipat mengambang daripada menghapusnya sepenuhnya.
   - *Alasan*: Menjaga seluruh fungsionalitas game engine tetap ada tanpa membebani layar pengguna secara visual.
2. **Badge Performa Mini vs Full Monitor**:
   - *Keputusan*: Mengubah performance HUD menjadi pill ringkas `60 FPS · 16ms` dengan kemampuan expand-on-tap.
   - *Alasan*: Monitor grafis 6 baris sebelumnya menutupi 20% area atas layar; versi mini menjaga pandangan tetap leluasa saat menguji game.
3. **Pembagian 4 Tab Mandiri pada AssetManagerSheet**:
   - *Keputusan*: Memecah modal impor aset yang berukuran 1400 baris menjadi sistem tab modular dengan scrolling terisolasi.
   - *Alasan*: Mencegah scroll vertikal tanpa batas dan memastikan semua nama file serta opsi terlihat jelas di layar ponsel.

---

### 4. Technical Architecture & Implementation Steps

```
┌────────────────────────────────────────────────────────────────────────┐
│                            EngineScreen.kt                             │
│                                                                        │
│   ┌───────────────────┐  ┌──────────────────┐  ┌───────────────────┐   │
│   │   TopActionRow    │  │  CompactPerfPill │  │   StatusBarView   │   │
│   └───────────────────┘  └──────────────────┘  └───────────────────┘   │
│                                                                        │
│   ┌───────────────────┐                        ┌───────────────────┐   │
│   │ VirtualJoystick   │                        │ PrimaryActionGrid │   │
│   └───────────────────┘                        └───────────────────┘   │
│                                                ┌───────────────────┐   │
│                                                │ CollapsibleTray   │   │
│                                                │ (Dash/Slam/Cam...)│   │
│                                                └───────────────────┘   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
               ┌───────────────────┴───────────────────┐
               ▼                                       ▼
    ┌──────────────────────┐                ┌──────────────────────┐
    │ AssetManagerSheet.kt │                │GraphicsSettingsSheet │
    │ 4 Tab UI Bounded     │                │ Kartu Preset M3      │
    └──────────────────────┘                └──────────────────────┘
```

#### Langkah Pengerjaan:
1. **EngineScreen.kt**:
   - Tambahkan state `isSecondaryTrayExpanded` untuk mengontrol buka/tutup menu aksi sekunder.
   - Implementasikan komponen `CollapsibleActionTray` di atas kluster tombol aksi kanan.
   - Pindahkan tombol *ACTION_DASH*, *ACTION_SLAM*, *CROUCH*, *CAMERA_SWITCH*, *FLASHLIGHT*, dan *RESET_POS* ke dalam tray ini.
   - Sederhanakan tombol top bar dan buat `PerformanceHud` memiliki mode ringkas (mini pill).
2. **AssetManagerSheet.kt**:
   - Buat `AssetManagerTab` enum (`FILES`, `MODELS`, `TERRAIN`, `NPCS`).
   - Terapkan `TabRow` dengan indikator cyan di bawah header modal.
   - Pindahkan logika impor, daftar aset, konfigurasi medan, dan NPC ke tab masing-masing dengan wadah scroll terpisah (`weight(1f).verticalScroll`).
   - Pastikan seluruh baris chip menggunakan `horizontalScroll`.
3. **GraphicsSettingsSheet.kt**:
   - Pastikan tata letak kartu preset FPS dan tombol toggle responsif terhadap layar landscape/portrait.
4. **Verifikasi & Kompilasi**:
   - Jalankan `compile_applet` untuk memastikan seluruh perubahan terkompilasi bersih tanpa regresi.
