package com.dumbify.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dumbify.R
import com.dumbify.auth.GitHubOAuthManager
import com.dumbify.repository.AppConfigRepository
import com.dumbify.util.AiAnalyzer

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var aiAnalyzer: AiAnalyzer
    private lateinit var repository: AppConfigRepository
    private lateinit var oauthManager: GitHubOAuthManager
    
    // UI elements
    private lateinit var providerGroup: RadioGroup
    private lateinit var radioGemini: RadioButton
    private lateinit var radioGithub: RadioButton
    
    // Gemini section
    private lateinit var geminiConfigSection: View
    private lateinit var apiKeyInput: EditText
    private lateinit var saveButton: Button
    
    // GitHub section
    private lateinit var githubConfigSection: View
    private lateinit var githubStatusText: TextView
    private lateinit var githubConnectButton: Button
    private lateinit var githubDisconnectButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        aiAnalyzer = AiAnalyzer(this)
        repository = AppConfigRepository(this)
        oauthManager = GitHubOAuthManager(this)
        
        initializeViews()
        setupListeners()
        updateUI()
    }
    
    private fun initializeViews() {
        providerGroup = findViewById(R.id.ai_provider_group)
        radioGemini = findViewById(R.id.radio_gemini)
        radioGithub = findViewById(R.id.radio_github)
        
        geminiConfigSection = findViewById(R.id.gemini_config_section)
        apiKeyInput = findViewById(R.id.api_key_input)
        saveButton = findViewById(R.id.save_button)
        
        githubConfigSection = findViewById(R.id.github_config_section)
        githubStatusText = findViewById(R.id.github_status_text)
        githubConnectButton = findViewById(R.id.github_connect_button)
        githubDisconnectButton = findViewById(R.id.github_disconnect_button)
    }
    
    private fun setupListeners() {
        // Provider selection
        providerGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_gemini -> {
                    showGeminiConfig()
                    repository.selectedAiProvider = "gemini"
                }
                R.id.radio_github -> {
                    showGithubConfig()
                    repository.selectedAiProvider = "github"
                }
            }
        }
        
        // Gemini API key save
        saveButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                aiAnalyzer.setApiKey(apiKey)
                Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
            }
        }
        
        // GitHub OAuth connect
        githubConnectButton.setOnClickListener {
            oauthManager.startOAuthFlow(this)
        }
        
        // GitHub disconnect
        githubDisconnectButton.setOnClickListener {
            oauthManager.logout()
            updateGithubStatus()
            Toast.makeText(this, "Disconnected from GitHub", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {
        // Set selected provider
        when (repository.selectedAiProvider) {
            "github" -> {
                radioGithub.isChecked = true
                showGithubConfig()
            }
            else -> {
                radioGemini.isChecked = true
                showGeminiConfig()
            }
        }
        
        updateGithubStatus()
    }
    
    private fun showGeminiConfig() {
        geminiConfigSection.visibility = View.VISIBLE
        githubConfigSection.visibility = View.GONE
    }
    
    private fun showGithubConfig() {
        geminiConfigSection.visibility = View.GONE
        githubConfigSection.visibility = View.VISIBLE
    }
    
    private fun updateGithubStatus() {
        if (oauthManager.isAuthenticated()) {
            val scope = repository.githubTokenScope ?: "unknown"
            githubStatusText.text = "Connected (scope: $scope)"
            githubConnectButton.visibility = View.GONE
            githubDisconnectButton.visibility = View.VISIBLE
        } else {
            githubStatusText.text = "Not connected"
            githubConnectButton.visibility = View.VISIBLE
            githubDisconnectButton.visibility = View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update GitHub status when returning from OAuth flow
        updateGithubStatus()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
