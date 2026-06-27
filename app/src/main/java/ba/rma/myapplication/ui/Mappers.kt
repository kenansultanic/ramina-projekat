package ba.rma.myapplication.ui

import ba.rma.myapplication.data.AlbumCard

/**
 * Shared converters/helpers used by several screens. Keeping them in one place avoids duplicating
 * the same mapping logic in every screen.
 */

/** Converts a domain [AlbumCard] into the [CardUi] that [StickerCard] knows how to draw. */
fun AlbumCard.toCardUi(): CardUi {
    return CardUi(
        playerId = playerId,
        firstName = ime,
        lastName = prezime,
        jerseyNumber = brojDresa,
        team = reprezentacija,
        position = pozicija,
        imagePath = slicicaLokacija,
        isGolden = isGolden,
        isFavorite = isFavorite,
        ownedCount = ownedCount
    )
}

/** Translates the API's English position names into short Bosnian labels for display. */
fun positionLabelBs(position: String): String {
    return when (position) {
        "GOALKEEPER" -> "Golman"
        "DEFENDER" -> "Odbrana"
        "MIDFIELDER" -> "Vezni red"
        "FORWARD" -> "Napad"
        else -> position
    }
}
