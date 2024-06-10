package com.github.gurgenky.epubify.model.style

/**
 * Data class representing a custom font.
 *
 * @property name The name of the font.
 * @property bytes The bytes of the font.
 */
data class CustomFont(
    val name: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomFont

        if (name != other.name) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}