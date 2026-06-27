package ba.rma.myapplication.data

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Describes the two API endpoints we use, in Retrofit's style.
 *
 * Retrofit reads these annotations and writes the actual networking code for us. Each function is
 * marked `suspend`, which means it runs on a background thread (a coroutine) and does not freeze
 * the UI while we wait for the server to answer.
 */
interface ApiService {

    /**
     * The CATALOG: every player/sticker that exists in the album (109 of them).
     * Used by the Smart Suggestions feature to know what the user is still missing.
     */
    @GET("api/all-players")
    suspend fun getAllPlayers(): List<Player>

    /**
     * OPEN A PACK: ask the server for {count} random players. Duplicates are possible (that is the
     * point of a pack), so this is the non-unique endpoint. Used by the Shake-to-open feature.
     */
    @GET("api/random-players/{count}")
    suspend fun openPack(@Path("count") count: Int): List<Player>
}
