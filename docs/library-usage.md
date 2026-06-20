# Packaging & Using Lienzo UI as a Library

This guide explains how to compile the **Lienzo UI** library (both the JVM-based markup compiler and the Native runtime) and consume it in an external, application-only Kotlin/Native project.

---

## 🏗️ Architecture Overview

To use Lienzo UI as a library, we separate it into two main components:
1.  **Lienzo Compiler (JVM):** A code generator that parses `.lienzo` XML templates and outputs native-compatible Kotlin files.
2.  **Lienzo Runtime (Native):** The UI widgets, layout engines, state binding hooks, and Skia rendering context wrapper.

---

## 🛠️ Step 1: Package and Publish the Library

To make the components available to your application project, you can publish them to your local Maven repository (`mavenLocal()`).

### 1. Update `build.gradle.kts` in Lienzo UI
Apply the `maven-publish` plugin to standard KMP targets and define the publication coordinates:

```kotlin
plugins {
    kotlin("multiplatform") version "2.1.21"
    `maven-publish` // Add publishing capabilities
}

group = "org.lienzo"
version = "1.0.0-SNAPSHOT"

kotlin {
    // 1. JVM Compiler publication
    jvm("compiler")
    
    // 2. Native Runtime publication
    mingwX64("native") {
        binaries {
            // Note: Keep static/shared library configuration if you only want runtime libs,
            // or build executables for test-runs.
            sharedLib() // or staticLib()
        }
    }

    sourceSets {
        val nativeMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/lienzo"))
        }
    }
}
```

### 2. Publish to Maven Local
Run the following Gradle command inside the **Lienzo UI** project to build and publish both target libraries:

```powershell
.\gradlew.bat publishToMavenLocal
```

This will publish:
- `org.lienzo:kotlin-native-test-compiler:1.0.0-SNAPSHOT` (JVM jar)
- `org.lienzo:kotlin-native-test-native:1.0.0-SNAPSHOT` (Kotlin/Native `.klib`)

*(Note: The artifact names are based on your project directory name; you can customize them in `settings.gradle.kts` using `rootProject.name = "lienzo-ui"`).*

---

## 📱 Step 2: Set Up the Client App Project

Now, create a clean directory for your application project (e.g. `LienzoApp`).

### 1. File: `settings.gradle.kts`
Initialize the build:
```kotlin
rootProject.name = "lienzo-app"
```

### 2. File: `build.gradle.kts`
Create a Gradle configuration that depends on the published Lienzo library, handles `.lienzo` compilation, and copies the Skia runtime dependency (`libSkiaSharp.dll`).

```kotlin
plugins {
    kotlin("multiplatform") version "2.1.21"
}

repositories {
    mavenLocal() // Load local publications
    mavenCentral()
}

kotlin {
    // Define the Windows native target
    mingwX64("native") {
        binaries {
            executable {
                entryPoint = "app.main"
                linkerOpts.add("-mwindows")
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                // Depend on Lienzo Native Runtime published artifact
                implementation("org.lienzo:lienzo-ui-native:1.0.0-SNAPSHOT")
            }
            // Include generated files directory in compile path
            kotlin.srcDir(layout.buildDirectory.dir("generated/lienzo"))
        }
    }
}

// ----------------------------------------------------
// ⚙️ Custom Task to Run the Lienzo Compiler
// ----------------------------------------------------

// Resolve the Lienzo Compiler jar from Maven Local
val compilerClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    compilerClasspath("org.lienzo:lienzo-ui-compiler:1.0.0-SNAPSHOT")
}

val compileLienzo = tasks.register<JavaExec>("compileLienzo") {
    group = "build"
    description = "Compiles .lienzo XML layout files to Kotlin Native code."
    
    mainClass.set("lienzo.compiler.CompilerKt")
    classpath = compilerClasspath
    
    val inputDir = file("src/ui")
    val outputDir = layout.buildDirectory.dir("generated/lienzo")
    
    inputs.dir(inputDir)
    outputs.dir(outputDir)
    
    args(
        inputDir.absolutePath,
        outputDir.get().asFile.absolutePath,
        "app.generated" // Package name for generated views
    )
}

// Trigger markup generation before compiling native sources
tasks.named("compileKotlinNative") {
    dependsOn(compileLienzo)
}

// ----------------------------------------------------
// 📦 Stage Native Skia Assets
// ----------------------------------------------------
// Skia dll is required alongside the executable at runtime.
// You can fetch this directly from NuGet inside Gradle:

val skiaPackageVersion = "3.119.2"
val skiaPackageUrl = "https://www.nuget.org/api/v2/package/SkiaSharp.NativeAssets.Win32/$skiaPackageVersion"
val skiaPackageFile = layout.buildDirectory.file("downloads/skiasharp.nativeassets.win32.$skiaPackageVersion.nupkg")
val extractedSkiaRuntimeDir = layout.buildDirectory.dir("skia-runtime")

val downloadSkiaRuntimePackage = tasks.register("downloadSkiaRuntimePackage") {
    outputs.file(skiaPackageFile)
    doLast {
        val packageFile = skiaPackageFile.get().asFile
        if (!packageFile.exists()) {
            packageFile.parentFile.mkdirs()
            java.net.URI.create(skiaPackageUrl).toURL().openStream().use { input ->
                packageFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

val extractSkiaRuntime = tasks.register<Copy>("extractSkiaRuntime") {
    dependsOn(downloadSkiaRuntimePackage)
    from(zipTree(skiaPackageFile))
    include("runtimes/win-x64/native/libSkiaSharp.dll")
    eachFile { path = name }
    includeEmptyDirs = false
    into(extractedSkiaRuntimeDir)
}

val stageSkiaRuntime = tasks.register<Copy>("stageSkiaRuntime") {
    dependsOn(extractSkiaRuntime)
    from(extractedSkiaRuntimeDir)
    include("libSkiaSharp.dll")
    // Stage it into both Debug and Release build bins
    into(layout.buildDirectory.dir("bin/native/debugExecutable"))
}

tasks.named("linkDebugExecutableNative") {
    finalizedBy(stageSkiaRuntime)
}
```

---

## ✍️ Step 3: Implement Your App

In your new app project, you can now construct your UI designs and logic separately from Lienzo's source code.

### 1. File: `src/ui/AppWindow.lienzo`
Create your layout markup in the `src/ui/` folder:
```xml
<Window title="My Client Application" width="640" height="480" theme="mica">
    <Column padding="24" spacing="12" align="center">
        <Label text="Welcome to the Client App!"/>
        <Card>
            <Column spacing="8">
                <Label text="{bind { \"Pressed: ${clickCount.value} times\" }}"/>
                <Button text="Click Me" onClick="onIncrement"/>
            </Column>
        </Card>
    </Column>
</Window>
```

### 2. File: `src/nativeMain/kotlin/Main.kt`
Create your application entry point to register state, handle actions, and start the GUI thread:

```kotlin
package app

import SkiaRenderer
import Window
import lienzo.runtime.state
import app.generated.AppWindow // Generated class from AppWindow.lienzo

// 1. Reactive State
val clickCount = state(0)

// 2. Click Handler
fun onIncrement() {
    clickCount.value += 1
}

// 3. Entry point
fun main() {
    // Start Skia UI Renderer
    val renderer = SkiaRenderer()
    renderer.load() // Resolves local libSkiaSharp.dll

    // Bind layout
    val rootWidget = app.generated.AppWindow()
    renderer.rootWidget = rootWidget

    // Reactively refresh view on state mutations
    clickCount.observe {
        rootWidget.invalidate()
    }

    // Windows Native window loop
    val window = Window(renderer)
    window.run()
}
```

---

## 🚀 Step 4: Build and Run

To compile and launch your client app:

```powershell
# Compile generated files, build K/N binary, and stage SkiaSharp DLL
.\gradlew.bat build

# Run client application
.\build\bin\native\debugExecutable\lienzo-app.exe
```
