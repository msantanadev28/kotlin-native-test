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
    lateinit var paintSetStyle: CPointer<CFunction<(COpaquePointer?, Int) -> Unit>>
    lateinit var paintSetStrokeWidth: CPointer<CFunction<(COpaquePointer?, Float) -> Unit>>
    lateinit var canvasClear: CPointer<CFunction<(COpaquePointer?, UInt) -> Unit>>
    lateinit var canvasDrawCircle: CPointer<CFunction<(COpaquePointer?, Float, Float, Float, COpaquePointer?) -> Unit>>
    lateinit var canvasDrawRect: CPointer<CFunction<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit>>
    lateinit var canvasDrawRoundRect: CPointer<CFunction<(COpaquePointer?, COpaquePointer?, Float, Float, COpaquePointer?) -> Unit>>
    lateinit var canvasDrawSimpleText: CPointer<CFunction<(COpaquePointer?, kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?, Long, Int, Float, Float, COpaquePointer?, COpaquePointer?) -> Unit>>
    lateinit var fontNew: CPointer<CFunction<() -> COpaquePointer?>>
    lateinit var fontDelete: CPointer<CFunction<(COpaquePointer?) -> Unit>>
    lateinit var fontSetSize: CPointer<CFunction<(COpaquePointer?, Float) -> Unit>>
    lateinit var fontSetTypeface: CPointer<CFunction<(COpaquePointer?, COpaquePointer?) -> Unit>>
    lateinit var typefaceCreateFromName: CPointer<CFunction<(kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?, COpaquePointer?) -> COpaquePointer?>>
    lateinit var typefaceUnref: CPointer<CFunction<(COpaquePointer?) -> Unit>>
    lateinit var surfaceGetCanvas: CPointer<CFunction<(COpaquePointer?) -> COpaquePointer?>>
    lateinit var surfaceNewRasterDirect: CPointer<CFunction<(COpaquePointer?, COpaquePointer?, Long, COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?>>
    lateinit var surfaceUnref: CPointer<CFunction<(COpaquePointer?) -> Unit>>
    lateinit var fontMeasureText: CPointer<CFunction<(COpaquePointer?, COpaquePointer?, Long, Int, COpaquePointer?, COpaquePointer?) -> Float>>

    fun load() {
        handle = LoadLibraryA("libSkiaSharp.dll")
        if (handle == null) {
            error("Unable to load libSkiaSharp.dll. Build the project first so the runtime is staged next to the executable.")
        }

        paintNew = loadFunction("sk_paint_new")
        paintDelete = loadFunction("sk_paint_delete")
        paintSetColor = loadFunction("sk_paint_set_color")
        paintSetAntialias = loadFunction("sk_paint_set_antialias")
        paintSetStyle = loadFunction("sk_paint_set_style")
        paintSetStrokeWidth = loadFunction("sk_paint_set_stroke_width")
        canvasClear = loadFunction("sk_canvas_clear")
        canvasDrawCircle = loadFunction("sk_canvas_draw_circle")
        canvasDrawRect = loadFunction("sk_canvas_draw_rect")
        canvasDrawRoundRect = loadFunction("sk_canvas_draw_round_rect")
        canvasDrawSimpleText = loadFunction("sk_canvas_draw_simple_text")
        fontNew = loadFunction("sk_font_new")
        fontDelete = loadFunction("sk_font_delete")
        fontSetSize = loadFunction("sk_font_set_size")
        fontSetTypeface = loadFunction("sk_font_set_typeface")
        typefaceCreateFromName = loadFunction("sk_typeface_create_from_name")
        typefaceUnref = loadFunction("sk_typeface_unref")
        surfaceGetCanvas = loadFunction("sk_surface_get_canvas")
        surfaceNewRasterDirect = loadFunction("sk_surface_new_raster_direct")
        surfaceUnref = loadFunction("sk_surface_unref")
        fontMeasureText = loadFunction("sk_font_measure_text")
    }

    fun <T : CPointed> loadFunction(name: String): CPointer<T> {
        val library = handle ?: error("Skia library was not loaded")
        val symbol = GetProcAddress(library, name) ?: error("Unable to find Skia symbol '$name'")
        return symbol.reinterpret()
    }
}
