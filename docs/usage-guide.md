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

### Layout Containers in Detail

#### 1. `<Column>`
Arranges child elements sequentially in a vertical stack.
*   **Properties:**
    *   `spacing` (Int): Vertical margin in pixels inserted between each child. Default is `0`.
    *   `padding` (Int): Inset border in pixels surrounding the inside of the column. Default is `0`.
    *   `align` (String): Horizontal layout alignment for children. Valid values:
        *   `"start"` (default): Left aligns all children.
        *   `"center"`: Centers children horizontally.
        *   `"end"`: Right aligns all children.
    *   `grow` (Int): Specifies vertical flex-ratio when nested inside another layout. Default is `0`.
*   **Example:**
    ```xml
    <Column padding="20" spacing="15" align="center">
        <Label text="Centered Item 1"/>
        <Label text="Centered Item 2"/>
    </Column>
    ```

#### 2. `<Row>`
Arranges child elements sequentially in a horizontal line.
*   **Properties:**
    *   `spacing` (Int): Horizontal margin in pixels inserted between each child. Default is `0`.
    *   `padding` (Int): Inset border in pixels surrounding the inside of the row. Default is `0`.
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
    *   `columns` (Int): The number of columns.
    *   `rows` (Int): Optional fixed rows count (if `0` or omitted, rows are dynamically calculated).
    *   `spacing` (Int): Margin in pixels around grid cells.
    *   `grow` (Int): Flex-ratio.
*   **Example:**
    ```xml
    <Grid columns="3" spacing="12">
        <Card><Button text="A"/></Card>
        <Card><Button text="B"/></Card>
        <Card><Button text="C"/></Card>
    </Grid>
    ```

#### 4. `<Stack>`
Overlays child elements on top of each other. The layout size bounds are determined by the largest child.
*   **Properties:**
    *   `grow` (Int): Flex-ratio.
*   **Example:**
    ```xml
    <Stack>
        <Label text="Background text layer"/>
        <Button text="Foreground button layer"/>
    </Stack>
    ```

#### 5. `<Card>`
Stylized container card. It automatically renders a rounded white rectangle base sheet (`radius="12"`) with `16` pixels of padding around internal child elements.
*   **Properties:**
    *   `grow` (Int): Flex-ratio.
*   **Example:**
    ```xml
    <Card>
        <Column spacing="8">
            <Label text="Card Title"/>
            <Button text="Card Action"/>
        </Column>
    </Card>
    ```

---

### Display & Interactive Widgets

#### 1. `<Label>` / `<Text>`
Renders textual data. Can display raw static strings or evaluate reactive bindings.
*   **Properties:**
    *   `text` (String | Binding): Text content to draw.
    *   `grow` (Int): Flex-ratio.
*   **Example:**
    ```xml
    <!-- Static Text -->
    <Label text="Welcome to Lienzo UI"/>
    
    <!-- State-Bound Reactive Text -->
    <Label text="{bind { \"Count: ${counter.value}\" }}"/>
    ```

#### 2. `<Button>`
Clickable action trigger. Renders a rounded background panel that highlights on hover.
*   **Properties:**
    *   `text` (String): The label drawn inside the button.
    *   `onClick` (String): The name of a global Kotlin function to invoke.
    *   `enabled` (Boolean): Disable the button's visuals and event callbacks. Default is `true`.
*   **Example:**
    ```xml
    <Button text="Submit Data" onClick="onFormSubmit" enabled="true"/>
    ```

#### 3. `<Spacer>`
An empty invisible layout node that acts as a spring. It stretches to fill empty space.
*   **Properties:**
    *   `grow` (Int): Layout stretch ratio. Default is `1`.
*   **Example:**
    ```xml
    <Row>
        <Label text="Left Edge"/>
        <Spacer/> <!-- Pushes Right Edge to the right boundary -->
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
