package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkMonitor(
    context: Context,
    database: FirebaseDatabase
) {
    private val connectivityManager = try {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (_: Exception) {
        null
    }

    private val _isDeviceOnline = MutableStateFlow(checkInitialConnectivity())
    private val _isFirebaseConnected = MutableStateFlow(false)
    private val _isOnline = MutableStateFlow(checkInitialConnectivity())

    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val connectedRef = try { database.getReference(".info/connected") } catch (_: Exception) { null }
    private val connectedListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _isFirebaseConnected.value = connected
                updateOnlineState()
            } catch (_: Exception) {}
        }

        override fun onCancelled(error: DatabaseError) {
            // No-op
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isDeviceOnline.value = true
            updateOnlineState()
        }

        override fun onLost(network: Network) {
            _isDeviceOnline.value = checkInitialConnectivity()
            updateOnlineState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isDeviceOnline.value = hasInternet
            updateOnlineState()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Fallback gracefully
        }

        // Also track Firebase Realtime Database live connection status
        try {
            connectedRef?.addValueEventListener(connectedListener)
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val manager = connectivityManager ?: return true
            val activeNetwork = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun updateOnlineState() {
        // App is online if device has connectivity or active socket to Firebase
        _isOnline.value = _isDeviceOnline.value || _isFirebaseConnected.value
    }

    fun release() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        try {
            connectedRef?.removeEventListener(connectedListener)
        } catch (_: Exception) {}
    }
}
