package com.parspodcast.studio.domain.tts

object PersianSentenceSplitter {

    private const val DEFAULT_MAX_CHUNK_LENGTH = 180
    private const val MIN_FRAGMENT_LENGTH = 15

    fun splitIntoChunks(
        text: String,
        maxChunkLength: Int = DEFAULT_MAX_CHUNK_LENGTH
    ): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChunkLength) return listOf(trimmed)

        val result = mutableListOf<String>()

        // 1. Split text on primary sentence terminators
        val sentenceDelimiters = Regex("(?<=[.!?؛؟\\n])\\s+")
        val rawSentences = trimmed.split(sentenceDelimiters).filter { it.isNotBlank() }

        var currentBuffer = StringBuilder()

        for (sentence in rawSentences) {
            val s = sentence.trim()
            if (s.isEmpty()) continue

            if (s.length > maxChunkLength) {
                // If a single sentence exceeds the limit, split at comma or clause boundary
                if (currentBuffer.isNotEmpty()) {
                    result.add(currentBuffer.toString().trim())
                    currentBuffer = StringBuilder()
                }

                val subClauses = splitLongSentenceAtClauses(s, maxChunkLength)
                for (clause in subClauses) {
                    if (clause.isNotBlank()) {
                        result.add(clause.trim())
                    }
                }
            } else if (currentBuffer.length + s.length + 1 <= maxChunkLength) {
                if (currentBuffer.isNotEmpty()) {
                    currentBuffer.append(" ")
                }
                currentBuffer.append(s)
            } else {
                result.add(currentBuffer.toString().trim())
                currentBuffer = StringBuilder(s)
            }
        }

        if (currentBuffer.isNotEmpty()) {
            val remaining = currentBuffer.toString().trim()
            // If the last fragment is too short, merge with previous if possible
            if (result.isNotEmpty() && remaining.length < MIN_FRAGMENT_LENGTH) {
                val lastIdx = result.size - 1
                result[lastIdx] = result[lastIdx] + " " + remaining
            } else {
                result.add(remaining)
            }
        }

        return result
    }

    private fun splitLongSentenceAtClauses(sentence: String, maxChunkLength: Int): List<String> {
        val clauses = sentence.split(Regex("(?<=[،,])\\s+")).filter { it.isNotBlank() }
        if (clauses.size <= 1) {
            // Split by words as fallback
            return splitByWords(sentence, maxChunkLength)
        }

        val result = mutableListOf<String>()
        var buffer = StringBuilder()

        for (clause in clauses) {
            if (buffer.length + clause.length + 1 <= maxChunkLength) {
                if (buffer.isNotEmpty()) buffer.append(" ")
                buffer.append(clause)
            } else {
                if (buffer.isNotEmpty()) {
                    result.add(buffer.toString().trim())
                }
                if (clause.length > maxChunkLength) {
                    result.addAll(splitByWords(clause, maxChunkLength))
                    buffer = StringBuilder()
                } else {
                    buffer = StringBuilder(clause)
                }
            }
        }

        if (buffer.isNotEmpty()) {
            result.add(buffer.toString().trim())
        }

        return result
    }

    private fun splitByWords(text: String, maxChunkLength: Int): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.length + word.length + 1 <= maxChunkLength) {
                if (current.isNotEmpty()) current.append(" ")
                current.append(word)
            } else {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                }
                current = StringBuilder(word)
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }

        return chunks
    }
}
