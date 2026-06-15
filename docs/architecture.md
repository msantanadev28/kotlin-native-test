# Architecture Documentation

A Kotlin/Native Win32 application that renders Skia graphics (concentric circles) in a native Windows window using direct Skia C API interop via `libSkiaSharp.dll`.

---

## File Summary

| File | Class(es) | Role |
|------|-----------|------|
| `Main.kt` | — (top-level `main`) | Entry point |
| `Window.kt` | `Window` + top-level `windowProc` | Win32 window lifecycle |
| `SkiaRenderer.kt` | `SkiaRenderer` | Rendering orchestration |
| `BackBuffer.kt` | `BackBuffer` | GDI off-screen bitmap |
| `SkiaLibrary.kt` | `SkiaLibrary` | Skia DLL function pointers |

---

## Main.kt — Entry Point

**Purpose:** Bootstrap the application — create the renderer, load Skia, and run the window message loop.

### `fun main()`

```kotlin
@OptIn(ExperimentalForeignApi::class)
fun main() {
    val renderer = SkiaRenderer()
    renderer.load()

    val window = Window(renderer)
    window.run()
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Application entry point |
| **Intention** | Wire together the three core components: `SkiaLibrary` (via `SkiaRenderer.load()`), `SkiaRenderer`, and `Window` |
| **Description** | Creates a `SkiaRenderer`, loads the Skia native library, then hands control to the `Window` message loop. The `window.run()` call blocks until `PostQuitMessage(0)` is received. |

---

## SkiaLibrary.kt — Skia Function Pointer Loader

**Purpose:** Load `libSkiaSharp.dll` and resolve every Skia C function needed for rendering.

### `class SkiaLibrary`

**Properties**

| Property | Type | Description |
|----------|------|-------------|
| `handle` | `HMODULE?` | Module handle from `LoadLibraryA` |
| `paintNew` | `CPointer<CFunction<() -> COpaquePointer?>>` | `sk_paint_new` |
| `paintDelete` | `CPointer<CFunction<(COpaquePointer?) -> Unit>>` | `sk_paint_delete` |
| `paintSetColor` | `CPointer<CFunction<(COpaquePointer?, UInt) -> Unit>>` | `sk_paint_set_color` |
| `paintSetAntialias` | `CPointer<CFunction<(COpaquePointer?, Boolean) -> Unit>>` | `sk_paint_set_antialias` |
| `canvasClear` | `CPointer<CFunction<(COpaquePointer?, UInt) -> Unit>>` | `sk_canvas_clear` |
| `canvasDrawCircle` | `CPointer<CFunction<(COpaquePointer?, Float, Float, Float, COpaquePointer?) -> Unit>>` | `sk_canvas_draw_circle` |
| `surfaceGetCanvas` | `CPointer<CFunction<(COpaquePointer?) -> COpaquePointer?>>` | `sk_surface_get_canvas` |
| `surfaceNewRasterDirect` | `CPointer<CFunction<(COpaquePointer?, COpaquePointer?, Long, COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?>>` | `sk_surface_new_raster_direct` |
| `surfaceUnref` | `CPointer<CFunction<(COpaquePointer?) -> Unit>>` | `sk_surface_unref` |

#### `fun load()`

```kotlin
fun load() {
    handle = LoadLibraryA("libSkiaSharp.dll")
    if (handle == null) {
        error("Unable to load libSkiaSharp.dll. Build the project first so the runtime is staged next to the executable.")
    }
    paintNew = loadFunction("sk_paint_new")
    paintDelete = loadFunction("sk_paint_delete")
    // ...
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Load the Skia native DLL and resolve all function pointers |
| **Intention** | Discover every C function needed at runtime and assign it to a typed `CPointer<CFunction<...>>` field |
| **Description** | Calls `LoadLibraryA("libSkiaSharp.dll")` to load the SkiaSharp native asset, then resolves nine Skia C API symbols via `GetProcAddress` and stores them as typed function pointers. |

#### `fun <T : CPointed> loadFunction(name: String): CPointer<T>`

```kotlin
fun <T : CPointed> loadFunction(name: String): CPointer<T> {
    val library = handle ?: error("Skia library was not loaded")
    val symbol = GetProcAddress(library, name) ?: error("Unable to find Skia symbol '$name'")
    return symbol.reinterpret()
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Resolve a single symbol from the loaded library |
| **Intention** | Generic helper to `GetProcAddress` + `reinterpret` a `COpaquePointer` into a typed function pointer |
| **Description** | Takes the exported symbol name, looks it up in the loaded library via `GetProcAddress`, and reinterprets the returned `COpaquePointer` to the desired `CPointer<T>` type. |

---

## BackBuffer.kt — GDI Off-Screen Buffer

**Purpose:** Manage a Win32 GDI memory device context backed by a DIB section that Skia renders into.

### `class BackBuffer`

**Properties**

| Property | Type | Access | Description |
|----------|------|--------|-------------|
| `dc` | `HDC?` | public get / private set | Memory device context |
| `bitmap` | `HBITMAP?` | public get / private set | DIB section bitmap selected into the DC |
| `previousBitmap` | `HGDIOBJ?` | public get / private set | Previously selected bitmap (for restore on dispose) |
| `pixels` | `COpaquePointer?` | public get / private set | Pointer to the raw pixel memory of the DIB section |
| `width` | `Int` | public get / private set | Buffer width in pixels |
| `height` | `Int` | public get / private set | Buffer height in pixels |

#### `fun recreate(w: Int, h: Int)`

```kotlin
fun recreate(w: Int, h: Int) {
    dispose()
    width = w
    height = h
    val memoryDc = CreateCompatibleDC(null) ?: error("CreateCompatibleDC failed with ${GetLastError()}")
    memScoped {
        val bitmapInfo = alloc<BITMAPINFO>()
        bitmapInfo.bmiHeader.biSize = sizeOf<BITMAPINFOHEADER>().toUInt()
        bitmapInfo.bmiHeader.biWidth = w
        bitmapInfo.bmiHeader.biHeight = -h
        bitmapInfo.bmiHeader.biPlanes = 1u
        bitmapInfo.bmiHeader.biBitCount = 32u
        bitmapInfo.bmiHeader.biCompression = BI_RGB.toUInt()

        val pixelsPointer = alloc<COpaquePointerVar>()
        val newBitmap = CreateDIBSection(memoryDc, bitmapInfo.ptr, DIB_RGB_COLORS.toUInt(), pixelsPointer.ptr, null, 0u)
            ?: error("CreateDIBSection failed with ${GetLastError()}")

        val selectedObject = SelectObject(memoryDc, newBitmap)
        dc = memoryDc
        bitmap = newBitmap
        previousBitmap = selectedObject
        pixels = pixelsPointer.value
    }
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Create or recreate the off-screen GDI buffer at the given size |
| **Intention** | Set up a 32-bit BGRA DIB section compatible with the screen, select it into a memory DC, and expose the raw pixel pointer for Skia to draw into |
| **Description** | Disposes any existing buffer, creates a compatible DC, allocates a `BITMAPINFOHEADER` with negative height (top-down bitmap), creates a DIB section via `CreateDIBSection`, selects it into the DC, and stores all handles plus the raw pixel pointer. |

#### `fun dispose()`

```kotlin
fun dispose() {
    if (dc != null && previousBitmap != null) {
        SelectObject(dc, previousBitmap)
    }
    bitmap?.let { DeleteObject(it) }
    dc?.let { DeleteDC(it) }
    dc = null; bitmap = null; previousBitmap = null; pixels = null; width = 0; height = 0
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Release all GDI resources |
| **Intention** | Restore the original bitmap, delete the DIB section, delete the DC, and null out all state |
| **Description** | Selects the original bitmap back into the DC (cleanup protocol), deletes the DIB section bitmap and the memory DC, then resets all fields to their default values. |

---

## SkiaRenderer.kt — Rendering Orchestrator

**Purpose:** Coordinate Skia surface creation, scene drawing, and blitting to the screen via `SkiaLibrary` and `BackBuffer`.

### `class SkiaRenderer`

**Properties**

| Property | Type | Description |
|----------|------|-------------|
| `skia` | `SkiaLibrary` | Public reference to the Skia function pointer loader |
| `backBuffer` | `BackBuffer` | Private GDI back buffer |
| `surface` | `COpaquePointer?` | Private Skia raster surface handle |

#### `fun load()`

```kotlin
fun load() {
    skia.load()
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Delegate loading of the Skia library |
| **Intention** | Thin wrapper so the consumer only deals with `SkiaRenderer`, not `SkiaLibrary` directly |
| **Description** | Forwards the call to `skia.load()` to initialize all Skia function pointers. |

#### `fun resize(windowHandle: HWND?)`

```kotlin
fun resize(windowHandle: HWND?) {
    val clientRect = memScoped {
        val rect = alloc<RECT>()
        if (GetClientRect(windowHandle, rect.ptr) == 0) return
        Pair(rect.right - rect.left, rect.bottom - rect.top)
    }
    if (clientRect.first <= 0 || clientRect.second <= 0) {
        disposeBackBuffer(); return
    }
    if (clientRect.first == backBuffer.width && clientRect.second == backBuffer.height && surface != null) {
        return
    }
    backBuffer.recreate(clientRect.first, clientRect.second)
    surface = createRasterSurface(backBuffer.pixels)
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Handle `WM_SIZE` — resize the Skia surface and back buffer to match the client area |
| **Intention** | Query the client rectangle, skip or dispose if degenerate, early-out if size hasn't changed, otherwise recreate the GDI back buffer and a new Skia raster surface on top of it |
| **Description** | Calls `GetClientRect` to get the new client dimensions. If the area is non-positive, disposes everything. If the size is unchanged and a surface already exists, does nothing. Otherwise recreates the `BackBuffer` at the new size and creates a new Skia raster surface pointing at the back buffer's pixel memory. |

#### `fun paint(windowHandle: HWND?)`

```kotlin
fun paint(windowHandle: HWND?) {
    if (surface == null) resize(windowHandle)
    memScoped {
        val paintStruct = alloc<PAINTSTRUCT>()
        val targetDc = BeginPaint(windowHandle, paintStruct.ptr) ?: return
        drawScene()
        if (backBuffer.dc != null && backBuffer.width > 0 && backBuffer.height > 0) {
            BitBlt(targetDc, 0, 0, backBuffer.width, backBuffer.height, backBuffer.dc, 0, 0, SRCCOPY.toUInt())
        }
        EndPaint(windowHandle, paintStruct.ptr)
    }
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Handle `WM_PAINT` — draw the scene to the back buffer, then blit to the screen |
| **Intention** | Ensure a surface exists, begin painting, draw the Skia scene into the back buffer's pixel memory, blit the result to the target DC, then end painting |
| **Description** | Lazily calls `resize` if no surface exists yet. Opens a paint context via `BeginPaint`, renders the scene into the Skia surface (which writes directly into the DIB pixel memory), copies the whole back buffer to the target DC with `BitBlt`/`SRCCOPY`, then closes the paint context. |

#### `fun dispose()`

```kotlin
fun dispose() {
    disposeBackBuffer()
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Clean up all rendering resources |
| **Intention** | Triggered on `WM_DESTROY` — unref the Skia surface and dispose the GDI back buffer |
| **Description** | Delegates to `disposeBackBuffer()` which unrefs the Skia surface and calls `backBuffer.dispose()`. |

#### `private fun createRasterSurface(pixelMemory: COpaquePointer?): COpaquePointer?`

```kotlin
private fun createRasterSurface(pixelMemory: COpaquePointer?): COpaquePointer? {
    return memScoped {
        val imageInfoWords = allocArray<IntVar>(6)
        imageInfoWords[0] = 0
        imageInfoWords[1] = 0
        imageInfoWords[2] = backBuffer.width
        imageInfoWords[3] = backBuffer.height
        imageInfoWords[4] = skiaColorTypeBgra8888  // 6
        imageInfoWords[5] = skiaAlphaTypePremul     // 2
        skia.surfaceNewRasterDirect(
            imageInfoWords.reinterpret(),
            pixelMemory,
            backBuffer.width * 4L,
            null, null, null,
        )
    }
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Create a Skia raster surface wrapping the DIB section pixel memory |
| **Intention** | Pack Skia image info (width, height, color type, alpha type) into a raw `int[6]` C array, then call `sk_surface_new_raster_direct` to create a surface that draws directly into the back buffer's pixel memory |
| **Description** | Allocates a 6-element `IntVar` array representing the Skia `sk_imageinfo_t` struct (offset_x, offset_y, width, height, colorType, alphaType). Reinterprets it as `COpaquePointer` and passes it along with the pixel pointer, row bytes (width * 4 for 32-bit BGRA), and null for color space, surface properties, and release proc. |

#### `private fun drawScene()`

```kotlin
private fun drawScene() {
    val currentSurface = surface ?: return
    val canvas = skia.surfaceGetCanvas(currentSurface) ?: return
    skia.canvasClear(canvas, colorWhite)
    val paint = skia.paintNew()
    try {
        skia.paintSetAntialias(paint, true)
        skia.paintSetColor(paint, colorBlue)
        val centerX = backBuffer.width / 2f
        val centerY = backBuffer.height / 2f
        val outerRadius = min(backBuffer.width, backBuffer.height) * 0.3f
        val innerRadius = outerRadius * 0.45f
        skia.canvasDrawCircle(canvas, centerX, centerY, outerRadius, paint)
        skia.paintSetColor(paint, colorRed)
        skia.canvasDrawCircle(canvas, centerX, centerY, innerRadius, paint)
    } finally {
        skia.paintDelete(paint)
    }
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Render the concentric circles scene into the Skia surface |
| **Intention** | Clear to white, create a paint, draw a large blue circle, then draw a smaller red circle on top, then delete the paint |
| **Description** | Gets the Skia canvas from the current surface, clears it to white, creates a Skia paint with antialiasing enabled, draws an outer blue circle at the center (radius = 30% of the smaller dimension), then switches to red and draws an inner circle (radius = 45% of the outer radius). The paint is always deleted in the `finally` block to prevent leaks. |

#### `private fun disposeBackBuffer()`

```kotlin
private fun disposeBackBuffer() {
    surface?.let {
        skia.surfaceUnref(it)
        surface = null
    }
    backBuffer.dispose()
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Release the Skia surface and back buffer |
| **Intention** | Properly unref the Skia surface (decrement ref count), then dispose the GDI back buffer |
| **Description** | Calls `sk_surface_unref` on the surface handle, nulls it, then delegates to `backBuffer.dispose()` to clean up the GDI DC and DIB section. |

---

## Window.kt — Win32 Window Management

**Purpose:** Register a window class, create an overlapped window, and run the message pump. Routes WM_SIZE / WM_PAINT / WM_DESTROY to the `SkiaRenderer`.

### `class Window`

**Constructor**

```kotlin
class Window(private val renderer: SkiaRenderer)
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Hold a reference to the renderer and execute the Win32 window lifecycle |
| **Intention** | The renderer is passed in from `main()` and stored as a private field |

#### `fun run()`

```kotlin
fun run() {
    activeRenderer = renderer
    memScoped {
        val instance = GetModuleHandleA(null) ?: error("GetModuleHandleA failed")
        val windowClassNamePointer = windowClassName.cstr.getPointer(this)

        val windowClassDefinition = alloc<WNDCLASSEXA>()
        windowClassDefinition.cbSize = sizeOf<WNDCLASSEXA>().toUInt()
        windowClassDefinition.style = (CS_HREDRAW or CS_VREDRAW).toUInt()
        windowClassDefinition.lpfnWndProc = staticCFunction(::windowProc)
        windowClassDefinition.hInstance = instance
        windowClassDefinition.hCursor = LoadCursorW(null, IDC_ARROW)
        windowClassDefinition.hbrBackground = null
        windowClassDefinition.lpszClassName = windowClassNamePointer

        check(RegisterClassExA(windowClassDefinition.ptr) != 0.toUShort()) {
            "RegisterClassExA failed with ${GetLastError()}"
        }

        val windowHandle = CreateWindowExA(
            0u, windowClassName, windowTitle,
            (WS_OVERLAPPEDWINDOW or WS_VISIBLE).toUInt(),
            CW_USEDEFAULT, CW_USEDEFAULT, initialWindowWidth, initialWindowHeight,
            null, null, instance, null,
        ) ?: error("CreateWindowExA failed with ${GetLastError()}")

        ShowWindow(windowHandle, SW_SHOWDEFAULT)
        UpdateWindow(windowHandle)

        val message = alloc<MSG>()
        while (GetMessageA(message.ptr, null, 0u, 0u) > 0) {
            TranslateMessage(message.ptr)
            DispatchMessageA(message.ptr)
        }
    }
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Register the window class, create the window, and run the message loop |
| **Intention** | Set the global `activeRenderer` for the C callback, register a `WNDCLASSEXA` with the custom `windowProc`, create a visible overlapped window with default position and size (720×540), show it, then pump messages until `PostQuitMessage` is received |
| **Description** | Gets the HINSTANCE, converts the class name to a C string, fills in `WNDCLASSEXA` fields (class style with CS_HREDRAW \| CS_VREDRAW, window proc via `staticCFunction`, null background brush so Skia owns the rendering), registers it, creates the window, shows + updates it, then enters the `GetMessageA` / `TranslateMessage` / `DispatchMessageA` loop. The loop exits when `GetMessageA` returns 0 (WM_QUIT). |

### Top-Level `windowProc`

```kotlin
@OptIn(ExperimentalForeignApi::class)
private fun windowProc(windowHandle: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    val renderer = activeRenderer ?: return DefWindowProcA(windowHandle, message, wParam, lParam)
    when (message.toInt()) {
        WM_SIZE   -> { renderer.resize(windowHandle); InvalidateRect(windowHandle, null, 0); return 0 }
        WM_PAINT  -> { renderer.paint(windowHandle); return 0 }
        WM_DESTROY -> { renderer.dispose(); PostQuitMessage(0); return 0 }
    }
    return DefWindowProcA(windowHandle, message, wParam, lParam)
}
```

| Aspect | Detail |
|--------|--------|
| **Purpose** | Static Win32 window procedure that dispatches messages to the active renderer |
| **Intention** | A top-level function (required for `staticCFunction`) that reads the global `activeRenderer` and routes `WM_SIZE`, `WM_PAINT`, and `WM_DESTROY` to the appropriate `SkiaRenderer` methods. All other messages fall through to `DefWindowProcA`. |
| **Description** | On `WM_SIZE`: resizes the renderer and forces a repaint via `InvalidateRect`. On `WM_PAINT`: calls `renderer.paint()` which handles `BeginPaint`/`EndPaint`. On `WM_DESTROY`: disposes the renderer and posts `WM_QUIT` via `PostQuitMessage(0)`. |

### File-Level Constants

```kotlin
private const val windowClassName  = "SkiaCircleSampleWindow"
private const val windowTitle      = "Kotlin Native Skia Sample"
private const val initialWindowWidth  = 720
private const val initialWindowHeight = 540
```

| Constant | Value | Usage |
|----------|-------|-------|
| `windowClassName` | `"SkiaCircleSampleWindow"` | Registered atom and `CreateWindowExA` class name |
| `windowTitle` | `"Kotlin Native Skia Sample"` | Window caption |
| `initialWindowWidth` | `720` | Default window width |
| `initialWindowHeight` | `540` | Default window height |

### Global Bridge Variable

```kotlin
private var activeRenderer: SkiaRenderer? = null
```

**Purpose:** Bridge between the instance-based `Window` class and the C-compatible static `windowProc` function. Set by `Window.run()` before the message loop starts. Guarded by `?: DefWindowProcA(...)` in the proc so that messages received before the assignment fall through safely.

---

## Data Flow Diagram

```
main()
  |
  +--> SkiaRenderer.load()
  |       +--> SkiaLibrary.load()
  |              +--> LoadLibraryA("libSkiaSharp.dll")
  |              +--> GetProcAddress() x 9
  |
  +--> Window(renderer).run()
         +--> RegisterClassExA()  -- windowProc --> SkiaRenderer.resize()
         +--> CreateWindowExA()                    SkiaRenderer.paint()
         +--> ShowWindow()                         SkiaRenderer.dispose()
         +--> UpdateWindow()
         +--> GetMessageA / TranslateMessage / DispatchMessageA
                |
                +--> WM_SIZE  ---> SkiaRenderer.resize()
                |                   +--> GetClientRect()
                |                   +--> BackBuffer.recreate(w, h)
                |                   |       +--> CreateCompatibleDC()
                |                   |       +--> CreateDIBSection()
                |                   |       +--> SelectObject()
                |                   +--> createRasterSurface(pixels)
                |                           +--> sk_surface_new_raster_direct()
                |
                +--> WM_PAINT ---> SkiaRenderer.paint()
                |                   +--> BeginPaint()
                |                   +--> drawScene()
                |                   |       +--> sk_surface_get_canvas()
                |                   |       +--> sk_canvas_clear()
                |                   |       +--> sk_paint_new()
                |                   |       +--> sk_canvas_draw_circle() x2
                |                   |       +--> sk_paint_delete()
                |                   +--> BitBlt()   (back buffer -> screen)
                |                   +--> EndPaint()
                |
                +--> WM_DESTROY -> SkiaRenderer.dispose()
                                   +--> sk_surface_unref()
                                   +--> BackBuffer.dispose()
                                   |       +--> SelectObject() (restore)
                                   |       +--> DeleteObject()
                                   |       +--> DeleteDC()
                                   +--> PostQuitMessage(0)
```

---

## Skia Image Info Layout

The `int[6]` array passed to `sk_surface_new_raster_direct` represents:

| Index | Field | Value |
|-------|-------|-------|
| 0 | offset_x | 0 |
| 1 | offset_y | 0 |
| 2 | width | `backBuffer.width` |
| 3 | height | `backBuffer.height` |
| 4 | colorType | `6` = `kBGRA_8888_SkColorType` |
| 5 | alphaType | `2` = `kPremul_SkAlphaType` |
