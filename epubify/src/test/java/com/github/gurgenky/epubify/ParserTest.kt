package com.github.gurgenky.epubify

import com.github.gurgenky.epubify.parser.EpubParser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.apache.commons.text.StringEscapeUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
class ParserTest {


    @Test
    fun testParseEpub() = runTest {
        val file = getFileFromPath(this@ParserTest, "test2.epub")

        val epub = EpubParser.parse(file)

        assertEquals("Accessible EPUB 3", epub.title)
        assertEquals("Matt Garrish", epub.author)
        assertNotNull(epub.cover?.path)

        // Chapters include the cover pages as well
        assertEquals(7, epub.chapters.size)

        val title = StringEscapeUtils.unescapeHtml4("Chapter&nbsp;1.&nbsp;Introduction")
        assertEquals(StringEscapeUtils.unescapeHtml4(title), epub.chapters[3].title)
    }

    @Test
    fun testParseEpubInputStream() = runTest {
        val file = getFileFromPath(this@ParserTest, "test1.epub")

        val epub = EpubParser.parse(file.inputStream())

        assertEquals("Sample .epub Book", epub.title)
        assertEquals("Thomas Hansen", epub.author)
        assertNull(epub.cover?.path)
        assertEquals(4, epub.chapters.size)
        assertEquals("Chapter 1", epub.chapters[1].title)
    }

    @Test
    fun testNotExistingFile() = runTest {
        val file = getFileFromPath(this@ParserTest, "not_existing.epub")

        runCatching {
            EpubParser.parse(file)
        }.onFailure {
            assertEquals(FileNotFoundException::class, it::class)
        }.onSuccess {
            assert(false)
        }
    }

    private fun getFileFromPath(obj: Any, fileName: String): File {
        val classLoader = obj.javaClass.getClassLoader()
        val resource = classLoader?.getResource(fileName)
        return File(resource?.path ?: "")
    }
}