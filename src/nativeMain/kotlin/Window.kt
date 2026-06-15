import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
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

private const val windowClassName = "SkiaCircleSampleWindow"
private const val windowTitle = "Kotlin Native Skia Sample"
private const val initialWindowWidth = 720
private const val initialWindowHeight = 540

private var activeRenderer: SkiaRenderer? = null

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
        WM_SIZE -> {
            renderer.resize(windowHandle)
            InvalidateRect(windowHandle, null, 0)
            return 0
        }

        WM_PAINT -> {
            renderer.paint(windowHandle)
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
