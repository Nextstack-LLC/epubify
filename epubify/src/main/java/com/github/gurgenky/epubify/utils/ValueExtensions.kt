package com.github.gurgenky.epubify.utils

import com.github.gurgenky.epubify.model.Book

private const val HTML_TEMPLATE = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content='width=device-width, initial-scale=1.0,text/html,charset=utf-8' >
        <title>%s</title>
        <style>
            img {
                max-width: calc(100%%);"
            }
            pre {
              line-height: 1;
              resize: both;
              overflow: auto;
              white-space: pre-wrap;
            }
        </style>
    </head>
    <body>
            %s
    </body>
    </html>
"""

/**
 * Extension function to convert a [Book] to an HTML string.
 *
 * @return The HTML representation of the book.
 */
internal val Book.asHtml: String
    get() {
        val body = chapters.joinToString("\n") { it.content }
        return HTML_TEMPLATE.format(title, body)
    }