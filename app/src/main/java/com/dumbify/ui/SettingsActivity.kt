package com.dumbify.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dumbify.R
import com.dumbify.util.AiAnalyzer

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var aiAnalyzer: AiAnalyzer
    private lateinit var apiKeyInput: EditText
    private lateinit var saveButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        aiAnalyzer = AiAnalyzer(this)
        
        apiKeyInput = findViewById(R.id.api_key_input)
        saveButton = findViewById(R.id.save_button)
        
        saveButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                aiAnalyzer.setApiKey(apiKey)
                Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
