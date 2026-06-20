package lienzo.runtime.widgets

import lienzo.runtime.*

fun parseColor(colorStr: String): UInt {
    if (colorStr.isEmpty()) return 0u
    val clean = colorStr.trim()
    if (clean.startsWith("0x") || clean.startsWith("0X")) {
        val hex = clean.substring(2).removeSuffix("u").removeSuffix("U")
        return hex.toLong(16).toUInt()
    }
    if (clean.startsWith("#")) {
        val hex = clean.substring(1)
        if (hex.length == 6) {
            return ("FF" + hex).toLong(16).toUInt()
        }
        return hex.toLong(16).toUInt()
    }
    return clean.toUIntOrNull() ?: 0u
}

fun drawBackgroundAndBorder(
    canvas: DrawCanvas,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    cornerRadius: Int,
    backgroundColor: String,
    borderColor: String,
    borderThickness: Int
) {
    val hasBg = backgroundColor.isNotEmpty()
    val hasBorder = borderColor.isNotEmpty() && borderThickness > 0

    if (hasBorder) {
        val bColor = parseColor(borderColor)
        if (cornerRadius > 0) {
            canvas.drawRoundRect(x, y, width, height, cornerRadius.toFloat(), bColor)
        } else {
            canvas.drawRect(x, y, width, height, bColor)
        }

        val bgStr = if (hasBg) backgroundColor else "0xFFFFFFFF"
        val bgColor = parseColor(bgStr)
        val t = borderThickness.toFloat()
        val innerRadius = maxOf(0f, cornerRadius.toFloat() - t)

        if (innerRadius > 0f) {
            canvas.drawRoundRect(x + t, y + t, width - 2 * t, height - 2 * t, innerRadius, bgColor)
        } else {
            canvas.drawRect(x + t, y + t, width - 2 * t, height - 2 * t, bgColor)
        }
    } else if (hasBg) {
        val bg = parseColor(backgroundColor)
        if (cornerRadius > 0) {
            canvas.drawRoundRect(x, y, width, height, cornerRadius.toFloat(), bg)
        } else {
            canvas.drawRect(x, y, width, height, bg)
        }
    }
}


class WindowWidget(
    val title: String,
    val width: Int,
    val height: Int,
    val theme: String = ""
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
        if (theme != "mica") {
            canvas.drawRect(x, y, width, height, 0xFFF3F4F6u) // Tailored gray-100 theme bg
        }
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class ButtonWidget(
    val text: String,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    val cornerRadius: Int = 8,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
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
        val defaultBgColor = when {
            !enabled -> 0xFFD1D5DBu // Gray-300
            isHovered -> 0xFF1D4ED8u // Blue-700
            else -> 0xFF2563EBu // Blue-600
        }
        val bg = if (backgroundColor.isNotEmpty()) parseColor(backgroundColor) else defaultBgColor

        if (borderColor.isNotEmpty() && borderThickness > 0) {
            val bColor = parseColor(borderColor)
            canvas.drawRoundRect(x, y, width, height, cornerRadius.toFloat(), bColor)
            val t = borderThickness.toFloat()
            val innerRadius = maxOf(0f, cornerRadius.toFloat() - t)
            canvas.drawRoundRect(x + t, y + t, width - 2 * t, height - 2 * t, innerRadius, bg)
        } else {
            canvas.drawRoundRect(x, y, width, height, cornerRadius.toFloat(), bg)
        }

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
    theme: String = "",
    block: WindowWidget.() -> Unit = {}
): WindowWidget {
    return WindowWidget(title, width, height, theme).apply(block)
}

fun Widget.button(
    text: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    grow: Int = 0,
    cornerRadius: Int = 8,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0
): ButtonWidget {
    val b = ButtonWidget(text, onClick, enabled, cornerRadius, backgroundColor, borderColor, borderThickness).apply { this.grow = grow }
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
    grow: Int = 0,
    val cornerRadius: Int = 0,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
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
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class RowWidget(
    val spacing: Int = 0,
    val padding: Int = 0,
    val align: String = "start",
    grow: Int = 0,
    val cornerRadius: Int = 0,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
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
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class StackWidget(
    grow: Int = 0,
    val cornerRadius: Int = 0,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
) : Widget() {
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
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

class GridWidget(
    val columns: Int = 1,
    val rows: Int = 0,
    val spacing: Int = 0,
    grow: Int = 0,
    val cornerRadius: Int = 0,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
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
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
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

class FlexBoxWidget(
    val direction: String = "row",
    val wrap: Boolean = false,
    val justifyContent: String = "flex-start",
    val alignItems: String = "flex-start",
    val spacing: Int = 0,
    val padding: Int = 0,
    grow: Int = 0,
    val cornerRadius: Int = 0,
    val backgroundColor: String = "",
    val borderColor: String = "",
    val borderThickness: Int = 0
) : Widget() {
    init {
        this.grow = grow
    }

    private data class FlexLine(
        val children: List<Widget>,
        val sizes: List<Size>,
        val totalGrow: Int,
        val mainSize: Float,
        val crossSize: Float
    )

    private fun calculateLines(innerMaxWidth: Float, innerMaxHeight: Float): List<FlexLine> {
        val lines = mutableListOf<FlexLine>()
        var currentLineChildren = mutableListOf<Widget>()
        var currentLineSizes = mutableListOf<Size>()
        var currentLineMainSize = 0f
        var currentLineMaxCrossSize = 0f
        var currentLineTotalGrow = 0

        for (child in children) {
            val size = child.measure(innerMaxWidth, innerMaxHeight)
            val spacingCost = if (currentLineChildren.isNotEmpty()) spacing.toFloat() else 0f

            if (direction == "row") {
                if (wrap && currentLineChildren.isNotEmpty() && currentLineMainSize + spacingCost + size.width > innerMaxWidth) {
                    lines.add(
                        FlexLine(
                            currentLineChildren.toList(),
                            currentLineSizes.toList(),
                            currentLineTotalGrow,
                            currentLineMainSize,
                            currentLineMaxCrossSize
                        )
                    )
                    currentLineChildren = mutableListOf()
                    currentLineSizes = mutableListOf()
                    currentLineMainSize = 0f
                    currentLineMaxCrossSize = 0f
                    currentLineTotalGrow = 0
                }
                currentLineChildren.add(child)
                currentLineSizes.add(size)
                currentLineMainSize += (if (currentLineChildren.size > 1) spacing.toFloat() else 0f) + size.width
                currentLineMaxCrossSize = maxOf(currentLineMaxCrossSize, size.height)
                currentLineTotalGrow += child.grow
            } else {
                if (wrap && currentLineChildren.isNotEmpty() && currentLineMainSize + spacingCost + size.height > innerMaxHeight) {
                    lines.add(
                        FlexLine(
                            currentLineChildren.toList(),
                            currentLineSizes.toList(),
                            currentLineTotalGrow,
                            currentLineMainSize,
                            currentLineMaxCrossSize
                        )
                    )
                    currentLineChildren = mutableListOf()
                    currentLineSizes = mutableListOf()
                    currentLineMainSize = 0f
                    currentLineMaxCrossSize = 0f
                    currentLineTotalGrow = 0
                }
                currentLineChildren.add(child)
                currentLineSizes.add(size)
                currentLineMainSize += (if (currentLineChildren.size > 1) spacing.toFloat() else 0f) + size.height
                currentLineMaxCrossSize = maxOf(currentLineMaxCrossSize, size.width)
                currentLineTotalGrow += child.grow
            }
        }
        if (currentLineChildren.isNotEmpty()) {
            lines.add(
                FlexLine(
                    currentLineChildren.toList(),
                    currentLineSizes.toList(),
                    currentLineTotalGrow,
                    currentLineMainSize,
                    currentLineMaxCrossSize
                )
            )
        }
        return lines
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        val innerMaxWidth = maxOf(0f, maxWidth - 2 * padding.toFloat())
        val innerMaxHeight = maxOf(0f, maxHeight - 2 * padding.toFloat())

        val lines = calculateLines(innerMaxWidth, innerMaxHeight)

        if (direction == "row") {
            val contentWidth = lines.map { it.mainSize }.maxOrNull() ?: 0f
            val contentHeight = lines.map { it.crossSize }.sum() + if (lines.size > 1) (lines.size - 1) * spacing.toFloat() else 0f
            return Size(contentWidth + 2 * padding.toFloat(), contentHeight + 2 * padding.toFloat())
        } else {
            val contentHeight = lines.map { it.mainSize }.maxOrNull() ?: 0f
            val contentWidth = lines.map { it.crossSize }.sum() + if (lines.size > 1) (lines.size - 1) * spacing.toFloat() else 0f
            return Size(contentWidth + 2 * padding.toFloat(), contentHeight + 2 * padding.toFloat())
        }
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x
        boundsY = y
        boundsW = width
        boundsH = height

        val innerWidth = maxOf(0f, width - 2 * padding.toFloat())
        val innerHeight = maxOf(0f, height - 2 * padding.toFloat())

        val lines = calculateLines(innerWidth, innerHeight)

        if (direction == "row") {
            var currentY = y + padding.toFloat()
            for (line in lines) {
                val totalChildrenWidth = line.sizes.map { it.width }.sum()
                val rawLineMainSize = totalChildrenWidth + if (line.children.size > 1) (line.children.size - 1) * spacing.toFloat() else 0f
                val extraSpace = maxOf(0f, innerWidth - rawLineMainSize)

                val itemWidths = FloatArray(line.children.size)
                for (i in line.children.indices) {
                    val child = line.children[i]
                    val baseWidth = line.sizes[i].width
                    itemWidths[i] = if (line.totalGrow > 0 && child.grow > 0) {
                        baseWidth + (child.grow.toFloat() / line.totalGrow) * extraSpace
                    } else {
                        baseWidth
                    }
                }

                val lineMainSizeWithGrow = itemWidths.sum() + if (line.children.size > 1) (line.children.size - 1) * spacing.toFloat() else 0f
                val remainingExtraSpace = maxOf(0f, innerWidth - lineMainSizeWithGrow)

                var currentX = x + padding.toFloat()
                var gap = spacing.toFloat()

                when (justifyContent) {
                    "flex-end" -> {
                        currentX += remainingExtraSpace
                    }
                    "center" -> {
                        currentX += remainingExtraSpace / 2f
                    }
                    "space-between" -> {
                        if (line.children.size > 1) {
                            gap = spacing.toFloat() + remainingExtraSpace / (line.children.size - 1)
                        }
                    }
                    "space-around" -> {
                        if (line.children.size > 0) {
                            gap = spacing.toFloat() + remainingExtraSpace / line.children.size
                            currentX += gap / 2f
                        }
                    }
                }

                for (i in line.children.indices) {
                    val child = line.children[i]
                    val childWidth = itemWidths[i]
                    val childHeight = if (alignItems == "stretch") {
                        line.crossSize
                    } else {
                        minOf(line.crossSize, line.sizes[i].height)
                    }

                    val childY = when (alignItems) {
                        "center" -> currentY + (line.crossSize - childHeight) / 2f
                        "flex-end" -> currentY + (line.crossSize - childHeight)
                        else -> currentY
                    }

                    child.place(currentX, childY, childWidth, childHeight)
                    currentX += childWidth + gap
                }
                currentY += line.crossSize + spacing.toFloat()
            }
        } else {
            var currentX = x + padding.toFloat()
            for (line in lines) {
                val totalChildrenHeight = line.sizes.map { it.height }.sum()
                val rawLineMainSize = totalChildrenHeight + if (line.children.size > 1) (line.children.size - 1) * spacing.toFloat() else 0f
                val extraSpace = maxOf(0f, innerHeight - rawLineMainSize)

                val itemHeights = FloatArray(line.children.size)
                for (i in line.children.indices) {
                    val child = line.children[i]
                    val baseHeight = line.sizes[i].height
                    itemHeights[i] = if (line.totalGrow > 0 && child.grow > 0) {
                        baseHeight + (child.grow.toFloat() / line.totalGrow) * extraSpace
                    } else {
                        baseHeight
                    }
                }

                val lineMainSizeWithGrow = itemHeights.sum() + if (line.children.size > 1) (line.children.size - 1) * spacing.toFloat() else 0f
                val remainingExtraSpace = maxOf(0f, innerHeight - lineMainSizeWithGrow)

                var currentY = y + padding.toFloat()
                var gap = spacing.toFloat()

                when (justifyContent) {
                    "flex-end" -> {
                        currentY += remainingExtraSpace
                    }
                    "center" -> {
                        currentY += remainingExtraSpace / 2f
                    }
                    "space-between" -> {
                        if (line.children.size > 1) {
                            gap = spacing.toFloat() + remainingExtraSpace / (line.children.size - 1)
                        }
                    }
                    "space-around" -> {
                        if (line.children.size > 0) {
                            gap = spacing.toFloat() + remainingExtraSpace / line.children.size
                            currentY += gap / 2f
                        }
                    }
                }

                for (i in line.children.indices) {
                    val child = line.children[i]
                    val childHeight = itemHeights[i]
                    val childWidth = if (alignItems == "stretch") {
                        line.crossSize
                    } else {
                        minOf(line.crossSize, line.sizes[i].width)
                    }

                    val childX = when (alignItems) {
                        "center" -> currentX + (line.crossSize - childWidth) / 2f
                        "flex-end" -> currentX + (line.crossSize - childWidth)
                        else -> currentX
                    }

                    child.place(childX, currentY, childWidth, childHeight)
                    currentY += childHeight + gap
                }
                currentX += line.crossSize + spacing.toFloat()
            }
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

fun Widget.flexBox(
    direction: String = "row",
    wrap: Boolean = false,
    justifyContent: String = "flex-start",
    alignItems: String = "flex-start",
    spacing: Int = 0,
    padding: Int = 0,
    grow: Int = 0,
    cornerRadius: Int = 0,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: FlexBoxWidget.() -> Unit = {}
): FlexBoxWidget {
    val w = FlexBoxWidget(direction, wrap, justifyContent, alignItems, spacing, padding, grow, cornerRadius, backgroundColor, borderColor, borderThickness)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.column(
    spacing: Int = 0,
    padding: Int = 0,
    align: String = "start",
    grow: Int = 0,
    cornerRadius: Int = 0,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: ColumnWidget.() -> Unit = {}
): ColumnWidget {
    val w = ColumnWidget(spacing, padding, align, grow, cornerRadius, backgroundColor, borderColor, borderThickness)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.row(
    spacing: Int = 0,
    padding: Int = 0,
    align: String = "start",
    grow: Int = 0,
    cornerRadius: Int = 0,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: RowWidget.() -> Unit = {}
): RowWidget {
    val w = RowWidget(spacing, padding, align, grow, cornerRadius, backgroundColor, borderColor, borderThickness)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.stack(
    grow: Int = 0,
    cornerRadius: Int = 0,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: StackWidget.() -> Unit = {}
): StackWidget {
    val w = StackWidget(grow, cornerRadius, backgroundColor, borderColor, borderThickness)
    this.addChild(w)
    w.block()
    return w
}

fun Widget.grid(
    columns: Int = 1,
    rows: Int = 0,
    spacing: Int = 0,
    grow: Int = 0,
    cornerRadius: Int = 0,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: GridWidget.() -> Unit = {}
): GridWidget {
    val w = GridWidget(columns, rows, spacing, grow, cornerRadius, backgroundColor, borderColor, borderThickness)
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

class CardWidget(
    val cornerRadius: Int = 12,
    val backgroundColor: String = "",
    grow: Int = 0,
    val borderColor: String = "",
    val borderThickness: Int = 0
) : Widget() {
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
        drawBackgroundAndBorder(canvas, x, y, width, height, cornerRadius, backgroundColor, borderColor, borderThickness)
        for (child in children) {
            child.draw(canvas, child.boundsX, child.boundsY, child.boundsW, child.boundsH)
        }
    }
}

fun Widget.card(
    grow: Int = 0,
    cornerRadius: Int = 12,
    backgroundColor: String = "",
    borderColor: String = "",
    borderThickness: Int = 0,
    block: CardWidget.() -> Unit = {}
): CardWidget {
    val w = CardWidget(cornerRadius, backgroundColor, grow, borderColor, borderThickness)
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



