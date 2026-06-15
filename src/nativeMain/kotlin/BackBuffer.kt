import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.windows.BITMAPINFO
import platform.windows.BITMAPINFOHEADER
import platform.windows.BI_RGB
import platform.windows.CreateCompatibleDC
import platform.windows.CreateDIBSection
import platform.windows.DIB_RGB_COLORS
import platform.windows.DeleteDC
import platform.windows.DeleteObject
import platform.windows.GetLastError
import platform.windows.HBITMAP
import platform.windows.HDC
import platform.windows.HGDIOBJ
import platform.windows.SelectObject

@OptIn(ExperimentalForeignApi::class)
class BackBuffer {
    var dc: HDC? = null
        private set
    var bitmap: HBITMAP? = null
        private set
    var previousBitmap: HGDIOBJ? = null
        private set
    var pixels: COpaquePointer? = null
        private set
    var width = 0
        private set
    var height = 0
        private set

    fun recreate(w: Int, h: Int) {
        dispose()

        width = w
        height = h

        val memoryDc = CreateCompatibleDC(null) ?: error("CreateCompatibleDC failed with ${GetLastError()}")

        memScoped {
            val bitmapInfo = alloc<BITMAPINFO>()
            bitmapInfo.bmiHeader.biSize = sizeOf<BITMAPINFOHEADER>().toUInt()
            bitmapInfo.bmiHeader.biWidth = w
            bitmapInfo.bmiHeader.biHeight = -h
            bitmapInfo.bmiHeader.biPlanes = 1u
            bitmapInfo.bmiHeader.biBitCount = 32u
            bitmapInfo.bmiHeader.biCompression = BI_RGB.toUInt()

            val pixelsPointer = alloc<COpaquePointerVar>()
            val newBitmap = CreateDIBSection(memoryDc, bitmapInfo.ptr, DIB_RGB_COLORS.toUInt(), pixelsPointer.ptr, null, 0u)
                ?: error("CreateDIBSection failed with ${GetLastError()}")

            val selectedObject = SelectObject(memoryDc, newBitmap)
            dc = memoryDc
            bitmap = newBitmap
            previousBitmap = selectedObject
            pixels = pixelsPointer.value
        }
    }

    fun dispose() {
        if (dc != null && previousBitmap != null) {
            SelectObject(dc, previousBitmap)
        }

        bitmap?.let {
            DeleteObject(it)
        }

        dc?.let {
            DeleteDC(it)
        }

        dc = null
        bitmap = null
        previousBitmap = null
        pixels = null
        width = 0
        height = 0
    }
}
