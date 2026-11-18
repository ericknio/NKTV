/*
 * This file is part of NKTV.
 */
package app.opentv.core

import android.content.Context
import android.os.Build
import android.provider.Settings
import app.opentv.BuildConfig
import app.opentv.data.model.SourceKind
import app.opentv.data.repo.SourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AppConfig {
    const val APP_NAME = "NKTV"
    const val DEFAULT_SERVER_URL = "http://nktv.site"
    const val PANEL_PING_URL = "https://app.fbtec.site/api/v1/ping"
    private const val PREF_KEY_SERVER_URL = "nktv_server_url"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences("nktv_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_SERVER_URL
    }

    suspend fun syncWithRemote(context: Context, sourceRepo: SourceRepository? = null) = withContext(Dispatchers.IO) {
        runCatching {
            val deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }
            val model = Build.MODEL ?: "Android TV"
            val manufacturer = Build.MANUFACTURER ?: "Generic"
            val appVersion = BuildConfig.VERSION_NAME
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            val json = JSONObject().apply {
                put("device_id", deviceId)
                put("model", model)
                put("manufacturer", manufacturer)
                put("app_version", appVersion)
                put("android_version", androidVersion)
            }

            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(PANEL_PING_URL)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string().orEmpty()
                val respJson = JSONObject(respBody)
                val remoteUrl = respJson.optString("server_url", "").trim()
                if (remoteUrl.isNotBlank()) {
                    val prefs = context.getSharedPreferences("nktv_prefs", Context.MODE_PRIVATE)
                    val oldUrl = prefs.getString(PREF_KEY_SERVER_URL, DEFAULT_SERVER_URL)
                    prefs.edit().putString(PREF_KEY_SERVER_URL, remoteUrl).apply()

                    // Propaga o novo DNS automaticamente para as fontes salvas do usuário
                    if (sourceRepo != null && remoteUrl != oldUrl) {
                        val sources = sourceRepo.enabled()
                        for (src in sources) {
                            if (src.kind == SourceKind.XTREAM && (src.url == oldUrl || src.url == DEFAULT_SERVER_URL)) {
                                sourceRepo.save(src.copy(url = remoteUrl))
                            }
                        }
                    }
                }
            }
        }
    }
}
