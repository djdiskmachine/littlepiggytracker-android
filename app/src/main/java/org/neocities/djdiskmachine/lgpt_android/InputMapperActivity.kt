package org.neocities.djdiskmachine.lgpt_android

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class InputMapperActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InputMapper"
        private const val PREF_MAPPING_COMPLETE = "input_mapping_complete"

        val LGPT_EVENTS = listOf(
            "up", "down", "left", "right",
            "a", "b", "start", "select",
            "lshoulder", "rshoulder"
        )
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var configManager: ConfigManager

    private lateinit var instructionText: TextView
    private lateinit var detectedEventText: TextView
    private lateinit var progressText: TextView
    private lateinit var skipButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    private var currentEventIndex = 0
    private val mappedEvents = mutableMapOf<String, String>()
    private var isListeningForInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_mapper)

        prefs = getSharedPreferences("lgpt_prefs", MODE_PRIVATE)
        configManager = ConfigManager(this)

        setupUI()
        setupBackNavigation()

        mappedEvents.putAll(configManager.loadKeyBindings())

        if (mappedEvents.size == LGPT_EVENTS.size) {
            showFullyMappedDialog()
        } else {
            startMappingFlow()
        }
    }

    private fun setupUI() {
        instructionText = findViewById(R.id.instruction_text)
        detectedEventText = findViewById(R.id.detected_event_text)
        progressText = findViewById(R.id.progress_text)
        skipButton = findViewById(R.id.skip_button)
        saveButton = findViewById(R.id.save_button)
        resetButton = findViewById(R.id.reset_button)

        skipButton.setOnClickListener { skipCurrentEvent() }
        saveButton.setOnClickListener { saveAndFinish() }
        resetButton.setOnClickListener { resetAllMappings() }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentEventIndex > 0) {
                    currentEventIndex--
                    mappedEvents.remove(LGPT_EVENTS[currentEventIndex])
                    startListeningForCurrentEvent()
                } else {
                    finish()
                }
            }
        })
    }

    private fun startMappingFlow() {
        currentEventIndex = LGPT_EVENTS.indexOfFirst { !mappedEvents.containsKey(it) }
        if (currentEventIndex < 0) currentEventIndex = 0
        startListeningForCurrentEvent()
    }

    private fun startListeningForCurrentEvent() {
        if (currentEventIndex >= LGPT_EVENTS.size) { promptSaveAndFinish(); return }
        val event = LGPT_EVENTS[currentEventIndex]
        instructionText.text = "Press button/key for:\n$event\n\n(or skip)"
        detectedEventText.text = ""
        progressText.text = "${currentEventIndex + 1}/${LGPT_EVENTS.size} mapped"
        isListeningForInput = true
        Log.i(TAG, "Waiting for input for event: $event")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isListeningForInput) return super.onKeyDown(keyCode, event)
        val lgptEvent = LGPT_EVENTS[currentEventIndex]
        val mappingValue = keyCodeToMappingValue(keyCode)
        detectedEventText.text = "Detected: $mappingValue"
        Log.i(TAG, "Mapped $lgptEvent -> $mappingValue")
        mappedEvents[lgptEvent] = mappingValue
        isListeningForInput = false
        currentEventIndex++
        startListeningForCurrentEvent()
        return true
    }

    private fun skipCurrentEvent() {
        Log.i(TAG, "Skipped: ${LGPT_EVENTS[currentEventIndex]}")
        currentEventIndex++
        startListeningForCurrentEvent()
    }

    private fun promptSaveAndFinish() {
        instructionText.text = "Mapping complete!\n\n${mappedEvents.size}/${LGPT_EVENTS.size} buttons mapped"
        detectedEventText.text = ""
        progressText.text = "Ready to save"
        isListeningForInput = false
    }

    private fun saveAndFinish() {
        Log.i(TAG, "Saving ${mappedEvents.size} mapped events")
        configManager.saveKeyBindings(mappedEvents)
        prefs.edit().putBoolean(PREF_MAPPING_COMPLETE, true).apply()
        
        // Launch with the new config
        Log.i(TAG, "Starting LgptSDLActivity with new config")
        val intent = Intent(this, LgptSDLActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun resetAllMappings() {
        mappedEvents.clear()
        currentEventIndex = 0
        isListeningForInput = false
        startMappingFlow()
    }

    private fun showFullyMappedDialog() {
        instructionText.text = "Input mapping configured:\n\n${mappedEvents.size} events mapped"
        detectedEventText.text = "Current mappings loaded"
        progressText.text = "Complete"
        skipButton.text = "Start Game"
        skipButton.setOnClickListener {
            Log.i(TAG, "Starting with current mappings")
            val intent = Intent(this, LgptSDLActivity::class.java)
            startActivity(intent)
            finish()
        }
        resetButton.text = "Reconfigure"
        resetButton.setOnClickListener { resetAllMappings() }
        saveButton.visibility = android.view.View.GONE
    }

    // Maps an Android keycode to lgpt config format:
    // gamepad buttons → "but:0:N" (SDL joystick button index per SDL_sysjoystick.c)
    // keyboard keys   → "key:0:name"
    private fun keyCodeToMappingValue(keyCode: Int): String {
        val sdlButton = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A       -> 0   // SDL_CONTROLLER_BUTTON_A
            KeyEvent.KEYCODE_BUTTON_B       -> 1   // SDL_CONTROLLER_BUTTON_B
            KeyEvent.KEYCODE_BUTTON_X       -> 2   // SDL_CONTROLLER_BUTTON_X
            KeyEvent.KEYCODE_BUTTON_Y       -> 3   // SDL_CONTROLLER_BUTTON_Y
            KeyEvent.KEYCODE_BUTTON_SELECT  -> 4   // SDL_CONTROLLER_BUTTON_BACK
            KeyEvent.KEYCODE_BUTTON_MODE    -> 5   // SDL_CONTROLLER_BUTTON_GUIDE
            KeyEvent.KEYCODE_BUTTON_START   -> 6   // SDL_CONTROLLER_BUTTON_START
            KeyEvent.KEYCODE_BUTTON_THUMBL  -> 7   // SDL_CONTROLLER_BUTTON_LEFTSTICK
            KeyEvent.KEYCODE_BUTTON_THUMBR  -> 8   // SDL_CONTROLLER_BUTTON_RIGHTSTICK
            KeyEvent.KEYCODE_BUTTON_L1      -> 9   // SDL_CONTROLLER_BUTTON_LEFTSHOULDER
            KeyEvent.KEYCODE_BUTTON_R1      -> 10  // SDL_CONTROLLER_BUTTON_RIGHTSHOULDER
            KeyEvent.KEYCODE_DPAD_UP        -> 11  // SDL_CONTROLLER_BUTTON_DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN      -> 12  // SDL_CONTROLLER_BUTTON_DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT      -> 13  // SDL_CONTROLLER_BUTTON_DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT     -> 14  // SDL_CONTROLLER_BUTTON_DPAD_RIGHT
            KeyEvent.KEYCODE_BUTTON_L2      -> 15
            KeyEvent.KEYCODE_BUTTON_R2      -> 16
            KeyEvent.KEYCODE_BUTTON_C       -> 17
            KeyEvent.KEYCODE_BUTTON_Z       -> 18
            in 188..203                     -> 20 + (keyCode - 188) // KEYCODE_BUTTON_1..16
            else -> -1
        }
        if (sdlButton >= 0) return "but:0:$sdlButton"

        val keyName = when (keyCode) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ->
                ('a' + (keyCode - KeyEvent.KEYCODE_A)).toString()
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ->
                ('0' + (keyCode - KeyEvent.KEYCODE_0)).toString()
            KeyEvent.KEYCODE_SPACE        -> "space"
            KeyEvent.KEYCODE_ENTER        -> "return"
            KeyEvent.KEYCODE_BACK         -> "back"
            KeyEvent.KEYCODE_DEL          -> "backspace"
            KeyEvent.KEYCODE_ESCAPE       -> "escape"
            KeyEvent.KEYCODE_TAB          -> "tab"
            KeyEvent.KEYCODE_MINUS        -> "[-]"
            KeyEvent.KEYCODE_EQUALS       -> "="
            KeyEvent.KEYCODE_LEFT_BRACKET -> "["
            KeyEvent.KEYCODE_RIGHT_BRACKET-> "]"
            KeyEvent.KEYCODE_SEMICOLON    -> ";"
            KeyEvent.KEYCODE_APOSTROPHE   -> "'"
            KeyEvent.KEYCODE_SLASH        -> "/"
            KeyEvent.KEYCODE_BACKSLASH    -> "\\"
            KeyEvent.KEYCODE_COMMA        -> ","
            KeyEvent.KEYCODE_PERIOD       -> "."
            KeyEvent.KEYCODE_GRAVE        -> "`"
            KeyEvent.KEYCODE_SHIFT_LEFT   -> "left shift"
            KeyEvent.KEYCODE_SHIFT_RIGHT  -> "right shift"
            KeyEvent.KEYCODE_CTRL_LEFT    -> "left ctrl"
            KeyEvent.KEYCODE_CTRL_RIGHT   -> "right ctrl"
            KeyEvent.KEYCODE_ALT_LEFT     -> "left alt"
            KeyEvent.KEYCODE_ALT_RIGHT    -> "right alt"
            else -> {
                val name = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").lowercase()
                Log.w(TAG, "Unknown keyCode $keyCode, using: $name")
                name
            }
        }
        return "key:0:$keyName"
    }
}
