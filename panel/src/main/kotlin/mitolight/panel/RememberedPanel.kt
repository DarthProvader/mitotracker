package mitolight.panel

/**
 * A panel that has been connected to before. On Android an [id] is the panel's Bluetooth address,
 * which is enough to reach it again without scanning at all — so this is not merely a convenience
 * list, it is what makes reconnecting immediate.
 */
data class RememberedPanel(
    val id: String,
    val name: String,
    val capability: PanelCapability,
    val lastConnectedAtMillis: Long,
)

/**
 * Where remembered panels are kept between runs.
 *
 * An interface because [PanelConnection] has to be testable without a filesystem, and because a
 * remembered panel is a setting rather than history: it is one short list that is read whole and
 * never aggregated, so it does not belong in the session database (ADR 0005, ADR 0007).
 */
interface PanelMemory {

    suspend fun remembered(): List<RememberedPanel>

    /** Replaces any earlier entry with the same id, so the name and time stay current. */
    suspend fun remember(panel: RememberedPanel)

    suspend fun forget(id: String)
}

/** Remembers nothing beyond the life of the process. Used by tests and by the simulator. */
class TransientPanelMemory : PanelMemory {

    private val panels = mutableMapOf<String, RememberedPanel>()

    override suspend fun remembered(): List<RememberedPanel> =
        panels.values.sortedByDescending(RememberedPanel::lastConnectedAtMillis)

    override suspend fun remember(panel: RememberedPanel) {
        panels[panel.id] = panel
    }

    override suspend fun forget(id: String) {
        panels.remove(id)
    }
}
