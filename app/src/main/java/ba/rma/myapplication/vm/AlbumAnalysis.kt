package ba.rma.myapplication.vm

/**
 * The result of analyzing the user's album. These are plain data holders produced by
 * [SuggestionEngine] and read by the ViewModel. They contain no logic.
 */

/**
 * Progress for one "group" of the album. We use the same shape for two kinds of grouping:
 *  - by national team (e.g. "Argentina": 5/7 collected), and
 *  - by position (e.g. "FORWARD": 8/20 collected).
 *
 * @param name                the team name or position name.
 * @param owned               how many distinct players from this group the user owns.
 * @param total               how many players are in this group in total.
 * @param percent             owned / total as a 0..100 percentage (rounded).
 * @param missingPlayerNames  the full names of the players from this group the user still needs.
 */
data class GroupProgress(
    val name: String,
    val owned: Int,
    val total: Int,
    val percent: Int,
    val missingPlayerNames: List<String>
)

/**
 * A player the user owns more than once.
 * @param spareCopies how many EXTRA copies there are (total copies minus the one they keep).
 */
data class DuplicateInfo(
    val playerId: Int,
    val name: String,
    val spareCopies: Int
)

/**
 * The full picture of the album, everything the Smart Suggestions feature needs.
 *
 * @param ownedUnique          number of distinct players owned.
 * @param totalPlayers         total players that exist (109).
 * @param completionPercent    ownedUnique / totalPlayers as a percentage.
 * @param perTeam              progress per national team, best target first.
 * @param perPosition          progress per position.
 * @param duplicates           players owned more than once, most spares first.
 * @param goldenOwned          how many golden/rare stickers the user owns.
 * @param newCardChancePercent estimated chance the NEXT pulled card is new to the user.
 * @param suggestions          ready-to-show advice lines (already worded for the user).
 */
data class AlbumAnalysis(
    val ownedUnique: Int,
    val totalPlayers: Int,
    val completionPercent: Int,
    val perTeam: List<GroupProgress>,
    val perPosition: List<GroupProgress>,
    val duplicates: List<DuplicateInfo>,
    val goldenOwned: Int,
    val newCardChancePercent: Int,
    val suggestions: List<String>
)
