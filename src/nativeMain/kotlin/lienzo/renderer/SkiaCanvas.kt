@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package lienzo.renderer

import lienzo.runtime.DrawCanvas
import kotlinx.cinterop.*
import SkiaLibrary

class SkiaCanvas(
    private val skia: SkiaLibrary,
    private val nativeCanvas: COpaquePointer?
) : DrawCanvas {

    @OptIn(ExperimentalForeignApi::class)
    override fun drawRect(x: Float, y: Float, w: Float, h: Float, color: UInt) {
        memScoped {
            val rect = allocArray<FloatVar>(4)
            rect[0] = x
            rect[1] = y
            rect[2] = x + w
            rect[3] = y + h

            val paint = skia.paintNew.invoke()
            skia.paintSetColor.invoke(paint, color)
            skia.paintSetAntialias.invoke(paint, true)
            skia.canvasDrawRect.invoke(nativeCanvas, rect, paint)
            skia.paintDelete.invoke(paint)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: UInt) {
        memScoped {
            val rect = allocArray<FloatVar>(4)
            rect[0] = x
            rect[1] = y
            rect[2] = x + w
            rect[3] = y + h

            val paint = skia.paintNew.invoke()
            skia.paintSetColor.invoke(paint, color)
            skia.paintSetAntialias.invoke(paint, true)
            skia.canvasDrawRoundRect.invoke(nativeCanvas, rect, radius, radius, paint)
            skia.paintDelete.invoke(paint)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawText(text: String, x: Float, y: Float, color: UInt, size: Float) {
        memScoped {
            val paint = skia.paintNew.invoke()
            skia.paintSetColor.invoke(paint, color)
            skia.paintSetAntialias.invoke(paint, true)

            val font = skia.fontNew.invoke()
            skia.fontSetSize.invoke(font, size)

            val bytes = text.cstr
            skia.canvasDrawSimpleText.invoke(nativeCanvas, bytes.getPointer(this), (bytes.size - 1).toLong(), 0, x, y, font, paint)

            skia.fontDelete.invoke(font)
            skia.paintDelete.invoke(paint)
        }
    }
}
