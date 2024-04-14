package com.github.gurgenky.epubify

import com.github.gurgenky.epubify.parser.EpubParser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
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

        assertEquals("Sample .epub Book", epub.title)
        assertEquals("Thomas Hansen", epub.author)
        assertNull(epub.cover?.path)
        assertEquals(4, epub.chapters.size)
        assertEquals("toc.xhtml", epub.chapters[0].title)
        assertEquals("chapter_1.xhtml", epub.chapters[1].title)
        assertEquals("chapter_2.xhtml", epub.chapters[2].title)
        assertEquals("chapter_2.xhtml", epub.chapters[3].title)
    }

    @Test
    fun testParseEpubInputStream() = runTest {
        val file = getFileFromPath(this@ParserTest, "test1.epub")

        val epub = EpubParser.parse(file.inputStream())

        assertEquals("Sample .epub Book", epub.title)
        assertEquals("Thomas Hansen", epub.author)
        assertNull(epub.cover?.path)
        assertEquals(4, epub.chapters.size)
        assertEquals("toc.xhtml", epub.chapters[0].title)
        assertEquals("chapter_1.xhtml", epub.chapters[1].title)
        assertEquals("chapter_2.xhtml", epub.chapters[2].title)
        assertEquals("chapter_2.xhtml", epub.chapters[3].title)
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