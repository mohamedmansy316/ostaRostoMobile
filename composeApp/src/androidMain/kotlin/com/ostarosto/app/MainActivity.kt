package com.ostarosto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    // Cold-start deep links (ostarosto://order/{id}) are consumed by the
    // Navigation-Compose graph from this Activity's launch intent. Warm-start
    // (app already running) handling — navController.handleDeepLink(intent) in
    // onNewIntent — is deferred to Phase 4 alongside the FCM foreground flow.
}
