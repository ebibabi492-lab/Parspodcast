package com.parspodcast.studio.data.news

import com.parspodcast.studio.domain.news.ParsedNewsDraft
import com.parspodcast.studio.domain.news.RssAtomParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFeedFetcher @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "ParsPodcast/1.0 (Android Studio News Ingestion)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun fetchFeed(
        feedUrl: String,
        sourceName: String,
        defaultCategory: String = "عمومی"
    ): Result<List<ParsedNewsDraft>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("خطای سرور فید: کد ${response.code} (${response.message})"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("پاسخ سرور خالی است"))
            val stream = body.byteStream()

            val items = RssAtomParser.parse(
                inputStream = stream,
                fallbackSourceName = sourceName,
                sourceUrl = feedUrl,
                defaultCategory = defaultCategory
            )

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testFeed(feedUrl: String): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("پاسخ ناموفق از آدرس فید: کد ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("محتوای پاسخ خالی است"))
            val items = RssAtomParser.parse(
                inputStream = body.byteStream(),
                fallbackSourceName = "تست فید",
                sourceUrl = feedUrl
            )

            val detectedTitle = items.firstOrNull()?.sourceName ?: "فید استاندارد"
            Result.success(Pair(detectedTitle, items.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
