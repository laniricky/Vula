package com.vula.app.chat.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vula.app.core.data.SessionManager
import com.vula.app.core.di.NetworkModule
import com.vula.app.core.network.ApiMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) {

    private var webSocket: WebSocket? = null
    
    private val _messageEvents = MutableSharedFlow<ApiMessage>(extraBufferCapacity = 100)
    val messageEvents = _messageEvents.asSharedFlow()
    
    private val _typingEvents = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 50)
    val typingEvents = _typingEvents.asSharedFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val messageAdapter = moshi.adapter(ApiMessage::class.java)

    fun connect() {
        if (webSocket != null) return
        
        val userId = runBlocking { sessionManager.getUserIdNow() } ?: return
        
        // Derive WS URL from base URL
        // In a real app this would be injected via a config
        val baseUrl = "http://10.100.8.139:8081" // Hardcoded for WSA as per NetworkModule
        val wsUrl = baseUrl.replace("http://", "ws://") + "/ws/chat/$userId"
        
        val request = Request.Builder().url(wsUrl).build()
        
        // Use a client with longer timeouts for WS
        val wsClient = okHttpClient.newBuilder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
            
        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "message" -> {
                            val msgObj = json.optJSONObject("message")
                            if (msgObj != null) {
                                messageAdapter.fromJson(msgObj.toString())?.let {
                                    _messageEvents.tryEmit(it)
                                }
                            }
                        }
                        "typing" -> {
                            val roomId = json.optString("roomId")
                            val uId = json.optString("userId")
                            if (roomId.isNotEmpty() && uId.isNotEmpty()) {
                                _typingEvents.tryEmit(roomId to uId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@ChatWebSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@ChatWebSocketManager.webSocket = null
            }
        })
    }

    fun sendTypingIndicator(roomId: String) {
        val json = JSONObject().apply {
            put("type", "typing")
            put("roomId", roomId)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "App paused")
        webSocket = null
    }
}
