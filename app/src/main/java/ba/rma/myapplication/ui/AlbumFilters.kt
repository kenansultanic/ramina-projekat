package ba.rma.myapplication.ui

import ba.rma.myapplication.data.SortOrder

/**
 * Small filter/sort helpers shared by the Album screen and its ViewModel.
 */

/** A special value meaning "do not filter by team" (shown in the team dropdown). */
const val ALL_TEAMS = "Sve reprezentacije"

/**
 * Which stickers to show in the album. [label] is the Bosnian text shown on the filter chip.
 */
enum class StatusFilter(val label: String) {
    ALL("Sve"),
    OWNED("Skupljene"),
    MISSING("Nedostaju"),
    FAVORITES("Favoriti")
}

/** Bosnian label for each sort order (shown in the sort dropdown). */
fun sortLabel(order: SortOrder): String {
    return when (order) {
        SortOrder.NUMBER -> "Po broju dresa"
        SortOrder.NAME -> "Po imenu"
        SortOrder.ACQUIRED -> "Po datumu dobivanja"
    }
}
