package com.parspodcast.studio.domain.parser

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.parspodcast.studio.data.local.entities.ScriptSegmentEntity
import java.util.UUID

data class ParsedPodcastScript(
    val episodeTitle: String,
    val language: String = "fa-IR",
    val format: String = "dual_host",
    val introduction: String,
    val sections: List<ParsedScriptSection>,
    val conclusion: String,
    val estimatedDurationMinutes: Int = 10,
    val rawModelOutput: String = ""
)

data class ParsedScriptSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val speaker: String, // HOST_A or HOST_B
    val content: String,
    val sourceIds: List<String> = emptyList()
)

object ScriptJsonParser {

    private val gson = Gson()

    fun parse(rawText: String, defaultTitle: String = "اپیزود پادکست", isDualHost: Boolean = true): ParsedPodcastScript {
        val cleanedJson = extractJsonSubstring(rawText)

        return try {
            val jsonObject = JsonParser.parseString(cleanedJson).asJsonObject

            val title = jsonObject.get("episodeTitle")?.asString ?: defaultTitle
            val format = jsonObject.get("format")?.asString ?: if (isDualHost) "dual_host" else "single_host"
            val intro = jsonObject.get("introduction")?.asString ?: ""
            val conclusion = jsonObject.get("conclusion")?.asString ?: ""
            val duration = jsonObject.get("estimatedDurationMinutes")?.asInt ?: 10

            val sections = mutableListOf<ParsedScriptSection>()
            val sectionsArray = jsonObject.getAsJsonArray("sections")
            if (sectionsArray != null) {
                for (elem in sectionsArray) {
                    val secObj = elem.asJsonObject
                    val secId = secObj.get("id")?.asString ?: UUID.randomUUID().toString()
                    val secTitle = secObj.get("title")?.asString ?: "بخش خبری"
                    val secSpeaker = secObj.get("speaker")?.asString ?: "HOST_A"
                    val secContent = secObj.get("content")?.asString ?: ""

                    val sourceIdsList = mutableListOf<String>()
                    val srcArr = secObj.getAsJsonArray("sourceIds")
                    if (srcArr != null) {
                        for (src in srcArr) {
                            sourceIdsList.add(src.asString)
                        }
                    }

                    sections.add(
                        ParsedScriptSection(
                            id = secId,
                            title = secTitle,
                            speaker = secSpeaker,
                            content = secContent,
                            sourceIds = sourceIdsList
                        )
                    )
                }
            }

            ParsedPodcastScript(
                episodeTitle = title,
                format = format,
                introduction = intro,
                sections = sections,
                conclusion = conclusion,
                estimatedDurationMinutes = duration,
                rawModelOutput = rawText
            )
        } catch (e: Exception) {
            // Robust Repair Fallback: Do not discard original text if JSON parsing fails!
            repairMalformedScript(rawText, defaultTitle, isDualHost)
        }
    }

    private fun extractJsonSubstring(raw: String): String {
        var text = raw.trim()

        // Strip markdown backticks
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')

        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1).trim()
        } else {
            text
        }
    }

    private fun repairMalformedScript(
        rawText: String,
        defaultTitle: String,
        isDualHost: Boolean
    ): ParsedPodcastScript {
        val paragraphs = rawText
            .split("\n\n+")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("```") }

        if (paragraphs.isEmpty()) {
            return ParsedPodcastScript(
                episodeTitle = defaultTitle,
                introduction = "به استودیوی پارس‌پادکست خوش آمدید.",
                sections = emptyList(),
                conclusion = "تا برنامه‌ای دیگر خدانگهدار.",
                rawModelOutput = rawText
            )
        }

        val intro = paragraphs.firstOrNull() ?: "به استودیوی پارس‌پادکست خوش آمدید."
        val conclusion = if (paragraphs.size > 2) paragraphs.last() else "تا برنامه‌ای دیگر خدانگهدار."

        val middle = if (paragraphs.size > 2) {
            paragraphs.subList(1, paragraphs.size - 1)
        } else if (paragraphs.size == 2) {
            listOf(paragraphs[1])
        } else {
            listOf(paragraphs[0])
        }

        val sections = middle.mapIndexed { idx, p ->
            val speaker = if (!isDualHost) {
                "HOST_A"
            } else {
                if (idx % 2 == 0) "HOST_A" else "HOST_B"
            }

            ParsedScriptSection(
                id = "sec_${idx + 1}",
                title = "بخش خبری ${idx + 1}",
                speaker = speaker,
                content = p
            )
        }

        return ParsedPodcastScript(
            episodeTitle = defaultTitle,
            format = if (isDualHost) "dual_host" else "single_host",
            introduction = intro,
            sections = sections,
            conclusion = conclusion,
            rawModelOutput = rawText
        )
    }

    fun toScriptSegmentEntities(
        script: ParsedPodcastScript,
        scriptId: String = UUID.randomUUID().toString()
    ): List<ScriptSegmentEntity> {
        val result = mutableListOf<ScriptSegmentEntity>()
        var index = 1

        if (script.introduction.isNotEmpty()) {
            result.add(
                ScriptSegmentEntity(
                    id = "seg_${scriptId}_intro",
                    scriptId = scriptId,
                    segmentIndex = index++,
                    segmentType = "INTRO",
                    speakerRole = "PRESENTER_A",
                    textFa = script.introduction,
                    estimatedDurationSec = maxOf(6, (script.introduction.split("\\s+".toRegex()).size * 0.4).toInt()),
                    isApproved = true
                )
            )
        }

        script.sections.forEach { sec ->
            val role = if (sec.speaker == "HOST_B") "PRESENTER_B" else "PRESENTER_A"
            result.add(
                ScriptSegmentEntity(
                    id = "seg_${scriptId}_${sec.id}",
                    scriptId = scriptId,
                    segmentIndex = index++,
                    segmentType = "NEWS",
                    speakerRole = role,
                    textFa = sec.content,
                    estimatedDurationSec = maxOf(8, (sec.content.split("\\s+".toRegex()).size * 0.4).toInt()),
                    isApproved = true,
                    sourceIdsJson = gson.toJson(sec.sourceIds)
                )
            )
        }

        if (script.conclusion.isNotEmpty()) {
            val outroRole = if (script.format == "dual_host") "PRESENTER_B" else "PRESENTER_A"
            result.add(
                ScriptSegmentEntity(
                    id = "seg_${scriptId}_outro",
                    scriptId = scriptId,
                    segmentIndex = index++,
                    segmentType = "OUTRO",
                    speakerRole = outroRole,
                    textFa = script.conclusion,
                    estimatedDurationSec = maxOf(6, (script.conclusion.split("\\s+".toRegex()).size * 0.4).toInt()),
                    isApproved = true
                )
            )
        }

        return result
    }
}
