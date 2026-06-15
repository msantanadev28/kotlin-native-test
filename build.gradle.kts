plugins {
    kotlin("multiplatform") version "2.1.21"
}

import java.net.URI
import java.net.URL

val skiaPackageVersion = "3.119.2"
val skiaPackageUrl = "https://www.nuget.org/api/v2/package/SkiaSharp.NativeAssets.Win32/$skiaPackageVersion"
val skiaPackageFile = layout.buildDirectory.file("downloads/skiasharp.nativeassets.win32.$skiaPackageVersion.nupkg")
val extractedSkiaRuntimeDir = layout.buildDirectory.dir("skia-runtime")

repositories {
    mavenCentral()
}

kotlin {
    mingwX64("native") {
        binaries {
            executable {
                entryPoint = "main"
                linkerOpts.add("-mwindows")
            }
        }
    }

}

tasks.register("downloadSkiaRuntimePackage") {
    outputs.file(skiaPackageFile)

    doLast {
        val packageFile = skiaPackageFile.get().asFile
        if (!packageFile.exists()) {
            packageFile.parentFile.mkdirs()
            URI.create(skiaPackageUrl).toURL().openStream().use { input ->
                packageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

val extractSkiaRuntime = tasks.register<Copy>("extractSkiaRuntime") {
    dependsOn("downloadSkiaRuntimePackage")
    from(zipTree(skiaPackageFile))
    include("runtimes/win-x64/native/libSkiaSharp.dll")
    eachFile {
        path = name
    }
    includeEmptyDirs = false
    into(extractedSkiaRuntimeDir)
}

val stageSkiaRuntimeDebug = tasks.register<Copy>("stageSkiaRuntimeDebug") {
    dependsOn(extractSkiaRuntime)
    from(extractedSkiaRuntimeDir)
    include("libSkiaSharp.dll")
    into(layout.buildDirectory.dir("bin/native/debugExecutable"))
}

val stageSkiaRuntimeRelease = tasks.register<Copy>("stageSkiaRuntimeRelease") {
    dependsOn(extractSkiaRuntime)
    from(extractedSkiaRuntimeDir)
    include("libSkiaSharp.dll")
    into(layout.buildDirectory.dir("bin/native/releaseExecutable"))
}

tasks.named("linkDebugExecutableNative") {
    finalizedBy(stageSkiaRuntimeDebug)
}

tasks.named("linkReleaseExecutableNative") {
    finalizedBy(stageSkiaRuntimeRelease)
}

tasks.named("build") {
    dependsOn(stageSkiaRuntimeDebug)
}