package com.github.gurgenky.epubify.parser

import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist

/**
 * A whitelist for the EPUB parser.
 */
internal object EpubWhitelist : Safelist() {

    private val base64ImageRegex = Regex("^data:image/(png|jpe?g|gif);base64,[a-zA-Z0-9+/=]*$")

    init {
        addTags(
            "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
            "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
            "i", "img", "li", "ol", "p", "pre", "q", "small", "strike", "strong",
            "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
            "ul"
        )
        addAttributes("h1", "id")
        addAttributes("h2", "id")
        addAttributes("h3", "id")
        addAttributes("h4", "id")
        addAttributes("h5", "id")
        addAttributes("h6", "id")
        addAttributes("a", "href", "title")
        addAttributes("blockquote", "cite")
        addAttributes("col", "span", "width")
        addAttributes("colgroup", "span", "width")
        addAttributes("img", "align", "alt", "height", "src", "title", "width")
        addAttributes("ol", "start", "type")
        addAttributes("q", "cite")
        addAttributes("table", "summary", "width")
        addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
        addAttributes("th", "abbr", "axis", "colspan", "rowspan", "scope", "width")
        addAttributes("ul", "type")
        addProtocols("a", "href", "ftp", "http", "https", "mailto", "tel", "#")
        addProtocols("blockquote", "cite", "http", "https")
        addProtocols("cite", "cite", "http", "https")
        addProtocols("img", "src", "http", "https")
        addProtocols("q", "cite", "http", "https")
    }

    override fun isSafeAttribute(tagName: String, el: Element, attr: Attribute): Boolean {
        return when {
            tagName == "img" && "src" == attr.key -> {
                base64ImageRegex.matches(attr.value)
            }
            tagName == "a" && "href" == attr.key -> {
                isSafeHref(tagName, el, attr)
            }
            else -> super.isSafeAttribute(tagName, el, attr)
        }
    }

    /**
     * Checks if the href attribute is safe and modifies it if necessary.
     * @param tagName The tag name.
     * @param el The element.
     * @param attr The attribute.
     * @return Whether the href attribute is safe.
     */
    private fun isSafeHref(tagName: String, el: Element, attr: Attribute): Boolean {
        val href = attr.value
        val isChapterHref = href.contains(".xhtml") || href.contains(".html") || href.contains(".htm")
        if (isChapterHref) {
            val tagHref = href.split("#").getOrNull(1).orEmpty()
            attr.setValue("#$tagHref")
            return true
        }
        return super.isSafeAttribute(tagName, el, attr)
    }
}