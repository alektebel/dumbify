package com.dumbify.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AppControlAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val closeAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name") ?: return
            closeApp(packageName)
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Register receiver for close app requests
        val filter = IntentFilter("com.dumbify.CLOSE_APP")
        registerReceiver(closeAppReceiver, filter, RECEIVER_EXPORTED)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle events, we just use the service to close apps
    }
    
    override fun onInterrupt() {
        // Handle interruptions
    }
    
    private fun closeApp(packageName: String) {
        serviceScope.launch {
            try {
                // Method 1: Go to home screen (most reliable)
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                delay(500)
                
                // Method 2: Try to kill the app (requires root or system permissions)
                // This won't work on most devices without root
                // Just going home is usually enough to stop the user
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeAppReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceScope.cancel()
    }
}
