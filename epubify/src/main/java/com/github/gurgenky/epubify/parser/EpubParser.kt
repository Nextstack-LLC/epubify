package com.github.gurgenky.epubify.parser

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.Book
import com.github.gurgenky.epubify.model.Chapter
import com.github.gurgenky.epubify.model.Image
import com.github.gurgenky.epubify.model.Manifest
import com.github.gurgenky.epubify.model.Metadata
import com.github.gurgenky.epubify.model.Spine
import com.github.gurgenky.epubify.model.TempChapter
import com.github.gurgenky.epubify.model.Toc
import com.github.gurgenky.epubify.model.XmlTag
import com.github.gurgenky.epubify.model.XmlTree
import com.github.gurgenky.epubify.utils.parseDocument
import com.github.gurgenky.epubify.utils.selectTag
import com.github.gurgenky.epubify.utils.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
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
     * @return The parsed book.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun parse(inputStream: InputStream): Book {
        val tempFile = inputStream.toTempFile()
        return parse(tempFile)
    }

    /**
     * Parses an EPUB file from a file path.
     * @param filePath The path of the EPUB file.
     * @return The parsed book.
     */
    suspend fun parse(filePath: String): Book {
        return parse(File(filePath))
    }

    /**
     * Parses an EPUB file from a file.
     * @param file The EPUB file.
     * @return The parsed book.
     */
    suspend fun parse(file: File): Book {
        return parseAndCreateEbook(file)
    }

    /**
     * Parses an EPUB file and creates a book object.
     * @param file The EPUB file.
     * @return The parsed book.
     */
    private suspend fun parseAndCreateEbook(file: File): Book =
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
                metadata
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
        val img = coverImage?.let { Image(coverItem.href, it) }

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
        metadata: Metadata
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
                orderedFiles,
                manifest,
                images
            )
        } else {
            parseChaptersWithSpine(
                orderedFiles,
                images
            )
        }.map {
            Chapter(it.title.orEmpty(), it.content)
        }

        return Book(title, author, cover, chapters, images)
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
        orderedFiles: List<File>,
        manifest: Manifest,
        images: List<Image>
    ): List<TempChapter> {
        val filesBeforeToc = orderedFiles.takeWhile { file ->
            val item = manifest.items.find { item -> item.href == file.name }
            val tocItem = toc.entries.find { entry -> entry.href == item?.href }
            tocItem == null
        }

        return parseChaptersWithSpine(filesBeforeToc, images) + toc.entries.map { entry ->
            val content = if (entry.children.isNotEmpty()) {
                val outputs = entry.children.distinctBy {
                    val tocItemPath = it.href.split("#").first()
                    tocItemPath
                }.map {
                    val tocItemPath = it.href.split("#").first()
                    val item = manifest.items.find { item -> item.href == tocItemPath }
                    val file = File(parent.absolutePath + "/" + item?.href)
                    file.parseDocument(images)
                }
                TempChapter(entry.title, outputs.joinToString("\n\n") { it.content })
            } else {
                val tocItemPath = entry.href.split("#").first()
                val item = manifest.items.find { item -> item.href == tocItemPath }
                val file = File(parent.absolutePath + "/" + item?.href)
                file.parseDocument(images)
            }
            content
        }
    }

    /**
     * Parses the chapters of an EPUB file using ordered files from spine.
     * @param orderedFiles The ordered files of the EPUB file.
     * @param images The parsed images.
     * @return The parsed chapters.
     */
    private fun parseChaptersWithSpine(
        orderedFiles: List<File>,
        images: List<Image>
    ): List<TempChapter> {
        var chapterIndex = 0

        return orderedFiles.map {
            val output = it.parseDocument(images)

            val chapterTitle = output.title ?: if (chapterIndex == 0) "" else null
            if (chapterTitle != null)
                chapterIndex += 1

            output to chapterTitle
        }.groupBy {
            it.second
        }.map { (index, list) ->
            TempChapter(
                title = list.first().first.title ?: "Chapter $index",
                content = list.joinToString("\n\n") { it.first.content }
            )
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
    ): List<Image> {
        val imageExtensions = listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg")

        val unlistedImages = orderedFiles
            .asSequence()
            .filter { file ->
                imageExtensions.any { file.extension.equals(it, ignoreCase = true) }
            }
            .map { file ->
                Image(path = file.absolutePath, file.readBytes())
            }

        val listedImages = manifest.items
            .asSequence()
            .filter { item ->
                imageExtensions.any { item.href.endsWith(it, ignoreCase = true) }
            }
            .map { item ->
                val coverAbsolutePath = item.href.let { parent.absolutePath + "/" + it }
                Image(path = item.href, image = File(coverAbsolutePath).readBytes())
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
        for (list in lists.childTags) {
            val listItems = list.childTags.filter { it.name == "li" }
            for (item in listItems) {
                val link = item.selectTag("a")
                val text = link?.content ?: ""
                val href = link?.getAttributeValue("href") ?: ""

                val sublist = item.selectTag("ol") ?: item.selectTag("ul")
                val children = sublist?.let { parseLinks(it) } ?: emptyList()
                entries.add(Toc.Entry(text, href, children))
            }
        }

        return entries
    }
}