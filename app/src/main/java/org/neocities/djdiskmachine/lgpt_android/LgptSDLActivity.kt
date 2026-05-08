package org.neocities.djdiskmachine.lgpt_android

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.content.Intent
import org.libsdl.app.SDLActivity

/**
 * Extended SDL Activity with on-screen button overlay and menu button support
 */
class LgptSDLActivity : SDLActivity() {
    
    companion object {
        private const val TAG = "LgptSDLActivity"
        private const val PREF_BUTTONS_ENABLED = "onscreen_buttons_enabled"
    }
    
    private var buttonOverlay: OnScreenButtonOverlay? = null
    private lateinit var prefs: SharedPreferences
    private var overlaySetupDone = false
    private var menuButtonSetupDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("lgpt_prefs", MODE_PRIVATE)
        
        // Listen for preference changes (e.g., when toggle is changed in SettingsActivity)
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_BUTTONS_ENABLED && buttonOverlay != null) {
                val enabled = prefs.getBoolean(PREF_BUTTONS_ENABLED, true)
                // Post to main thread to ensure UI updates properly
                runOnUiThread {
                    buttonOverlay!!.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
                    Log.i(TAG, "Button overlay visibility updated from preference: $enabled")
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        // Add overlay and menu button once window has focus and SDL surface is ready
        if (hasFocus && !overlaySetupDone) {
            setupButtonOverlay()
            overlaySetupDone = true
        }
        if (hasFocus && !menuButtonSetupDone) {
            setupMenuButton()
            menuButtonSetupDone = true
        }
    }

    private fun setupButtonOverlay() {
        try {
            // Find the SDL content view (parent of all SDL views)
            val contentView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            
            // Create button overlay
            buttonOverlay = OnScreenButtonOverlay(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            // Add to content view on top of SDL surface
            contentView.addView(buttonOverlay)
            
            // Set initial visibility
            val buttonsEnabled = prefs.getBoolean(PREF_BUTTONS_ENABLED, true)
            buttonOverlay?.visibility = if (buttonsEnabled) android.view.View.VISIBLE else android.view.View.GONE
            
            Log.i(TAG, "Button overlay setup complete. Visible: $buttonsEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup button overlay", e)
            e.printStackTrace()
        }
    }

    private fun setupMenuButton() {
        try {
            val contentView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            
            // Create hamburger menu button (top-left corner)
            val menuButton = ImageButton(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    80,
                    80,
                    android.view.Gravity.TOP or android.view.Gravity.START
                ).apply {
                    topMargin = 20
                    leftMargin = 20
                }
                setImageResource(android.R.drawable.ic_menu_more)
                setBackgroundColor(0xFF7B68A6.toInt())
                setOnClickListener {
                    openSettings()
                }
            }
            
            contentView.addView(menuButton)
            Log.i(TAG, "Menu button setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup menu button", e)
            e.printStackTrace()
        }
    }

    private fun openSettings() {
        Log.i(TAG, "Opening settings")
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun toggleButtonOverlay(enabled: Boolean) {
        buttonOverlay?.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        prefs.edit().putBoolean(PREF_BUTTONS_ENABLED, enabled).apply()
        Log.i(TAG, "Button overlay toggled: $enabled")
    }

    fun isButtonOverlayVisible(): Boolean = buttonOverlay?.visibility == android.view.View.VISIBLE
}
