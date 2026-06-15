import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import platform.windows.GetProcAddress
import platform.windows.HMODULE
import platform.windows.LoadLibraryA

@OptIn(ExperimentalForeignApi::class)
class SkiaLibrary {
    var handle: HMODULE? = null
        private set

    lateinit var paintNew: CPointer<CFunction<() -> COpaquePointer?>>
    lateinit var paintDelete: CPointer<CFunction<(COpaquePointer?) -> Unit>>
    lateinit var paintSetColor: CPointer<CFunction<(COpaquePointer?, UInt) -> Unit>>
    lateinit var paintSetAntialias: CPointer<CFunction<(COpaquePointer?, Boolean) -> Unit>>
    lateinit var canvasClear: CPointer<CFunction<(COpaquePointer?, UInt) -> Unit>>
    lateinit var canvasDrawCircle: CPointer<CFunction<(COpaquePointer?, Float, Float, Float, COpaquePointer?) -> Unit>>
    lateinit var surfaceGetCanvas: CPointer<CFunction<(COpaquePointer?) -> COpaquePointer?>>
    lateinit var surfaceNewRasterDirect: CPointer<CFunction<(COpaquePointer?, COpaquePointer?, Long, COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?>>
    lateinit var surfaceUnref: CPointer<CFunction<(COpaquePointer?) -> Unit>>

    fun load() {
        handle = LoadLibraryA("libSkiaSharp.dll")
        if (handle == null) {
            error("Unable to load libSkiaSharp.dll. Build the project first so the runtime is staged next to the executable.")
        }

        paintNew = loadFunction("sk_paint_new")
        paintDelete = loadFunction("sk_paint_delete")
        paintSetColor = loadFunction("sk_paint_set_color")
        paintSetAntialias = loadFunction("sk_paint_set_antialias")
        canvasClear = loadFunction("sk_canvas_clear")
        canvasDrawCircle = loadFunction("sk_canvas_draw_circle")
        surfaceGetCanvas = loadFunction("sk_surface_get_canvas")
        surfaceNewRasterDirect = loadFunction("sk_surface_new_raster_direct")
        surfaceUnref = loadFunction("sk_surface_unref")
    }

    fun <T : CPointed> loadFunction(name: String): CPointer<T> {
        val library = handle ?: error("Skia library was not loaded")
        val symbol = GetProcAddress(library, name) ?: error("Unable to find Skia symbol '$name'")
        return symbol.reinterpret()
    }
}
