import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.3"
    id("app.cash.sqldelight") version "2.2.1"
}

group = "com.ankideku"
version = "2.0.0"

dependencies {
    // Compose
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Ktor (HTTP client)
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")

    // SQLDelight
    implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
    implementation("app.cash.sqldelight:coroutines-extensions:2.2.1")

    // Koin (DI)
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-compose:4.1.1")

    // Diff
    implementation("org.bitbucket.cowwoc:diff-match-patch:1.2")

    // Tokenization
    implementation("com.knuddels:jtokkit:1.0.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.6")
}

sqldelight {
    databases {
        create("AnkiDekuDb") {
            packageName.set("com.ankideku.data.local.database")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.2.1")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ankideku.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AnkiDeku"
            packageVersion = "1.0.0"
            description = "AI-powered Anki deck improvement tool"
            vendor = "AnkiDeku"

            windows {
                menuGroup = "AnkiDeku"
                upgradeUuid = "8e79a0b5-3c4d-4f5e-9a1b-2c3d4e5f6a7b"
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
            }

            macOS {
                bundleID = "com.ankideku"
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
            }

            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Disable SQLDelight migration verification (Windows file locking issues with SQLite JDBC)
afterEvaluate {
    tasks.findByName("verifyMainAnkiDekuDbMigration")?.enabled = false
    tasks.findByName("verifySqlDelightMigration")?.enabled = false
}
