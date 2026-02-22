package com.dumbify.repository

import android.content.Context
import android.content.SharedPreferences
import com.dumbify.model.AppCategory
import com.dumbify.model.AppConfig
import com.dumbify.model.BlockedDomain
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppConfigRepository(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_APP_CONFIGS = "app_configs"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
        private const val KEY_DAILY_SOCIAL_LIMIT = "daily_social_limit"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_DNS_ENABLED = "dns_enabled"
        
        // Default productive apps
        val DEFAULT_PRODUCTIVE_APPS = listOf(
            "com.whatsapp",
            "com.amazon.kindle",
            "com.google.android.apps.books",
            "com.twitter.android", // X
            "com.linkedin.android",
            "com.github.android",
            "notion.id",
            "com.evernote",
            "com.microsoft.office.outlook",
            "com.google.android.apps.docs"
        )
        
        // Default blocked domains
        val DEFAULT_BLOCKED_DOMAINS = listOf(
            BlockedDomain("bet365.com", "gambling"),
            BlockedDomain("draftkings.com", "gambling"),
            BlockedDomain("fanduel.com", "gambling"),
            BlockedDomain("pornhub.com", "adult"),
            BlockedDomain("xvideos.com", "adult"),
            BlockedDomain("xnxx.com", "adult"),
            BlockedDomain("reddit.com/r/nsfw", "adult"),
            BlockedDomain("casino.com", "gambling"),
            BlockedDomain("pokerstars.com", "gambling")
        )
    }
    
    fun getAppConfig(packageName: String): AppConfig? {
        val configs = getAllAppConfigs()
        return configs[packageName]
    }
    
    fun getAllAppConfigs(): Map<String, AppConfig> {
        val json = prefs.getString(KEY_APP_CONFIGS, null) ?: return getDefaultConfigs()
        val type = object : TypeToken<Map<String, AppConfig>>() {}.type
        return gson.fromJson(json, type)
    }
    
    private fun getDefaultConfigs(): Map<String, AppConfig> {
        val configs = mutableMapOf<String, AppConfig>()
        
        // Add default productive apps
        DEFAULT_PRODUCTIVE_APPS.forEach { packageName ->
            configs[packageName] = AppConfig(
                packageName = packageName,
                category = AppCategory.PRODUCTIVE,
                timeLimit = 0,
                warningThreshold = 0,
                autoClose = false
            )
        }
        
        // Add social media with limits
        configs["com.twitter.android"] = AppConfig(
            packageName = "com.twitter.android",
            category = AppCategory.SOCIAL_MEDIA,
            timeLimit = 30,
            warningThreshold = 20,
            autoClose = true
        )
        
        configs["com.linkedin.android"] = AppConfig(
            packageName = "com.linkedin.android",
            category = AppCategory.SOCIAL_MEDIA,
            timeLimit = 30,
            warningThreshold = 20,
            autoClose = true
        )
        
        configs["com.instagram.android"] = AppConfig(
            packageName = "com.instagram.android",
            category = AppCategory.ENTERTAINMENT,
            timeLimit = 15,
            warningThreshold = 10,
            autoClose = true
        )
        
        configs["com.facebook.katana"] = AppConfig(
            packageName = "com.facebook.katana",
            category = AppCategory.ENTERTAINMENT,
            timeLimit = 15,
            warningThreshold = 10,
            autoClose = true
        )
        
        configs["com.zhiliaoapp.musically"] = AppConfig( // TikTok
            packageName = "com.zhiliaoapp.musically",
            category = AppCategory.BLOCKED,
            timeLimit = 0,
            warningThreshold = 0,
            autoClose = true
        )
        
        return configs
    }
    
    fun saveAppConfig(config: AppConfig) {
        val configs = getAllAppConfigs().toMutableMap()
        configs[config.packageName] = config
        saveAllConfigs(configs)
    }
    
    private fun saveAllConfigs(configs: Map<String, AppConfig>) {
        val json = gson.toJson(configs)
        prefs.edit().putString(KEY_APP_CONFIGS, json).apply()
    }
    
    fun getBlockedDomains(): List<BlockedDomain> {
        val json = prefs.getString(KEY_BLOCKED_DOMAINS, null) ?: return DEFAULT_BLOCKED_DOMAINS
        val type = object : TypeToken<List<BlockedDomain>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun saveBlockedDomains(domains: List<BlockedDomain>) {
        val json = gson.toJson(domains)
        prefs.edit().putString(KEY_BLOCKED_DOMAINS, json).apply()
    }
    
    fun addBlockedDomain(domain: BlockedDomain) {
        val domains = getBlockedDomains().toMutableList()
        domains.add(domain)
        saveBlockedDomains(domains)
    }
    
    var dailySocialMediaLimit: Int
        get() = prefs.getInt(KEY_DAILY_SOCIAL_LIMIT, 60) // 60 minutes default
        set(value) = prefs.edit().putInt(KEY_DAILY_SOCIAL_LIMIT, value).apply()
    
    var isAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()
    
    var isDnsFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_DNS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DNS_ENABLED, value).apply()
}
