package ba.rma.myapplication.data

/**
 * The combined view of one player that the UI actually shows: catalog info + whether/how many the
 * user owns + favorite status. The repository builds this by joining the cached catalog
 * ([PlayerEntity]) with the user's collection ([OwnedSticker]).
 *
 * Field names are kept in Bosnian to match the API/Player model (no confusing renaming).
 */
data class AlbumCard(
    val playerId: Int,
    val ime: String,
    val prezime: String,
    val brojDresa: Int,
    val reprezentacija: String,
    val pozicija: String,
    val slicicaLokacija: String,
    val ownedCount: Int,     // 0 = the user does not have this sticker yet
    val isGolden: Boolean,   // false when not owned
    val isFavorite: Boolean,
    val acquiredAt: Long     // 0 when not owned
) {
    /** True if the user owns at least one copy. */
    val isOwned: Boolean
        get() = ownedCount > 0

    /** "Lionel" + "Messi" -> "Lionel Messi". */
    val fullName: String
        get() = "$ime $prezime"
}
