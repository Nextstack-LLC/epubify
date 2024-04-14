package com.github.gurgenky.epubify.parser

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.gurgenky.epubify.model.Book
import com.github.gurgenky.epubify.model.Chapter
import com.github.gurgenky.epubify.model.Image
import com.github.gurgenky.epubify.model.Manifest
import com.github.gurgenky.epubify.model.Metadata
import com.github.gurgenky.epubify.model.Spine
import com.github.gurgenky.epubify.model.XmlTag
import com.github.gurgenky.epubify.model.XmlTree
import com.github.gurgenky.epubify.utils.selectTag
import com.github.gurgenky.epubify.utils.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

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
    suspend fun parse(inputStream: FileInputStream): Book {
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

            val spine = parseSpine(spineTag)

            val orderedDocuments = getOrderedDocuments(
                epubRoot,
                manifest,
                spine
            )

            return@withContext createBook(
                epubRoot,
                manifest,
                metadata,
                orderedDocuments
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
                Manifest.Item(id, href, mediaType)
            }

        return Manifest(items)
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
     * Gets the ordered documents of an EPUB file.
     * @param obfDirectory The OBF directory.
     * @param manifest The parsed manifest.
     * @param spine The parsed spine.
     * @return The ordered documents.
     */
    private fun getOrderedDocuments(
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
     * Creates a book object from parsed data.
     * @param parent The parent file of the EPUB file.
     * @param manifest The parsed manifest.
     * @param metadata The parsed metadata.
     * @param orderedDocuments The ordered documents.
     * @return The created book.
     */
    private fun createBook(
        parent: File,
        manifest: Manifest,
        metadata: Metadata,
        orderedDocuments: List<File>
    ): Book {
        val title = metadata.title
        val author = metadata.author
        val cover = metadata.cover
        val chapters = orderedDocuments.map { parseChapter(it) }

        val images = parseImages(parent, manifest, orderedDocuments)

        return Book(title, author, cover, chapters, images)
    }

    /**
     * Parses a chapter from a file.
     * @param file The file of the chapter.
     * @return The parsed chapter.
     */
    private fun parseChapter(file: File): Chapter {
        val document = Jsoup.parse(file, "UTF-8")

        val title = document.title()
        val content = document.body().html()

        return Chapter(title, content)
    }


    /**
     * Parses images from an EPUB file.
     * @param parent The parent file of the EPUB file.
     * @param manifest The parsed manifest.
     * @param files The files of the EPUB file.
     * @return The parsed images.
     */
    private fun parseImages(
        parent: File,
        manifest: Manifest,
        files: List<File>
    ): List<Image> {
        val imageExtensions = listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg")

        val unlistedImages = files
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
}