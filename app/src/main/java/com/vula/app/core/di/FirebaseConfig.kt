package com.vula.app.core.di

/**
 * Vula backend configuration.
 * The actual base URL is resolved at runtime by NetworkModule
 * based on the device type (emulator vs physical device vs WSA).
 */
object BackendConfig {
    /** Docker host IP when running on a physical device connected to the dev machine's hotspot. */
    const val HOTSPOT_HOST = "192.168.137.1"
    const val PORT = 8081
    const val WS_PATH = "/ws/chat"
}
