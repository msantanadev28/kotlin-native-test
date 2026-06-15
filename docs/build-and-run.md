# Lienzo UI — Build and Run Guide

This guide describes how the Lienzo UI framework compiles `.lienzo` markup templates at build-time and runs them in a native Windows application with Skia rendering.

---

## 📋 Prerequisites

- **Java Development Kit (JDK):** Version 17 or higher (used by Gradle and the JVM-based UI compiler).
- **Windows OS (mingwX64):** Target platform for the native client.

---

## 🛠️ How to Build

The project is structured as a Kotlin Multiplatform build compiling a JVM target for the compiler and a Native target for the UI runtime.

To clean, download dependencies, compile the code generator, compile the native code, and link everything:

```powershell
# Restore/update wrapper (if required) and run the build
.\gradlew.bat build
```

### Build Pipeline Stages
1. **Download & Stage Skia:** Downloads the native Skia library (`libSkiaSharp.dll`) and places it next to the output executable.
2. **Compile Compiler:** Compiles the JVM compiler (`src/compilerMain`) which parses `.lienzo` markup.
3. **Generate UI:** The compile task (`compileLienzo`) parses files in `src/ui/` and generates Kotlin source files in `build/generated/lienzo/`.
4. **Compile & Link Native App:** Compiles the native client code along with the generated UI files into a Windows executable.

---

## 🚀 How to Run

After a successful build, you can execute the generated native application:

```powershell
# Run the executable
.\build\bin\native\debugExecutable\kotlin-native-sample.exe
```

### Interactive Features of the Single Button App
- **Hover State:** The button coordinates pointer movements and changes background color when hovered (Blue-600 defaults to Blue-700 on hover).
- **Click Actions:** Clicking the button dispatches a click event down the widget hierarchy to invoke `onButtonClick` defined in `Main.kt`, outputting:
  ```
  Button was clicked! Lienzo UI works successfully!
  ```
