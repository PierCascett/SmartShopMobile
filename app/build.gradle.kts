import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("jacoco")
}

// Rileva automaticamente l'IP locale del PC a tempo di build
fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in interfaces) {
            val addresses = iface.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip != null && !ip.startsWith("127.")) {
                        return ip
                    }
                }
            }
        }
    } catch (_: Exception) {
        println("Errore rilevamento IP, uso fallback")
    }
    return "10.0.2.2" // fallback per emulatore
}

val detectedBackendHost: String = getLocalIpAddress()
println("Backend Host rilevato: $detectedBackendHost")

android {
    namespace = "it.unito.smartshopmobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.unito.smartshopmobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        android.buildFeatures.buildConfig = true

        // Inietta l'IP rilevato in BuildConfig
        buildConfigField("String", "BACKEND_HOST", "\"$detectedBackendHost\"")
        buildConfigField("String", "BACKEND_PORT", "\"3000\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")

    // Material Icons (for nicer login UI icons)
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation(libs.androidx.compose.runtime)
    implementation(libs.core.ktx)

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Retrofit for API calls
    val retrofitVersion = "2.11.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // DataStore per sessione utente
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Test
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // QR generation
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// JaCoCo Configuration - 3 Separate Reports
val fileFilter = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*\$ViewInjector*.*",
    "**/*\$ViewBinder*.*",
    "**/databinding/*",
    "**/android/databinding/*",
    "**/androidx/databinding/*",
    "**/di/module/*",
    "**/*MapperImpl*.*",
    "**/*\$Lambda$*.*",
    "**/*Companion*.*",
    "**/*Module*.*",
    "**/*Dagger*.*",
    "**/*MembersInjector*.*",
    "**/*_Factory*.*",
    "**/*_Provide*.*",
    "**/*Extensions*.*"
)

// 1️⃣ UNIT TESTS - src/test/java/*/unitTest/**
tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDebugUnitTest")

    group = "Coverage Reports"
    description = "Coverage report for Unit Tests (src/test/*/unitTest/**)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unitTest/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/unitTest/jacocoTestReport.xml"))
    }

    val debugTree = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// --- Aggiunto: alias per compatibilità con nomi usati altrove ---
// Questo evita errori quando altri task (generate*) riferiscono
// `jacocoUnitViewModelTestReport` che non era stato creato.
tasks.register<JacocoReport>("jacocoUnitViewModelTestReport") {
    // replica il comportamento di `jacocoUnitTestReport` ma scrive
    // l'output in una cartella `unitViewModelTest` per compatibilità
    dependsOn("testDebugUnitTest")

    group = "Coverage Reports"
    description = "Coverage report for Unit ViewModel Tests (src/test/*/unitViewModelTest/**)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unitViewModelTest/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/unitViewModelTest/jacocoTestReport.xml"))
    }

    val debugTree = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// 2️⃣ INTEGRATION TESTS - src/test/java/*/integrationTest/**
// ⚠️ NOTA: Condivide execution data con unitViewModelTest perché entrambi fanno parte
// dello stesso source set "test". Per separarli completamente servirebbero source sets separati.
tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    dependsOn("testDebugUnitTest")

    group = "Coverage Reports"
    description = "Coverage report for Integration Tests (src/test/*/integrationTest/**)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/integrationTest/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/integrationTest/jacocoTestReport.xml"))
    }

    val debugTree = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// 3️⃣ UI TESTS - src/androidTest/java/*/uiTest/**
tasks.register<JacocoReport>("jacocoUITestReport") {
    dependsOn("createDebugCoverageReport")

    group = "Coverage Reports"
    description = "Coverage report for UI Tests (src/androidTest/*/uiTest/**)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/uiTest/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/uiTest/jacocoTestReport.xml"))
    }

    val debugTree = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
    })
}

// 🎯 ALL COVERAGE COMBINED
tasks.register<JacocoReport>("jacocoAllTestsReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")

    group = "Coverage Reports"
    description = "Combined coverage report for ALL tests"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/allTests/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/allTests/jacocoTestReport.xml"))
    }

    val debugTree = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
        )
    })
}

// 📊 GENERATE UNIT + INTEGRATION COVERAGE REPORTS (NO EMULATOR NEEDED)
tasks.register("generateBasicCoverageReports") {
    dependsOn("jacocoUnitViewModelTestReport", "jacocoIntegrationTestReport")

    group = "Coverage Reports"
    description = "Generate Unit ViewModel and Integration coverage reports (NO emulator needed)"

    doLast {
        println("""
        
        ╔═══════════════════════════════════════════════════════════════════╗
        ║            📊 BASIC COVERAGE REPORTS GENERATED                    ║
        ╠═══════════════════════════════════════════════════════════════════╣
        ║                                                                   ║
        ║  🔬 UNIT VIEWMODEL TESTS                                         ║
        ║     📂 app/build/reports/jacoco/unitViewModelTest/html/index.html║
        ║                                                                   ║
        ║  🔗 INTEGRATION TESTS                                            ║
        ║     📂 app/build/reports/jacoco/integrationTest/html/index.html  ║
        ║                                                                   ║
        ║  ⚠️  NOTA: Entrambi i report mostrano gli stessi valori perché   ║
        ║     condividono lo stesso file di execution data (.exec).        ║
        ║     Questo è normale - entrambi fanno parte dello stesso         ║
        ║     source set "test" di Gradle.                                 ║
        ║                                                                   ║
        ║  ℹ️  Per i report UI, esegui prima i test con emulatore:         ║
        ║     Gradle → app → verification → connectedDebugAndroidTest      ║
        ║     Poi: Gradle → app → Coverage Reports → jacocoUITestReport    ║
        ║                                                                   ║
        ╚═══════════════════════════════════════════════════════════════════╝
        
        """.trimIndent())
    }
}

// 📊 GENERATE ALL COVERAGE REPORTS IN ONE COMMAND (REQUIRES EMULATOR)
tasks.register("generateAllCoverageReports") {
    dependsOn("jacocoUnitViewModelTestReport", "jacocoIntegrationTestReport", "jacocoUITestReport", "jacocoAllTestsReport")

    group = "Coverage Reports"
    description = "Generate ALL coverage reports: Unit ViewModel, Integration, UI, and Combined (requires emulator)"

    doLast {
        println("""
        
        ╔═══════════════════════════════════════════════════════════════════╗
        ║                    📊 COVERAGE REPORTS GENERATED                  ║
        ╠═══════════════════════════════════════════════════════════════════╣
        ║                                                                   ║
        ║  🔬 UNIT VIEWMODEL TESTS                                         ║
        ║     📂 app/build/reports/jacoco/unitViewModelTest/html/index.html║
        ║                                                                   ║
        ║  🔗 INTEGRATION TESTS                                            ║
        ║     📂 app/build/reports/jacoco/integrationTest/html/index.html  ║
        ║                                                                   ║
        ║  📱 UI TESTS                                                     ║
        ║     📂 app/build/reports/jacoco/uiTest/html/index.html           ║
        ║                                                                   ║
        ║  🎯 ALL COMBINED                                                 ║
        ║     📂 app/build/reports/jacoco/allTests/html/index.html         ║
        ║                                                                   ║
        ║  ⚠️  NOTA: unitViewModelTest e integrationTest mostrano gli      ║
        ║     stessi valori perché condividono lo stesso execution data.   ║
        ║     Questo è normale in Gradle.                                  ║
        ║                                                                   ║
        ╚═══════════════════════════════════════════════════════════════════╝
        
        """.trimIndent())
    }
}

// 🔧 UTILITY: Run all tests and generate combined report (continue on test failures)
tasks.register("testAllAndReport") {
    group = "Coverage Reports"
    description = "Run ALL tests and generate reports (continues even if some tests fail)"

    doLast {
        println("🚀 Starting comprehensive test execution...")

        // Run Unit/Integration tests
        try {
            println("📝 Running Unit & Integration Tests...")
            project.exec {
                commandLine("./gradlew", "testDebugUnitTest", "--continue")
                isIgnoreExitValue = true
            }
        } catch (e: Exception) {
            println("⚠️  Some unit/integration tests failed, but continuing...")
        }

        // Run UI tests
        try {
            println("📱 Running UI Tests...")
            project.exec {
                commandLine("./gradlew", "connectedDebugAndroidTest", "--continue")
                isIgnoreExitValue = true
            }
        } catch (e: Exception) {
            println("⚠️  Some UI tests failed, but continuing...")
        }

        // Generate all reports
        println("📊 Generating coverage reports...")
        try {
            project.exec {
                commandLine("./gradlew", "jacocoUnitTestReport", "jacocoIntegrationTestReport")
            }
            println("✅ Unit & Integration reports generated")
        } catch (e: Exception) {
            println("⚠️  Error generating basic reports")
        }

        try {
            project.exec {
                commandLine("./gradlew", "jacocoUITestReport", "jacocoAllTestsReport")
            }
            println("✅ UI & Combined reports generated")
        } catch (e: Exception) {
            println("⚠️  Error generating UI reports (may need UI test execution data)")
        }

        println("""
        
        ╔═══════════════════════════════════════════════════════════════════╗
        ║                    ✅ TEST EXECUTION COMPLETED                    ║
        ╠═══════════════════════════════════════════════════════════════════╣
        ║                                                                   ║
        ║  Check test results at:                                          ║
        ║  📂 app/build/reports/tests/                                     ║
        ║                                                                   ║
        ║  Check coverage reports at:                                      ║
        ║  📂 app/build/reports/jacoco/                                    ║
        ║                                                                   ║
        ╚═══════════════════════════════════════════════════════════════════╝
        
        """.trimIndent())
    }
}
