@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package ui

import SkiaRenderer
import Window

fun onButtonClick() {
    println("Button was clicked! Lienzo UI works successfully!")
}

fun main() {
    val renderer = SkiaRenderer()
    renderer.load()

    // Instantiate generated MainWindow widget tree
    val rootWidget = ui.generated.MainWindow()
    renderer.rootWidget = rootWidget

    val window = Window(renderer)
    window.run()
}
