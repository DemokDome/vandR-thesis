package com.domedemok.travelplanner.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class NetworkMonitor(private val context: Context) {

    private val connectivityManager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private val _isOnline = MutableStateFlow(hasActiveConnection())
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // registerDefaultNetworkCallback tracks the single network Android actually
    // uses for internet traffic. onLost here means "no default network" = offline,
    // without needing to re-check other networks like registerNetworkCallback does.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }
        override fun onLost(network: Network) {
            _isOnline.value = false
        }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            // NET_CAPABILITY_VALIDATED = Android confirmed this network has real internet.
            _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    actual fun start() {
        val cm = connectivityManager ?: return
        try {
            cm.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) { }
    }

    actual fun stop() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) { }
    }

    private fun hasActiveConnection(): Boolean {
        val cm            = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps          = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
