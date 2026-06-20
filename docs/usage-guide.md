# Lienzo UI — Reference & Usage Guide

This guide provides comprehensive documentation on the markup language, layout widgets, event handlers, reactivity bindings, and bootstrapping mechanisms available in the **Lienzo UI** library.

---

## 📋 Table of Contents

1. [Project Directory Architecture](#project-directory-architecture)
2. [Lienzo Markup Language (`.lienzo`) Reference](#lienzo-markup-language-lienzo-reference)
   - [Document Hierarchy](#document-hierarchy)
   - [Attribute Types](#attribute-types)
   - [Layout Containers in Detail (Columns, Rows, Grids, Stacks, Cards)](#layout-containers-in-detail)
   - [Display & Interactive Widgets (Labels, Buttons, Spacers)](#display--interactive-widgets)
3. [State Reactivity & Property Bindings](#state-reactivity--property-bindings)
4. [Event Bindings & Callback Functions](#event-bindings--callback-functions)
5. [Full Bootstrap Code Example](#full-bootstrap-code-example)
6. [Compilation & Execution commands](#compilation--execution-commands)

---

## 📁 Project Directory Architecture

Lienzo UI splits code into compile-time templates and runtime Native code:

```
kotlin-native-test/
├── src/
│   ├── ui/
│   │   └── MainWindow.lienzo    ← Design layouts here
│   └── nativeMain/kotlin/
│       ├── Main.kt              ← Application entry point, states, and logic
│       └── lienzo/
│           ├── renderer/        ← Skia Canvas wrapper
│           ├── runtime/         ← UI widgets, layouts, state tree structure
```

*Note: Generating the UI build produces a Kotlin source file at `build/generated/lienzo/MainWindow.kt` containing an instantiated Kotlin layout builder.*

---

## ✍️ Lienzo Markup Language (`.lienzo`) Reference

### Document Hierarchy
Every `.lienzo` file must have a single root `<Window>` tag. Inside this container, you layout child widgets hierarchically:

```xml
<Window title="App Window" width="800" height="600" theme="mica">
    <Column padding="16" spacing="10">
        <!-- Child components go here -->
    </Column>
</Window>
```

### Attribute Types
The parser distinguishes three types of values:
1.  **Static Strings:** Regular values like `title="Button App"` or `align="center"`.
2.  **Primitive Numbers & Booleans:** Evaluated directly in Kotlin, e.g. `spacing="16"`, `width="500"`, `enabled="false"`.
3.  **Dynamic Bindings:** Wrapped in `{bind { <Kotlin Expression> }}` blocks. This binds the property directly to a reactive state.

---

### Root Window Component

#### `<Window>`
The absolute root element of every `.lienzo` template file.
*   **Properties:**
    *   `title` (String): The title of the window shown in the OS window frame. **Required.**
    *   `width` (Int): The initial width of the window in pixels. **Required.**
    *   `height` (Int): The initial height of the window in pixels. **Required.**
    *   `theme` (String): The visual theme of the window. Valid values:
        *   `"mica"`: Activates the translucent mica window background style.
        *   `""` (default/empty): Renders a standard gray-100 theme background (`0xFFF3F4F6u`).
*   **Example:**
    ```xml
    <Window title="My App Dashboard" width="1024" height="768" theme="mica">
        <!-- Root Container -->
    </Window>
    ```

### Common Container Styling Properties
All container elements (`<Column>`, `<Row>`, `<Grid>`, `<Stack>`, `<Card>`, `<FlexBox>`) support common properties to draw backgrounds and modify shapes:
*   **`backgroundColor`** (String): Fills the container background. Supports standard CSS hex codes (e.g. `"#FF0000"`, `"#2563EB"`) or standard Kotlin Native hexes (e.g. `"0xFFFFFFFF"`). Default is `""` (no background).
*   **`cornerRadius`** (Int): Sets the rounding radius in pixels for container corners. Default is `0`.
*   **`borderColor`** (String): Color of the outline border (e.g. `"#000000"`, `"0xFF00FF00"`). Default is `""` (no border).
*   **`borderThickness`** (Int): Inset border outline width in pixels. Requires `borderColor` to be visible. Default is `0`.

---

### Layout Containers in Detail

#### 1. `<Column>`
Arranges child elements sequentially in a vertical stack.
*   **Properties:**
    *   `spacing` (Int): Vertical margin in pixels inserted between each child. Default is `0`.
    *   `padding` (Int): Inset border in pixels surrounding the inside boundary of the column. Default is `0`.
    *   `align` (String): Horizontal layout alignment for children. Valid values:
        *   `"start"` (default): Left aligns all children.
        *   `"center"`: Centers children horizontally.
        *   `"end"`: Right aligns all children.
    *   `grow` (Int): Specifies vertical flex-ratio when nested inside another layout. Default is `0`.
*   **Example:**
    ```xml
    <Column padding="20" spacing="15" align="center" grow="1">
        <Label text="Centered Item 1"/>
        <Label text="Centered Item 2"/>
    </Column>
    ```

#### 2. `<Row>`
Arranges child elements sequentially in a horizontal line.
*   **Properties:**
    *   `spacing` (Int): Horizontal margin in pixels inserted between each child. Default is `0`.
    *   `padding` (Int): Inset border in pixels surrounding the inside boundary of the row. Default is `0`.
    *   `align` (String): Vertical alignment of children within the row height. Valid values:
        *   `"start"` (default): Top aligns all children.
        *   `"center"`: Centers children vertically.
        *   `"end"`: Bottom aligns all children.
    *   `grow` (Int): Horizontal flex-ratio when nested inside another layout. Default is `0`.
*   **Example:**
    ```xml
    <Row padding="10" spacing="8" align="center" grow="1">
        <Button text="Left Action"/>
        <Button text="Right Action"/>
    </Row>
    ```

#### 3. `<Grid>`
Organizes child elements into a uniform tabular structure.
*   **Properties:**
    *   `columns` (Int): The number of grid columns. Default is `1`.
    *   `rows` (Int): Fixed rows count. If `0` or omitted, rows are dynamically calculated. Default is `0`.
    *   `spacing` (Int): Margin in pixels around grid cells. Default is `0`.
    *   `grow` (Int): Flex-ratio. Default is `0`.
*   **Example:**
    ```xml
    <Grid columns="3" rows="2" spacing="12" grow="1">
        <Card><Button text="A"/></Card>
        <Card><Button text="B"/></Card>
        <Card><Button text="C"/></Card>
        <Card><Button text="D"/></Card>
        <Card><Button text="E"/></Card>
        <Card><Button text="F"/></Card>
    </Grid>
    ```

#### 4. `<Stack>`
Overlays child elements on top of each other. The layout size bounds are determined by the largest child.
*   **Properties:**
    *   `grow` (Int): Flex-ratio. Default is `0`.
*   **Example:**
    ```xml
    <Stack grow="1">
        <Label text="Background text layer"/>
        <Button text="Foreground button layer"/>
    </Stack>
    ```

#### 5. `<Card>`
Stylized container card. It automatically renders a rounded white rectangle base sheet (`radius="12"`) with `16` pixels of padding around internal child elements.
*   **Properties:**
    *   `grow` (Int): Flex-ratio. Default is `0`.
*   **Example:**
    ```xml
    <Card grow="1">
        <Column spacing="8">
            <Label text="Card Title"/>
            <Button text="Card Action"/>
        </Column>
    </Card>
    ```

#### 6. `<FlexBox>`
A highly flexible container that lays out child items in a single direction (row or column) with support for resizing, wrapping, space distribution, and item alignment.
*   **Properties:**
    *   `direction` (String): Layout direction. Valid values:
        *   `"row"` (default): Lays out children horizontally.
        *   `"column"`: Lays out children vertically.
    *   `wrap` (Boolean): If `true`, wraps items onto new lines if space on the main axis is exceeded. Default is `false`.
    *   `justifyContent` (String): Space distribution on the main axis. Valid values:
        *   `"flex-start"` (default): Align items at the start of the line.
        *   `"flex-end"`: Align items at the end of the line.
        *   `"center"`: Center items on the line.
        *   `"space-between"`: Evenly distribute extra space between items.
        *   `"space-around"`: Evenly distribute extra space around items (half space at the edges).
    *   `alignItems` (String): Align items along the cross-axis. Valid values:
        *   `"flex-start"` (default): Align items to the top/left of the cross-axis.
        *   `"center"`: Center items along the cross-axis.
        *   `"flex-end"`: Align items to the bottom/right of the cross-axis.
        *   `"stretch"`: Stretch items to match the cross-axis height/width.
    *   `spacing` (Int): Margin in pixels inserted between items. Default is `0`.
    *   `padding` (Int): Inset boundary in pixels surrounding the inside of the FlexBox container. Default is `0`.
    *   `grow` (Int): Flex-ratio. Default is `0`.
*   **Example:**
    ```xml
    <FlexBox direction="row" wrap="true" justifyContent="space-between" alignItems="center" spacing="8" padding="10" grow="1">
        <Button text="Wrapped Item 1"/>
        <Button text="Wrapped Item 2"/>
        <Button text="Wrapped Item 3"/>
    </FlexBox>
    ```

---

### Display & Interactive Widgets

#### 1. `<Label>` / `<Text>`
Renders textual data. Can display raw static strings or evaluate reactive bindings.
*   **Properties:**
    *   `text` (String | Binding): Text content to draw. **Required.**
    *   `grow` (Int): Specifies the layout flex-ratio. Default is `0`.
*   **Example:**
    ```xml
    <!-- Static Text with flex grow -->
    <Label text="Welcome to Lienzo UI" grow="1"/>
    
    <!-- State-Bound Reactive Text -->
    <Label text="{bind { \"Count: ${counter.value}\" }}"/>
    ```

#### 2. `<Button>`
Clickable action trigger. Renders a rounded background panel that highlights on hover.
*   **Properties:**
    *   `text` (String): The label drawn inside the button. **Required.**
    *   `onClick` (String): The name of a global/package-level Kotlin function to invoke on click.
    *   `enabled` (Boolean): Enables/disables the button's visuals and event callbacks. Default is `true`.
    *   `grow` (Int): Specifies the layout flex-ratio. Default is `0`.
    *   `cornerRadius` (Int): Sets the rounding radius in pixels for button corners. Default is `8`.
    *   `backgroundColor` (String): Fills the button background. If set, overrides the state-based default background.
    *   `borderColor` (String): Outlining border color. Default is `""` (no border).
    *   `borderThickness` (Int): Width in pixels of the border stroke. Requires `borderColor`. Default is `0`.
*   **Example:**
    ```xml
    <Button text="Submit Data" onClick="onFormSubmit" enabled="true" grow="0" cornerRadius="12" backgroundColor="#10B981" borderColor="#047857" borderThickness="2"/>
    ```

#### 3. `<Spacer>`
An empty invisible layout node that acts as a spring. It stretches to fill empty space.
*   **Properties:**
    *   `grow` (Int): Layout stretch ratio. Default is `1`.
*   **Example:**
    ```xml
    <Row>
        <Label text="Left Edge"/>
        <Spacer grow="1"/> <!-- Pushes Right Edge to the right boundary -->
        <Label text="Right Edge"/>
    </Row>
    ```

---

## 🔄 State Reactivity & Property Bindings

Lienzo's reactive runtime binds property functions to properties in code templates:

```kotlin
// In your Main.kt file
import lienzo.runtime.state

val counter = state(0) // State container holding Int
val appTitle = state("Workspace") // State container holding String
```

By referencing the state using the `{bind { ... }}` block, the compiler attaches observers to your states:
```xml
<Label text="{bind { \"Current User: ${appTitle.value} (id: ${counter.value})\" }}"/>
```

---

## ⚡ Event Bindings & Callback Functions

When the user specifies event hooks (e.g. `onClick="onResetClick"`), the compiler registers a method lookup. The function must be declared globally (or as an imported package-level function) in your target Kotlin code:

```kotlin
// In Main.kt
fun onResetClick() {
    counter.value = 0
    println("Counter cleared!")
}
```

---

## 🖼️ Full Bootstrap Code Example

Here is a full application definition combining these components.

### 1. File: `src/ui/MainWindow.lienzo`
```xml
<Window title="Developer Dashboard" width="600" height="450" theme="mica">
    <Column padding="20" spacing="12" align="center">
        
        <!-- Header -->
        <Label text="{bind { \"Active Session: ${userSession.value}\" }}"/>
        
        <!-- Main Form Panel -->
        <Card>
            <Column spacing="10">
                <Label text="Perform Actions:"/>
                <Row spacing="8">
                    <Button text="Login User" onClick="loginUser"/>
                    <Button text="Logout" onClick="logoutUser"/>
                </Row>
            </Column>
        </Card>
        
        <Spacer grow="1"/>
        
        <!-- Footer -->
        <Row spacing="10">
            <Label text="Build: v0.1"/>
            <Spacer/>
            <Button text="Quit" onClick="exitApp"/>
        </Row>
        
    </Column>
</Window>
```

### 2. File: `src/nativeMain/kotlin/Main.kt`
```kotlin
package ui

import SkiaRenderer
import Window
import lienzo.runtime.state
import kotlin.system.exitProcess

// 1. Declare state variables
val userSession = state("Guest User")

// 2. Declare action handlers
fun loginUser() {
    userSession.value = "Administrator"
}

fun logoutUser() {
    userSession.value = "Guest User"
}

fun exitApp() {
    exitProcess(0)
}

// 3. Entry point
fun main() {
    // Load renderer DLL
    val renderer = SkiaRenderer()
    renderer.load()

    // Instantiate generated MainWindow class
    val rootWidget = ui.generated.MainWindow()
    renderer.rootWidget = rootWidget

    // Re-evaluate layouts and draw on state mutation
    userSession.observe {
        rootWidget.invalidate()
    }

    // Launch window loop
    val window = Window(renderer)
    window.run()
}
```

---

## 🛠️ Compilation & Execution commands

Because Lienzo integrates directly into standard Gradle task graphs, build tasks handle compilation:

```powershell
# 1. Compile the compiler, generate Kotlin UI code, and build native mingwX64 binary
.\gradlew.bat build

# 2. Run your app
.\gradlew.bat run
```
