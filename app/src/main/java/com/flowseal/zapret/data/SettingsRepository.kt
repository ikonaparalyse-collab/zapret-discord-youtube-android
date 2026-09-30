package com.flowseal.zapret.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flowseal_zapret_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRESET_ID = "selected_preset_id"
        private const val KEY_DNS = "selected_dns"
        private const val KEY_CUSTOM_SPLIT = "custom_split_pos"
        private const val KEY_CUSTOM_DISORDER = "custom_disorder"
        private const val KEY_CUSTOM_FAKE = "custom_fake"
        private const val KEY_CUSTOM_TTL = "custom_ttl"
        private const val KEY_PER_APP = "per_app_proxy"
        private const val KEY_SELECTED_APPS = "selected_apps"
    }

    var selectedPresetId: String
        get() = prefs.getString(KEY_PRESET_ID, "general") ?: "general"
        set(value) = prefs.edit().putString(KEY_PRESET_ID, value).apply()

    var selectedDns: String
        get() = prefs.getString(KEY_DNS, "1.1.1.1") ?: "1.1.1.1"
        set(value) = prefs.edit().putString(KEY_DNS, value).apply()

    var customSplitPos: Int
        get() = prefs.getInt(KEY_CUSTOM_SPLIT, 1)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_SPLIT, value).apply()

    var customDisorder: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_DISORDER, true)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_DISORDER, value).apply()

    var customFake: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_FAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_FAKE, value).apply()

    var customTtl: Int
        get() = prefs.getInt(KEY_CUSTOM_TTL, 4)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_TTL, value).apply()

    var isPerAppRouting: Boolean
        get() = prefs.getBoolean(KEY_PER_APP, false)
        set(value) = prefs.edit().putBoolean(KEY_PER_APP, value).apply()

    var selectedApps: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_APPS, setOf("com.google.android.youtube", "com.discord")) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_APPS, value).apply()
}
