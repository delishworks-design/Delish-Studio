package com.delish.pinster

import android.graphics.*

data class PinterestAngle(
    val id: String,
    val title: String,
    val number: Int,
    val shortDescription: String,
    val artDirection: String,
    val promptInstructions: String,
    val completePrompt: String,
    var productBitmap: Bitmap,
    val productImageUrl: String? = null,
    var isSaved: Boolean = false
)

object PinterestAngles {

    data class AngleDefinition(
        val id: String,
        val title: String,
        val number: Int,
        val shortDescription: String,
        val artDirection: String,
        val promptInstructions: String
    )

    val definitions = listOf(
        AngleDefinition(
            id = "HERO_PRODUCT",
            title = "HERO PRODUCT",
            number = 1,
            shortDescription = "Premium commercial product showcase",
            artDirection = "Make the product the dominant visual subject with a premium commercial product-photography composition. The product should occupy the central focal area with strong visual hierarchy. Use a clean, uncluttered environment appropriate to the product category. Emphasize product identity, premium appearance, realistic materials, and professional lighting. The environment should complement the product — kitchen products in a modern kitchen, beauty products on an elegant vanity, home products in a sophisticated interior, office products in a premium workspace, fitness products in a realistic fitness environment.",
            promptInstructions = "Create a premium, photorealistic hero product photograph. The product is the clear hero of the composition. Use dramatic but natural lighting that highlights the product's materials and form. Create a believable environment appropriate to the product category. Professional commercial product photography style."
        ),
        AngleDefinition(
            id = "LIFESTYLE",
            title = "LIFESTYLE",
            number = 2,
            shortDescription = "Product naturally used in real-life situations",
            artDirection = "Show the product naturally being used in a believable real-life situation. The image should communicate: 'This is how this product fits into everyday life.' The scene must be appropriate to the product — coffee makers in morning coffee routines, kitchen tools in realistic food preparation, beauty products in skincare routines, office accessories in productive desk setups, home products naturally integrated into living spaces. The product must remain visually accurate and identifiable. The lifestyle scene must not overpower the product.",
            promptInstructions = "Create a lifestyle photograph showing the product naturally integrated into a believable everyday scenario. The product should be clearly visible and in use. Show real-life context that helps the viewer imagine owning and using this product. Warm, approachable, authentic feel. The product is recognizable and visually accurate."
        ),
        AngleDefinition(
            id = "PROBLEM_SOLUTION",
            title = "PROBLEM \u2192 SOLUTION",
            number = 3,
            shortDescription = "Visual storytelling: the problem this product solves",
            artDirection = "Visually communicate a relatable problem and show how the product naturally addresses it. Tell a simple visual story through the scene composition — show the 'before' state (clutter, difficulty, inconvenience) resolved by the product's presence. Do not create a literal advertisement with excessive text. Use visual storytelling through environment and context. Examples: storage product shows cluttered-to-organized transformation, kitchen gadget shows difficult preparation made easier, cleaning product shows messy area becoming clean, office product shows inefficient workspace becoming organized. The scene must be realistic and commercially attractive.",
            promptInstructions = "Create a problem-solution photograph that visually tells a story. Show a relatable everyday problem being solved by the product. Use environmental storytelling — the scene composition communicates the problem and the product as the natural solution. Realistic, commercially attractive, premium feel. No text overlays or infographics."
        ),
        AngleDefinition(
            id = "FEATURE_BENEFIT",
            title = "FEATURE / BENEFIT",
            number = 4,
            shortDescription = "Emphasizes the product's strongest verified feature",
            artDirection = "Visually emphasize the product's strongest useful feature or benefit. Use the actual product information to determine which feature or benefit should be highlighted. If the product has multiple functions, compact design, portability, large capacity, ergonomic design, adjustable settings, easy cleaning, organization benefits, or time-saving functionality — visually communicate the most relevant verified benefit. The image should remain premium and realistic rather than looking like an infographic. Show the benefit in context, not as a diagram.",
            promptInstructions = "Create a photograph that visually emphasizes the product's strongest verified feature or benefit. Show the benefit in a realistic context that helps the viewer understand the product's value. Premium, photorealistic commercial photography. The feature or benefit should be visually apparent through the scene composition. No infographic style — keep it natural and aspirational."
        ),
        AngleDefinition(
            id = "PINTEREST_INSPIRATION",
            title = "PINTEREST INSPIRATION",
            number = 5,
            shortDescription = "Aspirational, editorial, save-worthy Pinterest aesthetic",
            artDirection = "Create a highly aesthetic, aspirational Pinterest-style editorial image. This should feel like something users would naturally save to a Pinterest board. Prioritize beautiful composition, sophisticated environment, cohesive styling, natural lighting, premium editorial photography, tasteful props, realistic materials, visual harmony, and aspirational lifestyle. The product must still be clearly recognizable and accurately represented. Do not sacrifice product accuracy for aesthetics. The image should have strong save-worthy visual appeal — the kind of image that makes someone think 'I want this in my life.'",
            promptInstructions = "Create a beautiful, aspirational Pinterest editorial photograph. Sophisticated styling, cohesive aesthetic, natural lighting, premium feel. The product is featured prominently in a beautifully styled environment. Tasteful props and context that enhance the aspirational quality. The image should be immediately save-worthy — visually stunning, harmonious, and inspiring."
        )
    )
}

object PinterestAngleBuilder {

    private const val MASTER_QUALITY_HEADER = """Create a premium, photorealistic Pinterest image using the provided product image as the exact visual reference.

Preserve the actual product's identity, proportions, materials, colors, shape, and recognizable details. The product must NOT be redesigned, substituted, or altered in any way.

Use realistic commercial photography with natural lighting, physically believable shadows, realistic materials, authentic depth of field, professional composition, and high visual quality. The image should look like a professionally produced lifestyle or editorial product photograph rather than an AI-generated graphic.

Use a vertical Pinterest composition with a 2:3 aspect ratio. The product should remain clearly visible and visually important.

Avoid clutter, unnecessary decorative elements, unrealistic objects, distorted products, duplicate products, floating objects, malformed hands or people, fake product features, invented specifications, prices, promotional badges, watermarks, and unnecessary text."""

    private const val PRODUCT_IMAGE_REFERENCE = """PRODUCT IMAGE REFERENCE:
Use the attached product image as the exact visual reference for this generation. The product shown in the image must be preserved exactly — same identity, shape, proportions, colors, materials, controls, buttons, logos, and all recognizable details. Do NOT redesign, substitute, or alter the product in any way. The attached product image is the ground truth for the product's appearance."""

    fun buildAll(
        product: ProductEvidence?,
        seo: SeoResult?,
        productImageUrl: String?,
        productBitmap: Bitmap? = null
    ): List<PinterestAngle> {
        val angles = mutableListOf<PinterestAngle>()
        val fallbackBitmap = productBitmap ?: buildFallbackBitmap()

        for (def in PinterestAngles.definitions) {
            val prompt = buildPrompt(def, product, seo)
            angles.add(
                PinterestAngle(
                    id = def.id,
                    title = def.title,
                    number = def.number,
                    shortDescription = def.shortDescription,
                    artDirection = def.artDirection,
                    promptInstructions = def.promptInstructions,
                    completePrompt = prompt,
                    productBitmap = fallbackBitmap,
                    productImageUrl = productImageUrl
                )
            )
        }

        return angles
    }

    fun buildAllFromHistory(
        savedAngles: List<org.json.JSONObject>,
        product: ProductEvidence?,
        seo: SeoResult?,
        productBitmap: Bitmap? = null
    ): List<PinterestAngle> {
        val angles = mutableListOf<PinterestAngle>()
        val fallbackBitmap = productBitmap ?: buildFallbackBitmap()

        for (saved in savedAngles) {
            val id = saved.optString("id", "")
            val title = saved.optString("title", "")
            val prompt = saved.optString("prompt", "")
            val number = saved.optInt("number", 0)
            val shortDesc = saved.optString("shortDescription", "")

            val def = PinterestAngles.definitions.find { it.id == id }

            angles.add(
                PinterestAngle(
                    id = id,
                    title = title,
                    number = number,
                    shortDescription = shortDesc,
                    artDirection = def?.artDirection ?: "",
                    promptInstructions = def?.promptInstructions ?: "",
                    completePrompt = prompt,
                    productBitmap = fallbackBitmap,
                    productImageUrl = null,
                    isSaved = saved.optBoolean("isSaved", false)
                )
            )
        }

        return angles
    }

    private fun buildPrompt(
        def: PinterestAngles.AngleDefinition,
        product: ProductEvidence?,
        seo: SeoResult?
    ): String {
        val sb = StringBuilder()

        sb.appendLine(MASTER_QUALITY_HEADER)
        sb.appendLine()
        sb.appendLine(PRODUCT_IMAGE_REFERENCE)
        sb.appendLine()

        sb.appendLine("PRODUCT DATA:")
        val productData = buildProductData(product, seo)
        sb.appendLine(productData)

        sb.appendLine("ANGLE: ${def.title}")
        sb.appendLine()
        sb.appendLine("ART DIRECTION:")
        sb.appendLine(def.artDirection)
        sb.appendLine()
        sb.appendLine("INSTRUCTIONS:")
        sb.appendLine(def.promptInstructions)

        return sb.toString()
    }

    private fun buildProductData(product: ProductEvidence?, seo: SeoResult?): String {
        val sb = StringBuilder()

        val name = seo?.title ?: product?.title ?: ""
        if (name.isNotBlank()) {
            sb.appendLine("Product name: $name")
        }

        val brand = product?.brand ?: ""
        if (brand.isNotBlank()) {
            sb.appendLine("Brand: $brand")
        }

        val category = product?.category ?: ""
        if (category.isNotBlank()) {
            sb.appendLine("Category: $category")
        }

        val description = seo?.description ?: product?.description ?: ""
        if (description.isNotBlank()) {
            sb.appendLine("Description: $description")
        }

        val features = extractFeatures(product)
        if (features.isNotBlank()) {
            sb.appendLine("Key features: $features")
        }

        val useCase = extractUseCase(product, seo)
        if (useCase.isNotBlank()) {
            sb.appendLine("Primary use case: $useCase")
        }

        val altText = seo?.altText ?: ""
        if (altText.isNotBlank()) {
            sb.appendLine("Alt text: $altText")
        }

        return sb.toString().trimEnd()
    }

    private fun extractFeatures(product: ProductEvidence?): String {
        if (product == null) return ""

        val evidence = product.evidenceText
        val features = mutableListOf<String>()

        val specPatterns = listOf(
            Regex("""(\d+)\s*(?:GB|MB|TB|mAh|W|watt|inch|cm|mm|kg|lb|oz|L|ml)""", RegexOption.IGNORE_CASE),
            Regex("""(?:wireless|bluetooth|USB-C|USB-C|rechargeable|waterproof|BPA-free|stainless steel|aluminum|silicone|non-stick|dishwasher|microwave|oven-safe|portable|compact|foldable|adjustable|ergonomic|anti-slip|leak-proof|BPA|FDA)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(?:speed|level|mode|setting|function|piece|set|pack)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in specPatterns) {
            val matches = pattern.findAll(evidence)
            for (match in matches) {
                val matched = match.value.trim()
                if (matched.length in 3..60 && matched !in features) {
                    features.add(matched)
                }
            }
        }

        return features.take(5).joinToString(", ")
    }

    private fun extractUseCase(product: ProductEvidence?, seo: SeoResult?): String {
        if (product == null) return ""

        val text = "${product.title} ${product.description} ${seo?.description ?: ""}"
        val useCaseKeywords = listOf(
            "for kitchen", "for home", "for office", "for bathroom", "for bedroom",
            "for outdoor", "for travel", "for gym", "for kids", "for pets",
            "for cooking", "for cleaning", "for storage", "for organization",
            "for skincare", "for haircare", "for fitness", "for work",
            "perfect for", "ideal for", "designed for", "great for",
            "morning routine", "daily use", "everyday", "professional"
        )

        for (keyword in useCaseKeywords) {
            val idx = text.indexOf(keyword, ignoreCase = true)
            if (idx >= 0) {
                val start = maxOf(0, idx - 20)
                val end = minOf(text.length, idx + keyword.length + 40)
                val context = text.substring(start, end).trim()
                if (context.length in 10..80) {
                    return context
                }
            }
        }

        return product.category
    }

    fun buildPlaceholderBitmap(
        def: PinterestAngles.AngleDefinition,
        width: Int = 1000,
        height: Int = 1500
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        val bgColor = when (def.id) {
            "HERO_PRODUCT" -> Color.rgb(18, 18, 22)
            "LIFESTYLE" -> Color.rgb(24, 22, 18)
            "PROBLEM_SOLUTION" -> Color.rgb(18, 22, 24)
            "FEATURE_BENEFIT" -> Color.rgb(22, 18, 22)
            "PINTEREST_INSPIRATION" -> Color.rgb(20, 20, 18)
            else -> Color.rgb(18, 18, 18)
        }

        bgPaint.color = bgColor
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val accentPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(20, 255, 255, 255)
        }
        c.drawCircle(
            width * 0.7f, height * 0.3f,
            width * 0.45f, accentPaint
        )

        val numberPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            textSize = width * 0.35f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText(
            String.format("%02d", def.number),
            width * 0.5f, height * 0.42f,
            numberPaint
        )

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = width * 0.055f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.12f
        }

        val titleLines = def.title.split("\n")
        var titleY = height * 0.56f
        for (line in titleLines) {
            c.drawText(line, width * 0.5f, titleY, titlePaint)
            titleY += width * 0.07f
        }

        val descPaint = Paint().apply {
            color = Color.argb(140, 255, 255, 255)
            textSize = width * 0.028f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val descWords = def.shortDescription.split(" ")
        val descLines = mutableListOf<String>()
        var currentLine = ""
        for (word in descWords) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (descPaint.measureText(testLine) > width * 0.7f) {
                descLines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotBlank()) descLines.add(currentLine)

        var descY = height * 0.66f
        for (line in descLines) {
            c.drawText(line, width * 0.5f, descY, descPaint)
            descY += width * 0.04f
        }

        val dividerPaint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        c.drawLine(
            width * 0.35f, height * 0.72f,
            width * 0.65f, height * 0.65f,
            dividerPaint
        )

        val promptPreview = def.promptInstructions.take(80) + "..."
        val previewPaint = Paint().apply {
            color = Color.argb(60, 255, 255, 255)
            textSize = width * 0.02f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val previewWords = promptPreview.split(" ")
        var previewLine = ""
        var previewY = height * 0.78f
        for (word in previewWords) {
            val testLine = if (previewLine.isEmpty()) word else "$previewLine $word"
            if (previewPaint.measureText(testLine) > width * 0.65f) {
                c.drawText(previewLine, width * 0.5f, previewY, previewPaint)
                previewLine = word
                previewY += width * 0.032f
            } else {
                previewLine = testLine
            }
        }
        if (previewLine.isNotBlank()) {
            c.drawText(previewLine, width * 0.5f, previewY, previewPaint)
        }

        return bmp
    }

    private fun buildPlaceholderBitmapFromData(
        number: Int,
        title: String,
        shortDescription: String
    ): Bitmap {
        val width = 1000
        val height = 1500
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(18, 18, 18)
        }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val numberPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            textSize = width * 0.35f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText(
            String.format("%02d", number.coerceIn(1, 99)),
            width * 0.5f, height * 0.42f,
            numberPaint
        )

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = width * 0.055f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.12f
        }
        c.drawText(title, width * 0.5f, height * 0.56f, titlePaint)

        if (shortDescription.isNotBlank()) {
            val descPaint = Paint().apply {
                color = Color.argb(140, 255, 255, 255)
                textSize = width * 0.028f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            c.drawText(shortDescription, width * 0.5f, height * 0.64f, descPaint)
        }

        return bmp
    }

    private fun buildFallbackBitmap(
        width: Int = 1000,
        height: Int = 1500
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(18, 18, 18)
        }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val textPaint = Paint().apply {
            color = Color.argb(60, 255, 255, 255)
            textSize = width * 0.04f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText("Product Image", width * 0.5f, height * 0.5f, textPaint)

        return bmp
    }
}
