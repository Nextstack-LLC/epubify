package com.github.gurgenky.epubify.model

/**
 * Data class representing the table of contents of an EPUB file.
 *
 * @property entries The list of entries in the table of contents.
 */
internal data class Toc(
    val entries: List<Entry>
) {

    /**
     * Data class representing an entry in the table of contents of an EPUB file.
     *
     * @property title The title of the entry.
     * @property href The href of the entry.
     * @property children The child entries of this entry.
     */
    internal data class Entry(
        val title: String,
        val href: String,
        val children: List<Entry> = emptyList()
    )
}