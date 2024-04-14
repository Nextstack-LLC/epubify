package com.github.gurgenky.epubify.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.XmlTag
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Selects a tag with the given name from the list of child tags.
 * @param name The name of the tag to select.
 * @return The selected tag, or null if no tag with the given name was found.
 */
internal fun XmlTag.selectTag(name: String): XmlTag? {
    return childTags.find { it.name == name }
}

/**
 * Converts a file input stream to a temporary file.
 * @return The temporary file.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun FileInputStream.toTempFile() : File {
    val tempFile = File.createTempFile("temp-epub", ".epub")
    tempFile.deleteOnExit()

    Files.copy(this, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    return tempFile
}