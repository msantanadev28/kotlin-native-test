@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.windows.*

private const val DWMWA_SYSTEMBACKDROP_TYPE = 38u
private const val DWMSBT_MAINWINDOW = 2
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20u

fun rgb(r: Int, g: Int, b: Int): UInt = (r or (g shl 8) or (b shl 16)).toUInt()

fun isDarkModeActive(): Boolean = memScoped {
    val keyPath = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    val valueName = "AppsUseLightTheme"
    val hKey = alloc<HKEYVar>()
    val value = alloc<UIntVar>()
    value.value = 1u
    val size = alloc<UIntVar>()
    size.value = sizeOf<UIntVar>().convert()

    if (RegOpenKeyExW(HKEY_CURRENT_USER, keyPath, 0u, KEY_READ.toUInt(), hKey.ptr) == ERROR_SUCCESS) {
        RegQueryValueExW(
            hKey.value,
            valueName,
            null,
            null,
            value.ptr.reinterpret(),
            size.ptr
        )
        RegCloseKey(hKey.value)
    }

    value.value == 0u
}

fun setBackdrop(hwnd: HWND?) = memScoped {
    val backdrop = alloc<IntVar>()
    backdrop.value = DWMSBT_MAINWINDOW
    DwmSetWindowAttribute(
        hwnd,
        DWMWA_SYSTEMBACKDROP_TYPE,
        backdrop.ptr,
        sizeOf<IntVar>().convert()
    )

    val dark = alloc<BOOLVar>()
    dark.value = if (isDarkModeActive()) 1 else 0
    DwmSetWindowAttribute(
        hwnd,
        DWMWA_USE_IMMERSIVE_DARK_MODE,
        dark.ptr,
        sizeOf<BOOLVar>().convert()
    )

    val margins = alloc<MARGINS>()
    margins.cxLeftWidth = -1
    margins.cxRightWidth = -1
    margins.cyTopHeight = -1
    margins.cyBottomHeight = -1
    DwmExtendFrameIntoClientArea(hwnd, margins.ptr)
}
