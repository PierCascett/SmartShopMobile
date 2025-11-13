package it.unito.smartshopmobile.ui.map

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Load polygon definitions from a JSON file placed in assets (e.g. assets/map/supermarket.json).
 * Supports two shapes:
 * - Root JSON array of arrays of {x,y} points: [ [ {x,y}, ... ], [ ... ] ]
 * - Root JSON array of objects with "content" arrays like the original export:
 *   [ { "content": [ {x,y}, ... ], "labels": {"labelName":"..."}, ... }, ... ]
 */
@Composable
fun rememberPolygonsFromJson(assetPath: String): List<ShelfPolygon> {
    val ctx = LocalContext.current
    return remember(assetPath) {
        runCatching {
            val text = ctx.assets.open(assetPath).bufferedReader().use { it.readText() }
            parsePolygonsJson(text)
        }.onFailure { Log.w("MapJsonLoader", "Failed to load polygons from $assetPath: ${it.message}") }
            .getOrDefault(emptyList())
    }
}

fun parsePolygonsJson(text: String): List<ShelfPolygon> {
    val out = mutableListOf<ShelfPolygon>()
    val trimmed = text.trim()
    try {
        if (trimmed.startsWith("[")) {
            val root = JSONArray(trimmed)
            for (i in 0 until root.length()) {
                val elem = root.get(i)
                when (elem) {
                    is JSONArray -> {
                        val pts = parsePointsArray(elem)
                        out.add(ShelfPolygon(id = "S${out.size + 1}", points = pts, label = "${out.size + 1}"))
                    }
                    is JSONObject -> {
                        // object may have 'content' array
                        val content = if (elem.has("content")) elem.get("content") else null
                        if (content is JSONArray) {
                            val pts = parsePointsArray(content)
                            val id = if (elem.has("id")) elem.optString("id") else "S${out.size + 1}"
                            var label: String? = null
                            if (elem.has("labels")) {
                                val labels = elem.get("labels")
                                if (labels is JSONObject) label = labels.optString("labelName", null)
                            }
                            if (label == null) label = id
                            out.add(ShelfPolygon(id = id, points = pts, label = label))
                        } else {
                            // Possible different structure: find any array-valued field
                            val keys = elem.keys()
                            var found = false
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val v = elem.get(k)
                                if (v is JSONArray) {
                                    // if array of objects with x/y
                                    val pts = parsePointsArray(v)
                                    if (pts.isNotEmpty()) {
                                        out.add(ShelfPolygon(id = "S${out.size + 1}", points = pts, label = "${out.size + 1}"))
                                        found = true
                                        break
                                    }
                                }
                            }
                            if (!found) {
                                // skip
                            }
                        }
                    }
                }
            }
        } else if (trimmed.startsWith("{")) {
            // try to find arrays inside object
            val rootObj = JSONObject(trimmed)
            // try common keys
            val candidates = listOf("polygons", "items", "shapes", "features")
            var parsed = false
            for (k in candidates) {
                if (rootObj.has(k)) {
                    val arr = rootObj.get(k)
                    if (arr is JSONArray) {
                        val res = parsePolygonsJson(arr.toString())
                        out.addAll(res)
                        parsed = true
                        break
                    }
                }
            }
            if (!parsed) {
                // try any array property
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = rootObj.get(k)
                    if (v is JSONArray) {
                        val res = parsePolygonsJson(v.toString())
                        out.addAll(res)
                        break
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.w("MapJsonLoader", "Error parsing polygon JSON: ${e.message}")
    }
    return out
}

private fun parsePointsArray(arr: JSONArray): List<Offset> {
    val pts = mutableListOf<Offset>()
    for (j in 0 until arr.length()) {
        val item = arr.get(j)
        if (item is JSONObject) {
            val x = item.optDouble("x", Double.NaN)
            val y = item.optDouble("y", Double.NaN)
            if (!x.isNaN() && !y.isNaN()) {
                pts.add(Offset(x.toFloat(), y.toFloat()))
            }
        }
    }
    return pts
}
