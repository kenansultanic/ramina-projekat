package ba.rma.myapplication.data

import com.google.gson.annotations.SerializedName

/**
 * One football player = one collectible sticker ("slicica").
 *
 * The SAME class is used for two different API responses:
 *  - GET /api/all-players          -> the full catalog (109 players). These items do NOT include
 *                                     the pack-only fields below.
 *  - GET /api/random-players/{n}   -> a freshly opened pack. These items DO include the extra
 *                                     fields (tipSlicice, zlatna, vjerovatnoca).
 *
 * Because the catalog response is missing the three pack-only fields, those fields MUST be
 * nullable with a default of null. If we made them non-null, Gson would crash when parsing the
 * catalog (the JSON simply has no such keys there).
 *
 * @SerializedName maps the JSON key (left, exactly as the server spells it) to our Kotlin name
 * (right). The server uses Bosnian snake_case keys, so we rename them to nicer camelCase here.
 */
data class Player(
    @SerializedName("id") val id: Int,
    @SerializedName("ime") val ime: String,                       // first name
    @SerializedName("prezime") val prezime: String,               // last name
    @SerializedName("broj_dresa") val brojDresa: Int,             // jersey number
    @SerializedName("reprezentacija") val reprezentacija: String, // national team, e.g. "Argentina"
    @SerializedName("pozicija") val pozicija: String,             // GOALKEEPER/DEFENDER/MIDFIELDER/FORWARD
    @SerializedName("slicica_lokacija") val slicicaLokacija: String, // image path, e.g. "/api/slicica/1"

    // --- Pack-only fields (present only on /api/random-players responses) ---
    @SerializedName("tip_slicice") val tipSlicice: String? = null, // sticker type, e.g. "obicna"
    @SerializedName("zlatna") val zlatna: Boolean? = null,         // true = golden / rare sticker
    @SerializedName("vjerovatnoca") val vjerovatnoca: Int? = null  // pull probability/weight
)
