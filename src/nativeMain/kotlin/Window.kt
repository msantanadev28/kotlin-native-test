@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.windows.CS_HREDRAW
import platform.windows.CS_VREDRAW
import platform.windows.CW_USEDEFAULT
import platform.windows.CreateWindowExA
import platform.windows.DefWindowProcA
import platform.windows.DispatchMessageA
import platform.windows.GetLastError
import platform.windows.GetMessageA
import platform.windows.GetModuleHandleA
import platform.windows.HMODULE
import platform.windows.HWND
import platform.windows.IDC_ARROW
import platform.windows.InvalidateRect
import platform.windows.LPARAM
import platform.windows.LoadCursorW
import platform.windows.LRESULT
import platform.windows.MSG
import platform.windows.PostQuitMessage
import platform.windows.RegisterClassExA
import platform.windows.SW_SHOWDEFAULT
import platform.windows.ShowWindow
import platform.windows.TranslateMessage
import platform.windows.UINT
import platform.windows.UpdateWindow
import platform.windows.WM_DESTROY
import platform.windows.WM_PAINT
import platform.windows.WM_SIZE
import platform.windows.WNDCLASSEXA
import platform.windows.WPARAM
import platform.windows.WS_OVERLAPPEDWINDOW
import platform.windows.WS_VISIBLE
import platform.windows.*

private const val DWMWA_SYSTEMBACKDROP_TYPE = 38u
private const val DWMSBT_MAINWINDOW = 2
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20u

private const val windowClassName = "SkiaCircleSampleWindow"
private const val windowTitle = "Kotlin Native Skia Sample"
private const val initialWindowWidth = 720
private const val initialWindowHeight = 540

private var activeRenderer: SkiaRenderer? = null
private var activeWindowHandle: HWND? = null

@OptIn(ExperimentalForeignApi::class)
private fun isDarkModeActive(): Boolean = memScoped {
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

@OptIn(ExperimentalForeignApi::class)
private fun setBackdrop(hwnd: HWND?) = memScoped {
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

@OptIn(ExperimentalForeignApi::class)
class Window(private val renderer: SkiaRenderer) {
    fun run() {
        activeRenderer = renderer

        memScoped {
            val instance = GetModuleHandleA(null) ?: error("GetModuleHandleA failed")
            val windowClassNamePointer = windowClassName.cstr.getPointer(this)

            val windowClassDefinition = alloc<WNDCLASSEXA>()
            windowClassDefinition.cbSize = sizeOf<WNDCLASSEXA>().toUInt()
            windowClassDefinition.style = (CS_HREDRAW or CS_VREDRAW).toUInt()
            windowClassDefinition.lpfnWndProc = staticCFunction(::windowProc)
            windowClassDefinition.hInstance = instance
            windowClassDefinition.hCursor = LoadCursorW(null, IDC_ARROW)
            windowClassDefinition.hbrBackground = null
            windowClassDefinition.lpszClassName = windowClassNamePointer

            check(RegisterClassExA(windowClassDefinition.ptr) != 0.toUShort()) {
                "RegisterClassExA failed with ${GetLastError()}"
            }

            val windowHandle = CreateWindowExA(
                0u,
                windowClassName,
                windowTitle,
                (WS_OVERLAPPEDWINDOW or WS_VISIBLE).toUInt(),
                CW_USEDEFAULT,
                CW_USEDEFAULT,
                initialWindowWidth,
                initialWindowHeight,
                null,
                null,
                instance,
                null,
            ) ?: error("CreateWindowExA failed with ${GetLastError()}")

            activeWindowHandle = windowHandle

            // Hook requestRedraw for the root widget to trigger repaint
            lienzo.runtime.onGlobalRequestRedraw = {
                InvalidateRect(windowHandle, null, 0)
            }

            val root = renderer.rootWidget as? lienzo.runtime.widgets.WindowWidget
            if (root?.theme == "mica") {
                setBackdrop(windowHandle)
            }

            ShowWindow(windowHandle, SW_SHOWDEFAULT)
            UpdateWindow(windowHandle)

            val message = alloc<MSG>()
            while (GetMessageA(message.ptr, null, 0u, 0u) > 0) {
                TranslateMessage(message.ptr)
                DispatchMessageA(message.ptr)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun windowProc(windowHandle: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    val renderer = activeRenderer ?: return DefWindowProcA(windowHandle, message, wParam, lParam)
    when (message.toInt()) {
        platform.windows.WM_ERASEBKGND -> {
            return 1
        }

        platform.windows.WM_SETTINGCHANGE -> {
            val root = renderer.rootWidget as? lienzo.runtime.widgets.WindowWidget
            if (root?.theme == "mica") {
                val dark = if (isDarkModeActive()) 1 else 0
                memScoped {
                    val value = alloc<BOOLVar>()
                    value.value = dark
                    DwmSetWindowAttribute(
                        windowHandle,
                        DWMWA_USE_IMMERSIVE_DARK_MODE,
                        value.ptr,
                        sizeOf<BOOLVar>().convert()
                    )
                }
                InvalidateRect(windowHandle, null, 1)
            }
            return 0
        }

        WM_SIZE -> {
            renderer.resize(windowHandle)
            InvalidateRect(windowHandle, null, 0)
            return 0
        }

        WM_PAINT -> {
            renderer.paint(windowHandle)
            return 0
        }

        platform.windows.WM_LBUTTONDOWN -> {
            val x = (lParam.toInt() and 0xFFFF).toFloat()
            val y = ((lParam.toInt() ushr 16) and 0xFFFF).toFloat()
            val root = renderer.rootWidget
            if (root != null) {
                val target = lienzo.runtime.hitTest(root, x, y)
                target?.handleEvent(lienzo.runtime.UiEvent.Click(x, y))
            }
            return 0
        }

        platform.windows.WM_MOUSEMOVE -> {
            val x = (lParam.toInt() and 0xFFFF).toFloat()
            val y = ((lParam.toInt() ushr 16) and 0xFFFF).toFloat()
            val root = renderer.rootWidget
            if (root != null) {
                fun dispatchMouseMove(w: lienzo.runtime.Widget, mx: Float, my: Float) {
                    w.handleEvent(lienzo.runtime.UiEvent.MouseMove(mx, my))
                    for (c in w.children) {
                        dispatchMouseMove(c, mx, my)
                    }
                }
                dispatchMouseMove(root, x, y)
            }
            return 0
        }

        WM_DESTROY -> {
            renderer.dispose()
            PostQuitMessage(0)
            return 0
        }
    }

    return DefWindowProcA(windowHandle, message, wParam, lParam)
}
