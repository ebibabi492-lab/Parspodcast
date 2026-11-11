package com.parspodcast.studio.data.hosting

import org.junit.Assert.*
import org.junit.Test

class WebDavHostingProviderTest {

    @Test
    fun testWebDavHostingProvider_enforcesHttps() {
        try {
            WebDavHostingProvider(
                serverUrl = "http://insecure.parspodcast.ir/webdav/",
                remoteBasePath = "/podcasts/",
                publicBaseUrl = "https://media.parspodcast.ir/",
                username = "admin",
                passwordPlain = "secret"
            )
            fail("Should throw IllegalArgumentException when server URL is not HTTPS")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("https://"))
        }
    }

    @Test
    fun testWebDavHostingProvider_getPublicUrl() {
        val provider = WebDavHostingProvider(
            serverUrl = "https://dav.parspodcast.ir/webdav/",
            remoteBasePath = "/podcasts/parspodcast/",
            publicBaseUrl = "https://media.parspodcast.ir/podcasts/parspodcast/",
            username = "admin",
            passwordPlain = "secret"
        )

        val publicAudioUrl = provider.getPublicUrl("ep1.m4a")
        assertEquals("https://media.parspodcast.ir/podcasts/parspodcast/ep1.m4a", publicAudioUrl)

        val publicFeedUrl = provider.getPublicUrl("podcast.xml")
        assertEquals("https://media.parspodcast.ir/podcasts/parspodcast/podcast.xml", publicFeedUrl)
    }
}
