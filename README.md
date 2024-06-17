# Jetpack Compose ePub Viewer Library

A Jetpack Compose library for displaying and parsing ePub files. This library provides components and utilities to integrate ePub viewing capabilities into your Android applications using Jetpack Compose.

## Features

- Display ePub content using Jetpack Compose UI components.
- Parse ePub files to extract text, metadata, and other resources.
- Support for navigating through the pages of an ePub document.

## Getting Started

To use this library in your project, follow these steps:

### Prerequisites

- Obtain the necessary credentials and set them up in your `publishing.properties` file. You can use the provided `publishing.example.properties` file as a template.

### Installation

Add the following dependencies to your project:

```kotlin
val mavenPropertiesFile = rootProject.file("publishing.properties")
val mavenProperties = Properties()
mavenProperties.load(FileInputStream(mavenPropertiesFile))

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Nextstack-LLC/epubify")
        credentials {
            username = mavenProperties["gpr.user"] ?: System.getenv("USERNAME")
            password = mavenProperties["gpr.token"] ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation("org.nextstack:epubify:[latest release]")
}

```

### Usage

1. Initialize the ePub viewer in your Composable function:

```kotlin
import org.nextstack.epubify.ui.EpubViewer

@Composable
fun EpubViewScreen() {
    val epubFile = // Load your ePub file here
    EpubViewer(epubFile)
}
```

2. Load and display ePub content within your Composable UI.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.