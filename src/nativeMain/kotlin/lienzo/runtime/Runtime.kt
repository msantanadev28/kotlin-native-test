package lienzo.runtime

class State<T>(initial: T) {
    private var _value: T = initial
    private val observers = mutableListOf<() -> Unit>()

    var value: T
        get() = _value
        set(new) {
            if (new != _value) {
                _value = new
                observers.forEach { it() }
            }
        }

    fun observe(block: () -> Unit) {
        observers += block
    }
}

fun <T> state(initial: T): State<T> = State(initial)

class Binding<T>(val read: () -> T)
fun <T> bind(read: () -> T): Binding<T> = Binding(read)

interface DrawCanvas {
    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: UInt)
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: UInt,
        size: Float,
        fontFamily: String = "",
        shadowColor: UInt = 0u,
        shadowOffset: Float = 0f,
        fontWeight: Int = 400
    )
    fun drawRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: UInt)
}

sealed class UiEvent {
    data class Click(val x: Float, val y: Float) : UiEvent()
    data class MouseMove(val x: Float, val y: Float) : UiEvent()
}

data class Size(val width: Float, val height: Float)

var onGlobalRequestRedraw: (() -> Unit)? = null

abstract class Widget {
    val children = mutableListOf<Widget>()
    var parent: Widget? = null

    var boundsX = 0f
    var boundsY = 0f
    var boundsW = 0f
    var boundsH = 0f
    var grow = 0

    var margin: Int = 0
    var marginTop: Int = -1
    var marginBottom: Int = -1
    var marginLeft: Int = -1
    var marginRight: Int = -1

    val effectiveMarginTop: Float
        get() = (if (marginTop >= 0) marginTop else margin).toFloat()
    val effectiveMarginBottom: Float
        get() = (if (marginBottom >= 0) marginBottom else margin).toFloat()
    val effectiveMarginLeft: Float
        get() = (if (marginLeft >= 0) marginLeft else margin).toFloat()
    val effectiveMarginRight: Float
        get() = (if (marginRight >= 0) marginRight else margin).toFloat()

    fun measureWithMargins(maxWidth: Float, maxHeight: Float): Size {
        val extraW = effectiveMarginLeft + effectiveMarginRight
        val extraH = effectiveMarginTop + effectiveMarginBottom
        val innerW = maxOf(0f, maxWidth - extraW)
        val innerH = maxOf(0f, maxHeight - extraH)
        val size = measure(innerW, innerH)
        return Size(size.width + extraW, size.height + extraH)
    }

    fun placeWithMargins(x: Float, y: Float, width: Float, height: Float) {
        val extraL = effectiveMarginLeft
        val extraT = effectiveMarginTop
        val extraR = effectiveMarginRight
        val extraB = effectiveMarginBottom
        place(x + extraL, y + extraT, maxOf(0f, width - extraL - extraR), maxOf(0f, height - extraT - extraB))
    }

    fun applyLayout(
        grow: Int = 0,
        margin: Int = 0,
        marginTop: Int = -1,
        marginBottom: Int = -1,
        marginLeft: Int = -1,
        marginRight: Int = -1
    ) {
        this.grow = grow
        this.margin = margin
        this.marginTop = marginTop
        this.marginBottom = marginBottom
        this.marginLeft = marginLeft
        this.marginRight = marginRight
    }

    abstract fun measure(maxWidth: Float, maxHeight: Float): Size
    abstract fun place(x: Float, y: Float, width: Float, height: Float)
    abstract fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float)

    fun addChild(child: Widget) {
        child.parent = this
        children.add(child)
    }

    fun invalidate() {
        parent?.invalidate() ?: requestRedraw()
    }

    open fun requestRedraw() {
        onGlobalRequestRedraw?.invoke()
    }

    open fun handleEvent(event: UiEvent) {
        parent?.handleEvent(event)
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= boundsX && x <= (boundsX + boundsW) && y >= boundsY && y <= (boundsY + boundsH)
    }
}

fun hitTest(root: Widget, x: Float, y: Float): Widget? {
    for (child in root.children.asReversed()) {
        val hit = hitTest(child, x, y)
        if (hit != null) return hit
    }
    return if (root.contains(x, y)) root else null
}
