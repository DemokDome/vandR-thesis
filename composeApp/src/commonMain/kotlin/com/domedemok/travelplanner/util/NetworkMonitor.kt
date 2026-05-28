package com.domedemok.travelplanner.util

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific connectivity probe.
 *
 * Implementations expose the current online state via [isOnline] and update it
 * eagerly whenever the OS reports a connectivity change. Consumers should:
 *  1. call [start] once at app launch (typically from DI),
 *  2. observe [isOnline] reactively (e.g. via `LocalIsOnline` in the UI),
 *  3. call [stop] only on full app shutdown — short-lived screens must not stop it.
 *
 * The flow always has a value; treat the initial emission as authoritative.
 */
expect class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    fun start()
    fun stop()
}
