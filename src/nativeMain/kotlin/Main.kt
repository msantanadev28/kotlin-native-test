import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val renderer = SkiaRenderer()
    renderer.load()

    val window = Window(renderer)
    window.run()
}
