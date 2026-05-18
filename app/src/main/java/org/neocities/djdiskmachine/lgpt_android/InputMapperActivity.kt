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
        val keyName = convertKeyCodeToString(keyCode)
        detectedEventText.text = "Detected: $keyName"
        Log.i(TAG, "Mapped $lgptEvent -> key:0:$keyName")
        mappedEvents[lgptEvent] = "key:0:$keyName"
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
        
        // Launch the game with the new config
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
            Log.i(TAG, "Starting game with current mappings")
            val intent = Intent(this, LgptSDLActivity::class.java)
            startActivity(intent)
            finish()
        }
        resetButton.text = "Reconfigure"
        resetButton.setOnClickListener { resetAllMappings() }
        saveButton.visibility = android.view.View.GONE
    }

    private fun convertKeyCodeToString(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> "a"
            KeyEvent.KEYCODE_B -> "b"
            KeyEvent.KEYCODE_C -> "c"
            KeyEvent.KEYCODE_D -> "d"
            KeyEvent.KEYCODE_E -> "e"
            KeyEvent.KEYCODE_F -> "f"
            KeyEvent.KEYCODE_G -> "g"
            KeyEvent.KEYCODE_H -> "h"
            KeyEvent.KEYCODE_I -> "i"
            KeyEvent.KEYCODE_J -> "j"
            KeyEvent.KEYCODE_K -> "k"
            KeyEvent.KEYCODE_L -> "l"
            KeyEvent.KEYCODE_M -> "m"
            KeyEvent.KEYCODE_N -> "n"
            KeyEvent.KEYCODE_O -> "o"
            KeyEvent.KEYCODE_P -> "p"
            KeyEvent.KEYCODE_Q -> "q"
            KeyEvent.KEYCODE_R -> "r"
            KeyEvent.KEYCODE_S -> "s"
            KeyEvent.KEYCODE_T -> "t"
            KeyEvent.KEYCODE_U -> "u"
            KeyEvent.KEYCODE_V -> "v"
            KeyEvent.KEYCODE_W -> "w"
            KeyEvent.KEYCODE_X -> "x"
            KeyEvent.KEYCODE_Y -> "y"
            KeyEvent.KEYCODE_Z -> "z"
            KeyEvent.KEYCODE_0 -> "0"
            KeyEvent.KEYCODE_1 -> "1"
            KeyEvent.KEYCODE_2 -> "2"
            KeyEvent.KEYCODE_3 -> "3"
            KeyEvent.KEYCODE_4 -> "4"
            KeyEvent.KEYCODE_5 -> "5"
            KeyEvent.KEYCODE_6 -> "6"
            KeyEvent.KEYCODE_7 -> "7"
            KeyEvent.KEYCODE_8 -> "8"
            KeyEvent.KEYCODE_9 -> "9"
            KeyEvent.KEYCODE_SPACE -> "space"
            KeyEvent.KEYCODE_ENTER -> "return"
            KeyEvent.KEYCODE_DPAD_UP -> "up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "right"
            KeyEvent.KEYCODE_BACK -> "back"
            KeyEvent.KEYCODE_DEL -> "backspace"
            KeyEvent.KEYCODE_ESCAPE -> "escape"
            KeyEvent.KEYCODE_TAB -> "tab"
            KeyEvent.KEYCODE_MINUS -> "[-]"
            KeyEvent.KEYCODE_EQUALS -> "="
            KeyEvent.KEYCODE_LEFT_BRACKET -> "["
            KeyEvent.KEYCODE_RIGHT_BRACKET -> "]"
            KeyEvent.KEYCODE_SEMICOLON -> ";"
            KeyEvent.KEYCODE_APOSTROPHE -> "'"
            KeyEvent.KEYCODE_SLASH -> "/"
            KeyEvent.KEYCODE_BACKSLASH -> "\\"
            KeyEvent.KEYCODE_COMMA -> ","
            KeyEvent.KEYCODE_PERIOD -> "."
            KeyEvent.KEYCODE_GRAVE -> "`"
            KeyEvent.KEYCODE_SHIFT_LEFT -> "left shift"
            KeyEvent.KEYCODE_SHIFT_RIGHT -> "right shift"
            KeyEvent.KEYCODE_CTRL_LEFT -> "left ctrl"
            KeyEvent.KEYCODE_CTRL_RIGHT -> "right ctrl"
            KeyEvent.KEYCODE_ALT_LEFT -> "left alt"
            KeyEvent.KEYCODE_ALT_RIGHT -> "right alt"
            else -> {
                val name = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").lowercase()
                Log.w(TAG, "Unknown keyCode $keyCode, using: $name")
                name
            }
        }
    }
}
