import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
    id("maven-publish")
}

val mavenPropertiesFile = rootProject.file("publishing.properties")
val mavenProperties = Properties()
mavenProperties.load(FileInputStream(mavenPropertiesFile))

android {
    namespace = "org.nextstack.epubify"
    compileSdk = 34

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("test").resources.srcDirs("src/test/resources")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Nextstack-LLC/epubify")
                credentials {
                    username = mavenProperties["gpr.user"] as String
                    password = mavenProperties["gpr.token"] as String
                }
            }
        }
        publications {
            create<MavenPublication>("release") {
                groupId = "org.nextstack"
                artifactId = "epubify"
                version = "0.1.5"
                from(components["release"])
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.jsoup)
    implementation(libs.xmlpull)
    implementation(libs.zipf4j)
    implementation(libs.apache.commons.text)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}