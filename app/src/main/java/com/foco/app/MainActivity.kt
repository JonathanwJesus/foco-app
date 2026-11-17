package com.foco.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foco.app.notifications.Reminders
import com.foco.app.ui.FocoApp
import com.foco.app.ui.FocoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.ensureChannel(this)
        setContent {
            FocoTheme {
                FocoApp()
            }
        }
    }
}
