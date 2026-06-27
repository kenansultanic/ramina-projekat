package ba.rma.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object. The list of database operations the app needs, for BOTH tables:
 *  - owned_stickers   (what the user owns)
 *  - catalog_players  (the cached list of all players, for offline use)
 * Room writes the actual SQL implementation for us based on these annotations.
 */
@Dao
interface CollectionDao {

    // ---------------- Owned stickers ----------------

    /**
     * Watch the user's owned stickers. Returns a Flow, so whenever the table changes (e.g. a pack is
     * opened) every observer is notified automatically with the new list.
     */
    @Query("SELECT * FROM owned_stickers")
    fun observeOwned(): Flow<List<OwnedSticker>>

    /** Look up one owned sticker by player id, or null if the user does not own it yet. */
    @Query("SELECT * FROM owned_stickers WHERE playerId = :playerId LIMIT 1")
    suspend fun getOne(playerId: Int): OwnedSticker?

    /** Add a brand-new owned sticker (first time the user gets a given player). */
    @Insert
    suspend fun insertNew(sticker: OwnedSticker)

    /** The user got a duplicate: add 1 to the count of a player they already own. */
    @Query("UPDATE owned_stickers SET count = count + 1 WHERE playerId = :playerId")
    suspend fun incrementCount(playerId: Int)

    // ---------------- Cached catalog ----------------

    /** Watch the whole cached catalog (all players), ordered by id. */
    @Query("SELECT * FROM catalog_players ORDER BY id")
    fun observeCatalog(): Flow<List<PlayerEntity>>

    /**
     * Insert-or-update the catalog rows. @Upsert inserts new rows and updates existing ones, which
     * is exactly what we want when refreshing the cache from the network.
     */
    @Upsert
    suspend fun upsertCatalog(players: List<PlayerEntity>)

    /** How many players are cached (0 means we have never downloaded the catalog yet). */
    @Query("SELECT COUNT(*) FROM catalog_players")
    suspend fun catalogCount(): Int

    /** The ids of all players currently marked as favorite (used to preserve favorites on refresh). */
    @Query("SELECT id FROM catalog_players WHERE isFavorite = 1")
    suspend fun favoriteIds(): List<Int>

    /** Mark/unmark a player as favorite. */
    @Query("UPDATE catalog_players SET isFavorite = :fav WHERE id = :playerId")
    suspend fun setFavorite(playerId: Int, fav: Boolean)
}
