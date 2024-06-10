package com.github.gurgenky.epubify.utils

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.Image
import com.github.gurgenky.epubify.model.JsoupOutput
import com.github.gurgenky.epubify.model.format.Manifest
import com.github.gurgenky.epubify.model.format.Toc
import com.github.gurgenky.epubify.model.format.XmlTag
import com.github.gurgenky.epubify.parser.EpubWhitelist
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The template for the body of a document.
 */
private val documentBodyTemplate = """
    <div id="%s">
        %s
    </div>
""".trimIndent()

/**
 * The template for the default fonts of a document.
 */
internal val bodyDefaultFontsTemplate = """
    body {
        font-family: %s;
    }
""".trimIndent()

/**
 * Template for a font declaration.
 */
internal val fontFamilyDeclarationTemplate = """
    @font-face {
        font-family: '%s';
        src: url(data:font/ttf;base64,%s);
    }
""".trimIndent()

/**
 * Flattens a Toc to a list of entries.
 * @return The list of all entries.
 */
internal val Toc.flatten: List<Toc.Entry> get() = entries.flatMap { it.allChildren }

/**
 * Gets all children of a Toc.Entry.
 * @return The list of all children.
 */
internal val Toc.Entry.allChildren: List<Toc.Entry> get() {
    val allChildren = mutableSetOf<Toc.Entry>()

    fun visit(entry: Toc.Entry) {
        allChildren.add(entry)
        entry.children.forEach { visit(it) }
    }

    visit(this)
    return allChildren.toList()
}

/**
 * Selects a tag with the given name from the list of child tags.
 * @param name The name of the tag to select.
 * @param searchInChildren Whether to search in the children of the child tags.
 * @return The selected tag, or null if no tag with the given name was found.
 */
internal fun XmlTag.selectTag(name: String, searchInChildren: Boolean = false): XmlTag? {
    return childTags.find { it.name == name } ?: if (searchInChildren) {
        childTags.firstNotNullOfOrNull { it.selectTag(name, true) }
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
    manifest: Manifest.Item,
    parsedImages: List<Image>
) : JsoupOutput {
    val document = Jsoup.parse(this, "UTF-8")
    var bodyContent: String

    val bodyElement: Element? = document.body()
    val titleElement: Element? = document.selectFirst("h1, h2, h3, h4, h5, h6")
    val title: String = StringEscapeUtils.unescapeHtml4(titleElement?.html()) ?: ""
    val href = manifest.href.substringBefore(".")

    bodyContent = bodyElement?.let { modifyImageEntries(it, parsedImages).html() } ?: ""
    bodyContent = documentBodyTemplate.format(href, bodyContent)
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
private fun handleImageNode(
    image: Image
) : Node {
    val base64 = Base64.encodeToString(image.image, Base64.NO_WRAP)
    val dataUri = "data:image/${image.path.substringAfterLast(".")};base64,$base64"
    return Element("img").attr("src", dataUri)
}