package org.nextstack.epubify.utils

/**
 * Regex for removing @font-face declarations from a CSS file.
 */
internal val fontFaceRegex = Regex("""@font-face\s*\{[^}]*\}""", RegexOption.DOT_MATCHES_ALL)

/**
 * Regex for extracting font urls.
 */
internal val fontUrlRegex: Regex = Regex("src:\\s*url\\(([^)]+\\.(woff|woff2|ttf|otf|eot))\\);")

/**
 * Regex for extracting font families.
 */
internal val fontFamilyRegex = Regex("""font-family:\s*([^;]+);""")