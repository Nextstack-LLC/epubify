package org.nextstack.epubify.utils

import android.content.Context
import android.content.res.Resources
import org.nextstack.epubify.R
import org.nextstack.epubify.model.Book
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
internal suspend fun org.nextstack.epubify.model.Book.asHtml(
    context: Context
): String {
    val htmlTemplate = withContext(Dispatchers.IO) {
        context.resources.readRawResource(R.raw.epub_template)
    }

    val body = chapters.joinToString("\n") {
        chapterTemplate.format(it.content)
    }

    val style = styles.joinToString("\n") { it.css }

    return htmlTemplate.format(title, style, body)
}

/**
 * Reads a raw resource file and returns its content as a string.
 */
internal fun Resources.readRawResource(resourceId: Int): String {
    return openRawResource(resourceId).bufferedReader().use { it.readText() }
}