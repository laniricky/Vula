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
