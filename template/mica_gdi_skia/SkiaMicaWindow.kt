@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import platform.windows.*
import kotlin.math.min

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20u

fun skiaWindowProc(hwnd: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    return when (msg) {
        WM_ERASEBKGND.toUInt() -> 1L
        WM_SETTINGCHANGE.toUInt() -> {
            val dark = if (isDarkModeActive()) 1 else 0
            memScoped {
                val value = alloc<BOOLVar>()
                value.value = dark
                DwmSetWindowAttribute(
                    hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE,
                    value.ptr,
                    sizeOf<BOOLVar>().convert()
                )
            }
            InvalidateRect(hwnd, null, 1)
            0L
        }
        WM_PAINT.toUInt() -> memScoped {
            val ps = alloc<PAINTSTRUCT>()
            val hdc = BeginPaint(hwnd, ps.ptr)
            val rc = alloc<RECT>()
            GetClientRect(hwnd, rc.ptr)

            val width = rc.right - rc.left
            val height = rc.bottom - rc.top

            // Use our Skia classes
            val info = ImageInfo.makeN32Premul(width, height)
            val surface = Surface.makeRasterDirect(info, hdc)
            val canvas = surface.canvas

            // Clear to black for transparent Mica composition
            canvas.clear(0)

            // Draw with Skia Paint and drawCircle
            val paint = Paint().apply {
                color = 0xFFFF4D6D.toInt() // Pink circle
                isAntiAlias = true
            }
            val radius = min(width, height) / 4f
            canvas.drawCircle(width / 2f, height / 2f, radius, paint)

            EndPaint(hwnd, ps.ptr)
            0L
        }
        WM_DESTROY.toUInt() -> {
            PostQuitMessage(0)
            0L
        }
        else -> DefWindowProcW(hwnd, msg, wParam, lParam)
    }
}

class SkiaMicaWindow {
    fun run() = memScoped {
        val hInstance = GetModuleHandleW(null)
        val className = "SkiaMicaSampleWindow"
        val title = "Skia Mica Sample"
        val classNameW = className.wcstr

        val wc = alloc<WNDCLASSW>()
        wc.lpfnWndProc = staticCFunction(::skiaWindowProc)
        wc.hInstance = hInstance
        wc.lpszClassName = classNameW.ptr
        wc.hCursor = LoadCursorW(null, IDC_ARROW)
        wc.hbrBackground = null

        if (RegisterClassW(wc.ptr) == 0u.toUShort()) return

        val hwnd = CreateWindowExW(
            0u,
            className,
            title,
            WS_OVERLAPPEDWINDOW.toUInt(),
            CW_USEDEFAULT,
            CW_USEDEFAULT,
            800,
            500,
            null,
            null,
            hInstance,
            null
        )

        if (hwnd == null) return

        setBackdrop(hwnd)

        ShowWindow(hwnd, SW_SHOW)
        UpdateWindow(hwnd)

        val msg = alloc<MSG>()
        while (GetMessageW(msg.ptr, null, 0u, 0u) > 0) {
            TranslateMessage(msg.ptr)
            DispatchMessageW(msg.ptr)
        }
    }
}
