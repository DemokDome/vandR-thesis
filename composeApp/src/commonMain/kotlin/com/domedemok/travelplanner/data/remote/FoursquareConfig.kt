package com.domedemok.travelplanner.data.remote

import com.domedemok.travelplanner.BuildKonfig

/**
 * Whether HTTP calls to Foursquare must be routed through a CORS proxy.
 *
 * `true` on JS targets: browsers enforce same-origin restrictions and
 *   `places-api.foursquare.com` does not send the required `Access-Control-Allow-Origin`
 *   header for arbitrary web origins.
 * `false` on Android: the OS network stack has no CORS notion.
 */
expect val USE_CORS_PROXY: Boolean

/**
 * Static configuration for the Foursquare Places API client.
 *
 * [API_KEY] is injected at build time from `local.properties` via BuildKonfig
 * to keep the secret out of source control.
 */
object FoursquareConfig {
    const val BASE_URL    = "https://places-api.foursquare.com/places/"
    const val API_VERSION = "2025-06-17"
    val API_KEY: String   = BuildKonfig.FOURSQUARE_API_KEY

    /**
     * CORS proxy prefix for browser requests (see [USE_CORS_PROXY]).
     *
     * SECURITY NOTE: the Foursquare `Authorization: Bearer` header transits this
     * host, so a public proxy (the default `corsproxy.io`) effectively exposes
     * the key to a third party. Override `FOURSQUARE_CORS_PROXY` in
     * local.properties with a self-hosted proxy, or move Foursquare calls behind
     * your own backend, before any production use.
     */
    val CORS_PROXY: String = BuildKonfig.FOURSQUARE_CORS_PROXY
}
