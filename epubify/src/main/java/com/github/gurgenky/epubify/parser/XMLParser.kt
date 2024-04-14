package com.github.gurgenky.epubify.parser

import com.github.gurgenky.epubify.model.XmlTag
import com.github.gurgenky.epubify.model.XmlTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.util.Stack

internal object XMLParser {

    suspend fun parse(file: File): XmlTree? {
        return withContext(Dispatchers.IO) {
            parseXMLFile(file)
        }
    }

    private suspend fun parseXMLFile(file: File): XmlTree? {
        if (file.exists().not())
            return null

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        return try {
            withContext(Dispatchers.IO) {
                parser.setInput(FileInputStream(file), null)
            }
            return parseTags(parser)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTags(parser: XmlPullParser): XmlTag? {
        var rootTag: XmlTag? = null
        var currentTag: XmlTag? = null
        val tagStack = Stack<XmlTag>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = XmlTag(parser.name)
                    for (i in 0 until parser.attributeCount) {
                        tag.attributes[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                    }
                    currentTag?.childTags?.add(tag)
                    tagStack.push(tag)
                    currentTag = tag
                    if (rootTag == null) {
                        rootTag = tag
                    }
                }
                XmlPullParser.TEXT -> {
                    currentTag?.content = parser.text
                }
                XmlPullParser.END_TAG -> {
                    tagStack.pop()
                    currentTag = if (tagStack.isEmpty()) null else tagStack.peek()
                }
            }
        }
        return rootTag
    }
}