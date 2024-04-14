package com.github.gurgenky.epubify.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.XmlTag
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption


internal fun XmlTag.selectTag(name: String): XmlTag? {
    return childTags.find { it.name == name }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun FileInputStream.toTempFile() : File {
    val tempFile = File.createTempFile("temp-epub", ".epub")
    tempFile.deleteOnExit()

    Files.copy(this, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    return tempFile
}