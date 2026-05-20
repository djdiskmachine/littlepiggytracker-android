package org.neocities.djdiskmachine.lgpt_android

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Button
import android.widget.ScrollView
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback

/**
 * Quick settings menu for LGPT Android
 * Provides toggles for on-screen buttons, screen rotation, and other settings
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    
    companion object {
        private const val TAG = "SettingsActivity"
        private const val PREF_BUTTONS_ENABLED = "onscreen_buttons_enabled"
        private const val PREF_SCREEN_ROTATION = "screen_rotation_mode"  // "portrait", "landscape", "auto"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("lgpt_prefs", MODE_PRIVATE)
        
        // Setup back gesture handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Return to piggy
                finish()
            }
        })
        
        // Create UI programmatically for simplicity
        setupUI()
    }

    private fun setupUI() {
        // Create scrollable container
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = true
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Add padding to account for status bar and margins
            setPadding(20, 60, 20, 20)
        }
        
        // Title
        val titleText = TextView(this).apply {
            text = "LGPT Settings"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 300
                bottomMargin = topMargin
            }
        }
        layout.addView(titleText)
        
        // On-screen buttons toggle
        val buttonSettingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        
        val buttonLabel = TextView(this).apply {
            text = "On-Screen Buttons"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val buttonsEnabled = prefs.getBoolean(PREF_BUTTONS_ENABLED, true)
        val buttonSwitch = Switch(this).apply {
            isChecked = buttonsEnabled
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(PREF_BUTTONS_ENABLED, isChecked).apply()
                Log.i(TAG, "On-screen buttons toggled: $isChecked")
            }
        }
        
        buttonSettingLayout.addView(buttonLabel)
        buttonSettingLayout.addView(buttonSwitch)
        layout.addView(buttonSettingLayout)
        
        // Screen rotation toggle
        val rotationSettingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        
        val rotationLabel = TextView(this).apply {
            text = "Screen Rotation"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }
        rotationSettingLayout.addView(rotationLabel)
        
        val currentRotation = prefs.getString(PREF_SCREEN_ROTATION, "auto") ?: "auto"
        
        // Create all buttons first, then setup click listeners to ensure proper reference capture
        val portraitBtn = Button(this).apply {
            text = if (currentRotation == "portrait") "✓ Portrait" else "Portrait"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        
        val landscapeBtn = Button(this).apply {
            text = if (currentRotation == "landscape") "✓ Landscape" else "Landscape"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        
        val autoBtn = Button(this).apply {
            text = if (currentRotation == "auto") "✓ Auto" else "Auto"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        
        // Now set click listeners
        portraitBtn.setOnClickListener {
            prefs.edit().putString(PREF_SCREEN_ROTATION, "portrait").apply()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.i(TAG, "Screen rotation set to PORTRAIT")
            portraitBtn.text = "✓ Portrait"
            landscapeBtn.text = "Landscape"
            autoBtn.text = "Auto"
        }
        
        landscapeBtn.setOnClickListener {
            prefs.edit().putString(PREF_SCREEN_ROTATION, "landscape").apply()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Log.i(TAG, "Screen rotation set to LANDSCAPE")
            portraitBtn.text = "Portrait"
            landscapeBtn.text = "✓ Landscape"
            autoBtn.text = "Auto"
        }
        
        autoBtn.setOnClickListener {
            prefs.edit().putString(PREF_SCREEN_ROTATION, "auto").apply()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            Log.i(TAG, "Screen rotation set to AUTO")
            portraitBtn.text = "Portrait"
            landscapeBtn.text = "Landscape"
            autoBtn.text = "✓ Auto"
        }
        
        // Add buttons to layout
        rotationSettingLayout.addView(portraitBtn)
        rotationSettingLayout.addView(landscapeBtn)
        rotationSettingLayout.addView(autoBtn)
        
        layout.addView(rotationSettingLayout)
        
        // Add layout to scroll view and set as content
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
