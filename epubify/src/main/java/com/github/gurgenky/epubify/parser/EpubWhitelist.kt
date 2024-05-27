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
        addProtocols("a", "href", "ftp", "http", "https", "mailto")
        addProtocols("blockquote", "cite", "http", "https")
        addProtocols("cite", "cite", "http", "https")
        addProtocols("img", "src", "http", "https")
        addProtocols("q", "cite", "http", "https")
    }

    override fun isSafeAttribute(tagName: String, el: Element, attr: Attribute): Boolean {
        return ("img" == tagName && "src" == attr.key && base64ImageRegex.matches(attr.value))
                || super.isSafeAttribute(tagName, el, attr)
    }
}