package com.glycin.springaria.gameplay

import org.springframework.stereotype.Component

enum class HotbarItem(val label: String, val isWeapon: Boolean = false) {
    PICKAXE("Deleting Mode"),
    HAMMER("Building Mode"),
    UZI("Fast Iteration Gun", isWeapon = true),
    SOAKER("Extensibility Soaker", isWeapon = true),
    BAZOOKA("Garbage Bazooka", isWeapon = true),
}

private const val LABEL_DISPLAY_MS = 1500L

@Component
class Hotbar {

    val items = HotbarItem.entries.toTypedArray()
    var selectedIndex: Int = 0
    var switchTime: Long = 0L

    val selectedItem: HotbarItem get() = items[selectedIndex]

    val switchLabel: String?
        get() = if (System.currentTimeMillis() - switchTime < LABEL_DISPLAY_MS) selectedItem.label else null

    fun select(index: Int) {
        if (index in items.indices && index != selectedIndex) {
            selectedIndex = index
            switchTime = System.currentTimeMillis()
        }
    }
}
