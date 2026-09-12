package com.delish.pinster

object DocumentContextBuilder {

    data class RelevantContext(
        val documentSummary: String,
        val relevantSections: List<DocumentProfile.Section>,
        val relevantFacts: List<String>,
        val relevantDefinitions: List<DocumentProfile.TermDefinition>,
        val sourceExcerpts: List<String>,
        val fullContext: String
    )

    fun build(profile: DocumentProfile, question: String, fullText: String? = null): RelevantContext {
        val q = question.lowercase()

        val relevantSections = profile.sections.filter { section ->
            val sectionText = "${section.title} ${section.summary}".lowercase()
            val words = q.split(Regex("\\s+")).filter { it.length > 3 }
            words.any { word -> sectionText.contains(word) }
        }.take(5)

        val relevantFacts = profile.keyPoints.filter { fact ->
            val fw = fact.lowercase()
            val words = q.split(Regex("\\s+")).filter { it.length > 3 }
            words.any { word -> fw.contains(word) }
        }.take(5)

        val relevantDefs = profile.definitions.filter { def ->
            val dt = "${def.term} ${def.definition}".lowercase()
            val words = q.split(Regex("\\s+")).filter { it.length > 3 }
            words.any { word -> dt.contains(word) }
        }.take(3)

        val sourceExcerpts = mutableListOf<String>()
        if (fullText != null && relevantSections.isNotEmpty()) {
            for (section in relevantSections.take(2)) {
                val excerpt = findRelevantExcerpt(fullText, section.title)
                if (excerpt.isNotBlank()) sourceExcerpts.add(excerpt)
            }
        }

        val contextParts = mutableListOf<String>()
        contextParts.add("DOCUMENT SUMMARY:\n${profile.documentSummary}")
        contextParts.add("DOCUMENT TYPE: ${profile.documentType}")
        contextParts.add("MAIN TOPIC: ${profile.mainTopic}")

        if (relevantSections.isNotEmpty()) {
            contextParts.add("\nRELEVANT SECTIONS:")
            for (s in relevantSections) {
                contextParts.add("- ${s.title}: ${s.summary}")
            }
        }

        if (relevantFacts.isNotEmpty()) {
            contextParts.add("\nRELEVANT FACTS:")
            for (f in relevantFacts) {
                contextParts.add("- $f")
            }
        }

        if (relevantDefs.isNotEmpty()) {
            contextParts.add("\nRELEVANT DEFINITIONS:")
            for (d in relevantDefs) {
                contextParts.add("- ${d.term}: ${d.definition}")
            }
        }

        if (profile.conclusions.isNotEmpty()) {
            contextParts.add("\nCONCLUSIONS:")
            for (c in profile.conclusions) {
                contextParts.add("- $c")
            }
        }

        if (sourceExcerpts.isNotEmpty()) {
            contextParts.add("\nSOURCE EXCERPTS:")
            for (e in sourceExcerpts) {
                contextParts.add("---\n$e\n---")
            }
        }

        return RelevantContext(
            documentSummary = profile.documentSummary,
            relevantSections = relevantSections,
            relevantFacts = relevantFacts,
            relevantDefinitions = relevantDefs,
            sourceExcerpts = sourceExcerpts,
            fullContext = contextParts.joinToString("\n")
        )
    }

    fun buildForEdit(profile: DocumentProfile, selectedText: String, instruction: String): String {
        val parts = mutableListOf<String>()
        parts.add("DOCUMENT CONTEXT:")
        parts.add("Summary: ${profile.documentSummary}")
        parts.add("Type: ${profile.documentType}")
        parts.add("Topic: ${profile.mainTopic}")

        val relevantSections = profile.sections.filter { section ->
            val combined = "${section.title} ${section.summary} $selectedText".lowercase()
            profile.keywords.any { kw -> combined.contains(kw.lowercase()) }
        }.take(3)

        if (relevantSections.isNotEmpty()) {
            parts.add("\nRelevant sections:")
            for (s in relevantSections) {
                parts.add("- ${s.title}: ${s.summary}")
            }
        }

        parts.add("\nSELECTED TEXT:\n$selectedText")
        parts.add("\nINSTRUCTION: $instruction")

        return parts.joinToString("\n")
    }

    private fun findRelevantExcerpt(fullText: String, sectionTitle: String): String {
        if (sectionTitle.isBlank()) return ""
        val idx = fullText.indexOf(sectionTitle, ignoreCase = true)
        if (idx == -1) {
            val words = sectionTitle.split(Regex("\\s+")).filter { it.length > 3 }
            for (word in words) {
                val wIdx = fullText.indexOf(word, ignoreCase = true)
                if (wIdx != -1) {
                    val start = maxOf(0, wIdx - 200)
                    val end = minOf(fullText.length, wIdx + 800)
                    return fullText.substring(start, end).trim()
                }
            }
            return ""
        }
        val start = maxOf(0, idx - 100)
        val end = minOf(fullText.length, idx + 1000)
        return fullText.substring(start, end).trim()
    }
}
