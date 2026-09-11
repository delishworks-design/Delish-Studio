package com.delish.pinster

import android.content.Context
import android.graphics.*

data class TypographySystem(
    val headlineTypeface: Typeface,
    val supportTypeface: Typeface,
    val eyebrowTypeface: Typeface,
    val labelTypeface: Typeface,
    val headlineWeight: Int,
    val supportWeight: Int,
    val labelWeight: Int,
    val headlineTransform: TextTransform,
    val supportTransform: TextTransform,
    val labelTransform: TextTransform,
    val headlineLineSpacing: Float,
    val supportLineSpacing: Float,
    val tracking: Float,
    val labelTracking: Float
)

enum class TextTransform { NONE, UPPERCASE, TITLE_CASE, LOWERCASE }

object TypographyEngine {

    private var playfair: Typeface? = null
    private var playfairBold: Typeface? = null
    private var inter: Typeface? = null
    private var nunito: Typeface? = null
    private var dmSerif: Typeface? = null
    private var spaceGrotesk: Typeface? = null
    private var spaceGroteskBold: Typeface? = null
    private var outfit: Typeface? = null
    private var outfitBold: Typeface? = null

    fun init(ctx: Context) {
        try {
            playfair = Typeface.createFromAsset(ctx.assets, "fonts/PlayfairDisplay.ttf")
            playfairBold = Typeface.create(playfair, Typeface.BOLD)
            inter = Typeface.createFromAsset(ctx.assets, "fonts/Inter.ttf")
            nunito = Typeface.createFromAsset(ctx.assets, "fonts/Nunito.ttf")
            dmSerif = Typeface.createFromAsset(ctx.assets, "fonts/DMSerifDisplay-Regular.ttf")
            spaceGrotesk = Typeface.createFromAsset(ctx.assets, "fonts/SpaceGrotesk-Regular.ttf")
            spaceGroteskBold = Typeface.createFromAsset(ctx.assets, "fonts/SpaceGrotesk-Bold.ttf")
            outfit = Typeface.createFromAsset(ctx.assets, "fonts/Outfit-Regular.ttf")
            outfitBold = Typeface.createFromAsset(ctx.assets, "fonts/Outfit-Bold.ttf")
        } catch (_: Exception) {
            playfair = Typeface.SERIF
            playfairBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            inter = Typeface.DEFAULT
            nunito = Typeface.DEFAULT
            dmSerif = Typeface.SERIF
            spaceGrotesk = Typeface.DEFAULT
            spaceGroteskBold = Typeface.DEFAULT_BOLD
            outfit = Typeface.DEFAULT
            outfitBold = Typeface.DEFAULT_BOLD
        }
    }

    fun systemFor(direction: ArtDirection, variation: Int, concept: CreativeConcept): TypographySystem {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> TypographySystem(
                headlineTypeface = playfairBold ?: dmSerif ?: Typeface.SERIF,
                supportTypeface = inter ?: Typeface.DEFAULT,
                eyebrowTypeface = inter ?: Typeface.DEFAULT,
                labelTypeface = inter ?: Typeface.DEFAULT,
                headlineWeight = Typeface.BOLD,
                supportWeight = Typeface.NORMAL,
                labelWeight = Typeface.NORMAL,
                headlineTransform = if (variation % 2 == 0) TextTransform.UPPERCASE else TextTransform.TITLE_CASE,
                supportTransform = TextTransform.NONE,
                labelTransform = TextTransform.UPPERCASE,
                headlineLineSpacing = 1.08f,
                supportLineSpacing = 1.4f,
                tracking = 0.06f,
                labelTracking = 0.20f
            )
            ArtDirection.MODERN_MINIMAL -> TypographySystem(
                headlineTypeface = spaceGroteskBold ?: Typeface.DEFAULT_BOLD,
                supportTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                eyebrowTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                labelTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                headlineWeight = Typeface.BOLD,
                supportWeight = Typeface.NORMAL,
                labelWeight = Typeface.NORMAL,
                headlineTransform = if (variation % 2 == 0) TextTransform.UPPERCASE else TextTransform.TITLE_CASE,
                supportTransform = TextTransform.NONE,
                labelTransform = TextTransform.UPPERCASE,
                headlineLineSpacing = 1.08f,
                supportLineSpacing = 1.35f,
                tracking = 0.01f,
                labelTracking = 0.16f
            )
            ArtDirection.ORGANIC_LIFESTYLE -> TypographySystem(
                headlineTypeface = dmSerif ?: Typeface.DEFAULT,
                supportTypeface = nunito ?: Typeface.DEFAULT,
                eyebrowTypeface = outfit ?: Typeface.DEFAULT,
                labelTypeface = outfit ?: Typeface.DEFAULT,
                headlineWeight = Typeface.BOLD,
                supportWeight = Typeface.NORMAL,
                labelWeight = Typeface.NORMAL,
                headlineTransform = TextTransform.TITLE_CASE,
                supportTransform = TextTransform.NONE,
                labelTransform = TextTransform.UPPERCASE,
                headlineLineSpacing = 1.12f,
                supportLineSpacing = 1.45f,
                tracking = 0.01f,
                labelTracking = 0.14f
            )
            ArtDirection.BOLD_CAMPAIGN -> TypographySystem(
                headlineTypeface = outfitBold ?: Typeface.DEFAULT_BOLD,
                supportTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                eyebrowTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                labelTypeface = spaceGrotesk ?: Typeface.DEFAULT,
                headlineWeight = Typeface.BOLD,
                supportWeight = Typeface.NORMAL,
                labelWeight = Typeface.NORMAL,
                headlineTransform = TextTransform.UPPERCASE,
                supportTransform = TextTransform.NONE,
                labelTransform = TextTransform.UPPERCASE,
                headlineLineSpacing = 1.02f,
                supportLineSpacing = 1.35f,
                tracking = 0.03f,
                labelTracking = 0.18f
            )
            ArtDirection.SOFT_PINTEREST -> TypographySystem(
                headlineTypeface = playfairBold ?: dmSerif ?: Typeface.DEFAULT_BOLD,
                supportTypeface = nunito ?: Typeface.DEFAULT,
                eyebrowTypeface = outfit ?: Typeface.DEFAULT,
                labelTypeface = outfit ?: Typeface.DEFAULT,
                headlineWeight = Typeface.BOLD,
                supportWeight = Typeface.NORMAL,
                labelWeight = Typeface.NORMAL,
                headlineTransform = TextTransform.TITLE_CASE,
                supportTransform = TextTransform.NONE,
                labelTransform = TextTransform.UPPERCASE,
                headlineLineSpacing = 1.10f,
                supportLineSpacing = 1.4f,
                tracking = 0.01f,
                labelTracking = 0.12f
            )
        }
    }

    // ─── TITLE CLEANUP ───────────────────────────

    private val noisePatterns = listOf(
        Regex("\\b\\d+[\\s]*[xX\u00d7]\\d+\\b"),
        Regex("\\b\\d+\\.?\\d*\\s*(inch|inches|cm|mm|ft|feet|yard|yd)s?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d+\\s*(pack|pcs|piece|count|set|pcs\\.?|pk)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(red|blue|black|white|green|gray|grey|brown|pink|purple|orange|yellow|navy|beige|silver|gold|charcoal|ivory|cream|tan|maroon|olive|teal|cyan|magenta|coral|salmon|burgundy|camel|khaki|wine|rose|blush|natural|transparent|clear)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(v\\d+|version\\s*\\d+|gen\\s*\\d+|generation\\s*\\d+)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(new|latest|20\\d{2}|upgrade|upgraded|improved|enhanced|premium|pro|plus|max|ultra|elite|deluxe|professional|commercial|industrial|heavy.duty|reinforced)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(free\\s*shipping|fast\\s*delivery|limited\\s*time|best\\s*seller|hot\\s*deal|sale|discount|\\d+%\\s*off|save|promo|coupon)\\b", RegexOption.IGNORE_CASE),
        Regex("[,;:!?\u2013\u2014\u2015/&@#\$%\u00b0\"'()\\[\\]{}<>|\\\\]"),
        Regex("\\s+"),
    )

    private val fillerWords = setOf(
        "the","a","an","and","or","but","in","on","at","to","for","of","with","by",
        "from","as","is","was","are","were","be","been","being","has","have","had",
        "do","does","did","will","would","could","should","may","might","can","shall",
        "that","this","these","those","it","its","not","no","nor","so","if","then",
        "than","too","very","just","also","back","even","still","new","old","great",
        "good","best","top","high","low","big","large","small","little","long",
        "short","wide","deep","full","half","double","single","one","two","three",
        "free","shipping","fast","delivery","limited","time","best","seller","hot",
        "deal","sale","discount","save","promo","coupon","premium","pro","plus",
        "max","ultra","elite","professional","commercial","industrial","heavy","duty",
        "reinforced","upgrade","upgraded","improved","enhanced","deluxe","latest",
        "package","includes","comes","feature","features","specifications","material",
        "dimensions","color","size","weight","capacity","warranty","guarantee",
        "made","china","usa","imported","original","genuine","authentic","certified",
        "approved","tested","verified"
    )

    fun cleanTitle(raw: String): String {
        var s = raw.trim()
        for (p in noisePatterns) s = p.replace(s, " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        val words = s.split(" ").filter { w -> w.length > 1 && !fillerWords.contains(w.lowercase()) }
        var result = words.joinToString(" ").replace(Regex("\\s+"), " ").trim()
        if (result.length > 55) {
            val wl = result.split(" "); val sb = StringBuilder()
            for (w in wl) { if (sb.length + w.length + 1 > 50) break; if (sb.isNotEmpty()) sb.append(" "); sb.append(w) }
            result = sb.toString()
        }
        return if (result.isBlank()) "PRODUCT" else result
    }

    fun headlineLines(hook: String): List<String> {
        val raw = hook.trim()
        val lines = raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 2) return lines.map { transform(TextTransform.UPPERCASE, it) }
        val words = raw.split(" ").filter { it.isNotBlank() }
        if (words.size <= 3) return listOf(transform(TextTransform.UPPERCASE, raw))
        if (words.size <= 6) {
            val mid = words.size / 2
            return listOf(
                transform(TextTransform.UPPERCASE, words.take(mid).joinToString(" ")),
                transform(TextTransform.UPPERCASE, words.drop(mid).joinToString(" "))
            )
        }
        val third = words.size / 3
        return listOf(
            transform(TextTransform.UPPERCASE, words.take(third).joinToString(" ")),
            transform(TextTransform.UPPERCASE, words.drop(third).take(third).joinToString(" ")),
            transform(TextTransform.UPPERCASE, words.drop(third * 2).joinToString(" "))
        )
    }

    fun extractSupport(raw: String, maxChars: Int = 70): String {
        var s = raw.trim().replace(Regex("\\s+"), " ")
        val sentences = s.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length in 10..100 }
        for (sent in sentences) {
            if (sent.length <= maxChars) {
                var r = sent.trim(); if (!r.endsWith(".") && r.length < maxChars - 3) r += "."; return r
            }
        }
        return ""
    }

    fun transform(t: TextTransform, text: String): String {
        return when (t) {
            TextTransform.UPPERCASE -> text.uppercase()
            TextTransform.TITLE_CASE -> text.split(" ").joinToString(" ") { w ->
                if (w.length > 3) w.lowercase().replaceFirstChar { it.uppercase() } else w.lowercase()
            }
            TextTransform.LOWERCASE -> text.lowercase()
            TextTransform.NONE -> text
        }
    }

    // ─── TEXT MEASUREMENT ────────────────────────

    fun fitHeadline(paint: Paint, lines: List<String>, maxW: Float, start: Float, min: Float, maxH: Float = Float.MAX_VALUE, lineSpacing: Float = 1.1f): Float {
        var sz = start
        while (sz >= min) {
            paint.textSize = sz
            val fitsWidth = lines.maxOfOrNull { paint.measureText(it) }?.let { it <= maxW } == true
            val totalHeight = lines.size * sz * lineSpacing
            val fitsHeight = totalHeight <= maxH
            if (fitsWidth && fitsHeight) return sz
            sz -= 2f
        }
        return min
    }

    fun fitSupport(paint: Paint, lines: List<String>, maxW: Float, start: Float, min: Float, maxH: Float = Float.MAX_VALUE, lineSpacing: Float = 1.35f): Float {
        var sz = start
        while (sz >= min) {
            paint.textSize = sz
            val fitsWidth = lines.maxOfOrNull { paint.measureText(it) }?.let { it <= maxW } == true
            val totalHeight = lines.size * sz * lineSpacing
            val fitsHeight = totalHeight <= maxH
            if (fitsWidth && fitsHeight) return sz
            sz -= 1f
        }
        return min
    }

    fun wrapText(text: String, paint: Paint, maxW: Float): List<String> {
        val words = text.split(" "); val lines = mutableListOf<String>(); var cur = ""
        for (w in words) {
            val test = if (cur.isEmpty()) w else "$cur $w"
            if (paint.measureText(test) > maxW && cur.isNotEmpty()) { lines.add(cur); cur = w } else cur = test
        }
        if (cur.isNotEmpty()) lines.add(cur)
        return lines
    }

    fun drawLines(c: Canvas, lines: List<String>, paint: Paint, x: Float, startY: Float, maxLines: Int, lineSpacing: Float = 1.3f) {
        var y = startY
        for (line in lines.take(maxLines)) {
            c.drawText(line, x, y + paint.textSize, paint)
            y += paint.textSize * lineSpacing
        }
    }

    fun drawEyebrow(c: Canvas, text: String, paint: Paint, x: Float, y: Float, align: TextAlign) {
        paint.textAlign = when (align) {
            TextAlign.LEFT -> Paint.Align.LEFT
            TextAlign.CENTER -> Paint.Align.CENTER
            TextAlign.RIGHT -> Paint.Align.RIGHT
        }
        c.drawText(text, x, y, paint)
    }

    fun drawLabel(c: Canvas, text: String, paint: Paint, rect: RectF, align: TextAlign) {
        val x = when (align) {
            TextAlign.LEFT -> rect.left
            TextAlign.CENTER -> rect.centerX()
            TextAlign.RIGHT -> rect.right
        }
        paint.textAlign = when (align) {
            TextAlign.LEFT -> Paint.Align.LEFT
            TextAlign.CENTER -> Paint.Align.CENTER
            TextAlign.RIGHT -> Paint.Align.RIGHT
        }
        c.drawText(text, x, rect.top + paint.textSize, paint)
    }
}
