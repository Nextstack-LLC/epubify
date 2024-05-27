package com.github.gurgenky.epubify.utils

import android.content.Context
import android.content.res.Resources
import com.github.gurgenky.epubify.R
import com.github.gurgenky.epubify.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The template for a chapter in the HTML representation of a book.
 */
private val chapterTemplate = """
    <div class="chapter">
        %s
    </div>
""".trimIndent()

/**
 * Extension function to convert a [Book] to an HTML string.
 *
 * @return The HTML representation of the book.
 */
internal suspend fun Book.asHtml(
    context: Context
): String {
        val htmlTemplate = withContext(Dispatchers.IO) {
            context.resources.readRawResource(R.raw.epub_template)
        }

        val body = chapters.joinToString("\n") {
            chapterTemplate.format(it.content)
        }
        return htmlTemplate.format(title, body)
    }

/**
 * Reads a raw resource file and returns its content as a string.
 */
internal fun Resources.readRawResource(resourceId: Int): String {
    return openRawResource(resourceId).bufferedReader().use { it.readText() }
}