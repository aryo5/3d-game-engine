package com.example.engine3d.controller

enum class ExpandedContainerType(val displayName: String) {
    RADIAL_WHEEL("Roda Melingkar (Radial Wheel)"),
    HORIZONTAL_BAR("Bilah Horizontal (Baris)"),
    VERTICAL_GRID("Kotak Grid Popup"),
    POPUP_CARD("Kartu Menu Melayang")
}

enum class ExpandedContentType(val displayName: String) {
    ACTIONS_PALETTE("Palet Jurus Aksi (Slash, Slam, Dash, Leap)"),
    QUICK_GADGETS("Peralatan Cepat (Senter, Hoverboard, Reset, Teleport)"),
    STANCE_SELECTOR("Pilihan Postur (Berdiri, Jongkok, Meluncur, TPP/FPP)"),
    CUSTOM_ITEMS("Slot Bebas Kostum")
}

data class ExpandedControlItem(
    val id: String,
    val label: String,
    val iconName: String,
    val actionId: String? = null
)

data class ExpandedContainerConfig(
    var containerType: ExpandedContainerType = ExpandedContainerType.RADIAL_WHEEL,
    var contentType: ExpandedContentType = ExpandedContentType.ACTIONS_PALETTE,
    var itemScale: Float = 1.0f,
    var containerRadiusDp: Float = 95.0f,
    var alpha: Float = 0.90f,
    var autoCloseOnSelect: Boolean = true
) {
    fun copy(): ExpandedContainerConfig = ExpandedContainerConfig(
        containerType, contentType, itemScale, containerRadiusDp, alpha, autoCloseOnSelect
    )
}
