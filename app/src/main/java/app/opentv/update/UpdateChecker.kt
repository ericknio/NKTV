/*
 * This file is part of NKTV.
 */
package app.opentv.update

import app.opentv.data.remote.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class UpdateChecker(
    private val http: OkHttpClient,
    private val currentVersionName: String,
    private val repoSlug: String = REPO_SLUG,
) {
    data class Update(
        val versionName: String,
        val title: String,
        val notes: String,
        val apkUrl: String,
        val apkSizeBytes: Long,
        val releaseUrl: String,
        val sha256: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Verifica atualizações no GitHub Releases do repositório ericknio/NKTV.
     * Caso o GitHub esteja inacessível ou atinja rate-limit, consulta o painel web próprio como contingência.
     */
    suspend fun check(): Update? = withContext(Dispatchers.IO) {
        // 1. Tenta GitHub Releases oficial
        val gh = runCatching { checkGitHub() }.getOrNull()
        if (gh != null) return@withContext gh

        // 2. Fallback para a API do painel
        runCatching { checkPanel() }.getOrNull()
    }

    private fun checkGitHub(): Update? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repoSlug/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NKTV")
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val release = json.decodeFromString<GitHubRelease>(body)
            if (release.draft || release.prerelease) return null

            val asset = release.apkAsset() ?: return null
            if (!isNewer(release.tagName, currentVersionName)) return null

            val sha256 = Regex("""(?i)\b(?:sha[-_]?256|hash)\s*[:=]\s*([a-f0-9]{64})\b""")
                .find(release.body)?.groupValues?.get(1)

            return Update(
                versionName = release.tagName.trimStart('v', 'V'),
                title = release.name.ifBlank { "NKTV v${release.tagName}" },
                notes = cleanNotes(release.body),
                apkUrl = asset.browserDownloadUrl,
                apkSizeBytes = asset.size,
                releaseUrl = release.htmlUrl,
                sha256 = sha256,
            )
        }
    }

    private fun checkPanel(): Update? {
        val request = Request.Builder()
            .url(PANEL_UPDATE_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "NKTV")
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val j = JSONObject(body)
            val rawVersion = j.optString("version_name", "").trim()
            val apkUrl = j.optString("apk_url", "").trim()
            if (rawVersion.isBlank() || apkUrl.isBlank()) return null

            if (!isNewer(rawVersion, currentVersionName)) return null

            val sha256 = j.optString("sha256", j.optString("apk_sha256", ""))
                .trim().takeIf { it.length == 64 }

            return Update(
                versionName = rawVersion.trimStart('v', 'V'),
                title = j.optString("title", "NKTV v$rawVersion"),
                notes = cleanNotes(j.optString("notes", "")),
                apkUrl = apkUrl,
                apkSizeBytes = j.optLong("apk_size_bytes", 0L),
                releaseUrl = apkUrl,
                sha256 = sha256,
            )
        }
    }

    companion object {
        const val REPO_SLUG = "ericknio/NKTV"
        const val PANEL_UPDATE_URL = "https://app.fbtec.site/api/v1/update"

        fun cleanNotes(raw: String): String {
            var text = raw
            val cutFrom = listOf(
                "Built by GitHub Actions",
                "**Full Changelog**",
                "## What's Changed",
                "<!--",
            )
            for (marker in cutFrom) {
                val i = text.indexOf(marker, ignoreCase = true)
                if (i >= 0) text = text.substring(0, i)
            }
            text = text
                .replace(Regex("""\[([^\]]+)\]\([^)]*\)"""), "$1")
                .replace(Regex("""[*_]{1,3}([^*_\n]+)[*_]{1,3}"""), "$1")
                .replace("`", "")
                .replace(Regex("""(?m)^\s{0,3}#{1,6}\s*"""), "")
                .replace(Regex("""(?m)^\s*[-*]\s+"""), "• ")
                .replace(Regex("""(?m)^\s*-{3,}\s*$"""), "")
            return text.lines().joinToString("\n") { it.trimEnd() }
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
                .take(1200)
        }

        fun isNewer(tag: String, current: String): Boolean {
            val a = versionParts(tag)
            val b = versionParts(current)
            val n = maxOf(a.size, b.size)
            for (i in 0 until n) {
                val ai = a.getOrElse(i) { 0 }
                val bi = b.getOrElse(i) { 0 }
                if (ai != bi) return ai > bi
            }
            return false
        }

        private fun versionParts(v: String): List<Int> =
            v.trim().trimStart('v', 'V')
                .split('.', '-', '+')
                .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    }
}
