package ba.rma.myapplication.ui

/**
 * Base address of the API for building image URLs in the UI layer.
 *
 * NOTE the missing trailing slash (on purpose): a player's image path already starts with a slash
 * (e.g. "/api/slicica/1"), so we just glue them together: API_BASE_URL + imagePath. Adding a slash
 * here would create a broken double-slash URL.
 */
const val API_BASE_URL = "http://49.13.125.189:3300"
