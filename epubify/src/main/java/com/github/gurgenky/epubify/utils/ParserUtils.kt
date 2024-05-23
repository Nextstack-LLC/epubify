package com.github.gurgenky.epubify.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.Image
import com.github.gurgenky.epubify.model.JsoupOutput
import com.github.gurgenky.epubify.model.XmlTag
import com.github.gurgenky.epubify.parser.EpubWhitelist
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Selects a tag with the given name from the list of child tags.
 * @param name The name of the tag to select.
 * @param recursive Whether to search recursively.
 * @return The selected tag, or null if no tag with the given name was found.
 */
internal fun XmlTag.selectTag(name: String, recursive: Boolean = false): XmlTag? {
    return childTags.find { it.name == name } ?: if (recursive) {
        childTags.find { it.selectTag(name, true) != null }
    } else {
        null
    }
}

/**
 * Converts a file input stream to a temporary file.
 * @return The temporary file.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal fun InputStream.toTempFile() : File {
    val tempFile = File.createTempFile("temp-epub", ".epub")
    tempFile.deleteOnExit()

    Files.copy(this, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    return tempFile
}

/**
 * Parses a document from a file.
 * @param parsedImages The list of parsed image files.
 * @return The parsed document.
 */
internal fun File.parseDocument(
    parsedImages: List<Image>
) : JsoupOutput {
    val document = Jsoup.parse(this, "UTF-8")
    var bodyContent: String

    val bodyElement: Element? = document.body()
    val titleElement: Element? = document.selectFirst("h1, h2, h3, h4, h5, h6")
    val title: String = titleElement?.html() ?: ""

    bodyContent = bodyElement?.let { modifyImageEntries(it, parsedImages).html() } ?: ""
    bodyContent = Jsoup.clean(bodyContent, EpubWhitelist)

    return JsoupOutput(title, bodyContent)
}

/**
 * Modifies image entries in the document.
 * @param element The element to modify.
 * @param parsedImages The list of parsed images.
 * @return The modified element.
 */
private fun modifyImageEntries(
    element: Element,
    parsedImages: List<Image>
): Element {
    val children: List<Node> = element.childNodes()
    val modifiedChildren: List<Node> = children.map { child ->
        if (child is Element) {
            if (child.tagName() == "img" || child.tagName() == "image") {
                val image = parsedImages.find { it.path == child.attr("src") }
                image?.let { handleImageNode(it) } ?: TextNode("")
            } else {
                modifyImageEntries(child, parsedImages)
            }
        } else {
            child
        }
    }

    return element
        .clone()
        .empty()
        .insertChildren(0, modifiedChildren)
}

/**
 * Handles an image node by replacing it with a placeholder.
 * @param image The image to replace the element with.
 * @return The modified element.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun handleImageNode(
    image: Image
) : Node {
    val base64 = Base64.encode(image.image)
    val dataUri = "data:image/${image.path.substringAfterLast(".")};base64,$base64"
    return Element("img").attr("src", dataUri)
}