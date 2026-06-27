package ba.rma.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The CACHED catalog. We copy every player from the API into this table so the app still works
 * OFFLINE (the spec requires offline access). The list/detail/stats screens all read from here,
 * never directly from the network.
 *
 * `isFavorite` lives here so a user can mark any player (owned or not) as a favorite. It is part of
 * the catalog row rather than a separate table to keep things simple.
 */
@Entity(tableName = "catalog_players")
data class PlayerEntity(
    @PrimaryKey val id: Int,
    val ime: String,
    val prezime: String,
    val brojDresa: Int,
    val reprezentacija: String,
    val pozicija: String,
    val slicicaLokacija: String,
    val isFavorite: Boolean = false
)

/** Converts a freshly downloaded [Player] into a cacheable [PlayerEntity] (not a favorite yet). */
fun Player.toEntity(): PlayerEntity {
    return PlayerEntity(
        id = id,
        ime = ime,
        prezime = prezime,
        brojDresa = brojDresa,
        reprezentacija = reprezentacija,
        pozicija = pozicija,
        slicicaLokacija = slicicaLokacija,
        isFavorite = false
    )
}

/**
 * Converts a cached [PlayerEntity] back into a [Player]. Used to feed the Smart Suggestions engine,
 * which works with the catalog Player model. The pack-only fields stay null (the catalog has none).
 */
fun PlayerEntity.toPlayer(): Player {
    return Player(
        id = id,
        ime = ime,
        prezime = prezime,
        brojDresa = brojDresa,
        reprezentacija = reprezentacija,
        pozicija = pozicija,
        slicicaLokacija = slicicaLokacija
    )
}
