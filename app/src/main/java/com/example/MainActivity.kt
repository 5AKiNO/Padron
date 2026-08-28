package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainScreen
import com.example.ui.VoterViewModel
import com.example.ui.theme.VoterAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VoterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appConfig by viewModel.appConfig.collectAsState()

            VoterAppTheme(
                primaryColorHex = appConfig.primaryColorHex,
                themeMode = appConfig.themeMode
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
