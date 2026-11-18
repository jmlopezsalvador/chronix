import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources) // decodeToImageBitmap (miniaturas de los iconos)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            // Cliente de Magic Friends (includeBuild en settings.gradle.kts). Trae coroutines y serialization-json como `api`.
            implementation("com.magicfriends:magicfriends-client")
            // El constructor del cliente expone `HttpClient` de Ktor pero la librería lo declara `implementation`: sin esto
            // Chronix no compila. Misma versión que la librería (ktorVersion en magicfriends/client/build.gradle.kts).
            implementation("io.ktor:ktor-client-core:3.0.3")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

// --- Configuración de Magic Friends (URL del servidor y appKey). El repo es público: estos valores NO van en el código
// ni en ficheros versionados. Se leen, por este orden, de: propiedad de Gradle (-Pmagicfriends.serverUrl=…), variable de
// entorno (MAGICFRIENDS_SERVER_URL / MAGICFRIENDS_APP_KEY, así los pasa el CI desde secretos del repo) o `local.properties`
// (ignorado por git). Se genera MagicFriendsConfig.kt en build/ (también ignorado). Sin valores, Magic Friends queda
// desactivado y la app compila igual.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun magicFriendsValue(prop: String, env: String): String =
    (providers.gradleProperty(prop).orNull
        ?: providers.environmentVariable(env).orNull
        ?: localProps.getProperty(prop)
        ?: "").trim()

val generateMagicFriendsConfig by tasks.registering {
    val serverUrl = magicFriendsValue("magicfriends.serverUrl", "MAGICFRIENDS_SERVER_URL")
    val appKey = magicFriendsValue("magicfriends.appKey", "MAGICFRIENDS_APP_KEY")
    val outDir = layout.buildDirectory.dir("generated/magicfriends/kotlin")
    inputs.property("serverUrl", serverUrl)
    inputs.property("appKey", appKey)
    outputs.dir(outDir)
    doLast {
        fun literal(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        val dir = outDir.get().asFile.resolve("com/chronix/ui")
        dir.mkdirs()
        dir.resolve("MagicFriendsConfig.kt").writeText(
            """
            package com.chronix.ui

            // Generado por Gradle (generateMagicFriendsConfig) desde local.properties / entorno. No editar ni versionar.
            internal object MagicFriendsConfig {
                const val SERVER_URL: String = ${literal(serverUrl)}
                const val APP_KEY: String = ${literal(appKey)}
                val configured: Boolean get() = SERVER_URL.isNotBlank() && APP_KEY.isNotBlank()
            }
            """.trimIndent() + "\n",
            Charsets.UTF_8,
        )
    }
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateMagicFriendsConfig.map { it.outputs.files.singleFile })

android {
    namespace = "com.chronix.shared"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
