import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.get
import platform.windows.BeginPaint
import platform.windows.BitBlt
import platform.windows.EndPaint
import platform.windows.GetClientRect
import platform.windows.HWND
import platform.windows.InvalidateRect
import platform.windows.PAINTSTRUCT
import platform.windows.RECT
import platform.windows.SRCCOPY
import kotlin.math.min

private const val skiaColorTypeBgra8888 = 6
private const val skiaAlphaTypePremul = 2

private const val colorWhite = 0xFFFFFFFFu
private const val colorBlue = 0xFF0000FFu
private const val colorRed = 0xFFFF0000u

@OptIn(ExperimentalForeignApi::class)
class SkiaRenderer {
    val skia = SkiaLibrary()
    private val backBuffer = BackBuffer()
    private var surface: COpaquePointer? = null

    fun load() {
        skia.load()
    }

    fun resize(windowHandle: HWND?) {
        val clientRect = memScoped {
            val rect = alloc<RECT>()
            if (GetClientRect(windowHandle, rect.ptr) == 0) {
                return
            }
            Pair(rect.right - rect.left, rect.bottom - rect.top)
        }

        if (clientRect.first <= 0 || clientRect.second <= 0) {
            disposeBackBuffer()
            return
        }

        if (clientRect.first == backBuffer.width && clientRect.second == backBuffer.height && surface != null) {
            return
        }

        backBuffer.recreate(clientRect.first, clientRect.second)
        surface = createRasterSurface(backBuffer.pixels)
    }

    fun paint(windowHandle: HWND?) {
        if (surface == null) {
            resize(windowHandle)
        }

        memScoped {
            val paintStruct = alloc<PAINTSTRUCT>()
            val targetDc = BeginPaint(windowHandle, paintStruct.ptr) ?: return

            drawScene()

            if (backBuffer.dc != null && backBuffer.width > 0 && backBuffer.height > 0) {
                BitBlt(targetDc, 0, 0, backBuffer.width, backBuffer.height, backBuffer.dc, 0, 0, SRCCOPY.toUInt())
            }

            EndPaint(windowHandle, paintStruct.ptr)
        }
    }

    fun dispose() {
        disposeBackBuffer()
    }

    private fun createRasterSurface(pixelMemory: COpaquePointer?): COpaquePointer? {
        return memScoped {
            val imageInfoWords = allocArray<IntVar>(6)
            imageInfoWords[0] = 0
            imageInfoWords[1] = 0
            imageInfoWords[2] = backBuffer.width
            imageInfoWords[3] = backBuffer.height
            imageInfoWords[4] = skiaColorTypeBgra8888
            imageInfoWords[5] = skiaAlphaTypePremul

            skia.surfaceNewRasterDirect(
                imageInfoWords.reinterpret(),
                pixelMemory,
                backBuffer.width * 4L,
                null,
                null,
                null,
            )
        }
    }

    private fun drawScene() {
        val currentSurface = surface ?: return
        val canvas = skia.surfaceGetCanvas(currentSurface) ?: return

        skia.canvasClear(canvas, colorWhite)

        val paint = skia.paintNew()
        try {
            skia.paintSetAntialias(paint, true)
            skia.paintSetColor(paint, colorBlue)

            val centerX = backBuffer.width / 2f
            val centerY = backBuffer.height / 2f
            val outerRadius = min(backBuffer.width, backBuffer.height) * 0.3f
            val innerRadius = outerRadius * 0.45f

            skia.canvasDrawCircle(canvas, centerX, centerY, outerRadius, paint)

            skia.paintSetColor(paint, colorRed)
            skia.canvasDrawCircle(canvas, centerX, centerY, innerRadius, paint)
        } finally {
            skia.paintDelete(paint)
        }
    }

    private fun disposeBackBuffer() {
        surface?.let {
            skia.surfaceUnref(it)
            surface = null
        }
        backBuffer.dispose()
    }
}
