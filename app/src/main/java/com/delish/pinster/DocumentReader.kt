package com.delish.pinster

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipFile

object DocumentReader {

    private const val NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    fun read(context: Context, uri: Uri, fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return try {
            when {
                ext == "pdf" -> readPdf(context, uri)
                ext == "docx" -> readDocx(context, uri)
                ext == "xlsx" -> readXlsx(context, uri)
                ext == "pptx" -> readPptx(context, uri)
                ext in listOf("txt","log","md","csv","json","xml","html","css","js","kt","java","py","rb") -> readText(context, uri)
                ext == "rtf" -> readRtf(context, uri)
                else -> readText(context, uri)
            }
        } catch (e: Exception) {
            if (ext == "pdf") "[PDF extraction failed: ${e.message}]"
            else tryAnyZip(context, uri, fileName)
        }
    }

    // ── PDF via pdfbox-android ──────────────────────────────────────────

    private fun readPdf(context: Context, uri: Uri): String {
        val tmp = copyToTemp(context, uri) ?: return "[Could not open PDF file]"
        try {
            val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(tmp)
            try {
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                stripper.sortByPosition = false
                return stripper.getText(doc).trim()
            } finally { doc.close() }
        } finally { tmp.delete() }
    }

    // ── DOCX ───────────────────────────────────────────────────────────

    private fun readDocx(context: Context, uri: Uri): String {
        val tmp = copyToTemp(context, uri) ?: return "[Could not open file]"
        try {
            ZipFile(tmp).use { zip ->
                val mainEntry = zip.entries().asSequence().firstOrNull {
                    it.name == "word/document.xml" || it.name.endsWith("/document.xml")
                } ?: return "[Not a valid DOCX — no document.xml]"

                val mainXml = safeReadEntry(zip, mainEntry)
                val mainText = extractDocxParagraphs(mainXml)

                val extras = mutableListOf<String>()

                for (entryName in listOf("word/header1.xml", "word/header2.xml", "word/header3.xml")) {
                    val entry = zip.entries().asSequence().firstOrNull { it.name == entryName } ?: continue
                    val hdrXml = safeReadEntry(zip, entry)
                    val hdrText = extractDocxParagraphs(hdrXml)
                    if (hdrText.isNotBlank()) extras.add("--- Header ---\n$hdrText")
                }

                for (entryName in listOf("word/footer1.xml", "word/footer2.xml", "word/footer3.xml")) {
                    val entry = zip.entries().asSequence().firstOrNull { it.name == entryName } ?: continue
                    val ftrXml = safeReadEntry(zip, entry)
                    val ftrText = extractDocxParagraphs(ftrXml)
                    if (ftrText.isNotBlank()) extras.add("--- Footer ---\n$ftrText")
                }

                val footEntry = zip.entries().asSequence().firstOrNull { it.name == "word/footnotes.xml" }
                if (footEntry != null) {
                    val fnXml = safeReadEntry(zip, footEntry)
                    val fnText = extractDocxParagraphs(fnXml)
                    if (fnText.isNotBlank()) extras.add("--- Footnotes ---\n$fnText")
                }

                val all = mutableListOf<String>()
                if (mainText.isNotBlank()) all.add(mainText)
                all.addAll(extras)

                if (all.isEmpty()) {
                    // Last resort: strip ALL XML tags from the main document
                    val rawText = stripXmlTags(mainXml)
                    if (rawText.isNotBlank()) return rawText
                    return "[DOCX file has no readable text]"
                }
                return all.joinToString("\n\n").trim()
            }
        } finally { tmp.delete() }
    }

    private fun extractDocxParagraphs(xml: String): String {
        if (xml.isBlank()) return ""
        val result = tryXmlParser(xml)
        if (result.isNotBlank() && result.length > 10) return result
        return regexExtractWText(xml)
    }

    private fun tryXmlParser(xml: String): String {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            val paragraphs = mutableListOf<String>()
            val buf = StringBuilder()
            var inParagraph = false
            var inRun = false
            var isBold = false
            var isItalic = false
            var currentStyle = ""
            var outlineLevel = -1

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    val ns = parser.namespace ?: ""
                    val name = parser.name
                    when {
                        ns == NS_W && name == "p" -> { inParagraph = true; buf.clear(); currentStyle = ""; outlineLevel = -1 }
                        ns == NS_W && name == "r" -> { inRun = true; isBold = false; isItalic = false }
                        ns == NS_W && name == "pStyle" -> currentStyle = parser.getAttributeValue(NS_W, "val") ?: ""
                        ns == NS_W && name == "outlineLvl" -> outlineLevel = parser.getAttributeValue(NS_W, "val")?.toIntOrNull() ?: -1
                        ns == NS_W && name == "b" && inRun -> isBold = true
                        ns == NS_W && name == "i" && inRun -> isItalic = true
                        ns == NS_W && name == "t" -> {
                            val text = parser.nextText()
                            if (text.isNotBlank()) {
                                var t = text
                                if (isBold) t = "**$t**"
                                if (isItalic) t = "*$t*"
                                buf.append(t)
                            }
                        }
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG) {
                    val ns = parser.namespace ?: ""
                    val name = parser.name
                    if (ns == NS_W && name == "p" && inParagraph) {
                        inParagraph = false
                        val text = buf.toString().trim()
                        if (text.isNotBlank()) {
                            val formatted = when {
                                currentStyle.contains("Heading1", true) || outlineLevel == 0 -> "# $text"
                                currentStyle.contains("Heading2", true) || outlineLevel == 1 -> "## $text"
                                currentStyle.contains("Heading3", true) || outlineLevel == 2 -> "### $text"
                                currentStyle.contains("Heading4", true) || outlineLevel == 3 -> "#### $text"
                                else -> text
                            }
                            paragraphs.add(formatted)
                        }
                    } else if (ns == NS_W && name == "r") {
                        inRun = false
                    }
                }
                parser.next()
            }
            paragraphs.joinToString("\n\n")
        } catch (_: Exception) { "" }
    }

    private fun regexExtractWText(xml: String): String {
        if (xml.isBlank()) return ""

        val textPattern = Regex("""<w:t[^>]*>([^<]*)</w:t>""")
        val allTexts = textPattern.findAll(xml).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
        if (allTexts.isEmpty()) return ""

        val boldPattern = Regex("""<w:b\s*/>""")
        val italicPattern = Regex("""<w:i\s*/>""")
        val paraPattern = Regex("""<w:p[\s>].*?</w:p>""", RegexOption.DOT_MATCHES_ALL)

        val paragraphs = mutableListOf<String>()
        for (paraMatch in paraPattern.findAll(xml)) {
            val paraXml = paraMatch.value
            val texts = textPattern.findAll(paraXml).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
            val paraText = texts.joinToString("")
            if (paraText.isNotBlank()) {
                var formatted = paraText
                if (boldPattern.containsMatchIn(paraXml)) formatted = "**$formatted**"
                if (italicPattern.containsMatchIn(paraXml)) formatted = "*$formatted*"
                paragraphs.add(formatted)
            }
        }

        if (paragraphs.isNotEmpty()) return paragraphs.joinToString("\n\n")
        return allTexts.joinToString(" ")
    }

    private fun stripXmlTags(xml: String): String {
        return xml
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&apos;"), "'")
            .replace(Regex("&#\\d+;"), "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n\\s*\\n"), "\n\n")
            .lines()
            .filter { it.trim().isNotEmpty() && !it.trim().startsWith("<?xml") && !it.trim().startsWith("<!") && !it.trim().startsWith("<!--") }
            .joinToString("\n")
            .trim()
    }

    // ── XLSX ───────────────────────────────────────────────────────────

    private fun readXlsx(context: Context, uri: Uri): String {
        val tmp = copyToTemp(context, uri) ?: return "[Could not open file]"
        try {
            ZipFile(tmp).use { zip ->
                val sharedStrings = readSharedStrings(zip)
                val sheetNames = readSheetNames(zip)
                val result = StringBuilder()

                zip.entries().asSequence()
                    .filter { it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
                    .sortedBy { it.name }
                    .forEach { entry ->
                        val sheetNum = Regex("sheet(\\d+)").find(entry.name)?.groupValues?.get(1) ?: "?"
                        val sheetName = sheetNames[sheetNum.toIntOrNull()?.minus(1) ?: 0] ?: "Sheet $sheetNum"
                        val sheetXml = safeReadEntry(zip, entry)
                        val sheetData = parseSheetData(sheetXml, sharedStrings)
                        if (sheetData.isNotBlank()) {
                            result.appendLine("--- $sheetName ---")
                            result.appendLine()
                            result.append(sheetData)
                            result.appendLine()
                        }
                    }

                if (result.isBlank()) return "[XLSX file has no readable data]"
                return result.toString().trim()
            }
        } finally { tmp.delete() }
    }

    private fun readSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.entries().asSequence().firstOrNull {
            it.name == "xl/sharedStrings.xml" || it.name.endsWith("/sharedStrings.xml")
        } ?: return emptyList()

        val xml = safeReadEntry(zip, entry)
        val strings = mutableListOf<String>()
        val siPattern = Regex("""<si[\s>].*?</si>""", RegexOption.DOT_MATCHES_ALL)
        val tPattern = Regex("""<t[^>]*>([^<]*)</t>""")
        for (si in siPattern.findAll(xml)) {
            val texts = tPattern.findAll(si.value).map { it.groupValues[1] }.toList()
            strings.add(texts.joinToString(""))
        }
        return strings
    }

    private fun readSheetNames(zip: ZipFile): Map<Int, String> {
        val entry = zip.entries().asSequence().firstOrNull {
            it.name == "xl/workbook.xml" || it.name.endsWith("/workbook.xml")
        } ?: return emptyMap()

        val xml = safeReadEntry(zip, entry)
        val names = mutableMapOf<Int, String>()
        val pattern = Regex("""<sheet\s[^>]*name="([^"]+)""")
        var idx = 0
        for (match in pattern.findAll(xml)) {
            names[idx] = match.groupValues[1]
            idx++
        }
        return names
    }

    private fun parseSheetData(xml: String, sharedStrings: List<String>): String {
        if (xml.isBlank()) return ""
        val result = StringBuilder()
        val rowPattern = Regex("""<row[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL)
        val cellPattern = Regex("""<c[^>]*r="([^"]+)"[^>]*(?:t="([^"]+)")?[^>]*>(?:.*?<v>([^<]*)</v>)?""", RegexOption.DOT_MATCHES_ALL)

        for (rowMatch in rowPattern.findAll(xml)) {
            for (cellMatch in cellPattern.findAll(rowMatch.value)) {
                val cellRef = cellMatch.groupValues[1]
                val cellType = cellMatch.groupValues[2]
                val rawValue = cellMatch.groupValues[3]
                if (rawValue.isBlank()) continue

                val display = when (cellType) {
                    "s" -> { val idx = rawValue.toIntOrNull() ?: -1; if (idx in sharedStrings.indices) sharedStrings[idx] else rawValue }
                    "b" -> if (rawValue == "1") "TRUE" else "FALSE"
                    else -> rawValue
                }
                if (display.isNotBlank()) result.appendLine("$cellRef: $display")
            }
            result.appendLine()
        }
        return result.toString()
    }

    // ── PPTX ───────────────────────────────────────────────────────────

    private fun readPptx(context: Context, uri: Uri): String {
        val tmp = copyToTemp(context, uri) ?: return "[Could not open file]"
        try {
            ZipFile(tmp).use { zip ->
                val slides = zip.entries().asSequence()
                    .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                    .sortedBy { it.name }
                    .toList()

                val result = StringBuilder()
                for (slide in slides) {
                    val slideNum = Regex("slide(\\d+)").find(slide.name)?.groupValues?.get(1) ?: "?"
                    val slideXml = safeReadEntry(zip, slide)
                    val texts = Regex("""<a:t[^>]*>([^<]*)</a:t>""").findAll(slideXml)
                        .map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
                    if (texts.isNotEmpty()) {
                        result.appendLine("--- Slide $slideNum ---")
                        result.appendLine()
                        result.appendLine(texts.joinToString(" "))
                        result.appendLine()
                    }
                }
                if (result.isBlank()) return "[PPTX file has no readable text]"
                return result.toString().trim()
            }
        } finally { tmp.delete() }
    }

    // ── Plain text ─────────────────────────────────────────────────────

    private fun readText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val reader = InputStreamReader(raw, Charsets.UTF_8)
            val sb = StringBuilder()
            val buf = CharArray(8192)
            var read: Int
            while (reader.read(buf).also { read = it } != -1) {
                sb.append(buf, 0, read)
            }
            val text = sb.toString()
            if (text.isBlank()) return "[File is empty]"
            return text.trim()
        } ?: return "[Could not open file]"
    }

    // ── RTF ────────────────────────────────────────────────────────────

    private fun readRtf(context: Context, uri: Uri): String {
        val raw = readText(context, uri)
        val plain = raw
            .replace(Regex("\\\\[a-zA-Z]+\\d*\\s?"), "")
            .replace(Regex("\\{[^}]*}"), "")
            .replace(Regex("\\\\['\"\\\\{}]"), "")
            .replace(Regex("[ ]+"), " ")
            .trim()
        return if (plain.isBlank()) "[File is empty]" else plain
    }

    // ── Universal ZIP fallback ─────────────────────────────────────────

    private fun tryAnyZip(context: Context, uri: Uri, fileName: String): String {
        val tmp = copyToTemp(context, uri) ?: return "[Could not open file]"
        try {
            ZipFile(tmp).use { zip ->
                val result = StringBuilder()

                val importantEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.size > 0 }
                    .sortedBy {
                        when {
                            it.name == "word/document.xml" -> 0
                            it.name.matches(Regex("word/header\\d*\\.xml")) -> 1
                            it.name.matches(Regex("word/footer\\d*\\.xml")) -> 2
                            it.name.matches(Regex("xl/sharedStrings\\.xml")) -> 3
                            it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) -> 4
                            it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) -> 5
                            it.name.endsWith(".xml") -> 10
                            else -> 20
                        }
                    }
                    .toList()

                for (entry in importantEntries.take(20)) {
                    try {
                        val content = safeReadEntry(zip, entry)
                        if (content.isBlank()) continue

                        if (content.startsWith("<?xml") || content.startsWith("<") || content.contains("<w:") || content.contains("<a:")) {
                            val text = stripXmlTags(content)
                            if (text.isNotBlank() && text.length > 5) {
                                val shortName = entry.name.substringAfterLast('/')
                                result.appendLine("--- $shortName ---")
                                result.appendLine(text)
                                result.appendLine()
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (result.isNotBlank()) return result.toString().trim()
                return "[Could not extract readable text from $fileName]"
            }
        } finally { tmp.delete() }
    }

    // ── Utilities ──────────────────────────────────────────────────────

    private fun safeReadEntry(zip: ZipFile, entry: java.util.zip.ZipEntry): String {
        return try {
            zip.getInputStream(entry).use { stream ->
                val bytes = stream.readBytes()
                try {
                    String(bytes, Charsets.UTF_8)
                } catch (_: Exception) {
                    try {
                        String(bytes, Charsets.ISO_8859_1)
                    } catch (_: Exception) {
                        ""
                    }
                }
            }
        } catch (_: Exception) { "" }
    }

    private fun copyToTemp(context: Context, uri: Uri): File? {
        return try {
            val tmp = File(context.cacheDir, "doc_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tmp
        } catch (e: Exception) {
            null
        }
    }
}
