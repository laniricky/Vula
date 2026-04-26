package com.vula.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.vula.app.core.data.PreferencesDataStore
import com.vula.app.core.ui.OnboardingScreen
import com.vula.app.core.ui.theme.VulaTheme
import com.vula.app.navigation.VulaApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataStore: PreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read synchronously once — tiny DataStore read is safe on main before setContent
        val hasSeenOnboarding = runBlocking { preferencesDataStore.hasSeenOnboarding.first() }

        // Handle Deep Link Invite
        val data = intent?.data
        if (data != null && data.scheme == "https" && data.host == "vulaapp.com" && data.path?.startsWith("/invite") == true) {
            val inviterId = data.getQueryParameter("ref")
            if (inviterId != null) {
                // In a full implementation, we'd save this to DataStore and 
                // auto-add the friend upon successful registration/login
                android.util.Log.d("VulaDeepLink", "Invited by user ID: $inviterId")
            }
        }

        setContent {
            VulaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember { mutableStateOf(!hasSeenOnboarding) }

                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinished = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    preferencesDataStore.setHasSeenOnboarding(true)
                                }
                                showOnboarding = false
                            }
                        )
                    } else {
                        VulaApp()
                    }
                }
            }
        }
    }
}
