package org.neocities.djdiskmachine.lgpt_android

import android.content.Context
import android.util.Log
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class MappingManager(private val context: Context) {
    companion object { private const val TAG = "MappingManager" }

    fun loadMappings(): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        val file = getMappingFile()
        if (!file.exists()) return mappings

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(file)
            val nodes = doc.getElementsByTagName("MAP")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val elem = node as? org.w3c.dom.Element ?: continue
                val src = elem.getAttribute("src")
                val dst = elem.getAttribute("dst")
                val eventName = dst.substringAfterLast('/')
                mappings[eventName] = src
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load mappings", e)
        }

        return mappings
    }

    fun saveMappings(mappings: Map<String, String>) {
        val file = getMappingFile()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.newDocument()
            val root = doc.createElement("MAPPINGS")
            doc.appendChild(root)
            root.appendChild(doc.createComment("MAPPING FOR ANDROID KEYBOARD/CONTROLLER (auto-generated)"))

            mappings.forEach { (event, src) ->
                val map = doc.createElement("MAP")
                map.setAttribute("src", src)
                map.setAttribute("dst", "/event/$event")
                root.appendChild(map)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty("indent", "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(DOMSource(doc), StreamResult(file))
            Log.i(TAG, "Saved ${mappings.size} mappings to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save mappings", e)
        }
    }

    private fun getMappingFile(): File {
        val dir = context.getExternalFilesDir("LittlePiggyTracker") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "mapping.xml")
    }
}