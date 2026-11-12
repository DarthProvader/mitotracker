package mitolight.app

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mitolight.domain.Area
import mitolight.domain.Preset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Presets made by the user.
 *
 * They are settings rather than history: the whole list is small, is read at once, and is never
 * queried or aggregated, so preferences are a better fit than the session database. JSON keeps the
 * format tolerant of fields being added later.
 */
class PresetPreferences(context: Context) {

    private val preferences = context.getSharedPreferences("presets", Context.MODE_PRIVATE)
    private val _custom = MutableStateFlow(read())
    val custom: StateFlow<List<Preset>> = _custom.asStateFlow()

    fun create(
        name: String,
        areas: List<Area>,
        brightnessByChannel: Map<Int, Int>,
        countdownMinutes: Int,
    ): Preset {
        val brightness = brightnessByChannel
            .toSortedMap()
            .mapValues { (_, percent) -> percent.coerceIn(0, 100) }
        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            areas = areas,
            brightnessByChannel = brightness,
            countdownMinutes = countdownMinutes.coerceIn(0, 60),
        )
        val next = _custom.value + preset
        write(next)
        _custom.value = next
        return preset
    }

    fun delete(id: String) {
        val next = _custom.value.filterNot { it.id == id }
        if (next.size == _custom.value.size) return
        write(next)
        _custom.value = next
    }

    private fun read(): List<Preset> {
        val stored = preferences.getString(KEY, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(stored)
            (0 until array.length()).mapNotNull { index ->
                val entry = array.getJSONObject(index)
                val name = entry.optString("name").trim()
                if (name.isEmpty()) return@mapNotNull null

                val areas = entry.optJSONArray("areas") ?: JSONArray()
                val brightness = entry.optJSONArray("brightness")
                    ?.takeIf { it.length() == 2 || it.length() == 6 }
                    ?.let { values ->
                        (0 until values.length()).associateWith { channel ->
                            values.optInt(channel).coerceIn(0, 100)
                        }
                    }
                    // Version one stored only the two group values. Keeping this read path is the
                    // migration: the next write records them in the new array without changing them.
                    ?: mapOf(
                        0 to entry.optInt("red").coerceIn(0, 100),
                        1 to entry.optInt("nearInfrared").coerceIn(0, 100),
                    )
                Preset(
                    id = entry.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = name,
                    areas = (0 until areas.length()).mapNotNull { areaIndex ->
                        runCatching { Area.valueOf(areas.getString(areaIndex)) }.getOrNull()
                    },
                    brightnessByChannel = brightness,
                    countdownMinutes = entry.optInt("countdownMinutes").coerceIn(0, 60),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun write(presets: List<Preset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            val areas = JSONArray()
            preset.areas.forEach { areas.put(it.name) }
            val brightness = JSONArray()
            preset.brightnessByChannel.toSortedMap().values.forEach { brightness.put(it) }
            val groups = preset.brightnessFor(channelCount = 2)

            array.put(
                JSONObject()
                    .put("id", preset.id)
                    .put("name", preset.name)
                    .put("areas", areas)
                    .put("brightness", brightness)
                    // Kept for a rollback to the previous app version.
                    .put("red", groups[0] ?: 0)
                    .put("nearInfrared", groups[1] ?: 0)
                    .put("countdownMinutes", preset.countdownMinutes),
            )
        }

        preferences.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "custom"
    }
}
