package ba.rma.myapplication.ui

/**
 * Small, UI-friendly data holders. The ViewModels convert the raw data/analysis types into these
 * so the screens stay simple: a screen just reads the fields and draws them, with no logic.
 */

/**
 * Everything one sticker card needs to draw itself.
 * [isFavorite] and [ownedCount] have defaults so older call sites (the pack reveal, which always
 * shows a freshly pulled, owned card) keep working without passing them.
 */
data class CardUi(
    val playerId: Int,
    val firstName: String,
    val lastName: String,
    val jerseyNumber: Int,
    val team: String,
    val position: String,
    val imagePath: String,   // the "/api/slicica/.." path; StickerCard builds the full URL
    val isGolden: Boolean,   // golden/rare stickers get a special highlight
    val isFavorite: Boolean = false,
    val ownedCount: Int = 1  // 0 = missing (drawn greyed out); >1 = show an "xN" badge
)

/** One progress row on the Suggestions screen (used for both teams and positions). */
data class GroupProgressUi(
    val name: String,
    val owned: Int,
    val total: Int
)

/** One duplicate entry (how many copies the user owns of a given player). */
data class DuplicateUi(
    val playerName: String,
    val count: Int
)

/** Everything the Smart Suggestions screen needs to draw itself. */
data class SuggestionsUi(
    val ownedDistinct: Int,
    val totalPlayers: Int,
    val teamProgress: List<GroupProgressUi>,
    val positionProgress: List<GroupProgressUi>,
    val worthItProbabilityPercent: Int,    // chance the next pulled card is new
    val duplicates: List<DuplicateUi>,
    val adviceLines: List<String>          // the ready-to-show suggestion strings
)

/** One bar in the per-team chart on the Stats screen. */
data class TeamStatUi(
    val team: String,
    val owned: Int,
    val total: Int
)

/** Everything the Stats (data visualization) screen needs. */
data class StatsUi(
    val ownedDistinct: Int,
    val totalPlayers: Int,
    val missing: Int,
    val goldenCount: Int,
    val totalDuplicates: Int,
    val teamStats: List<TeamStatUi>
)
