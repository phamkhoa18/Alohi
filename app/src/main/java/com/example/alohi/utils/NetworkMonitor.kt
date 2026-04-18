package com.example.alohi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
    Available, Unavailable
}

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(NetworkStatus.Unavailable)
            }
            
            override fun onUnavailable() {
                super.onUnavailable()
                trySend(NetworkStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (hasInternet) {
                    trySend(NetworkStatus.Available)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            // Initial state
            val checkStatus = checkCurrentNetwork()
            trySend(checkStatus)
        } catch (e: Exception) {
            trySend(NetworkStatus.Unavailable)
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {}
        }
    }.distinctUntilChanged()

    private fun checkCurrentNetwork(): NetworkStatus {
        val network = connectivityManager.activeNetwork ?: return NetworkStatus.Unavailable
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return NetworkStatus.Unavailable
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            NetworkStatus.Available
        } else {
            NetworkStatus.Unavailable
        }
    }
}
