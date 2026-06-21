@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package ui

import SkiaRenderer
import Window
import lienzo.runtime.state

val counter = state(0)
val emailText = state("")
val commentText = state("")
val labelText = state("No input yet")

fun onButtonClick() {
    counter.value++
    labelText.value = "Email: ${emailText.value} | Comment: ${commentText.value.replace("\n", " ")}"
    println("Button clicked! Counter value: ${counter.value}, Label text: ${labelText.value}")
}

fun onResetClick() {
    counter.value = 0
    emailText.value = ""
    commentText.value = ""
    labelText.value = "No input yet"
    println("Counter reset!")
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
    emailText.observe {
        println("Email typed: ${emailText.value}")
        rootWidget.invalidate()
    }
    commentText.observe {
        println("Comment typed: ${commentText.value}")
        rootWidget.invalidate()
    }
    labelText.observe {
        rootWidget.invalidate()
    }

    val window = Window(renderer)
    window.run()
}

