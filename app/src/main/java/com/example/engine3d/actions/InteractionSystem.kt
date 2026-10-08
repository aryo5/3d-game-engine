package com.example.engine3d.actions

import com.example.engine3d.math.Vec3

enum class InteractableType {
    POWER_BEACON,
    SUPPLY_CRATE,
    HOVERBOARD,
    QUANTUM_PORTAL,
    TERMINAL,
    HOUSE_DOOR_ENTER,
    HOUSE_DOOR_EXIT
}

data class WorldInteractable(
    val id: String,
    val name: String,
    val type: InteractableType,
    val position: Vec3,
    val targetTeleportPos: Vec3? = null,
    val interactionRadius: Float = 3.0f,
    var isActivated: Boolean = false,
    val promptText: String = "Tekan Interaksi",
    var meshFileName: String? = null,
    var visualScale: Float = 1.0f,
    var rotationY: Float = 0f,
    var visualOffset: Vec3 = Vec3(0f, 0f, 0f)
)

class InteractionSystem {
    val interactables = mutableListOf<WorldInteractable>()
    var currentNearbyObject: WorldInteractable? = null
    var statusMessage: String? = null
    var statusMessageTimer: Float = 0f

    // Special states
    var isHoverboardMounted: Boolean = false

    init {
        // Spawn world objects
        interactables.add(
            WorldInteractable(
                id = "beacon_1",
                name = "Ancient Energy Beacon",
                type = InteractableType.POWER_BEACON,
                position = Vec3(0f, 0f, 0f),
                promptText = "Aktifkan Sinar Beacon"
            )
        )
        interactables.add(
            WorldInteractable(
                id = "crate_1",
                name = "Supply Cache",
                type = InteractableType.SUPPLY_CRATE,
                position = Vec3(8f, 0f, 5f),
                promptText = "Buka Kotak Suplai"
            )
        )
        interactables.add(
            WorldInteractable(
                id = "hoverboard_1",
                name = "Cyber Hoverboard",
                type = InteractableType.HOVERBOARD,
                position = Vec3(-6f, 0f, -8f),
                promptText = "Kendarai Hoverboard"
            )
        )
        interactables.add(
            WorldInteractable(
                id = "portal_1",
                name = "Quantum Teleporter",
                type = InteractableType.QUANTUM_PORTAL,
                position = Vec3(18f, 0f, -15f),
                promptText = "Teleportasi ke Puncak Gunung"
            )
        )
    }

    fun updateProximity(playerPos: Vec3, dt: Float) {
        if (statusMessageTimer > 0f) {
            statusMessageTimer -= dt
            if (statusMessageTimer <= 0f) {
                statusMessage = null
            }
        }

        var closest: WorldInteractable? = null
        var closestDist = Float.MAX_VALUE

        for (item in interactables) {
            val dx = item.position.x - playerPos.x
            val dz = item.position.z - playerPos.z
            val dist = kotlin.math.sqrt(dx * dx + dz * dz)

            if (dist <= item.interactionRadius && dist < closestDist) {
                closest = item
                closestDist = dist
            }
        }

        currentNearbyObject = closest
    }

    fun interact(playerPos: Vec3): String {
        val target = currentNearbyObject ?: return "Tidak ada objek interaksi di dekatmu"
        target.isActivated = !target.isActivated

        val message = when (target.type) {
            InteractableType.POWER_BEACON -> {
                if (target.isActivated) "⚡ Beacon diaktifkan! Sinar energi menerangi langit." else "Beacon dinonaktifkan."
            }
            InteractableType.SUPPLY_CRATE -> {
                "📦 Membuka Kotak Suplai! Memperoleh kristal plasma."
            }
            InteractableType.HOVERBOARD -> {
                isHoverboardMounted = !isHoverboardMounted
                if (isHoverboardMounted) "🛹 Menaiki Cyber Hoverboard! Kecepatan gerak meningkat pesat." else "Turun dari Hoverboard."
            }
            InteractableType.QUANTUM_PORTAL -> {
                playerPos.set(0f, 15f, 25f)
                "🌀 Teleportasi sukses ke puncak lereng!"
            }
            InteractableType.TERMINAL -> {
                "💻 Terminal akses sistem game engine terbuka."
            }
            InteractableType.HOUSE_DOOR_ENTER -> {
                val targetPos = target.targetTeleportPos ?: Vec3(200f, 1.5f, 200f)
                playerPos.set(targetPos.x, targetPos.y, targetPos.z)
                "🏠 [Buka Pintu] Berhasil masuk ke dalam area rumah GLB!"
            }
            InteractableType.HOUSE_DOOR_EXIT -> {
                val targetPos = target.targetTeleportPos ?: Vec3(106f, 15f, 108f)
                playerPos.set(targetPos.x, targetPos.y, targetPos.z)
                "🚪 [Buka Pintu] Berhasil keluar kembali ke area luar pedesaan!"
            }
        }

        statusMessage = message
        statusMessageTimer = 3.5f
        return message
    }
}
