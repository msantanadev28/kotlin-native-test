package lienzo.runtime.widgets

import lienzo.runtime.*

class WindowWidget(
    val title: String,
    val width: Int,
    val height: Int
) : Widget() {

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        return Size(width.toFloat(), height.toFloat())
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height

        for (child in children) {
            val childSize = child.measure(width, height)
            val cx = x + (width - childSize.width) / 2f
            val cy = y + (height - childSize.height) / 2f
            child.place(cx, cy, childSize.width, childSize.height)
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        canvas.drawRect(x, y, width, height, 0xFFF3F4F6u) // Tailored gray-100 theme bg
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class ButtonWidget(
    val text: String,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true
) : Widget() {

    var isHovered = false

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val textWidth = text.length * 10f + 40f
        val textHeight = 44f
        return Size(textWidth, textHeight)
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        val bgColor = when {
            !enabled -> 0xFFD1D5DBu // Gray-300
            isHovered -> 0xFF1D4ED8u // Blue-700
            else -> 0xFF2563EBu // Blue-600
        }
        canvas.drawRoundRect(x, y, width, height, 8f, bgColor)

        val textColor = 0xFFFFFFFFu
        val textWidth = text.length * 8f
        val textX = x + (width - textWidth) / 2f
        val textY = y + (height - 14f) / 2f + 10f
        canvas.drawText(text, textX, textY, textColor, 16f)
    }

    override fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.Click -> {
                if (enabled) {
                    onClick?.invoke()
                }
            }
            is UiEvent.MouseMove -> {
                val hovered = contains(event.x, event.y)
                if (hovered != isHovered) {
                    isHovered = hovered
                    invalidate()
                }
            }
        }
    }
}

fun window(
    title: String,
    width: Int,
    height: Int,
    block: WindowWidget.() -> Unit = {}
): WindowWidget {
    return WindowWidget(title, width, height).apply(block)
}

fun Widget.button(
    text: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
): ButtonWidget {
    val b = ButtonWidget(text, onClick, enabled)
    this.addChild(b)
    return b
}
