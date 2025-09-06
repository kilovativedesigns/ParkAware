package com.kilovativedesigns.parkaware.ui.education

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Remote JSON shape:
 * {
 *   "NSW": {
 *     "onStreet": ["...", "..."],
 *     "carParks": [ ... ],
 *     ...
 *   },
 *   "VIC": { ... },
 *   ...
 * }
 */
object EducationTipsRepository {

    // ⚠️ Use the "Raw" URL to your GitHub JSON
    private const val GITHUB_JSON_URL =
        "https://raw.githubusercontent.com/<org>/<repo>/<branch>/education_tips.json"

    // Cache for the app session
    @Volatile private var cache: Map<String, Map<String, List<String>>>? = null

    /** Load full JSON once (all states). */
    suspend fun getAllStates(): Map<String, Map<String, List<String>>> =
        cache ?: fetchRemote().also { cache = it }

    /** Convenience: get tips for a specific state (e.g., "NSW"). */
    suspend fun getState(state: String): Map<String, List<String>> =
        getAllStates()[state] ?: emptyMap()

    // --- Internal -----------------------------------------------------------

    private suspend fun fetchRemote(): Map<String, Map<String, List<String>>> =
        withContext(Dispatchers.IO) {
            val conn = (URL(GITHUB_JSON_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.inputStream.bufferedReader().use { r -> parseStates(r.readText()) }
        }

    private fun parseStates(json: String): Map<String, Map<String, List<String>>> {
        val root = JSONObject(json)
        val states = mutableMapOf<String, Map<String, List<String>>>()

        for (stateKey in root.keys()) {
            val stateObj = root.optJSONObject(stateKey) ?: continue
            val categories = mutableMapOf<String, List<String>>()
            for (catKey in stateObj.keys()) {
                val arr: JSONArray = stateObj.optJSONArray(catKey) ?: JSONArray()
                val tips = buildList(arr.length()) {
                    for (i in 0 until arr.length()) add(arr.optString(i))
                }
                categories[catKey] = tips
            }
            states[stateKey] = categories
        }
        return states
    }
}