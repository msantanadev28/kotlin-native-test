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
    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: UInt,
        size: Float,
        fontFamily: String,
        shadowColor: UInt,
        shadowOffset: Float,
        fontWeight: Int
    ) {
        memScoped {
            val paint = skia.paintNew.invoke()
            skia.paintSetAntialias.invoke(paint, true)

            val font = skia.fontNew.invoke()
            skia.fontSetSize.invoke(font, size)

            val familyNamePtr = if (fontFamily.isNotEmpty()) fontFamily.cstr.getPointer(this) else null
            val fontStyle = allocArray<IntVar>(3)
            fontStyle[0] = fontWeight // weight
            fontStyle[1] = 5   // width
            fontStyle[2] = 0   // slant
            val typeface = skia.typefaceCreateFromName.invoke(familyNamePtr, fontStyle)
            if (typeface != null) {
                skia.fontSetTypeface.invoke(font, typeface)
                skia.typefaceUnref.invoke(typeface)
            }

            val bytes = text.cstr
            val textLen = (bytes.size - 1).toLong()

            if (shadowColor != 0u && shadowOffset != 0f) {
                skia.paintSetColor.invoke(paint, shadowColor)
                skia.canvasDrawSimpleText.invoke(nativeCanvas, bytes.getPointer(this), textLen, 0, x + shadowOffset, y + shadowOffset, font, paint)
            }

            skia.paintSetColor.invoke(paint, color)
            skia.canvasDrawSimpleText.invoke(nativeCanvas, bytes.getPointer(this), textLen, 0, x, y, font, paint)

            skia.fontDelete.invoke(font)
            skia.paintDelete.invoke(paint)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun drawRoundRectStroke(x: Float, y: Float, w: Float, h: Float, radius: Float, color: UInt, thickness: Float) {
        memScoped {
            val rect = allocArray<FloatVar>(4)
            val halfT = thickness / 2f
            rect[0] = x + halfT
            rect[1] = y + halfT
            rect[2] = x + w - halfT
            rect[3] = y + h - halfT

            val paint = skia.paintNew.invoke()
            skia.paintSetColor.invoke(paint, color)
            skia.paintSetAntialias.invoke(paint, true)
            skia.paintSetStyle.invoke(paint, 1) // 1 = Stroke
            skia.paintSetStrokeWidth.invoke(paint, thickness)
            skia.canvasDrawRoundRect.invoke(nativeCanvas, rect, maxOf(0f, radius - halfT), maxOf(0f, radius - halfT), paint)
            skia.paintDelete.invoke(paint)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun measureText(text: String, size: Float, fontFamily: String, fontWeight: Int): Float {
        if (text.isEmpty()) return 0f
        return memScoped {
            val font = skia.fontNew.invoke()
            skia.fontSetSize.invoke(font, size)

            val familyNamePtr = if (fontFamily.isNotEmpty()) fontFamily.cstr.getPointer(this) else null
            val fontStyle = allocArray<IntVar>(3)
            fontStyle[0] = fontWeight
            fontStyle[1] = 5
            fontStyle[2] = 0
            val typeface = skia.typefaceCreateFromName.invoke(familyNamePtr, fontStyle)
            if (typeface != null) {
                skia.fontSetTypeface.invoke(font, typeface)
                skia.typefaceUnref.invoke(typeface)
            }

            val bytes = text.cstr
            val textLen = (bytes.size - 1).toLong()
            val width = skia.fontMeasureText.invoke(font, bytes.getPointer(this), textLen, 0, null, null)

            skia.fontDelete.invoke(font)
            width
        }
    }
}
