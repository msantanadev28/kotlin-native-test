@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.windows.*

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20u

fun windowProc(hwnd: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
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
            val radius = if (width < height) width / 4 else height / 4
            val left = (width / 2) - radius
            val top = (height / 2) - radius
            val right = (width / 2) + radius
            val bottom = (height / 2) + radius

            // Fill client area with black (RGB 0,0,0 has 0 alpha in GDI, which DWM treats as transparent)
            val bgBrush = CreateSolidBrush(rgb(0, 0, 0))
            FillRect(hdc, rc.ptr, bgBrush)
            DeleteObject(bgBrush)

            val pen = CreatePen(PS_SOLID.toInt(), 1, rgb(255, 255, 255))
            val brush = CreateSolidBrush(rgb(255, 77, 109))
            val oldPen = SelectObject(hdc, pen)
            val oldBrush = SelectObject(hdc, brush)
            Ellipse(hdc, left, top, right, bottom)
            SelectObject(hdc, oldBrush)
            SelectObject(hdc, oldPen)
            DeleteObject(brush)
            DeleteObject(pen)
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

class MicaWindow {
    fun run() = memScoped {
        val hInstance = GetModuleHandleW(null)
        val className = "MicaSampleWindow"
        val title = "Mica Sample"
        val classNameW = className.wcstr

        val wc = alloc<WNDCLASSW>()
        wc.lpfnWndProc = staticCFunction(::windowProc)
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
