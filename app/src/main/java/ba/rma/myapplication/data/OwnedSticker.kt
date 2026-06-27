package ba.rma.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in our local database = one player the user OWNS, plus how many copies they have.
 *
 * We store a COUNT (not one row per physical sticker) so we can easily tell duplicates apart:
 *  - count == 1  -> the user has exactly this sticker, no spares.
 *  - count >  1  -> the user has duplicates they could trade away.
 *
 * Why store `isGolden` here too: the catalog endpoint (/api/all-players) does NOT tell us whether
 * a sticker is golden/rare; only the pack response does. So when we first obtain a sticker from a
 * pack we remember its golden status here ("sticky" rarity), and the Smart Suggestions feature can
 * use it later even though the catalog itself never reports it.
 *
 * `acquiredAt` records WHEN the user first got this sticker (milliseconds since 1970). The Album
 * screen uses it to offer "sort by acquisition date".
 */
@Entity(tableName = "owned_stickers")
data class OwnedSticker(
    @PrimaryKey val playerId: Int, // matches Player.id
    val count: Int,                // how many copies the user owns (always >= 1)
    val isGolden: Boolean = false, // true if this player's sticker is the golden/rare version
    val acquiredAt: Long = 0L      // when first acquired (epoch millis); 0 = unknown
)
