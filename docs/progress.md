# Lienzo UI — Project Progress Report

**Last Updated:** June 19, 2026

This document tracks the implementation progress of the **Lienzo UI** library for Kotlin Native on Windows (`mingwX64`) using Skia.

---

## 📊 Milestone Summary

Below is the status of the milestones outlined in the [Implementation Guide](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/docs/implementation-guide.md#L1059):

| Milestone | Description | Status | Verification / Artifacts |
| :--- | :--- | :---: | :--- |
| **M1** | Lexer tokenizes `<Button text="Save"/>` correctly | **Completed** | Tokenizer rules in [Compiler.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/compilerMain/kotlin/lienzo/compiler/Compiler.kt#L23) |
| **M2** | Parser produces `ElementNode` trees | **Completed** | AST generation in [Compiler.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/compilerMain/kotlin/lienzo/compiler/Compiler.kt#L153) |
| **M3** | Codegen outputs valid Kotlin from the AST | **Completed** | Builder-pattern output in [Compiler.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/compilerMain/kotlin/lienzo/compiler/Compiler.kt#L223) |
| **M4** | `State<T>` notifies observers on change | **Completed** | Reactive observers in [Runtime.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/lienzo/runtime/Runtime.kt#L3) |
| **M5** | `TextWidget` & `ButtonWidget` compile/link into binary | **Completed** | Widget definitions in [Widgets.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/lienzo/runtime/widgets/Widgets.kt#L53) |
| **M6** | `ColumnWidget` lays out widgets vertically with spacing | **Completed** | Layout logic in [Widgets.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/lienzo/runtime/widgets/Widgets.kt#L176) |
| **M7** | `SkiaCanvas` draws basic rects, round rects, and text | **Completed** | Skia C++ bindings in [SkiaCanvas.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/lienzo/renderer/SkiaCanvas.kt) |
| **M8** | Mouse input & click events invoke `onClick` lambdas | **Completed** | Event propagation in [Widgets.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/lienzo/runtime/widgets/Widgets.kt#L89) |
| **M9** | A `.lienzo` file is compiled & run as a Win32 window | **Completed** | Main executable loop in [Main.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/Main.kt#L20) |
| **M10**| `State` change triggers redraw with the new value | **Completed** | Reactive updates in [Main.kt](file:///C:/Users/rd28/Videos/Coding%202026/dump/kotlin%20native/kotlin-native-test/src/nativeMain/kotlin/Main.kt#L28) |
| **M11**| All 12 built-in components render and respond to events | **In Progress** | Basic components ready; advanced ones (Table, SplitPane, etc.) pending |
| **M12**| Theme files apply globally via a ThemeRegistry | **In Progress** | Basic style properties defined; Theme registry framework pending |

---

## 🛠️ Feature Breakdown

### 1. Compiler module (`compilerMain`)
* **Lexer:** Supports tags (`<`, `>`, `/>`, `</`), `=` identifiers, string literals, and ignores comments (`<!-- -->`).
* **Parser:** Recursively constructs node structures, mapping child lists and matching opening/closing tags.
* **Code Generator:** Generates equivalent Kotlin DSL structures dynamically during gradle build stages.

### 2. Runtime & Layout engine (`nativeMain`)
* **Reactivity:** `State<T>` container provides state hooks. `Binding<T>` connects states with UI properties to avoid full-tree redraws.
* **Layouts:**
  * `Column`: Vertical layouts, spacing, alignment constraints (start, center, end).
  * `Row`: Horizontal layout implementation using similar layout mechanics.
  * `FlexBox`: Flexible unidirectional layout supporting wrap, grow, justification presets, cross-axis alignment, spacing, and padding.
  * `Grid`: Distributes elements into rows/columns with margins and cells calculations.
  * `Stack`: Overlays elements directly.
  * `Spacer` & `Card`: Layout control, spacing adjustments, and rounded container backgrounds.
* **Controls:**
  * `Button`: Supports click event dispatch, mouse hover detection, and customized state-driven backgrounds.
  * `Label`: Simple string display linked to runtime reactive values.

---

## 🔮 Next Steps & Pending Items

To reach full release readiness, the remaining tasks are:
1. **Expand Built-in Components (M11):** Implement complex compound components such as `Table`, `TabView`, `Sidebar`, and `SplitPane`.
2. **Implement Theme System (M12):** Add global style registries, parser support for `.theme` files, and light/dark theme toggle triggers.
