package com.domedemok.travelplanner.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey

@Serializable data object Splash   : Route
@Serializable data object Login    : Route
@Serializable data object SignUp   : Route

// Main shell — hosts the 3 bottom-nav tabs (Discover, My Trips, Profile)
@Serializable data object Main     : Route

// Pushed on top of Main when the user opens a trip (replaces bottom nav with trip nav)
@Serializable data class  TripDetail(val tripId: String) : Route
