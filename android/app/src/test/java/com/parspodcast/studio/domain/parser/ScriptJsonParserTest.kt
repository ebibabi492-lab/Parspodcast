package com.parspodcast.studio.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptJsonParserTest {

    @Test
    fun testParseValidJson_returnsStructuredScript() {
        val json = """
        {
            "title": "پادکست فناوری امروز",
            "summary": "بررسی دستاوردهای تازه در زمینه هوش مصنوعی",
            "segments": [
                {
                    "speaker": "HOST_A",
                    "text": "سلام و درود به شنوندگان گرامی پارس‌پادکست.",
                    "notes": "آغاز پرانرژی"
                },
                {
                    "speaker": "HOST_B",
                    "text": "امروز با اخبار داغ فناوری در خدمت شما هستیم.",
                    "notes": ""
                }
            ]
        }
        """.trimIndent()

        val parsed = ScriptJsonParser.parseScriptResponse(json, defaultFormat = "TWO_PRESENTERS_CONVERSATIONAL")

        assertEquals("پادکست فناوری امروز", parsed.title)
        assertEquals("بررسی دستاوردهای تازه در زمینه هوش مصنوعی", parsed.summary)
        assertEquals(2, parsed.segments.size)
        assertEquals("HOST_A", parsed.segments[0].speaker)
        assertEquals("سلام و درود به شنوندگان گرامی پارس‌پادکست.", parsed.segments[0].text)
        assertEquals("HOST_B", parsed.segments[1].speaker)
        assertEquals("امروز با اخبار داغ فناوری در خدمت شما هستیم.", parsed.segments[1].text)
    }

    @Test
    fun testParseMarkdownWrappedJson_stripsFencesAndParses() {
        val markdownJson = """
        این هم سناریوی درخواستی شما:
        ```json
        {
            "title": "خبرنامه سریع",
            "summary": "خلاصه کوتاه اخبار",
            "segments": [
                {
                    "speaker": "HOST_A",
                    "text": "درود. نخستین خبر مربوط به بازار طلا است."
                }
            ]
        }
        ```
        امیدوارم مورد پسند باشد.
        """.trimIndent()

        val parsed = ScriptJsonParser.parseScriptResponse(markdownJson, defaultFormat = "SINGLE_PRESENTER")

        assertEquals("خبرنامه سریع", parsed.title)
        assertEquals(1, parsed.segments.size)
        assertEquals("HOST_A", parsed.segments[0].speaker)
    }

    @Test
    fun testParseMalformedJson_triggersRepairWithoutDataLoss() {
        // Missing closing brackets or conversational text format
        val malformedText = """
        گوینده اول: درود بر شما شنوندگان عزیز. امروز تحولات مهمی رخ داده است.
        گوینده دوم: بله دقیقا، به ویژه در حوزه اقتصاد و بورس تهران.
        مجری: در ادامه به جزئیات این خبر می‌پردازیم.
        """.trimIndent()

        val parsed = ScriptJsonParser.parseScriptResponse(malformedText, defaultFormat = "TWO_PRESENTERS_CONVERSATIONAL")

        assertNotNull(parsed)
        assertTrue(parsed.segments.isNotEmpty())
        // Verify repair attributed speakers accurately
        assertEquals("HOST_A", parsed.segments[0].speaker)
        assertTrue(parsed.segments[0].text.contains("درود بر شما شنوندگان عزیز"))
        assertEquals("HOST_B", parsed.segments[1].speaker)
        assertTrue(parsed.segments[1].text.contains("به ویژه در حوزه اقتصاد"))
    }

    @Test
    fun testParseEmptyResponse_returnsDefaultFallback() {
        val parsed = ScriptJsonParser.parseScriptResponse("", defaultFormat = "SINGLE_PRESENTER")
        assertNotNull(parsed)
        assertEquals(1, parsed.segments.size)
        assertEquals("HOST_A", parsed.segments[0].speaker)
    }
}
