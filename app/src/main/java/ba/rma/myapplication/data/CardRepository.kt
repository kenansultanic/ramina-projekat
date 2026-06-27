package ba.rma.myapplication.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * The repository is the SINGLE SOURCE OF TRUTH for the whole app. Every screen reads its data from
 * here (through Room Flows), and never talks to the network directly. The network is only used to
 * WRITE into Room (download the catalog, open packs). This is what makes the app work offline: if
 * there is no internet we simply keep showing whatever is already cached in Room.
 *
 * @param dao the database access object.
 */
class CardRepository(private val dao: CollectionDao) {

    // The network API, from our singleton.
    private val api = RetrofitClient.api

    // ---------------- READS (Flows the UI observes) ----------------

    /**
     * The whole album: every cached player combined with how many copies the user owns and whether
     * it is a favorite. We use the Flow `combine` operator so that whenever EITHER the catalog or
     * the owned table changes, a fresh combined list is produced automatically.
     */
    fun observeAlbum(): Flow<List<AlbumCard>> {
        return combine(dao.observeCatalog(), dao.observeOwned()) { catalog, owned ->
            // Build a quick lookup of owned stickers by player id.
            val ownedById = HashMap<Int, OwnedSticker>()
            for (sticker in owned) {
                ownedById[sticker.playerId] = sticker
            }
            // Turn each cached player into an AlbumCard, filling in ownership info.
            val result = ArrayList<AlbumCard>()
            for (player in catalog) {
                val mine = ownedById[player.id]
                result.add(
                    AlbumCard(
                        playerId = player.id,
                        ime = player.ime,
                        prezime = player.prezime,
                        brojDresa = player.brojDresa,
                        reprezentacija = player.reprezentacija,
                        pozicija = player.pozicija,
                        slicicaLokacija = player.slicicaLokacija,
                        ownedCount = mine?.count ?: 0,
                        isGolden = mine?.isGolden ?: false,
                        isFavorite = player.isFavorite,
                        acquiredAt = mine?.acquiredAt ?: 0L
                    )
                )
            }
            result
        }
    }

    /** The cached catalog as [Player] objects (used by the Smart Suggestions engine). */
    fun observeCatalogPlayers(): Flow<List<Player>> {
        return dao.observeCatalog().map { list ->
            val players = ArrayList<Player>()
            for (entity in list) {
                players.add(entity.toPlayer())
            }
            players
        }
    }

    /** The user's owned stickers (used by the Smart Suggestions engine). */
    fun observeOwned(): Flow<List<OwnedSticker>> {
        return dao.observeOwned()
    }

    /** Only the favorite cards. */
    fun observeFavorites(): Flow<List<AlbumCard>> {
        return observeAlbum().map { cards ->
            cards.filter { card -> card.isFavorite }
        }
    }

    /** One specific card by player id, or null if it is not in the catalog. */
    fun observePlayerDetail(playerId: Int): Flow<AlbumCard?> {
        return observeAlbum().map { cards ->
            cards.firstOrNull { card -> card.playerId == playerId }
        }
    }

    /** True if we have already cached the catalog at least once. */
    suspend fun hasCatalog(): Boolean {
        return dao.catalogCount() > 0
    }

    // ---------------- WRITES (network -> Room) ----------------

    /**
     * Downloads the full catalog and saves it into Room. Returns true on success, false if it failed
     * (e.g. no internet) — it NEVER throws to the UI and NEVER clears the existing cache, so offline
     * users keep seeing their cached data.
     *
     * We preserve favorites: the upsert would otherwise reset isFavorite to false, so we read the
     * current favorite ids first and re-apply them.
     */
    suspend fun refreshCatalog(): Boolean {
        return try {
            val players = api.getAllPlayers()
            val favIds = dao.favoriteIds().toSet()
            val entities = ArrayList<PlayerEntity>()
            for (player in players) {
                val entity = player.toEntity()
                if (favIds.contains(player.id)) {
                    entities.add(entity.copy(isFavorite = true))
                } else {
                    entities.add(entity)
                }
            }
            dao.upsertCatalog(entities)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Opens a pack on the server and returns [count] random players (duplicates possible). */
    suspend fun openPackFromServer(count: Int): List<Player> {
        return api.openPack(count)
    }

    /**
     * Saves one card from an opened pack. If the user already owns the player we increment the
     * count; otherwise we insert a new row with the current time as the acquisition date and the
     * golden flag from the pack (the catalog never reports golden).
     */
    suspend fun addOpenedCard(player: Player) {
        val existing = dao.getOne(player.id)
        if (existing == null) {
            dao.insertNew(
                OwnedSticker(
                    playerId = player.id,
                    count = 1,
                    isGolden = player.zlatna ?: false,
                    acquiredAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.incrementCount(player.id)
        }
    }

    /** Marks or unmarks a player as favorite. */
    suspend fun setFavorite(playerId: Int, fav: Boolean) {
        dao.setFavorite(playerId, fav)
    }

    companion object {
        @Volatile
        private var INSTANCE: CardRepository? = null

        /**
         * Returns the one shared repository. Every ViewModel gets the SAME instance via this method,
         * so they all read/write the same single source of truth.
         */
        fun get(context: Context): CardRepository {
            val existing = INSTANCE
            if (existing != null) {
                return existing
            }
            synchronized(this) {
                val again = INSTANCE
                if (again != null) {
                    return again
                }
                val created = CardRepository(AppDatabase.getInstance(context).collectionDao())
                INSTANCE = created
                return created
            }
        }
    }
}
