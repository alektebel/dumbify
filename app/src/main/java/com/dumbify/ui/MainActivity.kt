package com.dumbify.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dumbify.R
import com.dumbify.repository.AppConfigRepository
import com.dumbify.service.AppControlAccessibilityService
import com.dumbify.service.DnsFilterService
import com.dumbify.service.UsageMonitorService
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var repository: AppConfigRepository
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var monitoringSwitch: Switch
    private lateinit var dnsFilterSwitch: Switch
    private lateinit var statusText: TextView
    private lateinit var configureAppsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var statsButton: Button
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, DnsFilterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            repository.isDnsFilterEnabled = true
            updateStatus()
        } else {
            dnsFilterSwitch.isChecked = false
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        // VPN_REQUEST_CODE no longer needed with Activity Result API
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        repository = AppConfigRepository(this)
        
        initViews()
        checkPermissions()
    }
    
    private fun initViews() {
        monitoringSwitch = findViewById(R.id.monitoring_switch)
        dnsFilterSwitch = findViewById(R.id.dns_filter_switch)
        statusText = findViewById(R.id.status_text)
        configureAppsButton = findViewById(R.id.configure_apps_button)
        settingsButton = findViewById(R.id.settings_button)
        statsButton = findViewById(R.id.stats_button)
        
        monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasUsageStatsPermission()) {
                    startMonitoringService()
                } else {
                    monitoringSwitch.isChecked = false
                    requestUsageStatsPermission()
                }
            } else {
                stopMonitoringService()
            }
        }
        
        dnsFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startDnsFilter()
            } else {
                stopDnsFilter()
            }
        }
        
        configureAppsButton.setOnClickListener {
            // TODO: Open app configuration screen
            Toast.makeText(this, "Configure apps coming soon", Toast.LENGTH_SHORT).show()
        }
        
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        statsButton.setOnClickListener {
            // TODO: Open statistics screen
            Toast.makeText(this, "Statistics coming soon", Toast.LENGTH_SHORT).show()
        }
        
        updateStatus()
    }
    
    private fun checkPermissions() {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasUsageStatsPermission()) {
            missingPermissions.add("Usage Stats")
        }
        
        if (!isAccessibilityServiceEnabled()) {
            missingPermissions.add("Accessibility Service")
        }
        
        if (missingPermissions.isNotEmpty()) {
            showPermissionsDialog(missingPermissions)
        }
    }
    
    private fun showPermissionsDialog(permissions: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Required Permissions")
            .setMessage("Dumbify needs the following permissions to work:\n\n${permissions.joinToString("\n")}\n\nWould you like to grant them now?")
            .setPositiveButton("Grant") { _, _ ->
                if (permissions.contains("Usage Stats")) {
                    requestUsageStatsPermission()
                } else if (permissions.contains("Accessibility Service")) {
                    requestAccessibilityPermission()
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${AppControlAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    private fun requestAccessibilityPermission() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("Dumbify needs accessibility access to automatically close apps when time limits are exceeded.\n\nPlease enable 'Dumbify' in the accessibility settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startMonitoringService() {
        val intent = Intent(this, UsageMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus()
    }
    
    private fun stopMonitoringService() {
        val intent = Intent(this, UsageMonitorService::class.java)
        stopService(intent)
        updateStatus()
    }
    
    private fun startDnsFilter() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // VPN permission already granted
            val serviceIntent = Intent(this, DnsFilterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            repository.isDnsFilterEnabled = true
            updateStatus()
        }
    }
    
    private fun stopDnsFilter() {
        val intent = Intent(this, DnsFilterService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
        repository.isDnsFilterEnabled = false
        updateStatus()
    }
    
    private fun updateStatus() {
        val monitoring = monitoringSwitch.isChecked
        val dnsFilter = dnsFilterSwitch.isChecked
        
        val status = buildString {
            append("Status: ")
            when {
                monitoring && dnsFilter -> append("Fully Protected")
                monitoring -> append("Monitoring Active")
                dnsFilter -> append("DNS Filter Active")
                else -> append("Inactive")
            }
        }
        
        statusText.text = status
    }
    
    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
