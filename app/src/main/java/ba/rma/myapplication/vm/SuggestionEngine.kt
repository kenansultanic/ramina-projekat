package ba.rma.myapplication.vm

import ba.rma.myapplication.data.OwnedSticker
import ba.rma.myapplication.data.Player

/**
 * FEATURE A — SMART SUGGESTIONS (the local algorithm).
 *
 * This object looks at (1) the full catalog of players and (2) what the user currently owns, and
 * works out useful facts: how complete the album is, which team is closest to being finished, what
 * duplicates can be traded, how rare the collection is, and how likely the next card is to be new.
 * From those facts it writes short pieces of advice for the user.
 *
 * It is PURE: it only does math on the data it is given. It does no networking, no database access,
 * and knows nothing about Android or the UI. That makes it easy to read, reason about, and test.
 *
 * The code uses simple for-loops and small helper functions on purpose, so each step of the
 * analysis is easy to follow and explain.
 */
object SuggestionEngine {

    // When listing missing players we don't want to print 50 names; show at most this many.
    private const val MAX_NAMES_TO_LIST = 3

    /**
     * The one public entry point. Turns raw data into a full [AlbumAnalysis].
     *
     * @param catalog all players that exist (from /api/all-players).
     * @param owned   the user's collection rows (player id, how many copies, golden flag).
     */
    fun analyze(catalog: List<Player>, owned: List<OwnedSticker>): AlbumAnalysis {
        // --- Step 1: build quick lookup tables from the owned list. ---
        // countById:  playerId -> how many copies the user has.
        // goldenById: playerId -> is that sticker golden/rare.
        // Looking things up in a map is much faster than scanning the owned list again and again.
        val countById = HashMap<Int, Int>()
        val goldenById = HashMap<Int, Boolean>()
        for (sticker in owned) {
            countById[sticker.playerId] = sticker.count
            goldenById[sticker.playerId] = sticker.isGolden
        }

        val totalPlayers = catalog.size

        // --- Step 2: count distinct owned players and golden ones. ---
        var ownedUnique = 0
        var goldenOwned = 0
        for (player in catalog) {
            val copies = countById[player.id]
            if (copies != null && copies > 0) {
                ownedUnique = ownedUnique + 1
                if (goldenById[player.id] == true) {
                    goldenOwned = goldenOwned + 1
                }
            }
        }
        val completionPercent = percentOf(ownedUnique, totalPlayers)

        // --- Step 3: progress grouped by team and by position. ---
        val perTeam = buildGroups(catalog, countById, useTeam = true)
        val perPosition = buildGroups(catalog, countById, useTeam = false)

        // --- Step 4: find duplicates (players owned more than once). ---
        val duplicates = buildDuplicates(catalog, countById)

        // --- Step 5: dynamic probability that the next pulled card is NEW. ---
        // Every player is equally likely to come out of a pack, so the chance a random card is one
        // we don't have yet is simply (players we still miss) / (all players).
        val missingUnique = totalPlayers - ownedUnique
        val newCardChancePercent = percentOf(missingUnique, totalPlayers)

        // --- Step 6: turn all the numbers into human-readable advice. ---
        val suggestions = buildSuggestions(
            ownedUnique,
            totalPlayers,
            completionPercent,
            perTeam,
            duplicates,
            goldenOwned,
            newCardChancePercent
        )

        return AlbumAnalysis(
            ownedUnique = ownedUnique,
            totalPlayers = totalPlayers,
            completionPercent = completionPercent,
            perTeam = perTeam,
            perPosition = perPosition,
            duplicates = duplicates,
            goldenOwned = goldenOwned,
            newCardChancePercent = newCardChancePercent,
            suggestions = suggestions
        )
    }

    /**
     * Returns part/whole as a whole-number percentage (0..100), rounded to the nearest integer.
     * Guards against dividing by zero: if there is nothing to measure, the percentage is 0.
     */
    private fun percentOf(part: Int, whole: Int): Int {
        if (whole <= 0) {
            return 0
        }
        return Math.round(part * 100.0f / whole)
    }

    /**
     * Builds a [GroupProgress] for every group. If [useTeam] is true we group by national team,
     * otherwise we group by position.
     */
    private fun buildGroups(
        catalog: List<Player>,
        countById: Map<Int, Int>,
        useTeam: Boolean
    ): List<GroupProgress> {
        // First, gather the players that belong to each group key. We use a LinkedHashMap so the
        // groups keep the order they first appear in the catalog (stable, predictable output).
        val membersByGroup = LinkedHashMap<String, MutableList<Player>>()
        for (player in catalog) {
            val key = if (useTeam) player.reprezentacija else player.pozicija
            val list = membersByGroup[key]
            if (list == null) {
                val newList = ArrayList<Player>()
                newList.add(player)
                membersByGroup[key] = newList
            } else {
                list.add(player)
            }
        }

        // Now turn each group into a GroupProgress: count owned, percent, and list missing names.
        val result = ArrayList<GroupProgress>()
        for (entry in membersByGroup) {
            val name = entry.key
            val members = entry.value
            val total = members.size
            var owned = 0
            val missingNames = ArrayList<String>()
            for (player in members) {
                val copies = countById[player.id]
                if (copies != null && copies > 0) {
                    owned = owned + 1
                } else {
                    missingNames.add(fullName(player))
                }
            }
            val percent = percentOf(owned, total)
            result.add(GroupProgress(name, owned, total, percent, missingNames))
        }

        // Sort so the most useful group is first. We rank groups in three tiers:
        //   tier 0 = started but not finished  (the BEST place to focus next)
        //   tier 1 = already complete          (nothing left to do)
        //   tier 2 = not started at all        (a big investment)
        // Within the same tier, the one with the higher completion percentage comes first.
        result.sortWith(Comparator { a, b ->
            val tierA = targetTier(a)
            val tierB = targetTier(b)
            if (tierA != tierB) {
                tierA - tierB              // lower tier number shown first
            } else {
                b.percent - a.percent      // same tier: higher percent first
            }
        })
        return result
    }

    /** See the sorting comment above: 0 = in progress, 1 = complete, 2 = not started. */
    private fun targetTier(group: GroupProgress): Int {
        if (group.owned == 0) {
            return 2
        }
        if (group.owned >= group.total) {
            return 1
        }
        return 0
    }

    /** Builds the list of duplicates, the player with the most spare copies first. */
    private fun buildDuplicates(
        catalog: List<Player>,
        countById: Map<Int, Int>
    ): List<DuplicateInfo> {
        val result = ArrayList<DuplicateInfo>()
        for (player in catalog) {
            val copies = countById[player.id]
            if (copies != null && copies > 1) {
                val spare = copies - 1   // keep one for the album, the rest are spares
                result.add(DuplicateInfo(player.id, fullName(player), spare))
            }
        }
        // Most spares first -> these are the cards the user can most freely trade away.
        result.sortWith(Comparator { a, b -> b.spareCopies - a.spareCopies })
        return result
    }

    /**
     * Writes the advice lines, in plain language, based on everything we computed.
     * The two short-circuits at the top handle the empty and full collection cases cleanly.
     */
    private fun buildSuggestions(
        ownedUnique: Int,
        totalPlayers: Int,
        completionPercent: Int,
        perTeam: List<GroupProgress>,
        duplicates: List<DuplicateInfo>,
        goldenOwned: Int,
        newCardChancePercent: Int
    ): List<String> {
        val tips = ArrayList<String>()

        // Edge case: nothing collected yet.
        if (ownedUnique == 0) {
            tips.add("Tvoja kolekcija je prazna. Protresi telefon da otvoris prvi paket!")
            return tips
        }

        // Edge case: everything collected.
        if (ownedUnique >= totalPlayers) {
            tips.add("Cestitamo! Sakupio si svih " + totalPlayers + " slicica. Album je kompletan!")
            return tips
        }

        // Overall progress.
        tips.add("Sakupio si " + ownedUnique + " od " + totalPlayers + " slicica (" + completionPercent + "%).")

        // Closest team to finishing. perTeam is sorted so the first "in progress" team is the best
        // target; we still scan for it to be safe (e.g. if only complete/empty teams exist).
        val target = firstInProgressTeam(perTeam)
        if (target != null) {
            val missingCount = target.total - target.owned
            tips.add(
                "Najblize si zavrsetku tima " + target.name + " (" + target.owned + "/" + target.total +
                    "). Fali ti jos " + missingCount + ": " + joinNames(target.missingPlayerNames) + "."
            )
        }

        // Duplicates advice.
        if (duplicates.isNotEmpty()) {
            var spareTotal = 0
            for (dup in duplicates) {
                spareTotal = spareTotal + dup.spareCopies
            }
            val mostDuplicated = duplicates[0]
            tips.add(
                "Imas " + spareTotal + " viskova koje mozes mijenjati. Najvise imas: " +
                    mostDuplicated.name + " (" + mostDuplicated.spareCopies + " viska)."
            )
        }

        // Rare/golden advice.
        if (goldenOwned > 0) {
            tips.add("Posjedujes " + goldenOwned + " zlatnih (rijetkih) slicica - cuvaj ih, vrijede vise pri mijenjanju!")
        }

        // Dynamic probability + what to do about it. The advice flips as the chance drops:
        // high  -> packs are still rewarding; medium -> mix opening with trading; low -> trade.
        tips.add("Sansa da sljedeca izvucena slicica bude nova: oko " + newCardChancePercent + "%.")
        if (newCardChancePercent >= 50) {
            tips.add("Vrijedi nastaviti otvarati pakete - vecina slicica ti je jos nova.")
        } else if (newCardChancePercent >= 20) {
            tips.add("Sve cesce dobijas duplikate. Otvaraj pakete, ali pocni i mijenjati viskove.")
        } else {
            tips.add("Uglavnom dobijas duplikate. Sada se najvise isplati mijenjati viskove za slicice koje ti fale.")
        }

        return tips
    }

    /** Returns the first team that is started but not yet finished, or null if none. */
    private fun firstInProgressTeam(perTeam: List<GroupProgress>): GroupProgress? {
        for (group in perTeam) {
            if (group.owned > 0 && group.owned < group.total) {
                return group
            }
        }
        return null
    }

    /** "Lionel" + "Messi" -> "Lionel Messi". */
    private fun fullName(player: Player): String {
        return player.ime + " " + player.prezime
    }

    /**
     * Joins up to [MAX_NAMES_TO_LIST] names with commas. If there are more, it adds
     * "i jos N" ("and N more") so the line stays short.
     */
    private fun joinNames(names: List<String>): String {
        if (names.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        var i = 0
        while (i < names.size && i < MAX_NAMES_TO_LIST) {
            if (i > 0) {
                builder.append(", ")
            }
            builder.append(names[i])
            i = i + 1
        }
        if (names.size > MAX_NAMES_TO_LIST) {
            builder.append(" i jos ").append(names.size - MAX_NAMES_TO_LIST)
        }
        return builder.toString()
    }
}
