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
import android.widget.PopupMenu
import org.libsdl.app.SDLActivity

/**
 * Extended SDL Activity with on-screen button overlay and menu button support
 */
class LgptSDLActivity : SDLActivity() {
    
    companion object {
        private const val TAG = "LgptSDLActivity"
        private const val PREF_BUTTONS_ENABLED = "onscreen_buttons_enabled"
        private const val MENU_BUTTON_VIEW_ID = 0x00f00001
        private const val PREF_SURFACE_OFFSET_X = "surface_offset_x"
        private const val PREF_SURFACE_OFFSET_Y = "surface_offset_y"
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
            restoreSurfaceOffset()
            menuButtonSetupDone = true
        }
    }

    private fun setupButtonOverlay() {
        try {
            // Find the SDL content view (parent of all SDL views)
            val contentView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            // Set background color: read from config.xml if available, otherwise use game default
            val bgColor = readBackgroundColorFromConfig()
            contentView.setBackgroundColor(bgColor)
            
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
            val density = resources.displayMetrics.density
            val sizePx = (48 * density).toInt()
            val marginPx = (8 * density).toInt()
            val topMarginPx = (56 * density).toInt()  // 56dp to clear status bar

            // Create hamburger menu button (top-left corner)
            val menuButton = ImageButton(this).apply {
                id = MENU_BUTTON_VIEW_ID
                layoutParams = FrameLayout.LayoutParams(
                    sizePx,
                    sizePx,
                    android.view.Gravity.TOP or android.view.Gravity.START
                ).apply {
                    topMargin = topMarginPx
                    leftMargin = marginPx
                }
                setImageResource(android.R.drawable.ic_menu_more)
                setBackgroundColor(0xFF7B68A6.toInt())
                elevation = 100f
                setOnClickListener {
                    openSettings()
                }
            }

            contentView.addView(menuButton)
            menuButton.bringToFront()
            contentView.invalidate()
            Log.i(TAG, "Menu button setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup menu button", e)
            e.printStackTrace()
        }
    }

    private fun openSettings() {
        Log.i(TAG, "Opening menu")
        val contentView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val anchor = contentView.findViewById<ImageButton>(MENU_BUTTON_VIEW_ID) ?: contentView
        val popup = PopupMenu(this, anchor)
        popup.menu.add("X+").setOnMenuItemClickListener {
            adjustSurface(20f, 0f)
            true
        }
        popup.menu.add("X-").setOnMenuItemClickListener {
            adjustSurface(-20f, 0f)
            true
        }
        popup.menu.add("Y+").setOnMenuItemClickListener {
            adjustSurface(0f, 20f)
            true
        }
        popup.menu.add("Y-").setOnMenuItemClickListener {
            adjustSurface(0f, -20f)
            true
        }
        popup.menu.add("Settings").setOnMenuItemClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        popup.menu.add("Configure Input").setOnMenuItemClickListener {
            startActivity(Intent(this, InputMapperActivity::class.java))
            true
        }
        popup.show()
    }

    private fun adjustSurface(dx: Float, dy: Float) {
        mSurface?.let {
            it.translationX += dx
            it.translationY += dy
            val offsetX = prefs.getFloat(PREF_SURFACE_OFFSET_X, 0f) + dx
            val offsetY = prefs.getFloat(PREF_SURFACE_OFFSET_Y, 0f) + dy
            prefs.edit().putFloat(PREF_SURFACE_OFFSET_X, offsetX).putFloat(PREF_SURFACE_OFFSET_Y, offsetY).apply()
            Log.i(TAG, "Surface offset: X=$offsetX, Y=$offsetY")
        }
    }

    private fun restoreSurfaceOffset() {
        mSurface?.let {
            val offsetX = prefs.getFloat(PREF_SURFACE_OFFSET_X, 0f)
            val offsetY = prefs.getFloat(PREF_SURFACE_OFFSET_Y, 0f)
            it.translationX = offsetX
            it.translationY = offsetY
            Log.i(TAG, "Restored surface offset: X=$offsetX, Y=$offsetY")
        }
    }

    private fun readBackgroundColorFromConfig(): Int {
        // Default game color: 0x1D0A1F (dark purple)
        var r = 0x1D
        var g = 0x0A
        var b = 0x1F
        
        try {
            val configFile = getExternalFilesDir(null)?.let { java.io.File(it, "config.xml") }
            if (configFile?.exists() == true) {
                val content = configFile.readText()
                val bgPattern = """<BACKGROUND\s+value="([0-9A-Fa-f]{6})"\s*/?>""".toRegex()
                val match = bgPattern.find(content)
                if (match != null) {
                    val hexColor = match.groupValues[1]
                    r = hexColor.substring(0, 2).toInt(16)
                    g = hexColor.substring(2, 4).toInt(16)
                    b = hexColor.substring(4, 6).toInt(16)
                    Log.i(TAG, "Custom background color from config: $hexColor ($r, $g, $b)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading background color from config: ${e.message}")
        }
        
        return android.graphics.Color.rgb(r, g, b)
    }

    fun toggleButtonOverlay(enabled: Boolean) {
        buttonOverlay?.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        prefs.edit().putBoolean(PREF_BUTTONS_ENABLED, enabled).apply()
        Log.i(TAG, "Button overlay toggled: $enabled")
    }

    fun isButtonOverlayVisible(): Boolean = buttonOverlay?.visibility == android.view.View.VISIBLE
}
