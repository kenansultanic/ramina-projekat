package ba.rma.myapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.AlbumCard
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.data.SettingsManager
import ba.rma.myapplication.data.SortOrder
import ba.rma.myapplication.ui.ALL_TEAMS
import ba.rma.myapplication.ui.StatusFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the Album screen needs to draw itself in one immutable snapshot. The screen just reads
 * these fields; all the filtering/sorting logic lives here in the ViewModel.
 */
data class AlbumUiState(
    val cards: List<AlbumCard> = emptyList(),       // already filtered + sorted, ready to draw
    val teams: List<String> = listOf(ALL_TEAMS),    // dropdown options (ALL_TEAMS + every team)
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val teamFilter: String = ALL_TEAMS,
    val sortOrder: SortOrder = SortOrder.NUMBER,
    val isRefreshing: Boolean = false,              // true while pull-to-refresh is running
    val errorMessage: String? = null,
    val isEmpty: Boolean = false                    // true when the filtered list is empty
)

/**
 * The Album list ViewModel. This is the most complex screen: it has a search box, status filter
 * chips, a team dropdown and a sort dropdown, and it must keep all of those in sync with the live
 * album data coming from the repository.
 *
 * The filter/sort choices are kept in [SavedStateHandle] so they SURVIVE process death and config
 * changes (rotation). Enums are stored by their .name (a String) — never the enum object — and read
 * back with a safe fallback, exactly as the blueprint requires.
 *
 * We use [SavedStateHandle.getStateFlow] to turn each saved value into a Flow, then `combine` those
 * four filter Flows with the repository's album Flow. Whenever ANY of them changes, a fresh
 * [AlbumUiState] is produced automatically.
 *
 * AndroidViewModel + SavedStateHandle: the default `viewModel()` factory injects BOTH parameters,
 * so no custom factory is needed.
 */
class AlbumViewModel(
    app: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(app) {

    // The shared single source of truth.
    private val repo = CardRepository.get(app)

    // SavedStateHandle keys. Kept as constants so reads and writes can never disagree.
    private val keySearch = "album_search"
    private val keyStatus = "album_status"
    private val keyTeam = "album_team"
    private val keySort = "album_sort"

    // The starting sort order comes from the user's saved default, but only the FIRST time (when
    // there is no value already in SavedStateHandle, e.g. fresh launch). On rotation the saved
    // value wins so we don't override the user's in-screen choice.
    private val defaultSortName =
        (savedState.get<String>(keySort) ?: SettingsManager(app).defaultSort.name)

    // ---- The four filter/sort Flows, each backed by SavedStateHandle ----
    // Search and team are plain Strings. Status and sort are enums stored BY NAME (String).
    private val searchFlow: StateFlow<String> =
        savedState.getStateFlow(keySearch, "")

    private val statusFlow: StateFlow<String> =
        savedState.getStateFlow(keyStatus, StatusFilter.ALL.name)

    private val teamFlow: StateFlow<String> =
        savedState.getStateFlow(keyTeam, ALL_TEAMS)

    private val sortFlow: StateFlow<String> =
        savedState.getStateFlow(keySort, defaultSortName)

    // Transient (not persisted) UI flags: refreshing spinner + error text. These don't need to
    // survive process death, so a plain MutableStateFlow is enough.
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    /**
     * The combined screen state. We combine SIX flows:
     *  1) the live album from the repository,
     *  2-5) the four saved filter/sort flows,
     *  6) a small transient flow that carries (isRefreshing, errorMessage).
     *
     * `combine` only supports up to 5 typed flows in one call, so we fold the two transient flags
     * into a single Pair flow first and combine that as the sixth argument.
     */
    val uiState: StateFlow<AlbumUiState> =
        combine(
            repo.observeAlbum(),
            searchFlow,
            statusFlow,
            teamFlow,
            sortFlow
        ) { album, search, statusName, team, sortName ->
            // Decode the enums from their stored names, falling back safely if the text is invalid.
            val status = parseStatus(statusName)
            val sort = parseSort(sortName)

            // Build the team dropdown options: ALL_TEAMS first, then every distinct team sorted.
            val teamOptions = buildTeamOptions(album)

            // Apply filtering then sorting to get the list the screen will actually draw.
            val visible = applySort(applyFilters(album, search, status, team), sort)

            // We carry the "raw" pieces forward and merge the transient flags in the next step.
            AlbumUiState(
                cards = visible,
                teams = teamOptions,
                searchQuery = search,
                statusFilter = status,
                teamFilter = team,
                sortOrder = sort,
                isEmpty = visible.isEmpty()
            )
        }.combine(transientFlags()) { base, flags ->
            // Merge the refreshing/error flags into the state built above.
            base.copy(
                isRefreshing = flags.first,
                errorMessage = flags.second
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlbumUiState(sortOrder = parseSort(defaultSortName))
        )

    // Combine the two transient flags into one Pair flow so it fits as a single combine argument.
    private fun transientFlags() =
        combine(isRefreshing, errorMessage) { refreshing, error -> Pair(refreshing, error) }

    // ---------------- Public actions the screen calls ----------------

    /** User typed in the search box. */
    fun onSearchChange(query: String) {
        savedState[keySearch] = query
    }

    /** User tapped a status filter chip. We store the enum BY NAME. */
    fun onStatusChange(status: StatusFilter) {
        savedState[keyStatus] = status.name
    }

    /** User picked a team from the dropdown. */
    fun onTeamChange(team: String) {
        savedState[keyTeam] = team
    }

    /** User picked a sort order. We store the enum BY NAME. */
    fun onSortChange(order: SortOrder) {
        savedState[keySort] = order.name
    }

    /**
     * Toggle the favorite flag for one player. We read the current value from the latest emitted
     * list, then ask the repository to write the opposite. The album Flow will update automatically.
     */
    fun toggleFavorite(playerId: Int) {
        viewModelScope.launch {
            val current = uiState.value.cards.firstOrNull { it.playerId == playerId }?.isFavorite ?: false
            repo.setFavorite(playerId, !current)
        }
    }

    /**
     * Pull-to-refresh: try to re-download the catalog. The repository never throws and never clears
     * the cache, so on failure we simply show a friendly Bosnian error and keep the cached list.
     */
    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            val ok = repo.refreshCatalog()
            if (!ok) {
                errorMessage.value = "Nije moguće osvježiti album. Provjerite internet vezu."
            }
            isRefreshing.value = false
        }
    }

    /** Dismiss the current error message (e.g. after the user reads/closes it). */
    fun clearError() {
        errorMessage.value = null
    }

    // ---------------- Private filtering/sorting helpers ----------------

    /** Decode a StatusFilter from its stored name, falling back to ALL if it's somehow invalid. */
    private fun parseStatus(name: String): StatusFilter {
        return try {
            StatusFilter.valueOf(name)
        } catch (e: Exception) {
            StatusFilter.ALL
        }
    }

    /** Decode a SortOrder from its stored name, falling back to NUMBER if it's somehow invalid. */
    private fun parseSort(name: String): SortOrder {
        return try {
            SortOrder.valueOf(name)
        } catch (e: Exception) {
            SortOrder.NUMBER
        }
    }

    /** ALL_TEAMS first, then every distinct team name, sorted alphabetically. */
    private fun buildTeamOptions(album: List<AlbumCard>): List<String> {
        val distinct = album.map { it.reprezentacija }.distinct().sorted()
        val options = ArrayList<String>()
        options.add(ALL_TEAMS)
        options.addAll(distinct)
        return options
    }

    /** Apply the search, status and team filters to the raw album list. */
    private fun applyFilters(
        album: List<AlbumCard>,
        search: String,
        status: StatusFilter,
        team: String
    ): List<AlbumCard> {
        val query = search.trim().lowercase()
        return album.filter { card ->
            // 1) Search: match against the full name, case-insensitive. Empty query matches all.
            val matchesSearch = query.isEmpty() || card.fullName.lowercase().contains(query)

            // 2) Status filter.
            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.OWNED -> card.isOwned
                StatusFilter.MISSING -> !card.isOwned
                StatusFilter.FAVORITES -> card.isFavorite
            }

            // 3) Team filter: ALL_TEAMS means "no team filter".
            val matchesTeam = team == ALL_TEAMS || card.reprezentacija == team

            matchesSearch && matchesStatus && matchesTeam
        }
    }

    /** Sort the already-filtered list according to the chosen order. */
    private fun applySort(cards: List<AlbumCard>, order: SortOrder): List<AlbumCard> {
        return when (order) {
            SortOrder.NUMBER -> cards.sortedBy { it.brojDresa }
            // Last name first, then first name; both lowercased for case-insensitive ordering.
            SortOrder.NAME -> cards.sortedWith(
                compareBy({ it.prezime.lowercase() }, { it.ime.lowercase() })
            )
            // Most recently acquired first. Missing cards (acquiredAt == 0) sink to the bottom.
            SortOrder.ACQUIRED -> cards.sortedByDescending { it.acquiredAt }
        }
    }
}
