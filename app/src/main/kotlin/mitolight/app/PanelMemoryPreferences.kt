package mitolight.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mitolight.panel.PanelCapability
import mitolight.panel.PanelMemory
import mitolight.panel.RememberedPanel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Remembered panels kept in the app's preferences, as JSON so that adding a field later does not
 * mean parsing a home-made format. Preferences rather than the session database because this is a
 * three-line list of settings, read whole and never aggregated (ADR 0007).
 */
class PanelMemoryPreferences(context: Context) : PanelMemory {

    private val preferences = context.getSharedPreferences("panels", Context.MODE_PRIVATE)

    override suspend fun remembered(): List<RememberedPanel> = withContext(Dispatchers.IO) { read() }

    override suspend fun remember(panel: RememberedPanel) = withContext(Dispatchers.IO) {
        write(read().filterNot { it.id == panel.id } + panel)
    }

    override suspend fun forget(id: String) = withContext(Dispatchers.IO) {
        write(read().filterNot { it.id == id })
    }

    private fun read(): List<RememberedPanel> {
        val stored = preferences.getString(KEY, null) ?: return emptyList()

        val panels = runCatching {
            val array = JSONArray(stored)
            (0 until array.length()).map { index ->
                val entry = array.getJSONObject(index)
                RememberedPanel(
                    id = entry.getString("id"),
                    name = entry.getString("name"),
                    capability = runCatching { PanelCapability.valueOf(entry.getString("capability")) }
                        .getOrDefault(PanelCapability.TWO_CHANNEL),
                    lastConnectedAtMillis = entry.getLong("lastConnectedAt"),
                )
            }
        }

        return panels.getOrDefault(emptyList()).sortedByDescending(RememberedPanel::lastConnectedAtMillis)
    }

    private fun write(panels: List<RememberedPanel>) {
        val array = JSONArray()
        panels.forEach { panel ->
            array.put(
                JSONObject()
                    .put("id", panel.id)
                    .put("name", panel.name)
                    .put("capability", panel.capability.name)
                    .put("lastConnectedAt", panel.lastConnectedAtMillis),
            )
        }

        preferences.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "remembered"
    }
}
