package com.dumbify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dumbify.R
import com.dumbify.model.AppCategory
import com.dumbify.model.AppUsageData
import com.dumbify.repository.AppConfigRepository
import com.dumbify.ui.MainActivity
import com.dumbify.util.AiAnalyzer
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class UsageMonitorService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var repository: AppConfigRepository
    private lateinit var aiAnalyzer: AiAnalyzer
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var packageManager: PackageManager
    
    private val currentSessionStart = ConcurrentHashMap<String, Long>()
    private val dailyUsage = ConcurrentHashMap<String, Long>()
    private val appOpenCount = ConcurrentHashMap<String, Int>()
    private val recentApps = mutableListOf<String>()
    
    private var lastCheckedTime = System.currentTimeMillis()
    private var monitoringJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_monitor_channel"
        private const val CHECK_INTERVAL = 5000L // Check every 5 seconds
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = AppConfigRepository(this)
        aiAnalyzer = AiAnalyzer(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = packageManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        resetDailyCounters()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkUsage()
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private suspend fun checkUsage() {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastCheckedTime, now)
        val event = UsageEvents.Event()
        
        var currentForegroundApp: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentForegroundApp = event.packageName
                    handleAppOpened(event.packageName, event.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    handleAppClosed(event.packageName, event.timeStamp)
                }
            }
        }
        
        lastCheckedTime = now
        
        // Check current foreground app for time limits
        currentForegroundApp?.let { packageName ->
            checkTimeLimits(packageName)
        }
    }
    
    private suspend fun handleAppOpened(packageName: String, timestamp: Long) {
        if (packageName == applicationContext.packageName) return // Ignore self
        
        currentSessionStart[packageName] = timestamp
        appOpenCount[packageName] = (appOpenCount[packageName] ?: 0) + 1
        
        // Track recent apps
        recentApps.add(0, packageName)
        if (recentApps.size > 20) {
            recentApps.removeAt(recentApps.size - 1)
        }
        
        // AI analysis on app opening
        if (repository.isAiEnabled) {
            analyzeAppOpening(packageName)
        }
    }
    
    private fun handleAppClosed(packageName: String, timestamp: Long) {
        currentSessionStart[packageName]?.let { startTime ->
            val sessionDuration = timestamp - startTime
            dailyUsage[packageName] = (dailyUsage[packageName] ?: 0) + sessionDuration
            currentSessionStart.remove(packageName)
        }
    }
    
    private suspend fun checkTimeLimits(packageName: String) {
        val config = repository.getAppConfig(packageName) ?: return
        
        if (config.timeLimit == 0) return
        
        val usageMinutes = (dailyUsage[packageName] ?: 0) / 60000
        
        // Check warning threshold
        if (config.warningThreshold > 0 && usageMinutes >= config.warningThreshold) {
            if (usageMinutes < config.timeLimit) {
                sendWarningNotification(packageName, usageMinutes.toInt(), config.warningThreshold)
            }
        }
        
        // Check if time limit exceeded
        if (config.autoClose && usageMinutes >= config.timeLimit) {
            sendClosingNotification(packageName)
            requestAppClose(packageName)
        }
    }
    
    private suspend fun analyzeAppOpening(packageName: String) {
        serviceScope.launch {
            try {
                val appName = getAppName(packageName)
                val usageData = AppUsageData(
                    packageName = packageName,
                    appName = appName,
                    usageTimeMillis = dailyUsage[packageName] ?: 0,
                    openCount = appOpenCount[packageName] ?: 0,
                    lastUsedTimestamp = System.currentTimeMillis(),
                    isProductive = isProductiveApp(packageName)
                )
                
                // Only analyze if user is spending too much time
                val usageMinutes = usageData.usageTimeMillis / 60000
                if (usageMinutes > 10 && !usageData.isProductive) {
                    val analysis = aiAnalyzer.analyzeUsagePattern(
                        appName,
                        usageData,
                        recentApps.take(10)
                    )
                    
                    if (analysis.contains("problematic", ignoreCase = true) ||
                        analysis.contains("distraction", ignoreCase = true)) {
                        sendAiInsightNotification(appName, analysis)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun isProductiveApp(packageName: String): Boolean {
        val config = repository.getAppConfig(packageName)
        return config?.category == AppCategory.PRODUCTIVE
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun requestAppClose(packageName: String) {
        // Send broadcast to accessibility service to close the app
        val intent = Intent("com.dumbify.CLOSE_APP")
        intent.putExtra("package_name", packageName)
        sendBroadcast(intent)
    }
    
    private fun sendWarningNotification(packageName: String, currentMinutes: Int, threshold: Int) {
        serviceScope.launch {
            val appName = getAppName(packageName)
            val (shouldWarn, message) = aiAnalyzer.shouldWarnUser(appName, currentMinutes, threshold)
            
            if (shouldWarn) {
                val notification = NotificationCompat.Builder(this@UsageMonitorService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Time Check: $appName")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(packageName.hashCode(), notification)
            }
        }
    }
    
    private fun sendClosingNotification(packageName: String) {
        val appName = getAppName(packageName)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time Limit Reached")
            .setContentText("Closing $appName - daily limit exceeded")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode() + 1000, notification)
    }
    
    private fun sendAiInsightNotification(appName: String, insight: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AI Insight: $appName")
            .setContentText(insight)
            .setStyle(NotificationCompat.BigTextStyle().bigText(insight))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((appName + "ai").hashCode(), notification)
    }
    
    private fun resetDailyCounters() {
        serviceScope.launch {
            while (isActive) {
                // Reset at midnight
                val now = System.currentTimeMillis()
                val midnight = now - (now % 86400000) + 86400000
                val delayToMidnight = midnight - now
                
                delay(delayToMidnight)
                
                dailyUsage.clear()
                appOpenCount.clear()
                recentApps.clear()
            }
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_service_notification_title))
            .setContentText(getString(R.string.monitoring_service_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage for digital wellbeing"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
    }
}
