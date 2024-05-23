package com.github.gurgenky.epubify.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.JsoupOutput
import com.github.gurgenky.epubify.model.XmlTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
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

internal fun File.parseDocument() : JsoupOutput {
    val document = Jsoup.parse(this, "UTF-8")
    val bodyContent: String

    val bodyElement: org.jsoup.nodes.Element? = document.body()
    val title: String = document.selectFirst("h1, h2, h3, h4, h5, h6")?.text() ?: ""
    document.selectFirst("h1, h2, h3, h4, h5, h6")?.remove()
    bodyContent = bodyElement?.parentNode()?.let { getNodeStructuredText(it) } ?: ""

    return JsoupOutput(title, bodyContent)
}

private fun getNodeStructuredText(node: Node, singleNode: Boolean = false): String {
    val nodeActions = mapOf(
        "p" to { n: Node -> getPTraverse(n) },
        "br" to { "\n" },
        "hr" to { "\n\n" },
    )

    val action: (Node) -> String = { n: Node ->
        if (n is TextNode) {
            n.text().trim()
        } else {
            getNodeTextTraverse(n)
        }
    }

    val children = if (singleNode) listOf(node) else node.childNodes()
    return children.joinToString("") { child ->
        nodeActions[child.nodeName()]?.invoke(child) ?: action(child)
    }
}

private fun getPTraverse(node: Node): String {
    fun innerTraverse(node: Node): String =
        node.childNodes().joinToString("") { child ->
            when {
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child.nodeName() == "image" -> declareImgEntry(child)
                child is TextNode -> child.text()
                else -> innerTraverse(child)
            }
        }

    val paragraph = innerTraverse(node).trim()
    return if (paragraph.isNotEmpty()) "$paragraph\n\n" else ""
}

private fun getNodeTextTraverse(node: Node): String {
    val children = node.childNodes()
    if (children.isEmpty())
        return ""

    return children.joinToString("") { child ->
        when {
            child.nodeName() == "p" -> getPTraverse(child)
            child.nodeName() == "br" -> "\n"
            child.nodeName() == "hr" -> "\n\n"
            child.nodeName() == "img" -> declareImgEntry(child)
            child.nodeName() == "image" -> declareImgEntry(child)
            child is TextNode -> {
                val text = child.text().trim()
                if (text.isEmpty()) "" else text + "\n\n"
            }

            else -> getNodeTextTraverse(child)
        }
    }
}

private fun declareImgEntry(node: Node): String {
    val attrs = node.attributes().associate { it.key to it.value }
    val relPathEncoded = attrs["src"] ?: attrs["xlink:href"] ?: ""
//
//    val absolutePathImage = File(fileParentFolder, relPathEncoded.decodedURL)
//        .canonicalFile
//        .toPath()
//        .invariantSeparatorsPathString
//        .removePrefix("/")

    return "IMAGE_wnajkwndkwandkaw"
}
