package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.CurrencyExchangeRateService
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Interactive Currency Switcher component allowing international investors to toggle
 * between global currencies (EUR, USD, GBP, CHF, CAD, AUD, AED, JPY) in real time.
 */
@Composable
fun CurrencySwitcherBar(
    selectedCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showRatePill: Boolean = true
) {
    val liveRates by CurrencyExchangeRateService.liveRates.collectAsStateWithLifecycle()
    val isFetching by CurrencyExchangeRateService.isFetchingRates.collectAsStateWithLifecycle()
    val isLiveOnline by CurrencyExchangeRateService.isLiveOnline.collectAsStateWithLifecycle()
    val rateSourceLabel by CurrencyExchangeRateService.rateSourceLabel.collectAsStateWithLifecycle()

    // Spin animation for live FX refresh
    val infiniteTransition = rememberInfiniteTransition(label = "fx_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fx_spin_anim"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSlateBg,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("currency_switcher_bar")
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header / Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Cambio Valuta Globale",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Valuta di Visualizzazione (FX)",
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                if (showRatePill) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live Rate Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLiveOnline) EmeraldGreen.copy(alpha = 0.15f) else AmberGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isLiveOnline) EmeraldGreen.copy(alpha = 0.4f) else AmberGold.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLiveOnline) EmeraldGreen else AmberGold)
                                )
                                Text(
                                    text = CurrencyExchangeRateService.getExchangeRatePillText(selectedCurrency, liveRates),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLiveOnline) EmeraldGreen else AmberGold
                                )
                            }
                        }

                        // Refresh Rates Action Button
                        IconButton(
                            onClick = { CurrencyExchangeRateService.fetchLiveRatesAsync() },
                            enabled = !isFetching,
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Aggiorna tassi di cambio live" }
                                .testTag("refresh_fx_rates_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Aggiorna Tassi",
                                tint = if (isFetching) CyanAccent else TextMutedDark,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (isFetching) spinAngle else 0f)
                            )
                        }
                    }
                }
            }

            // Scrollable Currency Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppCurrency.entries.forEach { currency ->
                    val isSelected = currency == selectedCurrency
                    val rate = CurrencyExchangeRateService.getRate(currency, liveRates)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyanAccent.copy(alpha = 0.18f) else SurfaceCardDark,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyanAccent else SurfaceCardBorder
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onCurrencySelected(currency)
                                CurrencyExchangeRateService.setSelectedCurrency(currency)
                            }
                            .semantics {
                                contentDescription = "Seleziona valuta ${currency.displayName}"
                            }
                            .testTag("currency_chip_${currency.code.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = currency.flagEmoji,
                                fontSize = 14.sp
                            )

                            Column(verticalArrangement = Arrangement.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = currency.code,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CyanAccent else TextPrimaryDark
                                    )
                                    Text(
                                        text = "(${currency.symbol})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isSelected) CyanAccent.copy(alpha = 0.85f) else TextMutedDark
                                    )
                                }

                                if (currency != AppCurrency.EUR) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "1€ = %.2f", rate),
                                        fontSize = 9.sp,
                                        color = if (isSelected) TextPrimaryDark.copy(alpha = 0.8f) else TextMutedDark
                                    )
                                } else {
                                    Text(
                                        text = "Base EUR",
                                        fontSize = 9.sp,
                                        color = if (isSelected) TextPrimaryDark.copy(alpha = 0.8f) else TextMutedDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
