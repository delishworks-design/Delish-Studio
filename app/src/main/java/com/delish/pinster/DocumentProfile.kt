package com.delish.pinster

import org.json.JSONArray
import org.json.JSONObject

data class DocumentProfile(
    val documentSummary: String = "",
    val documentType: String = "",
    val mainTopic: String = "",
    val sections: List<Section> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val importantEntities: List<String> = emptyList(),
    val importantNumbers: List<String> = emptyList(),
    val dates: List<String> = emptyList(),
    val definitions: List<TermDefinition> = emptyList(),
    val conclusions: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val warningsOrLimitations: List<String> = emptyList()
) {
    data class Section(
        val id: String = "",
        val title: String = "",
        val summary: String = "",
        val startLocation: String = "",
        val endLocation: String = ""
    )

    data class TermDefinition(
        val term: String = "",
        val definition: String = ""
    )

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("documentSummary", documentSummary)
        json.put("documentType", documentType)
        json.put("mainTopic", mainTopic)

        val sectionsArr = JSONArray()
        for (s in sections) {
            sectionsArr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("summary", s.summary)
                put("startLocation", s.startLocation)
                put("endLocation", s.endLocation)
            })
        }
        json.put("sections", sectionsArr)

        json.put("keyPoints", JSONArray(keyPoints))
        json.put("importantEntities", JSONArray(importantEntities))
        json.put("importantNumbers", JSONArray(importantNumbers))
        json.put("dates", JSONArray(dates))

        val defsArr = JSONArray()
        for (d in definitions) {
            defsArr.put(JSONObject().apply {
                put("term", d.term)
                put("definition", d.definition)
            })
        }
        json.put("definitions", defsArr)

        json.put("conclusions", JSONArray(conclusions))
        json.put("keywords", JSONArray(keywords))
        json.put("warningsOrLimitations", JSONArray(warningsOrLimitations))

        return json
    }

    companion object {
        fun fromJson(json: JSONObject): DocumentProfile {
            val sections = mutableListOf<Section>()
            val sectionsArr = json.optJSONArray("sections")
            if (sectionsArr != null) {
                for (i in 0 until sectionsArr.length()) {
                    val s = sectionsArr.optJSONObject(i) ?: continue
                    sections.add(Section(
                        id = s.optString("id", ""),
                        title = s.optString("title", ""),
                        summary = s.optString("summary", ""),
                        startLocation = s.optString("startLocation", ""),
                        endLocation = s.optString("endLocation", "")
                    ))
                }
            }

            val definitions = mutableListOf<TermDefinition>()
            val defsArr = json.optJSONArray("definitions")
            if (defsArr != null) {
                for (i in 0 until defsArr.length()) {
                    val d = defsArr.optJSONObject(i) ?: continue
                    definitions.add(TermDefinition(
                        term = d.optString("term", ""),
                        definition = d.optString("definition", "")
                    ))
                }
            }

            return DocumentProfile(
                documentSummary = json.optString("documentSummary", ""),
                documentType = json.optString("documentType", ""),
                mainTopic = json.optString("mainTopic", ""),
                sections = sections,
                keyPoints = json.toStringList("keyPoints"),
                importantEntities = json.toStringList("importantEntities"),
                importantNumbers = json.toStringList("importantNumbers"),
                dates = json.toStringList("dates"),
                definitions = definitions,
                conclusions = json.toStringList("conclusions"),
                keywords = json.toStringList("keywords"),
                warningsOrLimitations = json.toStringList("warningsOrLimitations")
            )
        }

        private fun JSONObject.toStringList(key: String): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) list.add(v)
            }
            return list
        }
    }
}
