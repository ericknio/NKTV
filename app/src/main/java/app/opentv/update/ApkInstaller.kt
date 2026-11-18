/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Downloads an update APK, verifies its size, hash, and certificate integrity,
 * and hands it to the system installer.
 */
class ApkInstaller(private val http: OkHttpClient) {

    /** Progress as a 0f..1f fraction, or -1f when the total size is unknown. */
    fun interface Progress {
        fun onProgress(fraction: Float)
    }

    /**
     * Downloads [url], validates integrity and launches the installer.
     * Returns true once the installer intent has been fired.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        expectedBytes: Long,
        expectedSha256: String? = null,
        progress: Progress,
    ): Boolean {
        val apk = try {
            download(context, url, expectedBytes, progress)
        } catch (e: Throwable) {
            runCatching { File(context.cacheDir, "updates/opentv-update.apk").delete() }
            throw e
        }

        try {
            validateApk(context, apk, expectedBytes, expectedSha256)
            launchInstaller(context, apk)
            return true
        } catch (e: Throwable) {
            apk.delete()
            throw e
        }
    }

    private suspend fun download(
        context: Context,
        url: String,
        expectedBytes: Long,
        progress: Progress,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "opentv-update.apk")

        val request = Request.Builder().url(url).header("User-Agent", "OpenTV").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed: HTTP ${response.code}")
            val bodyStream = response.body?.byteStream() ?: error("Empty download")
            val total = if (expectedBytes > 0) expectedBytes else (response.body?.contentLength() ?: -1L)

            out.outputStream().use { sink ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                var written = 0L
                while (bodyStream.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    written += read
                    progress.onProgress(if (total > 0) (written.toFloat() / total) else -1f)
                }
            }

            if (expectedBytes > 0 && out.length() != expectedBytes) {
                out.delete()
                error("Truncated download: expected $expectedBytes bytes but received ${out.length()}")
            }
        }
        out
    }

    private fun validateApk(
        context: Context,
        apk: File,
        expectedBytes: Long,
        expectedSha256: String?,
    ) {
        if (!apk.exists() || apk.length() == 0L) {
            error("Downloaded APK file is missing or empty")
        }

        if (expectedBytes > 0 && apk.length() != expectedBytes) {
            error("APK size mismatch: got ${apk.length()} bytes, expected $expectedBytes")
        }

        if (!expectedSha256.isNullOrBlank()) {
            val actualSha = computeSha256(apk)
            if (!actualSha.equals(expectedSha256.trim(), ignoreCase = true)) {
                error("APK SHA-256 mismatch: got $actualSha, expected $expectedSha256")
            }
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Invalid or corrupted APK archive")

        if (archiveInfo.packageName != context.packageName) {
            error("APK package name mismatch: ${archiveInfo.packageName} (expected ${context.packageName})")
        }

        // Validate that the signing certificates match the installed app
        val currentInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull()

        if (currentInfo != null) {
            val currentSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentInfo.signingInfo?.signingCertificateHistory?.map { it.toByteArray() }
                    ?: currentInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
                    ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                currentInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            val archiveSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.signingInfo?.signingCertificateHistory?.map { it.toByteArray() }
                    ?: archiveInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
                    ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            if (currentSignatures.isNotEmpty() && archiveSignatures.isNotEmpty()) {
                val matches = currentSignatures.any { cur ->
                    archiveSignatures.any { arc -> cur.contentEquals(arc) }
                }
                if (!matches) {
                    error("APK certificate does not match the currently installed app certificate")
                }
            }
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun launchInstaller(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        context.startActivity(intent)
    }
}
