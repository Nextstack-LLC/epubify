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
    namespace = "com.github.gurgenky.epubify"
    compileSdk = 34

    publishing {
        singleVariant("release") {
            withSourcesJar()
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
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId =  mavenProperties["GROUP"] as String
                artifactId = mavenProperties["ARTIFACT_ID"] as String
                version = mavenProperties["VERSION_NAME"] as String

                pom {
                    packaging = "aar"
                    name.set(mavenProperties["ARTIFACT_NAME"] as String)
                    description.set(mavenProperties["POM_DESCRIPTION"] as String)
                    url.set(mavenProperties["POM_URL"] as String)
                    inceptionYear.set("2024")

                    licenses {
                        license {
                            name.set(mavenProperties["POM_LICENCE_NAME"] as String)
                            url.set(mavenProperties["POM_LICENCE_URL"] as String)
                        }
                    }

                    developers {
                        developer {
                            id.set(mavenProperties["POM_DEVELOPER_ID"] as String)
                            name.set(mavenProperties["POM_DEVELOPER_NAME"] as String)
                            email.set(mavenProperties["POM_DEVELOPER_EMAIL"] as String)
                        }
                    }

                    scm {
                        connection.set(mavenProperties["POM_SCM_CONNECTION"] as String)
                        developerConnection.set(mavenProperties["POM_SCM_DEV_CONNECTION"] as String)
                        url.set(mavenProperties["POM_SCM_URL"] as String)
                    }
                }
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