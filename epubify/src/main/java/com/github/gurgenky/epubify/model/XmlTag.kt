package com.github.gurgenky.epubify.model

/**
 * Data class representing an XML tag.
 *
 * @property name The name of the tag.
 */
internal class XmlTag(val name: String) {
    val attributes = mutableMapOf<String, String>()
    val childTags = mutableListOf<XmlTag>()

    var content: String? = null
        internal set

    fun getAttributeValue(name: String): String? {
        return attributes[name]
    }
}

internal typealias XmlTree = XmlTag