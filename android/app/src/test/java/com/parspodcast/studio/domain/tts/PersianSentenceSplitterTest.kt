package com.parspodcast.studio.domain.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianSentenceSplitterTest {

    @Test
    fun testSplitShortText_returnsSingleChunk() {
        val text = "سلام به همه شنوندگان گرامی پارس‌پادکست."
        val chunks = PersianSentenceSplitter.splitIntoChunks(text, maxChunkLength = 100)

        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun testSplitLongParagraph_splitsAtSentenceBoundaries() {
        val sentence1 = "امروز بازار سهام پس از روزها نوسان، به روند صعودی خود بازگشت."
        val sentence2 = "شاخص کل بورس تهران بیش از پانزده هزار واحد رشد را تجربه کرد."
        val sentence3 = "کارشناسان دلایل این تغییر مثبت را اعلام کردند."

        val combined = "$sentence1 $sentence2 $sentence3"
        val chunks = PersianSentenceSplitter.splitIntoChunks(combined, maxChunkLength = 90)

        assertTrue("Should split into multiple chunks", chunks.size >= 2)
        // Verify all words are preserved
        val reassembled = chunks.joinToString(" ")
        assertTrue(reassembled.contains("بازار سهام"))
        assertTrue(reassembled.contains("پانزده هزار واحد"))
        assertTrue(reassembled.contains("کارشناسان دلایل"))
    }

    @Test
    fun testSplitAtCommaClauses_whenSentenceExceedsLimit() {
        val longSentence = "در پی پیشرفت‌های علمی اخیر، پژوهشگران ایرانی موفق به توسعه روش‌های نوینی در زمینه تشخیص زودهنگام بیماری‌ها با بهره‌گیری از هوش مصنوعی شدند."
        val chunks = PersianSentenceSplitter.splitIntoChunks(longSentence, maxChunkLength = 70)

        assertTrue("Long sentence should be split into smaller clauses", chunks.size >= 2)
        for (chunk in chunks) {
            assertTrue("Each chunk must be within reasonable limits", chunk.length <= 80)
        }
    }

    @Test
    fun testEmptyOrBlankText_returnsEmptyList() {
        assertTrue(PersianSentenceSplitter.splitIntoChunks("").isEmpty())
        assertTrue(PersianSentenceSplitter.splitIntoChunks("   ").isEmpty())
    }
}
