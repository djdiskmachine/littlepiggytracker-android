package org.neocities.djdiskmachine.lgpt_android

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import org.libsdl.app.SDLActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Copy default config files from assets to external storage
        copyAssetToExternalStorage("mapping.xml")
        copyAssetToExternalStorage("config.xml")

        // Launch SDL activity
        val intent = Intent(this, SDLActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so back button won't return here
    }

    private fun copyAssetToExternalStorage(filename: String) {
        try {
            val externalDir = getExternalFilesDir(null) ?: run {
                Log.e(TAG, "External storage not available")
                return
            }
            
            val destFile = File(externalDir, filename)
            
            // Only copy if file doesn't exist
            if (!destFile.exists()) {
                Log.i(TAG, "Copying $filename from assets to ${destFile.absolutePath}")
                
                assets.open(filename).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.i(TAG, "Successfully copied $filename")
            } else {
                Log.i(TAG, "$filename already exists, skipping")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy $filename from assets", e)
        }
    }

    companion object {
        private const val TAG = "LgptMainActivity"
    }
}