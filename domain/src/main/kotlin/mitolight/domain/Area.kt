package mitolight.domain

/**
 * A part of the body light is aimed at, and the unit a **Limit** applies to.
 *
 * The list is fixed and the areas do not overlap, which is the whole reason minutes can be added
 * up: if "back" and "lower back" both existed, one exposure would count twice and every dose would
 * be a guess. A fixed enum also means an [Area] survives being written to history and read back
 * years later, which a user-editable list would not.
 */
enum class Area(val label: String) {
    SCALP("Scalp"),
    FACE_AND_NECK("Face & neck"),
    SHOULDERS("Shoulders"),
    CHEST("Chest"),
    ABDOMEN("Abdomen"),
    UPPER_BACK("Upper back"),
    LOWER_BACK("Lower back"),
    ARMS("Arms"),
    HIPS("Hips"),
    THIGHS("Thighs"),
    KNEES("Knees"),
    FEET("Feet"),
    ;

    val isFront: Boolean
        get() = this in setOf(FACE_AND_NECK, CHEST, ABDOMEN, THIGHS, KNEES)
}
