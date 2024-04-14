package com.github.gurgenky.epubify.model


/**
 * Data class representing the manifest of an EPUB file.
 *
 * @property items The list of items in the manifest.
 */
internal data class Manifest(
    val items: List<Item>
) {

    /**
     * Data class representing an item in the manifest of an EPUB file.
     *
     * @property id The ID of the item.
     * @property href The path to the item.
     * @property mediaType The media type of the item.
     */
    internal data class Item(
        val id: String,
        val href: String,
        val mediaType: String
    )
}