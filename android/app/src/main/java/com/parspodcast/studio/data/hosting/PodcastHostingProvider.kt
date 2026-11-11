package com.parspodcast.studio.data.hosting

import java.io.File

data class FileVerificationStatus(
    val isAccessible: Boolean,
    val httpStatusCode: Int,
    val contentType: String?,
    val contentLength: Long?,
    val isHttps: Boolean,
    val errorMessage: String? = null
)

interface PodcastHostingProvider {
    val providerId: String
    val providerDisplayName: String

    suspend fun testConnection(): Result<Boolean>

    suspend fun uploadFile(
        localFile: File,
        remoteRelativePath: String,
        mimeType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>

    suspend fun updateFile(
        localFile: File,
        remoteRelativePath: String,
        mimeType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>

    suspend fun deleteFile(remoteRelativePath: String): Result<Boolean>

    fun getPublicUrl(remoteRelativePath: String): String

    suspend fun verifyPublicFile(
        publicUrl: String,
        expectedMimeType: String? = null,
        expectedSizeBytes: Long? = null
    ): Result<FileVerificationStatus>
}
