package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import com.example.ui.NexaHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NexaBlack

class MainActivity : ComponentActivity() {

    private val notificationIntentKey = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("open_notifications", false) == true) {
            notificationIntentKey.intValue++
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NexaBlack
                ) {
                    NexaHomeScreen(
                        notificationIntentKey = notificationIntentKey.intValue
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("open_notifications", false)) {
            notificationIntentKey.intValue++
        }
    }
}
