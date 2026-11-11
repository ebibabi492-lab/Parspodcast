package com.parspodcast.studio.data.hosting

import com.parspodcast.studio.data.security.SecureCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebDavHostingProvider(
    val serverUrl: String,
    val remoteBasePath: String,
    val publicBaseUrl: String,
    val username: String,
    private val passwordPlain: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) : PodcastHostingProvider {

    override val providerId: String = "webdav"
    override val providerDisplayName: String = "WebDAV over HTTPS"

    init {
        require(serverUrl.startsWith("https://")) {
            "برای امنیت اطلاعات ورود، نشانی سرور WebDAV الزاما باید با https:// آغاز شود."
        }
    }

    private fun getBasicAuthHeader(): String {
        val credentials = "$username:$passwordPlain"
        val base64 = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        return "Basic $base64"
    }

    private fun cleanPath(base: String, relative: String): String {
        val b = base.trimEnd('/')
        val r = relative.trimStart('/')
        return "$b/$r"
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // PROPFIND or OPTIONS on serverUrl
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", null)
                .addHeader("Authorization", getBasicAuthHeader())
                .addHeader("Depth", "0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 207 || response.code == 405) {
                    Result.success(true)
                } else if (response.code == 401 || response.code == 403) {
                    Result.failure(Exception("خطای احراز هویت سرور WebDAV (نام کاربری یا رمز عبور نامعتبر است)."))
                } else {
                    Result.failure(Exception("پاسخ ناموفق سرور WebDAV: کد وضعیت ${response.code}"))
                }
            }
        } catch (e: Exception) {
            val safeMessage = SecureCredentialManager.redact(e.message ?: "خطای ناشناخته در اتصال به سرور")
            Result.failure(Exception("عدم برقراری ارتباط با سرور: $safeMessage"))
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remoteRelativePath: String,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!localFile.exists()) {
            return@withContext Result.failure(Exception("فایل محلی یافت نشد: ${localFile.name}"))
        }

        try {
            val targetUrl = cleanPath(serverUrl, cleanPath(remoteBasePath, remoteRelativePath))
            val mediaType = mimeType.toMediaTypeOrNull()

            val requestBody = object : RequestBody() {
                override fun contentType(): MediaType? = mediaType
                override fun contentLength(): Long = localFile.length()

                override fun writeTo(sink: BufferedSink) {
                    val fileLength = localFile.length()
                    var bytesWritten = 0L

                    localFile.inputStream().use { inputStream ->
                        val buffer = ByteArray(16384)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            bytesWritten += read
                            if (fileLength > 0) {
                                val progress = (bytesWritten.toFloat() / fileLength).coerceIn(0f, 1f)
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            val request = Request.Builder()
                .url(targetUrl)
                .put(requestBody)
                .addHeader("Authorization", getBasicAuthHeader())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204) {
                    val publicUrl = getPublicUrl(remoteRelativePath)
                    Result.success(publicUrl)
                } else {
                    Result.failure(Exception("خطای بارگذاری در سرور WebDAV: کد وضعیت ${response.code}"))
                }
            }
        } catch (e: Exception) {
            val safeMessage = SecureCredentialManager.redact(e.message ?: "خطا در جریان بارگذاری فایل")
            Result.failure(Exception("خطای بارگذاری فایل: $safeMessage"))
        }
    }

    override suspend fun updateFile(
        localFile: File,
        remoteRelativePath: String,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        return uploadFile(localFile, remoteRelativePath, mimeType, onProgress)
    }

    override suspend fun deleteFile(remoteRelativePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = cleanPath(serverUrl, cleanPath(remoteBasePath, remoteRelativePath))
            val request = Request.Builder()
                .url(targetUrl)
                .delete()
                .addHeader("Authorization", getBasicAuthHeader())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204 || response.code == 404) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("خطا در حذف فایل از سرور WebDAV: کد وضعیت ${response.code}"))
                }
            }
        } catch (e: Exception) {
            val safeMessage = SecureCredentialManager.redact(e.message ?: "خطا در حذف فایل")
            Result.failure(Exception("خطا در حذف فایل: $safeMessage"))
        }
    }

    override fun getPublicUrl(remoteRelativePath: String): String {
        return cleanPath(publicBaseUrl, remoteRelativePath)
    }

    override suspend fun verifyPublicFile(
        publicUrl: String,
        expectedMimeType: String?,
        expectedSizeBytes: Long?
    ): Result<FileVerificationStatus> = withContext(Dispatchers.IO) {
        try {
            val isHttps = publicUrl.startsWith("https://")

            // Prefer HEAD; if server doesn't support HEAD (405), fallback to GET range bytes=0-0
            var request = Request.Builder()
                .url(publicUrl)
                .head()
                .build()

            var response = client.newCall(request).execute()
            if (response.code == 405) {
                response.close()
                request = Request.Builder()
                    .url(publicUrl)
                    .addHeader("Range", "bytes=0-0")
                    .get()
                    .build()
                response = client.newCall(request).execute()
            }

            response.use { resp ->
                val code = resp.code
                val isSuccess = resp.isSuccessful || code == 206
                val cType = resp.header("Content-Type")
                val cLengthStr = resp.header("Content-Length")
                val cLength = cLengthStr?.toLongOrNull()

                Result.success(
                    FileVerificationStatus(
                        isAccessible = isSuccess,
                        httpStatusCode = code,
                        contentType = cType,
                        contentLength = cLength,
                        isHttps = isHttps,
                        errorMessage = if (!isSuccess) "سرور با کد وضعیت $code پاسخ داد" else null
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                FileVerificationStatus(
                    isAccessible = false,
                    httpStatusCode = 0,
                    contentType = null,
                    contentLength = null,
                    isHttps = publicUrl.startsWith("https://"),
                    errorMessage = e.message
                )
            )
        }
    }
}
