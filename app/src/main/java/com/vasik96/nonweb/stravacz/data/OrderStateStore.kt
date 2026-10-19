package com.vasik96.nonweb.stravacz.data

import android.content.Context
import org.json.JSONObject

/** Stores the last known local selection of ordered meals to detect external changes. */
class OrderStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("strava_orders_state", Context.MODE_PRIVATE)

    fun saveState(state: Map<String, String?>) {
        val json = JSONObject()
        state.forEach { (key, mealId) ->
            if (mealId != null) {
                json.put(key, mealId)
            }
        }
        prefs.edit().putString("saved_orders", json.toString()).apply()
    }

    fun loadState(): Map<String, String?> {
        val raw = prefs.getString("saved_orders", null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String?>()
            json.keys().forEach { key ->
                map[key] = json.optString(key).takeIf { it.isNotBlank() }
            }
            map
        }.getOrDefault(emptyMap())
    }
}
