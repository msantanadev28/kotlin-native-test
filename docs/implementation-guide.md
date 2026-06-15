# Lienzo UI — Implementation Guide

**Version:** 0.1
**Target:** Kotlin Native (mingwX64) + Skia via `libSkiaSharp.dll`

This guide walks through building the Lienzo UI library from scratch, phase by phase. Each phase builds on the previous one. The project already has a working Kotlin Native + Skia bootstrap — we extend it into a full declarative UI framework.

---

# Table of Contents

1. [Overview & Architecture](#overview--architecture)
2. [Phase 1 — Project Layout](#phase-1--project-layout)
3. [Phase 2 — Lexer](#phase-2--lexer)
4. [Phase 3 — Parser & AST](#phase-3--parser--ast)
5. [Phase 4 — Code Generator](#phase-4--code-generator)
6. [Phase 5 — Reactive State](#phase-5--reactive-state)
7. [Phase 6 — Widget System](#phase-6--widget-system)
8. [Phase 7 — Layout Engine](#phase-7--layout-engine)
9. [Phase 8 — Skia Renderer](#phase-8--skia-renderer)
10. [Phase 9 — Event System](#phase-9--event-system)
11. [Phase 10 — Compiler CLI & Gradle Plugin](#phase-10--compiler-cli--gradle-plugin)
12. [Phase 11 — Built-in Components](#phase-11--built-in-components)
13. [Phase 12 — Theming](#phase-12--theming)
14. [Testing Strategy](#testing-strategy)
15. [Milestone Checklist](#milestone-checklist)

---

# Overview & Architecture

Lienzo UI has four distinct layers:

```
.lienzo files
      │
      ▼
┌─────────────┐
│  Compiler   │  Lexer → Parser → AST → Kotlin codegen
└──────┬──────┘
       │ generates
       ▼
┌─────────────┐
│   Runtime   │  Widget tree, reactive state, layout engine, event bus
└──────┬──────┘
       │ draws via
       ▼
┌─────────────┐
│  Renderer   │  Skia canvas (libSkiaSharp.dll on Windows)
└──────┬──────┘
       │ runs in
       ▼
┌─────────────┐
│  Win32 Host │  Window, WM_PAINT, mouse/keyboard messages
└─────────────┘
```

The **compiler** is a separate JVM tool (or Kotlin Multiplatform module) that reads `.lienzo` files at build time and outputs Kotlin source. The **runtime** and **renderer** are Kotlin Native modules linked into the final executable.

---

# Phase 1 — Project Layout

Before writing any framework code you need a clean multi-module Gradle structure. The compiler, runtime, renderer, and sample app must be separate modules so each can have its own dependencies, targets, and build settings. The compiler is a JVM-only module (it runs on your machine at build time), while the runtime and renderer are Kotlin Native modules that get compiled into the final executable. Getting this structure right from the start prevents painful refactors later and ensures the Gradle plugin can wire everything together in Phase 10.

## 1.1 Create the Module Structure

Restructure `build.gradle.kts` to use subprojects:

```
lienzo/
├── compiler/               ← JVM module, runs at build time
│   └── src/main/kotlin/
├── runtime/                ← Kotlin Multiplatform (native target)
│   └── src/
│       ├── commonMain/kotlin/
│       └── nativeMain/kotlin/
├── renderer-skia/          ← Kotlin Native, links libSkiaSharp
│   └── src/nativeMain/kotlin/
├── gradle-plugin/          ← Wires the compiler into the build
│   └── src/main/kotlin/
└── sample-app/             ← Your test application
    └── src/
        ├── nativeMain/kotlin/
        └── ui/             ← .lienzo files live here
```

## 1.2 Root `settings.gradle.kts`

```kotlin
rootProject.name = "lienzo"

include(
    ":compiler",
    ":runtime",
    ":renderer-skia",
    ":gradle-plugin",
    ":sample-app"
)
```

## 1.3 `compiler/build.gradle.kts`

The compiler is a plain JVM application — no native compilation needed.

```kotlin
plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("lienzo.compiler.MainKt")
}

dependencies {
    implementation(kotlin("stdlib"))
}
```

## 1.4 `runtime/build.gradle.kts`

```kotlin
plugins {
    kotlin("multiplatform")
}

kotlin {
    mingwX64("native")
    // add macosArm64(), linuxX64() later

    sourceSets {
        val commonMain by getting
        val nativeMain by getting {
            dependsOn(commonMain)
        }
    }
}
```

---

# Phase 2 — Lexer

The lexer (also called a tokenizer) is the first stage of the compiler pipeline. It reads the raw `.lienzo` source text character by character and groups characters into meaningful units called **tokens** — things like `TAG_OPEN`, `IDENTIFIER`, `STRING`, and `BINDING`. The lexer does not care about structure or meaning; it only cares about recognizing individual pieces of syntax. This stage also captures line and column numbers for each token, which makes error messages in later stages point at the exact location in the source file. The output is a flat `List<Token>` that the parser will consume in Phase 3.

The lexer converts raw `.lienzo` text into a flat list of tokens.

## 2.1 Define Token Types

`compiler/src/main/kotlin/lienzo/compiler/lexer/TokenType.kt`

```kotlin
enum class TokenType {
    // Structure
    TAG_OPEN,           // <
    TAG_CLOSE,          // >
    TAG_SELF_CLOSE,     // />
    TAG_END_OPEN,       // </
    IDENTIFIER,         // Window, Column, Button, text, onClick …
    EQUALS,             // =
    STRING,             // "hello"
    BINDING,            // {expression}
    COMMENT,            // <!-- … -->
    EOF
}
```

## 2.2 Token Data Class

```kotlin
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val col: Int
)
```

## 2.3 Implement the Lexer

`compiler/src/main/kotlin/lienzo/compiler/lexer/Lexer.kt`

```kotlin
class Lexer(private val source: String) {

    private var pos = 0
    private var line = 1
    private var col = 1
    val tokens = mutableListOf<Token>()

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            skipWhitespace()
            when {
                peek(4) == "<!--" -> readComment()
                peek() == '<' && peek(2) == "</" -> readTagEndOpen()
                peek() == '<'    -> readTagOpen()
                peek() == '/'  && peek(2) == "/>" -> readSelfClose()
                peek() == '>'    -> emit(TokenType.TAG_CLOSE, ">").also { advance() }
                peek() == '"'    -> readString()
                peek() == '{'    -> readBinding()
                peek() == '='    -> emit(TokenType.EQUALS, "=").also { advance() }
                isIdentStart()   -> readIdentifier()
                else             -> advance() // skip unknown chars
            }
        }
        emit(TokenType.EOF, "")
        return tokens
    }

    private fun readTagOpen() {
        advance() // consume <
        if (peek() == '/') {
            advance()
            emit(TokenType.TAG_END_OPEN, "</")
        } else {
            emit(TokenType.TAG_OPEN, "<")
        }
    }

    private fun readSelfClose() {
        advance(); advance()   // consume /  >
        emit(TokenType.TAG_SELF_CLOSE, "/>")
    }

    private fun readString() {
        advance() // opening "
        val sb = StringBuilder()
        while (pos < source.length && peek() != '"') {
            sb.append(advance())
        }
        advance() // closing "
        emit(TokenType.STRING, sb.toString())
    }

    private fun readBinding() {
        advance() // {
        val sb = StringBuilder()
        while (pos < source.length && peek() != '}') {
            sb.append(advance())
        }
        advance() // }
        emit(TokenType.BINDING, sb.toString())
    }

    private fun readIdentifier() {
        val sb = StringBuilder()
        while (pos < source.length && (peek().isLetterOrDigit() || peek() == '-' || peek() == '_')) {
            sb.append(advance())
        }
        emit(TokenType.IDENTIFIER, sb.toString())
    }

    private fun readComment() {
        while (pos < source.length && peek(3) != "-->") advance()
        repeat(3) { advance() } // consume -->
    }

    // helpers: peek(), advance(), emit(), skipWhitespace(), isIdentStart() …
}
```

## 2.4 Test the Lexer

Write a small unit test before moving on:

```kotlin
val tokens = Lexer("""<Button text="Save" onClick="save"/>""").tokenize()
// Expected: TAG_OPEN, IDENTIFIER(Button), IDENTIFIER(text), EQUALS,
//           STRING(Save), IDENTIFIER(onClick), EQUALS, STRING(save),
//           TAG_SELF_CLOSE, EOF
```

---

# Phase 3 — Parser & AST

The parser is the second compiler stage. It takes the flat token list from the lexer and builds a tree — the **Abstract Syntax Tree (AST)** — that reflects the hierarchical structure of the `.lienzo` file. For example, a `<Column>` containing a `<Button>` becomes a parent `ElementNode` with a child `ElementNode`. Each node also stores its attributes, distinguishing between plain string literals (`"Save"`) and reactive bindings (`{users}`). The AST is the canonical representation of the UI structure that all subsequent stages — especially the code generator — will work from. This is also the right place to add validation: unknown tag names, missing required attributes, mismatched closing tags, and similar errors should be caught and reported here with the line numbers from the lexer.

The parser consumes the token list and builds an Abstract Syntax Tree.

## 3.1 Define the AST

`compiler/src/main/kotlin/lienzo/compiler/ast/Node.kt`

```kotlin
sealed class Node

data class DocumentNode(val children: List<ElementNode>) : Node()

data class ElementNode(
    val tag: String,
    val attributes: Map<String, AttributeValue>,
    val children: List<ElementNode>
) : Node()

sealed class AttributeValue {
    data class Literal(val value: String) : AttributeValue()   // "Save"
    data class Binding(val expr: String) : AttributeValue()    // {users}
}
```

## 3.2 Implement the Parser

`compiler/src/main/kotlin/lienzo/compiler/parser/Parser.kt`

```kotlin
class Parser(private val tokens: List<Token>) {

    private var pos = 0

    fun parse(): DocumentNode {
        val children = mutableListOf<ElementNode>()
        while (!isEof()) {
            children.add(parseElement())
        }
        return DocumentNode(children)
    }

    private fun parseElement(): ElementNode {
        expect(TokenType.TAG_OPEN)
        val tag = expect(TokenType.IDENTIFIER).value
        val attributes = parseAttributes()

        return if (current().type == TokenType.TAG_SELF_CLOSE) {
            advance()
            ElementNode(tag, attributes, emptyList())
        } else {
            expect(TokenType.TAG_CLOSE)
            val children = parseChildren(tag)
            ElementNode(tag, attributes, children)
        }
    }

    private fun parseAttributes(): Map<String, AttributeValue> {
        val attrs = mutableMapOf<String, AttributeValue>()
        while (current().type == TokenType.IDENTIFIER) {
            val key = advance().value
            expect(TokenType.EQUALS)
            val value = when (current().type) {
                TokenType.STRING  -> AttributeValue.Literal(advance().value)
                TokenType.BINDING -> AttributeValue.Binding(advance().value)
                else -> error("Expected attribute value at line ${current().line}")
            }
            attrs[key] = value
        }
        return attrs
    }

    private fun parseChildren(parentTag: String): List<ElementNode> {
        val children = mutableListOf<ElementNode>()
        while (!(current().type == TokenType.TAG_END_OPEN)) {
            if (isEof()) error("Unclosed tag <$parentTag>")
            children.add(parseElement())
        }
        expect(TokenType.TAG_END_OPEN)
        expect(TokenType.IDENTIFIER) // closing tag name (ignored for now)
        expect(TokenType.TAG_CLOSE)
        return children
    }

    // helpers: current(), advance(), expect(), isEof()
}
```

---

# Phase 4 — Code Generator

The code generator is the final compiler stage. It walks the AST produced by the parser and emits a `.kt` source file that constructs the widget tree at runtime using the builder DSL you will define in Phase 6. Each `ElementNode` maps to a builder function call (e.g., `<Button>` → `button(...)`), string literal attributes map to plain Kotlin strings, and binding attributes map to `bind { }` lambda expressions. The output is ordinary Kotlin code — no magic, no reflection — which means the Kotlin Native compiler fully type-checks and optimizes it. Because the generated code is just Kotlin, you can open it in IntelliJ and read it while debugging. **Important:** design the target DSL (what the generated code should look like) before writing codegen, so the runtime API in Phase 6 is shaped to match exactly what the generator produces.

The code generator walks the AST and emits Kotlin source that calls runtime APIs.

## 4.1 Target Runtime API (design first)

Before writing codegen, decide what the generated Kotlin will look like:

```kotlin
// Generated from MainWindow.lienzo
fun buildMainWindow(): Widget {
    return window(title = "My App", width = 1200, height = 800) {
        column(spacing = 12) {
            text(value = bind { username })
            button(text = "Save", onClick = ::save)
        }
    }
}
```

This means the runtime must expose builder DSL functions (`window`, `column`, `text`, `button`, etc.) before codegen can target them.

## 4.2 Implement the Code Generator

`compiler/src/main/kotlin/lienzo/compiler/codegen/KotlinCodeGen.kt`

```kotlin
class KotlinCodeGen(
    private val packageName: String,
    private val functionName: String
) {

    fun generate(doc: DocumentNode): String = buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import lienzo.runtime.*")
        appendLine()
        appendLine("fun $functionName(): Widget {")
        appendLine("    return ${generateElement(doc.children.first(), indent = 1)}")
        appendLine("}")
    }

    private fun generateElement(node: ElementNode, indent: Int): String {
        val pad = "    ".repeat(indent)
        val fn = node.tag.replaceFirstChar { it.lowercaseChar() }
        val args = node.attributes.entries.joinToString(", ") { (k, v) ->
            when (v) {
                is AttributeValue.Literal -> "$k = \"${v.value}\""
                is AttributeValue.Binding -> "$k = bind { ${v.expr} }"
            }
        }
        return if (node.children.isEmpty()) {
            "$fn($args)"
        } else {
            val childLines = node.children.joinToString("\n") {
                "$pad    ${generateElement(it, indent + 1)}"
            }
            "$fn($args) {\n$childLines\n$pad}"
        }
    }
}
```

## 4.3 Wire Lexer → Parser → Codegen

`compiler/src/main/kotlin/lienzo/compiler/Main.kt`

```kotlin
fun main(args: Array<String>) {
    val inputFile = args[0]          // e.g. src/ui/MainWindow.lienzo
    val outputFile = args[1]         // e.g. build/generated/MainWindow.kt
    val packageName = args.getOrElse(2) { "ui.generated" }

    val source = File(inputFile).readText()
    val tokens = Lexer(source).tokenize()
    val ast    = Parser(tokens).parse()
    val code   = KotlinCodeGen(packageName, File(inputFile).nameWithoutExtension).generate(ast)

    File(outputFile).also { it.parentFile.mkdirs() }.writeText(code)
    println("Compiled $inputFile → $outputFile")
}
```

---

# Phase 5 — Reactive State

Reactive state is the glue between your Kotlin ViewModel logic and the UI. Instead of manually calling redraw functions whenever data changes, widgets subscribe to `State<T>` objects and are notified automatically. When a `State` value changes, every observer (typically a widget's `invalidate()` call) fires synchronously, which propagates up to the root widget and triggers a `WM_PAINT`. This is the same core idea behind Jetpack Compose's `MutableState` and Slint's properties. The binding expression `{username}` in a `.lienzo` file becomes a `bind { username.value }` lambda in generated Kotlin, so the widget always reads the current value at draw time. Getting this phase right — especially preventing stale reads and unnecessary redraws — is critical for perceived performance.

The reactive state system lets the UI re-render automatically when data changes.

## 5.1 State Primitive

`runtime/src/commonMain/kotlin/lienzo/runtime/state/State.kt`

```kotlin
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

// Top-level helper matching the spec's syntax
fun <T> state(initial: T): State<T> = State(initial)
```

## 5.2 Binding DSL

```kotlin
class Binding<T>(val read: () -> T)

fun <T> bind(read: () -> T): Binding<T> = Binding(read)
```

## 5.3 Derived State

```kotlin
fun <A, B> derived(source: State<A>, transform: (A) -> B): State<B> {
    val derived = State(transform(source.value))
    source.observe { derived.value = transform(source.value) }
    return derived
}
```

---

# Phase 6 — Widget System

The widget system is the runtime's core data structure. Every element in a `.lienzo` file — `Button`, `Column`, `TextBox`, `Table` — maps to a corresponding `Widget` subclass. Widgets form a tree that mirrors the element hierarchy in the source file. Each widget is responsible for three things: reporting its preferred size (`measure`), accepting a final position from its parent (`place`), and drawing itself onto a `DrawCanvas` (`draw`). This three-responsibility design cleanly separates concerns and maps naturally onto the two-pass layout algorithm in Phase 7. The builder DSL functions (`button { }`, `column { }`, etc.) are also defined in this phase, since the code generator from Phase 4 needs to target them.

Widgets are the core runtime objects. They form a tree mirroring the `.lienzo` file structure.

## 6.1 Base Widget

`runtime/src/commonMain/kotlin/lienzo/runtime/widgets/Widget.kt`

```kotlin
abstract class Widget {
    val children = mutableListOf<Widget>()
    var parent: Widget? = null

    // Called by the layout engine
    abstract fun measure(maxWidth: Float, maxHeight: Float): Size

    // Called by the renderer with the final position
    abstract fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float)

    fun addChild(child: Widget) {
        child.parent = this
        children.add(child)
    }

    // Trigger a re-layout + redraw from this node up
    fun invalidate() {
        parent?.invalidate() ?: requestRedraw()
    }

    // Overridden by the root widget to schedule a repaint
    open fun requestRedraw() {}
}

data class Size(val width: Float, val height: Float)
```

## 6.2 Builder DSL Functions

These are what the code generator targets.

`runtime/src/commonMain/kotlin/lienzo/runtime/widgets/Builders.kt`

```kotlin
fun window(
    title: String,
    width: Int,
    height: Int,
    theme: String = "light",
    block: WindowWidget.() -> Unit = {}
): WindowWidget = WindowWidget(title, width, height, theme).apply(block)

fun column(
    spacing: Int = 0,
    padding: Int = 0,
    grow: Int = 0,
    block: ColumnWidget.() -> Unit = {}
): ColumnWidget = ColumnWidget(spacing, padding, grow).apply(block)

fun row(
    spacing: Int = 0,
    block: RowWidget.() -> Unit = {}
): RowWidget = RowWidget(spacing).apply(block)

fun text(
    value: String = "",
    bind: Binding<String>? = null,
    color: String = "#000000",
    size: Int = 14
): TextWidget = TextWidget(value, bind, color, size)

fun button(
    text: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
): ButtonWidget = ButtonWidget(text, onClick, enabled)

// … add one builder per component
```

---

# Phase 7 — Layout Engine

The layout engine decides where every widget sits on screen and how large it is. Lienzo uses a classic **two-pass algorithm**: a bottom-up *measure pass* where each widget asks its children how large they want to be, followed by a top-down *place pass* where each parent assigns a concrete position and size to each child. This is the same approach used by Android's `View` system and Flutter's `RenderObject`. The trickiest part is flex/grow: after measuring fixed-size children, the remaining space is distributed proportionally among children that have a `grow` value greater than zero. This phase is pure logic — no drawing, no Skia — so it is easy to unit-test by asserting that specific widget trees produce expected `boundsX/Y/W/H` values.

The layout engine computes the position and size of every widget before drawing.

## 7.1 Layout Algorithm

Lienzo uses a two-pass layout:

1. **Measure pass** — each widget reports its preferred size given available space
2. **Place pass** — the parent assigns each child a concrete `x, y, width, height`

`runtime/src/commonMain/kotlin/lienzo/runtime/layout/LayoutEngine.kt`

```kotlin
object LayoutEngine {

    fun layout(root: Widget, availableWidth: Float, availableHeight: Float) {
        root.measure(availableWidth, availableHeight)
        root.place(0f, 0f, availableWidth, availableHeight)
    }
}

// Add place() to Widget as an abstract method
abstract class Widget {
    // … existing code …
    abstract fun place(x: Float, y: Float, width: Float, height: Float)
}
```

## 7.2 Column Layout

```kotlin
class ColumnWidget(
    val spacing: Int,
    val padding: Int,
    val grow: Int
) : Widget() {

    private val sizes = mutableListOf<Size>()

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        sizes.clear()
        var totalHeight = padding * 2f
        var maxChildWidth = 0f

        for (child in children) {
            val s = child.measure(maxWidth - padding * 2f, maxHeight)
            sizes += s
            totalHeight += s.height + spacing
            maxChildWidth = maxOf(maxChildWidth, s.width)
        }
        if (children.isNotEmpty()) totalHeight -= spacing

        return Size(maxChildWidth + padding * 2f, totalHeight)
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        var cursor = y + padding
        children.forEachIndexed { i, child ->
            child.place(x + padding, cursor, width - padding * 2f, sizes[i].height)
            cursor += sizes[i].height + spacing
        }
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        children.forEach { it.draw(canvas, x, y, width, height) }
    }
}
```

## 7.3 Row Layout

Follow the same pattern as `ColumnWidget` but accumulate `totalWidth` and step `cursor` horizontally.

## 7.4 Grow / Flex

After the measure pass, collect remaining space and distribute it to children with `grow > 0`:

```kotlin
val fixedHeight = sizes.sumOf { it.height.toDouble() }.toFloat() + spacing * (children.size - 1)
val remaining = height - fixedHeight
val growUnits = children.sumOf { (it as? ColumnWidget)?.grow?.toDouble() ?: 0.0 }.toFloat()

if (growUnits > 0f) {
    children.forEachIndexed { i, child ->
        val g = (child as? ColumnWidget)?.grow ?: 0
        if (g > 0) sizes[i] = sizes[i].copy(height = sizes[i].height + remaining * g / growUnits)
    }
}
```

---

# Phase 8 — Skia Renderer

The renderer bridges the widget system and the native graphics library. Rather than letting widgets call Skia directly — which would tie every widget to platform-specific C interop code — a `DrawCanvas` interface is introduced in the `runtime` (common) module. The `SkiaCanvas` in `renderer-skia` implements this interface by invoking the Skia C API through the existing `SkiaLibrary` function-pointer loader. This separation means you could later swap the backend for a different renderer (e.g., Direct2D, OpenGL) without touching any widget code. This phase extends the existing `SkiaRenderer.kt` and `BackBuffer.kt` rather than replacing them — the frame loop already works, you are simply giving it a tree of widgets to draw instead of hardcoded circles.

The renderer translates widget draw calls into Skia API calls. The project already has `SkiaLibrary.kt` and `SkiaRenderer.kt` — extend them here.

## 8.1 DrawCanvas Interface

Introduce an abstraction so widgets don't depend on Skia directly:

`runtime/src/commonMain/kotlin/lienzo/runtime/DrawCanvas.kt`

```kotlin
interface DrawCanvas {
    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: UInt)
    fun drawText(text: String, x: Float, y: Float, color: UInt, size: Float)
    fun drawRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: UInt)
    fun drawImage(source: String, x: Float, y: Float, w: Float, h: Float)
    fun clipRect(x: Float, y: Float, w: Float, h: Float)
    fun restore()
}
```

## 8.2 SkiaCanvas Implementation

`renderer-skia/src/nativeMain/kotlin/lienzo/renderer/SkiaCanvas.kt`

```kotlin
class SkiaCanvas(
    private val skia: SkiaLibrary,
    private val nativeCanvas: COpaquePointer?
) : DrawCanvas {

    override fun drawRect(x: Float, y: Float, w: Float, h: Float, color: UInt) {
        val paint = skia.paintNew.invoke()
        skia.paintSetColor.invoke(paint, color)
        skia.paintSetAntialias.invoke(paint, true)
        skia.canvasDrawRect.invoke(nativeCanvas, x, y, x + w, y + h, paint)
        skia.paintDelete.invoke(paint)
    }

    override fun drawText(text: String, x: Float, y: Float, color: UInt, size: Float) {
        // Use sk_canvas_draw_simple_text or sk_textblob_* APIs
        val paint = skia.paintNew.invoke()
        skia.paintSetColor.invoke(paint, color)
        // … set text size, font, draw …
        skia.paintDelete.invoke(paint)
    }

    // implement drawRoundRect, drawImage, clipRect, restore …
}
```

## 8.3 Frame Loop Integration

In `SkiaRenderer.kt`, after creating the Skia surface, call the widget tree:

```kotlin
fun renderFrame(rootWidget: Widget, width: Int, height: Int) {
    val canvas = SkiaCanvas(library, skia.surfaceGetCanvas.invoke(surface))
    LayoutEngine.layout(rootWidget, width.toFloat(), height.toFloat())
    rootWidget.draw(canvas, 0f, 0f, width.toFloat(), height.toFloat())
}
```

## 8.4 Trigger Repaints

In `Window.kt`, when a `State` changes it calls `invalidate()` → `requestRedraw()` on the root widget, which posts a `WM_PAINT`:

```kotlin
override fun requestRedraw() {
    InvalidateRect(hwnd, null, 0)
}
```

---

# Phase 9 — Event System

The event system makes the UI interactive. Win32 delivers input as window messages (`WM_LBUTTONDOWN`, `WM_KEYDOWN`, `WM_MOUSEMOVE`, etc.) — this phase translates those raw messages into typed `UiEvent` objects and routes them to the correct widget. Routing is done via **hit testing**: a recursive walk of the widget tree that finds the deepest widget whose bounding box (stored during the place pass in Phase 7) contains the cursor coordinates. Once the target widget is found, the event is dispatched to it and, if unhandled, bubbles up to the parent — the same propagation model used in the browser DOM and JavaFX. Focus management (keeping track of which widget receives keyboard events) is also handled here.

## 9.1 Event Types

`runtime/src/commonMain/kotlin/lienzo/runtime/events/Event.kt`

```kotlin
sealed class UiEvent {
    data class Click(val x: Float, val y: Float) : UiEvent()
    data class DoubleClick(val x: Float, val y: Float) : UiEvent()
    data class KeyDown(val keyCode: Int, val char: Char?) : UiEvent()
    data class KeyUp(val keyCode: Int) : UiEvent()
    data class MouseMove(val x: Float, val y: Float) : UiEvent()
    data class TextChanged(val value: String) : UiEvent()
    object Focus : UiEvent()
    object Blur : UiEvent()
}
```

## 9.2 Hit Testing

Walk the widget tree in reverse order (top widget first), find the deepest widget whose bounding box contains the cursor:

```kotlin
fun hitTest(root: Widget, x: Float, y: Float): Widget? {
    for (child in root.children.asReversed()) {
        val hit = hitTest(child, x, y)
        if (hit != null) return hit
    }
    return if (root.contains(x, y)) root else null
}
```

Each widget tracks its placed bounds:

```kotlin
abstract class Widget {
    var boundsX = 0f; var boundsY = 0f
    var boundsW = 0f; var boundsH = 0f

    fun contains(x: Float, y: Float) =
        x in boundsX..(boundsX + boundsW) && y in boundsY..(boundsY + boundsH)
}
```

## 9.3 Win32 → UiEvent Bridge

In `Window.kt`, translate Win32 messages to `UiEvent` and dispatch:

```kotlin
WM_LBUTTONDOWN -> {
    val x = GET_X_LPARAM(lParam).toFloat()
    val y = GET_Y_LPARAM(lParam).toFloat()
    val target = hitTest(rootWidget, x, y)
    target?.handleEvent(UiEvent.Click(x, y))
}

WM_KEYDOWN -> {
    val focused = focusedWidget
    focused?.handleEvent(UiEvent.KeyDown(wParam.toInt(), null))
}
```

## 9.4 Widget Event Handling

```kotlin
abstract class Widget {
    open fun handleEvent(event: UiEvent) {
        // default: bubble to parent
        parent?.handleEvent(event)
    }
}

class ButtonWidget(
    val text: String,
    val onClick: (() -> Unit)?
) : Widget() {

    override fun handleEvent(event: UiEvent) {
        if (event is UiEvent.Click) {
            onClick?.invoke()
        }
    }
}
```

---

# Phase 10 — Compiler CLI & Gradle Plugin

At this point the compiler pipeline (Phases 2–4) works but must be run manually. This phase automates it by wrapping the compiler in a Gradle plugin that scans `src/ui/` for `.lienzo` files, compiles each one to Kotlin, places the output in `build/generated/lienzo/`, and hooks that directory into the `compileKotlinNative` task as an additional source set. The end result is transparent: you edit a `.lienzo` file, run `./gradlew build`, and the updated UI is compiled and linked into the executable automatically — no manual steps. The plugin also declares proper Gradle `inputs`/`outputs` so incremental builds work: files that have not changed since the last build are skipped.

## 10.1 Compiler CLI Usage

After Phase 4, you can compile `.lienzo` files manually:

```bash
java -jar compiler.jar src/ui/MainWindow.lienzo build/generated/MainWindow.kt com.myapp.ui
```

## 10.2 Gradle Plugin

Create a Gradle plugin that runs the compiler on every `.lienzo` file and adds the generated sources to the native compilation:

`gradle-plugin/src/main/kotlin/lienzo/gradle/LienzoPlugin.kt`

```kotlin
class LienzoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("lienzo", LienzoExtension::class.java)

        val compileUi = project.tasks.register("compileLienzoUi") { task ->
            val uiSrcDir = project.file("src/ui")
            val outDir = project.layout.buildDirectory.dir("generated/lienzo")

            task.inputs.dir(uiSrcDir)
            task.outputs.dir(outDir)

            task.doLast {
                uiSrcDir.walkTopDown()
                    .filter { it.extension == "lienzo" }
                    .forEach { file ->
                        val outFile = outDir.get().file("${file.nameWithoutExtension}.kt").asFile
                        project.exec {
                            it.commandLine(
                                "java", "-jar", ext.compilerJar,
                                file.absolutePath,
                                outFile.absolutePath,
                                ext.packageName
                            )
                        }
                    }
            }
        }

        // Hook generated sources into the Kotlin Native compilation
        project.afterEvaluate {
            project.tasks.named("compileKotlinNative") {
                it.dependsOn(compileUi)
            }
        }
    }
}

open class LienzoExtension {
    var compilerJar: String = ""
    var packageName: String = "ui.generated"
}
```

## 10.3 Using the Plugin in `sample-app/build.gradle.kts`

```kotlin
plugins {
    id("lienzo.gradle-plugin")
}

lienzo {
    compilerJar = rootProject.file("compiler/build/libs/compiler.jar").absolutePath
    packageName = "com.myapp.ui"
}
```

---

# Phase 11 — Built-in Components

With the layout engine, renderer, and event system in place, you can now build the full catalogue of Lienzo components one by one. The order matters: start with the simplest display-only widgets (`Text`, `Image`) that depend only on `DrawCanvas`, then move to layout containers (`Column`, `Row`) that depend on the layout engine, then interactive widgets (`Button`, `TextBox`) that depend on the event system, and finally complex compound widgets (`Table`, `TabView`, `SplitPane`) that depend on everything. Each component is a self-contained `Widget` subclass — implement it, write a unit test for its layout math, visually verify it in the sample app, and then move to the next. Resist building all components at once; incremental delivery catches design mistakes early.

| Order | Component   | Depends On                  |
| ----- | ----------- | --------------------------- |
| 1     | Text        | DrawCanvas.drawText         |
| 2     | Column, Row | Layout engine               |
| 3     | Button      | Text + event system         |
| 4     | TextBox     | Text + keyboard events      |
| 5     | Image       | DrawCanvas.drawImage        |
| 6     | Spacer      | Layout engine grow          |
| 7     | Card        | DrawCanvas.drawRoundRect    |
| 8     | Table       | Column + Row + data binding |
| 9     | Sidebar     | Column + NavItem            |
| 10    | TabView     | Row + conditional rendering |
| 11    | SplitPane   | Row + drag events           |

### Example: TextWidget

```kotlin
class TextWidget(
    private val staticValue: String,
    private val binding: Binding<String>?,
    private val color: String,
    private val size: Int
) : Widget() {

    private val currentValue: String
        get() = binding?.read?.invoke() ?: staticValue

    init {
        // Re-draw whenever the bound state changes
        // (runtime calls invalidate() via the state observer)
    }

    override fun measure(maxWidth: Float, maxHeight: Float): Size {
        // Estimate: real impl uses Skia sk_font_measureText
        return Size(currentValue.length * size * 0.6f, size * 1.4f)
    }

    override fun place(x: Float, y: Float, width: Float, height: Float) {
        boundsX = x; boundsY = y; boundsW = width; boundsH = height
    }

    override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
        canvas.drawText(currentValue, boundsX, boundsY, parseColor(color), size.toFloat())
    }
}
```

---

# Phase 12 — Theming

Theming gives the application a consistent visual identity and allows users to switch between appearances (e.g., light and dark mode) at runtime without restarting. A `.theme` file defines a named palette of colors and a set of named style presets. The `ThemeRegistry` singleton holds all loaded themes and tracks the active one. Widgets read colors and style values from `ThemeRegistry.active` at draw time rather than hardcoding them, so switching the active theme and calling `rootWidget.invalidate()` is sufficient to repaint the entire UI in the new appearance. This phase should be implemented after Phase 11 because it requires every component to be done before you can audit which colors and styles need to be theme-aware.

## 12.1 Theme File Format

`themes/Dark.theme`

```html
<Theme name="dark">

    <Color name="bg"       value="#1e1e2e"/>
    <Color name="surface"  value="#313244"/>
    <Color name="primary"  value="#89b4fa"/>
    <Color name="text"     value="#cdd6f4"/>
    <Color name="muted"    value="#6c7086"/>

    <Style name="button.primary">
        bg="{primary}"
        color="{text}"
        radius="6"
        padding="10"
    </Style>

</Theme>
```

## 12.2 Theme Registry

```kotlin
object ThemeRegistry {
    private val themes = mutableMapOf<String, Theme>()
    var active: Theme = Theme.Default

    fun register(theme: Theme) { themes[theme.name] = theme }
    fun activate(name: String) { active = themes[name] ?: Theme.Default }
}

class Theme(
    val name: String,
    val colors: Map<String, UInt>,
    val styles: Map<String, StyleDef>
) {
    companion object {
        val Default = Theme("light", defaultColors(), emptyMap())
    }
}
```

## 12.3 Using the Theme in Widgets

```kotlin
override fun draw(canvas: DrawCanvas, x: Float, y: Float, width: Float, height: Float) {
    val bg = ThemeRegistry.active.colors["surface"] ?: 0xFFFFFFFFu
    canvas.drawRoundRect(boundsX, boundsY, boundsW, boundsH, radius = 8f, color = bg)
}
```

---

# Testing Strategy

| Layer        | What to Test                                | Tool            |
| ------------ | ------------------------------------------- | --------------- |
| Lexer        | Token sequence for known inputs             | JUnit (JVM)     |
| Parser       | AST shape for valid and invalid inputs      | JUnit (JVM)     |
| Codegen      | Generated Kotlin string matches snapshot    | JUnit (JVM)     |
| State        | Observer fires on change, not on same value | kotlin-test     |
| Layout       | Column/Row measure and place coordinates    | kotlin-test     |
| Hit Testing  | Correct widget returned for x, y coords     | kotlin-test     |
| Integration  | Run sample-app, visually verify window      | Manual / screenshot diff |

---

# Milestone Checklist

Work through these in order. Each milestone is a runnable state.

- [ ] **M1** — Lexer tokenizes a `<Button text="Save"/>` correctly
- [ ] **M2** — Parser produces an `ElementNode` tree from a full `.lienzo` file
- [ ] **M3** — Codegen outputs valid Kotlin from the AST
- [ ] **M4** — `State<T>` notifies observers on change
- [ ] **M5** — `TextWidget` and `ButtonWidget` compile and link into the native binary
- [ ] **M6** — `ColumnWidget` lays out two `TextWidget`s vertically with correct spacing
- [ ] **M7** — `SkiaCanvas` draws a rectangle and text on screen
- [ ] **M8** — Button click (Win32 `WM_LBUTTONDOWN`) invokes the `onClick` lambda
- [ ] **M9** — A `.lienzo` file is compiled by the Gradle plugin and runs as a desktop window
- [ ] **M10** — `State` change triggers `InvalidateRect` and the window redraws with the new value
- [ ] **M11** — All 12 built-in components render and respond to events
- [ ] **M12** — Dark theme applies globally via `ThemeRegistry`
