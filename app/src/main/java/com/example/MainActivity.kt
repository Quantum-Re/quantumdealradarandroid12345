package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DealRadarViewModel
import com.example.ui.MainScreen
import com.example.ui.MainTab
import com.example.ui.theme.QuantumDealRadarTheme
import com.example.ui.theme.ThemeMode
import com.example.util.FcmPushManager

class MainActivity : ComponentActivity() {
    private val viewModel: DealRadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FcmPushManager.ensureChannels(this)
        FcmPushManager.fetchToken(this)
        handleNotificationIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            QuantumDealRadarTheme(darkTheme = isDark) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val openScreen = intent?.getStringExtra("OPEN_SCREEN") ?: return
        val dealId = intent.getLongExtra("DEAL_ID", -1L)
        when (openScreen) {
            "RADAR_FEED" -> {
                viewModel.setMainTab(MainTab.RADAR_FEED)
                if (dealId > 0) {
                    viewModel.openDealById(dealId)
                }
            }
            "ROI_CALCULATOR" -> {
                viewModel.setMainTab(MainTab.ROI_CALCULATOR)
                if (dealId > 0) {
                    viewModel.openDealById(dealId)
                }
            }
            "INVESTOR_BRIEF" -> {
                viewModel.setMainTab(MainTab.INVESTOR_BRIEF)
            }
            "DISTRESSED_PROPERTIES", "DISTRESSED" -> {
                viewModel.setMainTab(MainTab.DISTRESSED)
            }
            "SUPPLY_DEMAND_MONITOR" -> {
                viewModel.setMainTab(MainTab.SUPPLY_DEMAND_MONITOR)
            }
            "FCM_CENTER", "NOTIFICATIONS" -> {
                viewModel.setMainTab(MainTab.NOTIFICATION_CONFIG)
                if (openScreen == "FCM_CENTER") {
                    viewModel.openFcmPushCenter()
                }
            }
        }
    }
}

