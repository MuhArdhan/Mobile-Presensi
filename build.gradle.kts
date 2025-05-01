// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// Hapus semua deklarasi repositories di sini karena sudah diatur di settings.gradle.kts
// Gunakan subprojects untuk konfigurasi khusus modul
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.application") || plugins.hasPlugin("com.android.library")) {
            // Konfigurasi Android khusus bisa ditambahkan di sini
            extensions.configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(35)

                defaultConfig {
                    minSdk = 24
                    targetSdk = 35
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}