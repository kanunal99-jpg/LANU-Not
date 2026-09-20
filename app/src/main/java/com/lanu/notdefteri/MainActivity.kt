package com.lanu.notdefteri

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lanu.notdefteri.data.model.ThemeConfig
import com.lanu.notdefteri.ui.screens.HomeScreen
import com.lanu.notdefteri.ui.theme.LanuTheme
import com.lanu.notdefteri.ui.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
            LanuTheme(themeConfig = themeConfig) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
