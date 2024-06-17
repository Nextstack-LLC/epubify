package org.nextstack.epubify.parser

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import org.nextstack.epubify.model.format.Manifest
import org.nextstack.epubify.model.format.Spine
import org.nextstack.epubify.model.TempChapter
import org.nextstack.epubify.model.ParseOptions
import org.nextstack.epubify.model.format.Toc
import org.nextstack.epubify.model.format.XmlTag
import org.nextstack.epubify.model.XmlTree
import org.nextstack.epubify.model.style.CustomFont
import org.nextstack.epubify.model.style.Style
import org.nextstack.epubify.model.format.Metadata
import org.nextstack.epubify.utils.allChildren
import org.nextstack.epubify.utils.bodyDefaultFontsTemplate
import org.nextstack.epubify.utils.flatten
import org.nextstack.epubify.utils.fontFaceRegex
import org.nextstack.epubify.utils.fontFamilyDeclarationTemplate
import org.nextstack.epubify.utils.fontFamilyRegex
import org.nextstack.epubify.utils.fontUrlRegex
import org.nextstack.epubify.utils.parseDocument
import org.nextstack.epubify.utils.selectTag
import org.nextstack.epubify.utils.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.nextstack.epubify.model.Book
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * This object is responsible for parsing EPUB files.
 */
internal object EpubParser {

    /**
     * Parses an EPUB file from an input stream.
     * @param inputStream The input stream of the EPUB file.
     * @param options Options for parsing the book.
     * @return The parsed book.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun parse(inputStream: InputStream, options: ParseOptions): Book {
        val tempFile = inputStream.toTempFile()
        return parse(tempFile, options)
    }

    /**
     * Parses an EPUB file from a file path.
     * @param filePath The path of the EPUB file.
     * @param options Options for parsing the book.
     * @return The parsed book.
     */
    suspend fun parse(filePath: String, options: ParseOptions): Book {
        return parse(File(filePath), options)
    }

    /**
     * Parses an EPUB file from a file.
     * @param file The EPUB file.
     * @param options Options for parsing the book.
     * @return The parsed book.
     */
    suspend fun parse(file: File, options: ParseOptions): Book {
        return parseAndCreateEbook(file, options)
    }

    /**
     * Parses an EPUB file and creates a book object.
     * @param file The EPUB file.
     * @param options Options for parsing the book.
     * @return The parsed book.
     */
    private suspend fun parseAndCreateEbook(file: File, options: ParseOptions): Book =
        withContext(Dispatchers.IO) {
            val extractionRoot = file.parentFile?.resolve("extract")
                ?: throw FileNotFoundException("Failed to create temp directory or file not found")

            val zipFile = ZipFile(file)
            zipFile.extractAll(extractionRoot.path)

            val opfFile = extractionRoot.walkTopDown()
                .find { it.extension == "opf" }
                ?: throw FileNotFoundException(".opf file not found")

            val epubRoot = opfFile.parentFile as File

            val document = parseXMLFile(opfFile)
                ?: throw Exception(".opf file failed to parse data")
            val metadataTag = document.selectTag("metadata")
                ?: throw Exception(".opf file metadata section missing")
            val manifestTag = document.selectTag("manifest")
                ?: throw Exception(".opf file manifest section missing")
            val spineTag = document.selectTag("spine")
                ?: throw Exception(".opf file spine section missing")

            val manifest = parseManifest(manifestTag)

            val metadata = parseMetadata(
                epubRoot,
                metadataTag,
                manifest
            )

            val tocItem = manifest.items.find {
                it.properties?.contains("nav") == true ||
                it.id.contains("toc", ignoreCase = true)
            }

            val tocFile = tocItem?.let { item ->
                File(epubRoot.absolutePath + "/" + item.href)
            }

            val toc = tocFile?.let { file -> parseXMLFile(file)?.let { parseToc(it, tocFile.extension) } }
            val spine = parseSpine(spineTag)

            return@withContext createBook(
                epubRoot,
                toc,
                spine,
                manifest,
                metadata,
                options
            ).also {
                extractionRoot.deleteRecursively()
            }
        }

    /**
     * Parses the manifest tag of an OPF file.
     * @param manifest The manifest tag.
     * @return The parsed manifest.
     */
    private fun parseManifest(manifest: XmlTag): Manifest {
        val items = manifest.childTags
            .filter { it.name == "item" }
            .map {
                val id = it.getAttributeValue("id") ?: ""
                val href = it.getAttributeValue("href") ?: ""
                val mediaType = it.getAttributeValue("media-type") ?: ""
                val properties = it.getAttributeValue("properties")
                Manifest.Item(id, href, mediaType, properties)
            }

        return Manifest(items)
    }

    /**
     * Parses the TOC of an EPUB file.
     * @param toc The TOC file.
     * @param extension The extension of the TOC file.
     * @return The parsed TOC.
     */
    private fun parseToc(toc: XmlTree, extension: String): Toc? {
        return when (extension) {
            "ncx" -> {
                val navMap = toc.selectTag("navMap") ?: return Toc(emptyList())
                val entries = parseNavPoints(navMap)
                Toc(entries)
            }
            "xhtml" -> {
                val body = toc.selectTag("body") ?: return Toc(emptyList())
                val entries = parseLinks(body)
                Toc(entries)
            }
            else -> null
        }
    }

    /**
     * Parses the metadata tag of an OPF file.
     * @param parent The parent file of the OPF file.
     * @param metadata The metadata tag.
     * @param manifest The parsed manifest.
     * @return The parsed metadata.
     */
    private fun parseMetadata(parent: File, metadata: XmlTag, manifest: Manifest): Metadata {
        val title = metadata.selectTag("dc:title")?.content ?: "Unknown Title"
        val author = metadata.selectTag("dc:creator")?.content ?: "Unknown Author"

        val coverItem =
            manifest.items.find { it.mediaType.contains("image") && it.id.contains("cover") }
        val coverImage = coverItem?.href?.let { File(parent.absolutePath + "/" + it).readBytes() }
        val img = coverImage?.let { org.nextstack.epubify.model.Image(coverItem.href, it) }

        return Metadata(title, author, img)
    }

    /**
     * Parses the spine tag of an OPF file.
     * @param spine The spine tag.
     * @return The parsed spine.
     */
    private fun parseSpine(spine: XmlTag): Spine {
        val itemrefs = spine.childTags
            .filter { it.name == "itemref" }
            .map {
                val idref = it.getAttributeValue("idref") ?: ""
                Spine.Itemref(idref)
            }

        return Spine(itemrefs)
    }

    /**
     * Gets the ordered documents of an EPUB file using spine.
     * @param obfDirectory The OBF directory.
     * @param manifest The parsed manifest.
     * @param spine The parsed spine.
     * @return The ordered documents.
     */
    private fun getOrderedFiles(
        obfDirectory: File?,
        manifest: Manifest,
        spine: Spine
    ): List<File> {
        val orderedFiles = mutableListOf<File>()

        for (itemref in spine.itemrefs) {
            val item = manifest.items.find { it.id == itemref.idref }
            if (item != null) {
                orderedFiles.add(File(obfDirectory?.absolutePath + "/" + item.href))
            }
        }

        return orderedFiles
    }

    /**
     * Parses an XML file.
     * @param file The XML file.
     * @return The parsed XML tree.
     */
    private suspend fun parseXMLFile(file: File): XmlTree? {
        return XMLParser.parse(file)
    }


    /**
     * Creates a book object from the parsed data.
     * @param parent The parent file of the EPUB file.
     * @param toc The parsed TOC.
     * @param spine The parsed spine.
     * @param manifest The parsed manifest.
     * @param metadata The parsed metadata.
     * @return The created book.
     */
    private fun createBook(
        parent: File,
        toc: Toc?,
        spine: Spine,
        manifest: Manifest,
        metadata: Metadata,
        configuration: ParseOptions
    ): Book {
        val title = metadata.title
        val author = metadata.author
        val cover = metadata.cover

        val orderedFiles = getOrderedFiles(
            parent,
            manifest,
            spine
        )

        val images = parseImages(parent, manifest, orderedFiles)

        val chapters = if (toc != null) {
            parseChaptersUsingToc(
                parent,
                toc,
                manifest,
                orderedFiles,
                images
            )
        } else {
            parseChaptersWithSpine(
                manifest,
                orderedFiles,
                images
            )
        }.map {
            val tempChapter = it.second
            org.nextstack.epubify.model.Chapter(
                tempChapter.title.orEmpty(),
                tempChapter.content
            )
        }

        val styles = parseStyles(parent, configuration) + configuration.customStyles

        return Book(
            title,
            author,
            cover,
            chapters,
            images,
            styles
        )
    }

    /**
     * Parses the chapters of an EPUB file using the TOC.
     * @param parent The parent file of the EPUB file.
     * @param toc The parsed TOC.
     * @param orderedFiles The ordered files of the EPUB file.
     * @param images The parsed images.
     * @param manifest The parsed manifest.
     * @return The parsed chapters.
     */
    private fun parseChaptersUsingToc(
        parent: File,
        toc: Toc,
        manifest: Manifest,
        orderedFiles: List<File>,
        images: List<org.nextstack.epubify.model.Image>
    ): List<Pair<String, TempChapter>> {
        val filesMissingFromToc = orderedFiles.filter { file ->
            toc.flatten.none { entry ->
                val tocItemPath = entry.href.split("#").first()
                file.absolutePath.contains(tocItemPath)
            }
        }

        val chaptersFromSpine = parseChaptersWithSpine(manifest, filesMissingFromToc, images)
        val chaptersFromToc = toc.entries.map { entry ->
            if (entry.children.isNotEmpty()) {
                val tocItemPath = entry.href.split("#").first()

                val children = entry.allChildren.distinctBy {
                    val childPath = it.href.split("#").first()
                    childPath
                }.map { child ->
                    val childPath = child.href.split("#").first()
                    val item = manifest.items.find { item -> item.href == childPath }
                    val file = File(parent.absolutePath + "/" + item?.href)
                    file.parseDocument(item as Manifest.Item, images)
                }

                val merged = children.joinToString("\n") { it.content }
                tocItemPath to TempChapter(entry.title, merged)
            } else {
                val tocItemPath = entry.href.split("#").first()
                val item = manifest.items.find { item -> item.href == tocItemPath }
                val file = File(parent.absolutePath + "/" + item?.href)
                tocItemPath to file.parseDocument(item as Manifest.Item, images)
            }
        }

        return (chaptersFromSpine + chaptersFromToc).distinctBy {
            it.first.substringAfterLast("/")
        }.sortedBy { (path, _) ->
            orderedFiles.indexOfFirst { it.absolutePath.contains(path) }
        }
    }

    /**
     * Parses the chapters of an EPUB file using ordered files from spine.
     * @param manifest The parsed manifest.
     * @param orderedFiles The ordered files of the EPUB file.
     * @param images The parsed images.
     * @return The parsed chapters.
     */
    private fun parseChaptersWithSpine(
        manifest: Manifest,
        orderedFiles: List<File>,
        images: List<org.nextstack.epubify.model.Image>
    ): List<Pair<String, TempChapter>> {
        return orderedFiles.map {
            val item = manifest.items.find { item -> it.absolutePath.contains(item.href) }
            val output = it.parseDocument(item as Manifest.Item, images)

            it.path to output
        }
    }

    /**
     * Parses images from an EPUB file.
     * @param parent The parent file of the EPUB file.
     * @param manifest The parsed manifest.
     * @param orderedFiles The files of the EPUB file.
     * @return The parsed images.
     */
    private fun parseImages(
        parent: File,
        manifest: Manifest,
        orderedFiles: List<File>
    ): List<org.nextstack.epubify.model.Image> {
        val imageExtensions = listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg")

        val unlistedImages = orderedFiles
            .asSequence()
            .filter { file ->
                imageExtensions.any { file.extension.equals(it, ignoreCase = true) }
            }
            .map { file ->
                org.nextstack.epubify.model.Image(path = file.absolutePath, file.readBytes())
            }

        val listedImages = manifest.items
            .asSequence()
            .filter { item ->
                imageExtensions.any { item.href.endsWith(it, ignoreCase = true) }
            }
            .map { item ->
                val coverAbsolutePath = item.href.let { parent.absolutePath + "/" + it }
                org.nextstack.epubify.model.Image(
                    path = item.href,
                    image = File(coverAbsolutePath).readBytes()
                )
            }

        return (listedImages + unlistedImages).distinctBy { it.path }.toList()
    }

    /**
     * Parses the nav points of an NCX TOC file.
     * @param navPoint The nav point tag.
     * @return The parsed nav points.
     */
    private fun parseNavPoints(navPoint: XmlTag): List<Toc.Entry> {
        val entries = mutableListOf<Toc.Entry>()

        val navPoints = navPoint.childTags.filter { it.name == "navPoint" }
        for (point in navPoints) {
            val text = point.selectTag("navLabel")?.selectTag("text")?.content ?: ""
            val content = point.selectTag("content")?.getAttributeValue("src") ?: ""
            val children = parseNavPoints(point)
            entries.add(Toc.Entry(text, content, children))
        }

        return entries
    }

    /**
     * Parses the links of an XHTML TOC file.
     * @param body The body tag.
     * @return The parsed links.
     */
    private fun parseLinks(body: XmlTag): List<Toc.Entry> {
        val lists = body.selectTag("ol", true)
            ?: body.selectTag("ul", true)
            ?: return emptyList()

        val entries = mutableListOf<Toc.Entry>()

        for (item in lists.childTags) {
            if (item.name != "li") continue

            val text = item.selectTag("a")?.content ?: ""
            val content = item.selectTag("a")?.getAttributeValue("href") ?: ""

            val children = parseLinks(item)
            entries.add(Toc.Entry(text, content, children))
        }

        return entries
    }

    /**
     * Parses the CSS styles of an EPUB file.
     * @param file The root file of the EPUB file.
     * @param options Options for parsing the book.
     * @return The parsed CSS styles with base64 encoded fonts.
     */
    private fun parseStyles(file: File, options: ParseOptions): List<Style> {
        val cssFiles = file.parentFile?.walkTopDown()?.filter { it.extension == "css" }
        val fonts = file.parentFile?.walkTopDown()?.filter { it.extension == "ttf" || it.extension == "otf" }

        return cssFiles?.map {
            val content = it.readLines()
            val modified = content.joinToString("\n") { line ->
                when {
                    options.parseEpubFonts && fontUrlRegex.containsMatchIn(line) -> {
                        val url = line.substringAfter("url(").substringBefore(")").replace("\"", "")
                        val fontFile = fonts?.find { font -> font.name == url.substringAfterLast("/") }
                        if (fontFile != null) {
                            val fontContent = fontFile.readBytes()
                            val base64 = Base64.encodeToString(fontContent, Base64.NO_WRAP)
                            line.replace(url, "data:font/ttf;base64,$base64")
                        } else line
                    }
                    !options.parseEpubFonts && fontFamilyRegex.containsMatchIn(line) -> {
                        ""
                    }
                    else -> line
                }
            }
            val result = if (!options.parseEpubFonts) {
                replaceFontFaceDeclarations(modified, options.customFonts)
            } else modified

            Style(result)
        }?.toList() ?: emptyList()
    }

    /**
     * Removes @font-face declarations from a CSS file.
     * @param css The CSS file content.
     * @param customFonts The custom fonts.
     * @return The CSS file content without @font-face declarations.
     */
    private fun replaceFontFaceDeclarations(css: String, customFonts: List<CustomFont>): String {
        val contentWithoutFontFace = css.replace(fontFaceRegex, "")

        if (customFonts.isEmpty()) return contentWithoutFontFace

        val fonts = customFonts.map { font ->
            val base64 = Base64.encodeToString(font.bytes, Base64.NO_WRAP)
            font.name to fontFamilyDeclarationTemplate.format(font.name, base64)
        }

        val names = fonts.joinToString(", ") { "\'${it.first}\'" }
        val declarations = fonts.joinToString("\n") { it.second }
        val bodyFontStyle = bodyDefaultFontsTemplate.format(names)

        return "$declarations\n$bodyFontStyle\n$contentWithoutFontFace"
    }

}