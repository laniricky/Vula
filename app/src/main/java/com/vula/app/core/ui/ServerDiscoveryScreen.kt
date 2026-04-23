package com.vula.app.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vula.app.core.di.FirebaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

@Composable
fun ServerDiscoveryScreen(onServerFound: () -> Unit) {
    var status by remember { mutableStateOf("Initializing server discovery...") }

    LaunchedEffect(Unit) {
        val foundHost = discoverServer { currentStatus ->
            status = currentStatus
        }
        if (foundHost != null) {
            FirebaseConfig.emulatorHost = foundHost
        }
        // Proceed even if not found (it will use default and fail later, but we shouldn't block forever)
        onServerFound()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Finding Vula Server...\n$status",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

suspend fun discoverServer(onStatusUpdate: (String) -> Unit): String? = withContext(Dispatchers.IO) {
    val port = 8088
    val defaultHost = "10.100.8.139"
    val altHost = "10.100.4.198"

    onStatusUpdate("Checking default IPs ($defaultHost, $altHost)...")
    if (isPortOpen(defaultHost, port, 1000)) return@withContext defaultHost
    if (isPortOpen(altHost, port, 1000)) return@withContext altHost

    val localIp = getLocalIpAddress()
    if (localIp != null) {
        val parts = localIp.split(".")
        if (parts.size == 4) {
            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
            onStatusUpdate("Scanning local network $prefix.x...")
            
            // Scan in batches to avoid too many open sockets
            val allIps = (1..254).map { "$prefix.$it" }
            val chunks = allIps.chunked(50)
            
            for (chunk in chunks) {
                val deferreds = chunk.map { testIp ->
                    async {
                        if (isPortOpen(testIp, port, 400)) testIp else null
                    }
                }
                
                // Fast return if any is found
                val results = deferreds.awaitAll()
                val found = results.firstOrNull { it != null }
                if (found != null) {
                    onStatusUpdate("Found server at $found!")
                    return@withContext found
                }
            }
        }
    }
    
    onStatusUpdate("Server not found on local network.")
    null
}

fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
    return try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), timeout)
            true
        }
    } catch (e: Exception) {
        false
    }
}

fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        // Ignore
    }
    return null
}
