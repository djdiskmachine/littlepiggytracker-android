package org.neocities.djdiskmachine.lgpt_android

import android.content.Context
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ConfigManager(private val context: Context) {
    companion object {
        private const val TAG = "ConfigManager"
        private const val LPT_FOLDER = "LittlePiggyTracker"

        // Map LGPT event names to XML element names
        private val EVENT_TO_XML_TAG = mapOf(
            "up" to "KEY_UP",
            "down" to "KEY_DOWN",
            "left" to "KEY_LEFT",
            "right" to "KEY_RIGHT",
            "a" to "KEY_A",
            "b" to "KEY_B",
            "start" to "KEY_START",
            "select" to "KEY_SELECT",
            "lshoulder" to "KEY_LSHOULDER",
            "rshoulder" to "KEY_RSHOULDER"
        )
    }

    fun loadKeyBindings(): Map<String, String> {
        return try {
            val configFile = getPublicConfigFile()
            if (!configFile.exists()) {
                Log.w(TAG, "config.xml not found: ${configFile.absolutePath}")
                return emptyMap()
            }

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configFile)
            val bindings = mutableMapOf<String, String>()

            for ((event, xmlTag) in EVENT_TO_XML_TAG) {
                val elements = doc.getElementsByTagName(xmlTag)
                if (elements.length > 0) {
                    val value = (elements.item(0) as Element).getAttribute("value")
                    bindings[event] = value
                    Log.d(TAG, "Loaded: $xmlTag=$value")
                }
            }

            Log.i(TAG, "Loaded ${bindings.size} key bindings from ${configFile.absolutePath}")
            bindings
        } catch (e: Exception) {
            Log.e(TAG, "Error loading config.xml", e)
            emptyMap()
        }
    }

    fun saveKeyBindings(bindings: Map<String, String>) {
        try {
            val publicConfigFile = getPublicConfigFile()
            if (!publicConfigFile.exists()) {
                Log.e(TAG, "config.xml does not exist in LittlePiggyTracker folder, cannot save bindings")
                return
            }

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(publicConfigFile)

            // Update or create KEY_* elements
            for ((event, xmlTag) in EVENT_TO_XML_TAG) {
                val newValue = bindings[event] ?: continue

                var elements = doc.getElementsByTagName(xmlTag)
                if (elements.length > 0) {
                    // Update existing element
                    (elements.item(0) as Element).setAttribute("value", newValue)
                } else {
                    // Create new element
                    val rootElement = doc.documentElement
                    val newElement = doc.createElement(xmlTag)
                    newElement.setAttribute("value", newValue)
                    rootElement.appendChild(newElement)
                }

                Log.d(TAG, "Updated: $xmlTag=$newValue")
            }

            // Write back to public LittlePiggyTracker folder (user-editable)
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty("indent", "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")

            val source = DOMSource(doc)
            val result = StreamResult(publicConfigFile)
            transformer.transform(source, result)

            Log.i(TAG, "Saved key bindings to ${publicConfigFile.absolutePath}")

            // Copy updated config.xml from LittlePiggyTracker to app-specific folder
            copyToAppSpecificFolder(publicConfigFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving key bindings", e)
        }
    }

    private fun copyToAppSpecificFolder(sourceFile: File) {
        try {
            val destFile = getAppSpecificConfigFile()
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            Log.i(TAG, "Copied config.xml from LittlePiggyTracker to app-specific folder: ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying config.xml to app-specific folder", e)
        }
    }

    private fun getPublicConfigFile(): File {
        return File("/storage/emulated/0/LittlePiggyTracker/config.xml")
    }

    private fun getAppSpecificConfigFile(): File {
        return File(context.getExternalFilesDir(null), "config.xml")
    }
}
