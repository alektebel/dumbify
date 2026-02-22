package com.dumbify.model

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val openCount: Int,
    val lastUsedTimestamp: Long,
    val isProductive: Boolean
)

data class UsageSession(
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long
)

data class DailyStats(
    val date: String,
    val totalScreenTime: Long,
    val productiveTime: Long,
    val distractingTime: Long,
    val topApps: List<AppUsageData>,
    val aiInsight: String = ""
)

enum class AppCategory {
    PRODUCTIVE,
    SOCIAL_MEDIA,
    ENTERTAINMENT,
    BLOCKED,
    NEUTRAL
}

data class AppConfig(
    val packageName: String,
    val category: AppCategory,
    val timeLimit: Int = 0, // minutes per day
    val warningThreshold: Int = 0, // minutes before warning
    val autoClose: Boolean = false
)

data class BlockedDomain(
    val domain: String,
    val category: String,
    val enabled: Boolean = true
)
