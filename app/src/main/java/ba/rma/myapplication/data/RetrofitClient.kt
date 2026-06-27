package ba.rma.myapplication.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds and holds the single Retrofit instance for the whole app.
 *
 * We use `object` (a Kotlin singleton) so there is exactly ONE Retrofit/ApiService for the app.
 * Creating Retrofit is relatively expensive, so we want to do it only once and reuse it.
 */
object RetrofitClient {

    // IMPORTANT: the base URL MUST end with a slash, otherwise Retrofit throws at startup.
    // The API is plain HTTP (not HTTPS); cleartext is allowed for this host in
    // res/xml/network_security_config.xml.
    const val BASE_URL = "http://49.13.125.189:3300/"

    // The ready-to-use API. GsonConverterFactory turns the JSON responses into Player objects.
    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    /**
     * Builds a full image URL from a player's `slicicaLokacija` (e.g. "/api/slicica/1").
     * The path already starts with a slash, so we drop the trailing slash from BASE_URL to avoid
     * a double slash ("...3300//api/..."). Coil uses the returned URL to download the picture.
     */
    fun imageUrl(slicicaLokacija: String): String {
        return BASE_URL.dropLast(1) + slicicaLokacija
    }
}
