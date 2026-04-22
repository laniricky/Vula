package com.vula.app.core.util

object HashUtils {
    init {
        System.loadLibrary("vula-native")
    }

    /**
     * Native C++ implementation for extremely fast SSID+BSSID hashing.
     */
    external fun hashNetworkId(input: String): String

    fun sha256(input: String): String {
        return hashNetworkId(input)
    }
}
