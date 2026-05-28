package com.domedemok.travelplanner.data.remote

/**
 * Foursquare Places category-ID groups used as `categories=…` query values.
 *
 * Each constant is a comma-separated list of one or more category IDs that
 * map to a single user-facing browse bucket. Multi-ID buckets request the
 * union (Foursquare OR-s the IDs).
 *
 * Format: predominantly v2 hex IDs from `developer.foursquare.com/docs/categories`.
 * [NIGHTLIFE] additionally carries a v3 numeric ID alongside the legacy hex.
 */
object FoursquareCategories {
    // Monuments + historic landmarks
    const val SIGHTS        = "4bf58dd8d48988d12d941735,4de02b9f4765f83613cdba6e"

    // Museums
    const val MUSEUMS       = "4bf58dd8d48988d181941735"

    // Restaurants
    const val RESTAURANTS   = "4d4b7105d754a06374d81259"

    // Coffee shops
    const val COFFEE        = "4bf58dd8d48988d1e0931735"

    // Nightlife — v3 numeric ID first, legacy v2 root for fallback
    const val NIGHTLIFE     = "10032,4d4b7105d754a06376d81259"

    // Shopping
    const val SHOPPING      = "4d4b7105d754a06378d81259"

    // Parks, beaches, hiking trails, botanical gardens
    const val NATURE        = "4bf58dd8d48988d163941735,4bf58dd8d48988d1e2941735," +
                              "52e81612bcbc57f1066b7a22,4bf58dd8d48988d159941735"

    // Amusement parks, aquariums, zoos, water parks
    const val ENTERTAINMENT = "4bf58dd8d48988d182941735,4fceea171983d5d06c3e9823," +
                              "4bf58dd8d48988d17b941735,4bf58dd8d48988d193941735"

    // Spiritual sites — common parent for churches, mosques, synagogues, shrines
    const val SPIRITUAL     = "4bf58dd8d48988d131941735"

    // Desserts: ice cream, bakery, pastry shops
    const val SWEETS        = "4bf58dd8d48988d1d0941735,4bf58dd8d48988d1c9941735," +
                              "4bf58dd8d48988d16a941735"

    // Hotels, hostels, resorts
    const val ACCOMMODATION = "4bf58dd8d48988d1fa931735,4bf58dd8d48988d1ee931735," +
                              "4bf58dd8d48988d12f951735"

    // Airports, train stations, metro, bus terminals
    const val TRANSPORT     = "4bf58dd8d48988d1ed931735,4bf58dd8d48988d129951735," +
                              "4bf58dd8d48988d1fd931735,4bf58dd8d48988d1fe931735"
}
