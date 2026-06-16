@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package ui

import SkiaRenderer
import Window
import lienzo.runtime.state

val counter = state(0)

fun onButtonClick() {
    counter.value++
    println("Button clicked! Counter value: ${counter.value}")
}

fun main() {
    val renderer = SkiaRenderer()
    renderer.load()

    // Instantiate generated MainWindow widget tree
    val rootWidget = ui.generated.MainWindow()
    renderer.rootWidget = rootWidget

    counter.observe {
        rootWidget.invalidate()
    }

    val window = Window(renderer)
    window.run()
}

