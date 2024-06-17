package org.nextstack.epubify.model.format

/**
 * Data class representing the spine of an EPUB file.
 *
 * @property itemrefs The list of itemrefs in the spine.
 */
internal data class Spine(
    val itemrefs: List<Itemref>
) {

    /**
     * Data class representing an itemref in the spine of an EPUB file.
     *
     * @property idref The ID reference of the itemref.
     */
    internal data class Itemref(
        val idref: String
    )
}