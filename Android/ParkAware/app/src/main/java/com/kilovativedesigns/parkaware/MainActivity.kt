package com.kilovativedesigns.parkaware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // draw edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUi = rememberSystemUiController()
            val dark = isSystemInDarkTheme()

            // status + nav bar colors/icons
            SideEffect {
                systemUi.setStatusBarColor(Color.Transparent, darkIcons = !dark)
                systemUi.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = !dark,
                    navigationBarContrastEnforced = false
                )
            }

            // use MaterialTheme directly (no custom MyTheme needed)
            MaterialTheme {
                Scaffold(
                    bottomBar = {
                        AppBottomBar(
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                ) { innerPadding ->
                    // your screen content goes here; keep it simple for now
                    Box(Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Minimal placeholder bottom bar so the project compiles. */
@Composable
private fun AppBottomBar(modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = true,
            onClick = { /* TODO */ },
            icon = {
                // use any icon you have; fallback to text if you don't
                // If you don't have vector icons yet, comment this Icon and leave Text.
                // Icon(painterResource(R.drawable.ic_home), contentDescription = null)
                Text("Home")
            },
            label = null
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Text("Map") },
            label = null
        )
    }
}