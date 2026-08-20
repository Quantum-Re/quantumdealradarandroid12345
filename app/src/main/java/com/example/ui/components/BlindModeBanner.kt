package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvestorProfile
import com.example.ui.theme.*

@Composable
fun BlindModeBanner(
    investorProfile: InvestorProfile?,
    onToggleBlindMode: (Boolean) -> Unit,
    onOpenUpgradeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBlind = investorProfile?.isBlindModeActive ?: true
    val isPro = investorProfile?.isProSubscriber ?: false
    val tokens = investorProfile?.availableUnlockTokens ?: 1
    val unlockedCount = investorProfile?.getUnlockedDealIdsList()?.size ?: 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("blind_mode_banner"),
        shape = RoundedCornerShape(16.dp),
        color = if (isPro) PurpleIndigo.copy(alpha = 0.15f) else SurfaceCardDark,
        border = BorderStroke(1.dp, if (isPro) PurpleIndigo.copy(alpha = 0.6f) else AmberGold.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPro) PurpleIndigo.copy(alpha = 0.25f) else (if (isBlind) AmberGold.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPro) Icons.Default.WorkspacePremium else (if (isBlind) Icons.Default.Lock else Icons.Default.LockOpen),
                                contentDescription = null,
                                tint = if (isPro) PurpleIndigo else (if (isBlind) AmberGold else EmeraldGreen),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isPro) "Accesso INVESTOR PRO Illimitato" else (if (isBlind) "Modalità Blind Attiva (Nuovo Utente)" else "Tutti i Dati Sbloccati"),
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isPro) {
                                Surface(shape = RoundedCornerShape(4.dp), color = PurpleIndigo) {
                                    Text("PRO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text(
                            text = if (isPro) {
                                "Hai accesso illimitato a perizie CTU, indirizzi e offerte"
                            } else if (isBlind) {
                                "Dati sensibili protetti. Token disponibili: $tokens | Operazioni sbloccate: $unlockedCount"
                            } else {
                                "Visualizzazione completa di tutte le operazioni attiva"
                            },
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                // Action Pill Button
                if (!isPro) {
                    Button(
                        onClick = onOpenUpgradeDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("banner_upgrade_pro_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sblocca PRO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive simulation toggle for testing & user flexibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simula Vista Nuovo Utente (Blind Mode):",
                    color = TextMutedDark,
                    fontSize = 10.sp
                )
                Switch(
                    checked = isBlind,
                    onCheckedChange = { onToggleBlindMode(it) },
                    modifier = Modifier.size(width = 38.dp, height = 24.dp).testTag("toggle_blind_mode_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberGold,
                        checkedTrackColor = AmberGold.copy(alpha = 0.4f),
                        uncheckedThumbColor = EmeraldGreen,
                        uncheckedTrackColor = EmeraldGreen.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}
