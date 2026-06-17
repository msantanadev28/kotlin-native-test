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

        if (children.size == 1) {
            val child = children[0]
            val size = child.measure(width, height)
            child.place(x, y, width, height)
            return
        }

        val spacing = 16f
        val sizes = children.map { it.measure(width, height) }
        val totalHeight = sizes.map { it.height }.sum() + spacing * (children.size - 1)
        var currentY = y + (height - totalHeight) / 2f

        for (i in children.indices) {
            val child = children[i]
            val childSize = sizes[i]
            val cx = x + (width - childSize.width) / 2f
            child.place(cx, currentY, childSize.width, childSize.height)
            currentY += childSize.height + spacing
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

class LabelWidget(
    val textBinding: Binding<String>
) : Widget() {
    val text: String
        get() = textBinding.read()

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val str = text
        val textWidth = str.length * 10f + 40f
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
        val str = text
        val textColor = 0xFF111827u // Dark gray
        val textWidth = str.length * 8f
        val textX = x + (width - textWidth) / 2f
        val textY = y + (height - 14f) / 2f + 10f
        canvas.drawText(str, textX, textY, textColor, 16f)
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
    enabled: Boolean = true,
    grow: Int = 0
): ButtonWidget {
    val b = ButtonWidget(text, onClick, enabled).apply { this.grow = grow }
    this.addChild(b)
    return b
}

fun Widget.label(
    text: Binding<String>,
    grow: Int = 0
): LabelWidget {
    val l = LabelWidget(text).apply { this.grow = grow }
    this.addChild(l)
    return l
}

fun Widget.label(
    text: String,
    grow: Int = 0
): LabelWidget {
    val l = LabelWidget(bind { text }).apply { this.grow = grow }
    this.addChild(l)
    return l
}

class ColumnWidget(
    val spacing: Int = 0,
    val padding: Int = 0,
    val align: String = "start",
    grow: Int = 0
) : Widget() {
    init {
        this.grow = grow
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val innerMaxWidth = maxOf(0f, maxWidth - 2 * padding.toFloat())
        val innerMaxHeight = maxOf(0f, maxHeight - 2 * padding.toFloat())

        var totalHeight = 0f
        var maxChildWidth = 0f
        var visibleChildCount = 0

        for (child in children) {
            visibleChildCount++
            if (child.grow == 0) {
                val size = child.measure(innerMaxWidth, innerMaxHeight)
                totalHeight += size.height
                maxChildWidth = maxOf(maxChildWidth, size.width)
            }
        }

        val totalSpacing = if (visibleChildCount > 1) ((visibleChildCount - 1) * spacing).toFloat() else 0f
        val usedHeight = totalHeight + totalSpacing
        val remainingHeight = maxOf(0f, innerMaxHeight - usedHeight)

        val totalGrow = children.map { it.grow }.sum()
        for (child in children) {
            if (child.grow > 0) {
                val childHeight = (child.grow.toFloat() / totalGrow) * remainingHeight
                val size = child.measure(innerMaxWidth, childHeight)
                totalHeight += childHeight
                maxChildWidth = maxOf(maxChildWidth, size.width)
            }
        }

        return Size(
            width = maxChildWidth + 2 * padding.toFloat(),
            height = totalHeight + totalSpacing + 2 * padding.toFloat()
        )
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height

        val innerWidth = maxOf(0f, width - 2 * padding.toFloat())
        val innerHeight = maxOf(0f, height - 2 * padding.toFloat())

        var totalFixedHeight = 0f
        var visibleChildCount = 0
        for (child in children) {
            visibleChildCount++
            if (child.grow == 0) {
                totalFixedHeight += child.measure(innerWidth, innerHeight).height
            }
        }

        val totalSpacing = if (visibleChildCount > 1) ((visibleChildCount - 1) * spacing).toFloat() else 0f
        val remainingHeight = maxOf(0f, innerHeight - totalFixedHeight - totalSpacing)
        val totalGrow = children.map { it.grow }.sum()

        var currentY = y + padding.toFloat()
        val startX = x + padding.toFloat()

        for (child in children) {
            val childHeight = if (child.grow > 0) {
                (child.grow.toFloat() / totalGrow) * remainingHeight
            } else {
                child.measure(innerWidth, innerHeight).height
            }
            val childWidth = minOf(innerWidth, child.measure(innerWidth, childHeight).width)

            val childX = when (align) {
                "center" -> startX + (innerWidth - childWidth) / 2f
                "end" -> startX + innerWidth - childWidth
                else -> startX
            }

            child.place(childX, currentY, childWidth, childHeight)
            currentY += childHeight + spacing.toFloat()
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class RowWidget(
    val spacing: Int = 0,
    val padding: Int = 0,
    val align: String = "start",
    grow: Int = 0
) : Widget() {
    init {
        this.grow = grow
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val innerMaxWidth = maxOf(0f, maxWidth - 2 * padding.toFloat())
        val innerMaxHeight = maxOf(0f, maxHeight - 2 * padding.toFloat())

        var totalWidth = 0f
        var maxChildHeight = 0f
        var visibleChildCount = 0

        for (child in children) {
            visibleChildCount++
            if (child.grow == 0) {
                val size = child.measure(innerMaxWidth, innerMaxHeight)
                totalWidth += size.width
                maxChildHeight = maxOf(maxChildHeight, size.height)
            }
        }

        val totalSpacing = if (visibleChildCount > 1) ((visibleChildCount - 1) * spacing).toFloat() else 0f
        val usedWidth = totalWidth + totalSpacing
        val remainingWidth = maxOf(0f, innerMaxWidth - usedWidth)

        val totalGrow = children.map { it.grow }.sum()
        for (child in children) {
            if (child.grow > 0) {
                val childWidth = (child.grow.toFloat() / totalGrow) * remainingWidth
                val size = child.measure(childWidth, innerMaxHeight)
                totalWidth += childWidth
                maxChildHeight = maxOf(maxChildHeight, size.height)
            }
        }

        return Size(
            width = totalWidth + totalSpacing + 2 * padding.toFloat(),
            height = maxChildHeight + 2 * padding.toFloat()
        )
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height

        val innerWidth = maxOf(0f, width - 2 * padding.toFloat())
        val innerHeight = maxOf(0f, height - 2 * padding.toFloat())

        var totalFixedWidth = 0f
        var visibleChildCount = 0
        for (child in children) {
            visibleChildCount++
            if (child.grow == 0) {
                totalFixedWidth += child.measure(innerWidth, innerHeight).width
            }
        }

        val totalSpacing = if (visibleChildCount > 1) ((visibleChildCount - 1) * spacing).toFloat() else 0f
        val remainingWidth = maxOf(0f, innerWidth - totalFixedWidth - totalSpacing)
        val totalGrow = children.map { it.grow }.sum()

        var currentX = x + padding.toFloat()
        val startY = y + padding.toFloat()

        for (child in children) {
            val childWidth = if (child.grow > 0) {
                (child.grow.toFloat() / totalGrow) * remainingWidth
            } else {
                child.measure(innerWidth, innerHeight).width
            }
            val childHeight = minOf(innerHeight, child.measure(childWidth, innerHeight).height)

            val childY = when (align) {
                "center" -> startY + (innerHeight - childHeight) / 2f
                "end" -> startY + innerHeight - childHeight
                else -> startY
            }

            child.place(currentX, childY, childWidth, childHeight)
            currentX += childWidth + spacing.toFloat()
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class StackWidget(grow: Int = 0) : Widget() {
    init {
        this.grow = grow
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        var maxW = 0f
        var maxH = 0f
        for (child in children) {
            val size = child.measure(maxWidth, maxHeight)
            maxW = maxOf(maxW, size.width)
            maxH = maxOf(maxH, size.height)
        }
        return Size(maxW, maxH)
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height
        for (child in children) {
            child.place(x, y, width, height)
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class GridWidget(
    val columns: Int = 1,
    val rows: Int = 0,
    val spacing: Int = 0,
    grow: Int = 0
) : Widget() {
    init {
        this.grow = grow
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val cols = maxOf(1, columns)
        val totalCells = children.size
        val r = if (rows > 0) rows else {
            val calculated = (totalCells + cols - 1) / cols
            maxOf(1, calculated)
        }

        val horizontalSpacings = if (cols > 1) (cols - 1) * spacing.toFloat() else 0f
        val verticalSpacings = if (r > 1) (r - 1) * spacing.toFloat() else 0f

        val cellMaxWidth = maxOf(0f, (maxWidth - horizontalSpacings) / cols)
        val cellMaxHeight = maxOf(0f, (maxHeight - verticalSpacings) / r)

        var maxCellW = 0f
        var maxCellH = 0f
        for (child in children) {
            val size = child.measure(cellMaxWidth, cellMaxHeight)
            maxCellW = maxOf(maxCellW, size.width)
            maxCellH = maxOf(maxCellH, size.height)
        }

        return Size(
            width = maxCellW * cols + horizontalSpacings,
            height = maxCellH * r + verticalSpacings
        )
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height

        val cols = maxOf(1, columns)
        val totalCells = children.size
        val r = if (rows > 0) rows else {
            val calculated = (totalCells + cols - 1) / cols
            maxOf(1, calculated)
        }

        val horizontalSpacings = if (cols > 1) (cols - 1) * spacing.toFloat() else 0f
        val verticalSpacings = if (r > 1) (r - 1) * spacing.toFloat() else 0f

        val cellW = maxOf(0f, (width - horizontalSpacings) / cols)
        val cellH = maxOf(0f, (height - verticalSpacings) / r)

        for (i in children.indices) {
            val child = children[i]
            val colIdx = i % cols
            val rowIdx = i / cols

            val cx = x + colIdx * (cellW + spacing.toFloat())
            val cy = y + rowIdx * (cellH + spacing.toFloat())

            child.place(cx, cy, cellW, cellH)
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class SpacerWidget(grow: Int = 1) : Widget() {
    init {
        this.grow = grow
    }
    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        return Size(0f, 0f)
    }
    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height
    }
    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        // No-op
    }
}

fun Widget.column(
    spacing: Int = 0,
    padding: Int = 0,
    align: String = "start",
    grow: Int = 0,
    block: ColumnWidget.() -> Unit = {}
): ColumnWidget {
    val w = ColumnWidget(spacing, padding, align, grow)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.row(
    spacing: Int = 0,
    padding: Int = 0,
    align: String = "start",
    grow: Int = 0,
    block: RowWidget.() -> Unit = {}
): RowWidget {
    val w = RowWidget(spacing, padding, align, grow)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.stack(
    grow: Int = 0,
    block: StackWidget.() -> Unit = {}
): StackWidget {
    val w = StackWidget(grow)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.grid(
    columns: Int = 1,
    rows: Int = 0,
    spacing: Int = 0,
    grow: Int = 0,
    block: GridWidget.() -> Unit = {}
): GridWidget {
    val w = GridWidget(columns, rows, spacing, grow)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.spacer(
    grow: Int = 1
): SpacerWidget {
    val w = SpacerWidget(grow)
    this.addChild(w)
    return w
}

class CardWidget(grow: Int = 0) : Widget() {
    init {
        this.grow = grow
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val padding = 16f
        var maxW = 0f
        var maxH = 0f
        for (child in children) {
            val size = child.measure(maxWidth - 2 * padding, maxHeight - 2 * padding)
            maxW = maxOf(maxW, size.width)
            maxH = maxOf(maxH, size.height)
        }
        return Size(maxW + 2 * padding, maxH + 2 * padding)
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height
        val padding = 16f
        for (child in children) {
            child.place(x + padding, y + padding, width - 2 * padding, height - 2 * padding)
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        canvas.drawRoundRect(x, y, width, height, 12f, 0xFFFFFFFFu)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

fun Widget.card(
    grow: Int = 0,
    block: CardWidget.() -> Unit = {}
): CardWidget {
    val w = CardWidget(grow)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.text(
    value: String,
    grow: Int = 0
): LabelWidget = label(text = value, grow = grow)

fun Widget.text(
    value: Binding<String>,
    grow: Int = 0
): LabelWidget = label(text = value, grow = grow)



