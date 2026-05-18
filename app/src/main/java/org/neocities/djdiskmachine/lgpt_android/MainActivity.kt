package org.neocities.djdiskmachine.lgpt_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app folders and copy asset files
        val prefs = getSharedPreferences("lgpt_prefs", MODE_PRIVATE)
        val isFirstRun = !prefs.getBoolean("app_initialized", false)
    
        if (isFirstRun) {
            // Only on first launch
            initializeFiles()
            requestStoragePermissions()
            prefs.edit().putBoolean("app_initialized", true).apply()
        } else {
            // Subsequent launches - go straight to game
            copyPublicConfigIfExists()
            startLgptActivity()
        }
    }

    private fun initializeFiles() {
        try {
            // Copy config.xml and mapping.xml from assets to BOTH locations:
            // 1. App-specific folder (where game reads from)
            // 2. Public LittlePiggyTracker folder (where user can edit)
            
            val appSpecificFolder = getExternalFilesDir(null)
            val publicFolder = File("/storage/emulated/0/LittlePiggyTracker")
            
            // Ensure folders exist
            appSpecificFolder?.mkdirs()
            publicFolder.mkdirs()
            
            // Copy config.xml and mapping.xml to app-specific folder
            if (appSpecificFolder != null) {
                copyAssetToFolder("config.xml", appSpecificFolder)
                copyAssetToFolder("mapping.xml", appSpecificFolder)
            }
            
            // Copy config.xml and mapping.xml to public folder
            copyAssetToFolder("config.xml", publicFolder)
            copyAssetToFolder("mapping.xml", publicFolder)
            
            Log.i(TAG, "Files initialized successfully")
            Log.i(TAG, "App-specific folder: ${appSpecificFolder?.absolutePath}")
            Log.i(TAG, "Public folder: ${publicFolder.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize files", e)
        }
    }

    private fun copyAssetToFolder(filename: String, folder: File) {
        try {
            if (!folder.exists()) folder.mkdirs()
            
            val destFile = File(folder, filename)
            
            // Only copy if file doesn't exist (preserves user edits to config.xml)
            if (!destFile.exists()) {
                Log.i(TAG, "Copying $filename to ${folder.absolutePath}")
                assets.open(filename).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Successfully copied $filename to ${destFile.absolutePath}")
            } else {
                Log.d(TAG, "$filename already exists in ${folder.absolutePath}, skipping")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy $filename to ${folder.absolutePath}", e)
        }
    }

    private fun applyScreenRotationPreference() {
        val prefs = getSharedPreferences("lgpt_prefs", MODE_PRIVATE)
        val rotationMode = prefs.getString("screen_rotation_mode", "auto") ?: "auto"
        
        requestedOrientation = when (rotationMode) {
            "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
        }
        
        Log.i(TAG, "Applied screen rotation preference: $rotationMode")
    }

    private fun requestStoragePermissions() {
        // Apply saved screen rotation preference
        applyScreenRotationPreference()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: MANAGE_EXTERNAL_STORAGE requires sending user to Settings
            if (!Environment.isExternalStorageManager()) {
                Log.i(TAG, "Requesting MANAGE_EXTERNAL_STORAGE via Settings")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, STORAGE_PERMISSION_CODE)
                } catch (e: Exception) {
                    // Some devices don't support the direct package URI, fall back to general settings
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, STORAGE_PERMISSION_CODE)
                }
            } else {
                Log.i(TAG, "MANAGE_EXTERNAL_STORAGE already granted")
                createPublicFolder()
                startLgptActivity()
            }
        } else {
            // Android 10 and below: use regular runtime permissions
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needsRequest = permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsRequest) {
                Log.i(TAG, "Requesting storage permissions")
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
            } else {
                Log.i(TAG, "Storage permissions already granted")
                createPublicFolder()
                startLgptActivity()
            }
        }
    }

    private fun copyPublicConfigIfExists(): Boolean {
        try {
            val publicConfigFile = File(Environment.getExternalStorageDirectory(), "LittlePiggyTracker/config.xml")
            if (publicConfigFile.exists()) {
                val appSpecificDir = getExternalFilesDir(null) ?: run {
                    Log.w(TAG, "Cannot access app-specific dir to copy public config")
                    return false
                }
                val appSpecificConfigFile = File(appSpecificDir, "config.xml")
                Log.i(TAG, "Public config.xml found, copying to app-specific folder: ${appSpecificConfigFile.absolutePath}")
                publicConfigFile.inputStream().use { input ->
                    FileOutputStream(appSpecificConfigFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Successfully copied public config.xml to app-specific folder")
                return true
            } else {
                Log.i(TAG, "No public config.xml found, will use default from assets")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check/copy public config.xml", e)
            return false
        }
    }

    private fun createPublicFolder() {
        try {
            val publicFolder = File(Environment.getExternalStorageDirectory(), "LittlePiggyTracker")
            if (!publicFolder.exists()) {
                val created = publicFolder.mkdirs()
                Log.i(TAG, "Public LittlePiggyTracker folder: created=$created, path=${publicFolder.absolutePath}")
                if (created) {
                    android.widget.Toast.makeText(
                        this,
                        "Created folder ${publicFolder.absolutePath}\nPlace your lgpt_Projects and samplelib in there",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.i(TAG, "Public LittlePiggyTracker folder already exists: ${publicFolder.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create public LittlePiggyTracker folder", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                Log.i(TAG, "MANAGE_EXTERNAL_STORAGE granted")
                createPublicFolder()
            } else {
                Log.w(TAG, "MANAGE_EXTERNAL_STORAGE denied - using app-specific folder only")
            }
            startLgptActivity()
        }
    }

    private fun startLgptActivity() {
        val intent = Intent(this, LgptSDLActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.i(TAG, "Storage permissions granted")
                createPublicFolder()
            } else {
                Log.w(TAG, "Storage permissions denied - using app-specific folder only")
            }
            startLgptActivity()
        }
    }

    companion object {
        private const val TAG = "PiggyMainActivity"
        private const val STORAGE_PERMISSION_CODE = 100
    }

}