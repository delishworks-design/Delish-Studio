package com.delish.pinster

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

object GenerationLogger {

    private const val PREFS = "pinster_generation"

    data class GenerationSnapshot(
        val timestamp: Long,
        val productFingerprint: ProductFingerprint?,
        val concepts: List<CreativeConcept>,
        val palettes: List<PosterPalette>,
        val scores: List<QualityScore>,
        val bestIndex: Int
    )

    fun save(context: Context, snapshot: GenerationSnapshot) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject()

        json.put("timestamp", snapshot.timestamp)

        snapshot.productFingerprint?.let { fp ->
            val fpJson = JSONObject()
            fpJson.put("width", fp.width)
            fpJson.put("height", fp.height)
            fpJson.put("aspectRatio", fp.aspectRatio.toDouble())
            fpJson.put("averageBrightness", fp.averageBrightness.toDouble())
            fpJson.put("averageSaturation", fp.averageSaturation.toDouble())
            fpJson.put("edgeDensity", fp.edgeDensity.toDouble())
            fpJson.put("isDarkProduct", fp.isDarkProduct)
            fpJson.put("isWarmTone", fp.isWarmTone)
            fpJson.put("isCoolTone", fp.isCoolTone)
            fpJson.put("productCategory", fp.productCategory.name)
            json.put("productFingerprint", fpJson)
        }

        val conceptsArr = JSONArray()
        for (c in snapshot.concepts) {
            val cj = JSONObject()
            cj.put("hook", c.hook)
            cj.put("supportCopy", c.supportCopy)
            cj.put("productLabel", c.productLabel)
            cj.put("artDirection", c.artDirection.name)
            cj.put("hookStrategy", c.hookStrategy.name)
            cj.put("compositionMode", c.compositionMode.name)
            cj.put("productPlacement", c.productPlacement.name)
            cj.put("productScale", c.productScale.toDouble())
            cj.put("lightingMood", c.lightingMood)
            cj.put("visualMood", c.visualMood)
            cj.put("eyebrowText", c.eyebrowText)
            conceptsArr.put(cj)
        }
        json.put("concepts", conceptsArr)

        val palettesArr = JSONArray()
        for (p in snapshot.palettes) {
            val pj = JSONObject()
            pj.put("background", hex(p.background))
            pj.put("surface", hex(p.surface))
            pj.put("accent", hex(p.accent))
            pj.put("text", hex(p.text))
            pj.put("textMuted", hex(p.textMuted))
            pj.put("secondary", hex(p.secondary))
            pj.put("highlight", hex(p.highlight))
            palettesArr.put(pj)
        }
        json.put("palettes", palettesArr)

        val scoresArr = JSONArray()
        for (s in snapshot.scores) {
            val sj = JSONObject()
            sj.put("total", s.total.toDouble())
            sj.put("hookStrength", s.hookStrength.toDouble())
            sj.put("visualBalance", s.visualBalance.toDouble())
            sj.put("readability", s.readability.toDouble())
            sj.put("colorHarmony", s.colorHarmony.toDouble())
            sj.put("contrast", s.contrast.toDouble())
            sj.put("composition", s.composition.toDouble())
            sj.put("premiumAesthetic", s.premiumAesthetic.toDouble())
            sj.put("visualImpact", s.visualImpact.toDouble())
            sj.put("productIsolation", s.productIsolation.toDouble())
            sj.put("whitespaceQuality", s.whitespaceQuality.toDouble())
            sj.put("mobileLegibility", s.mobileLegibility.toDouble())
            sj.put("scrollStopPower", s.scrollStopPower.toDouble())
            scoresArr.put(sj)
        }
        json.put("scores", scoresArr)

        json.put("bestIndex", snapshot.bestIndex)

        prefs.edit().putString("latest", json.toString()).apply()
    }

    fun loadLatest(context: Context): GenerationSnapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("latest", null) ?: return null

        return try {
            val json = JSONObject(raw)
            val timestamp = json.optLong("timestamp", 0)

            val fpJson = json.optJSONObject("productFingerprint")
            val fp = if (fpJson != null) {
                ProductFingerprint(
                    width = fpJson.optInt("width", 100),
                    height = fpJson.optInt("height", 100),
                    aspectRatio = fpJson.optDouble("aspectRatio", 1.0).toFloat(),
                    alphaBounds = android.graphics.Rect(0, 0, fpJson.optInt("width", 100), fpJson.optInt("height", 100)),
                    productArea = 0f,
                    canvasFillRatio = 0f,
                    centroidX = 0.5f,
                    centroidY = 0.5f,
                    opticalCenterX = 0.5f,
                    opticalCenterY = 0.5f,
                    dominantColors = emptyList(),
                    averageBrightness = fpJson.optDouble("averageBrightness", 0.5).toFloat(),
                    averageSaturation = fpJson.optDouble("averageSaturation", 0.5).toFloat(),
                    hasAlpha = false,
                    edgeDensity = fpJson.optDouble("edgeDensity", 0.1).toFloat(),
                    silhouetteComplexity = 0f,
                    visualMass = 0f,
                    negativeSpaceTop = 0f,
                    negativeSpaceBottom = 0f,
                    negativeSpaceLeft = 0f,
                    negativeSpaceRight = 0f,
                    isDarkProduct = fpJson.optBoolean("isDarkProduct", false),
                    isWarmTone = fpJson.optBoolean("isWarmTone", false),
                    isCoolTone = fpJson.optBoolean("isCoolTone", false),
                    productCategory = try { ProductCategory.valueOf(fpJson.optString("productCategory", "UNKNOWN")) } catch (_: Exception) { ProductCategory.UNKNOWN }
                )
            } else null

            val scoresArr = json.optJSONArray("scores") ?: JSONArray()
            val scores = (0 until scoresArr.length()).map { i ->
                val sj = scoresArr.getJSONObject(i)
                QualityScore(
                    total = sj.optDouble("total", 50.0).toFloat(),
                    hookStrength = sj.optDouble("hookStrength", 50.0).toFloat(),
                    visualBalance = sj.optDouble("visualBalance", 50.0).toFloat(),
                    readability = sj.optDouble("readability", 50.0).toFloat(),
                    colorHarmony = sj.optDouble("colorHarmony", 50.0).toFloat(),
                    contrast = sj.optDouble("contrast", 50.0).toFloat(),
                    composition = sj.optDouble("composition", 80.0).toFloat(),
                    premiumAesthetic = sj.optDouble("premiumAesthetic", 50.0).toFloat(),
                    visualImpact = sj.optDouble("visualImpact", 50.0).toFloat(),
                    productProminence = 50f,
                    typographyHierarchy = 50f,
                    negativeSpace = 50f,
                    collision = 0f,
                    textDensity = 50f,
                    productIsolation = sj.optDouble("productIsolation", 50.0).toFloat(),
                    commercialClarity = 70f,
                    mobileLegibility = sj.optDouble("mobileLegibility", 50.0).toFloat(),
                    scrollStopPower = sj.optDouble("scrollStopPower", 50.0).toFloat(),
                    whitespaceQuality = sj.optDouble("whitespaceQuality", 50.0).toFloat()
                )
            }

            val conceptsArr = json.optJSONArray("concepts") ?: JSONArray()
            val concepts = (0 until conceptsArr.length()).map { i ->
                val cj = conceptsArr.getJSONObject(i)
                CreativeConcept(
                    hook = cj.optString("hook", ""),
                    supportCopy = cj.optString("supportCopy", ""),
                    productLabel = cj.optString("productLabel", ""),
                    artDirection = try { ArtDirection.valueOf(cj.optString("artDirection", "LUXURY_EDITORIAL")) } catch (_: Exception) { ArtDirection.LUXURY_EDITORIAL },
                    hookStrategy = try { HookStrategy.valueOf(cj.optString("hookStrategy", "EDITORIAL")) } catch (_: Exception) { HookStrategy.EDITORIAL },
                    compositionMode = try { CompositionMode.valueOf(cj.optString("compositionMode", "HERO")) } catch (_: Exception) { CompositionMode.HERO },
                    productPlacement = try { ProductPlacement.valueOf(cj.optString("productPlacement", "CENTER")) } catch (_: Exception) { ProductPlacement.CENTER },
                    productScale = cj.optDouble("productScale", 0.65).toFloat(),
                    lightingMood = cj.optString("lightingMood", "dramatic"),
                    visualMood = cj.optString("visualMood", ""),
                    emphasis = VisualWeight.SYMMETRIC,
                    eyebrowText = cj.optString("eyebrowText", ""),
                    headlineAlignment = TextAlign.CENTER,
                    supportAlignment = TextAlign.CENTER,
                    negativeSpaceBias = 0.4f,
                    decorationDensity = 1
                )
            }

            val palettesArr = json.optJSONArray("palettes") ?: JSONArray()
            val palettes = (0 until palettesArr.length()).map { i ->
                val pj = palettesArr.getJSONObject(i)
                PosterPalette(
                    background = parseColor(pj.optString("background", "#111111")),
                    surface = parseColor(pj.optString("surface", "#1a1a1a")),
                    primary = parseColor(pj.optString("accent", "#ff6b6b")),
                    secondary = parseColor(pj.optString("secondary", "#666666")),
                    accent = parseColor(pj.optString("accent", "#ff6b6b")),
                    text = parseColor(pj.optString("text", "#ffffff")),
                    textMuted = parseColor(pj.optString("textMuted", "#999999")),
                    textOnDark = Color.WHITE,
                    shadow = Color.BLACK,
                    highlight = parseColor(pj.optString("highlight", "#ffffff")),
                    vignette = Color.BLACK,
                    isDark = true,
                    atmosphere = Color.TRANSPARENT,
                    depthLayer = Color.TRANSPARENT
                )
            }

            val bestIndex = json.optInt("bestIndex", 0)

            GenerationSnapshot(
                timestamp = timestamp,
                productFingerprint = fp,
                concepts = concepts,
                palettes = palettes,
                scores = scores,
                bestIndex = bestIndex
            )
        } catch (e: Exception) {
            null
        }
    }

    fun buildContextString(context: Context): String {
        val snap = loadLatest(context) ?: return "No generation data yet. Ask the user to generate a poster first."

        val sb = StringBuilder()
        sb.appendLine("=== LATEST GENERATION DATA ===")
        sb.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(snap.timestamp))}")

        snap.productFingerprint?.let { fp ->
            sb.appendLine("\n--- PRODUCT ---")
            sb.appendLine("Size: ${fp.width}x${fp.height}")
            sb.appendLine("Aspect Ratio: ${"%.2f".format(fp.aspectRatio)}")
            sb.appendLine("Brightness: ${"%.1f".format(fp.averageBrightness * 100)}%")
            sb.appendLine("Saturation: ${"%.1f".format(fp.averageSaturation * 100)}%")
            sb.appendLine("Dark product: ${fp.isDarkProduct}")
            sb.appendLine("Warm tone: ${fp.isWarmTone}, Cool tone: ${fp.isCoolTone}")
            sb.appendLine("Category: ${fp.productCategory}")
        }

        sb.appendLine("\n--- POSTERS (5 directions) ---")
        for (i in snap.concepts.indices) {
            val c = snap.concepts[i]
            val s = if (i < snap.scores.size) snap.scores[i] else null
            val star = if (i == snap.bestIndex) " [BEST]" else ""

            sb.appendLine("\n${i + 1}. ${c.artDirection.name}$star")
            sb.appendLine("   Hook: ${c.hook}")
            sb.appendLine("   Support: ${c.supportCopy}")
            sb.appendLine("   Label: ${c.productLabel}")
            sb.appendLine("   Strategy: ${c.hookStrategy}")
            sb.appendLine("   Scale: ${"%.2f".format(c.productScale)}")
            sb.appendLine("   Lighting: ${c.lightingMood}")

            s?.let {
                sb.appendLine("   Score: ${"%.0f".format(it.total)}/100")
                sb.appendLine("     Hook: ${"%.0f".format(it.hookStrength)} | Balance: ${"%.0f".format(it.visualBalance)} | Readability: ${"%.0f".format(it.readability)}")
                sb.appendLine("     Color: ${"%.0f".format(it.colorHarmony)} | Contrast: ${"%.0f".format(it.contrast)} | Premium: ${"%.0f".format(it.premiumAesthetic)}")
                sb.appendLine("     Impact: ${"%.0f".format(it.visualImpact)} | Whitespace: ${"%.0f".format(it.whitespaceQuality)} | Mobile: ${"%.0f".format(it.mobileLegibility)} | ScrollStop: ${"%.0f".format(it.scrollStopPower)}")
            }
        }

        sb.appendLine("\nBEST: #${snap.bestIndex + 1} (${snap.concepts.getOrNull(snap.bestIndex)?.artDirection?.name ?: "?"})")

        return sb.toString()
    }

    private fun hex(c: Int): String = String.format("#%06X", 0xFFFFFF and c)

    private fun parseColor(hex: String): Int {
        return try { Color.parseColor(hex) } catch (_: Exception) { Color.GRAY }
    }
}
