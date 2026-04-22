package com.vula.app.local.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WifiDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getCurrentWifiDetails(): WifiDetails? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        if (wifiInfo == null || wifiInfo.networkId == -1) return null

        var ssid = wifiInfo.ssid ?: ""
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        val bssid = wifiInfo.bssid ?: ""

        if (ssid == "<unknown ssid>" || bssid.isEmpty()) return null

        return WifiDetails(ssid = ssid, bssid = bssid)
    }
}

data class WifiDetails(val ssid: String, val bssid: String)
