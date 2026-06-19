package org.jetbrains.skia

import kotlinx.cinterop.*
import platform.windows.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class ImageInfo(val width: Int, val height: Int) {
    companion object {
        fun makeN32Premul(width: Int, height: Int) = ImageInfo(width, height)
    }
}

class Paint {
    var color: Int = 0
    var isAntiAlias: Boolean = true
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Canvas(private val hdc: HDC?, private val width: Int, private val height: Int) {
    fun clear(color: Int) {
        memScoped {
            val rc = alloc<RECT>()
            rc.left = 0
            rc.top = 0
            rc.right = width
            rc.bottom = height
            val bgBrush = CreateSolidBrush(0u) // Black RGB(0,0,0)
            FillRect(hdc, rc.ptr, bgBrush)
            DeleteObject(bgBrush)
        }
    }

    fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        val left = (cx - radius).toInt()
        val top = (cy - radius).toInt()
        val right = (cx + radius).toInt()
        val bottom = (cy + radius).toInt()

        val r = (paint.color shr 16) and 0xFF
        val g = (paint.color shr 8) and 0xFF
        val b = paint.color and 0xFF

        val pen = CreatePen(PS_SOLID.toInt(), 1, 0xFFFFFFu) // White outline
        val brush = CreateSolidBrush((r or (g shl 8) or (b shl 16)).toUInt())
        val oldPen = SelectObject(hdc, pen)
        val oldBrush = SelectObject(hdc, brush)
        
        Ellipse(hdc, left, top, right, bottom)
        
        SelectObject(hdc, oldBrush)
        SelectObject(hdc, oldPen)
        DeleteObject(brush)
        DeleteObject(pen)
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Surface(val canvas: Canvas) {
    companion object {
        // Mimic Skia's makeRasterDirect using a Canvas wrapping our HDC/dimensions
        fun makeRasterDirect(info: ImageInfo, hdc: HDC?): Surface {
            return Surface(Canvas(hdc, info.width, info.height))
        }
    }
}
