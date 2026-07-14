package com.fitness.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.app.data.prefs.ThemeMode
import com.fitness.app.ui.nav.FitnessRootScreen
import com.fitness.app.ui.theme.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as FitnessApp).repository
        setContent {
            val themeMode by repo.settings.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            FitnessTheme(themeMode) {
                FitnessRootScreen(repo)
            }
        }
    }
}
