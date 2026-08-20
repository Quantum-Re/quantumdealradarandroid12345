package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AuthResult
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationDialog(
    email: String,
    isFirebaseConfigured: Boolean = true,
    onDismiss: () -> Unit,
    onCheckVerification: (onResult: (Boolean) -> Unit) -> Unit,
    onResendVerification: (onResult: (AuthResult) -> Unit) -> Unit,
    onSimulateVerification: (() -> Unit)? = null,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }
    var isVerifiedSuccess by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    // Resend cooldown timer
    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isChecking && !isResending) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isChecking,
            dismissOnClickOutside = !isChecking,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .testTag("email_verification_dialog"),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, if (isVerifiedSuccess) EmeraldGreen.copy(alpha = 0.8f) else SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isVerifiedSuccess) Color(0xFF064E3B)
                                    else Color(0xFF1E293B)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVerifiedSuccess) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                                contentDescription = null,
                                tint = if (isVerifiedSuccess) EmeraldGreen else AmberGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isVerifiedSuccess) "Email Verificata!" else "Verifica Email Obbligatoria",
                                color = TextPrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firebase Security Guard",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isChecking && !isResending,
                        modifier = Modifier.testTag("close_verification_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                AnimatedContent(
                    targetState = isVerifiedSuccess,
                    transitionSpec = {
                        (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                    },
                    label = "email_verification_content_transition"
                ) { success ->
                    if (success) {
                        // Success View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color(0xFF064E3B).copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Account Investitore Attivato!",
                                        color = EmeraldGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "L'indirizzo $email è stato verificato con successo. Tutte le funzionalità della piattaforma sono ora sbloccate.",
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onVerificationSuccess()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("verification_continue_btn")
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Entra nella Dashboard Deal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Pending Verification View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Per proteggere la community di investitori e garantire l'accesso esclusivo alle opportunità immobiliari, è necessario verificare la tua casella email.",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            // Target Email Box
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Email in attesa di conferma:",
                                            fontSize = 10.sp,
                                            color = TextMutedDark
                                        )
                                        Text(
                                            text = email,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                    }
                                    Surface(
                                        color = AmberGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "NON VERIFICATA",
                                            color = AmberGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Security Checklist
                            Surface(
                                color = DarkSlateBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "Clicca sul link ricevuto nella tua casella di posta per confermare.",
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "Non trovi l'email? Controlla la cartella Spam, Promozioni o Aggiornamenti.",
                                            fontSize = 11.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                }
                            }

                            // Status / Feedback message
                            if (statusMessage != null) {
                                Surface(
                                    color = if (isErrorMessage) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF064E3B).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isErrorMessage) Color(0xFFEF4444) else EmeraldGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("verification_status_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isErrorMessage) Icons.Default.WarningAmber else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isErrorMessage) Color(0xFFEF4444) else EmeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = statusMessage ?: "",
                                            color = if (isErrorMessage) Color(0xFFFCA5A5) else EmeraldGreen,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Primary Check Verification Button
                            Button(
                                onClick = {
                                    isChecking = true
                                    statusMessage = null
                                    onCheckVerification { verified ->
                                        isChecking = false
                                        if (verified) {
                                            isVerifiedSuccess = true
                                            isErrorMessage = false
                                            statusMessage = "Email verificata con successo!"
                                        } else {
                                            isErrorMessage = true
                                            statusMessage = "L'email non risulta ancora verificata sui server Firebase. Clicca prima sul link ricevuto nell'email e riprova."
                                        }
                                    }
                                },
                                enabled = !isChecking && !isResending,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("check_verification_btn")
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Controllo stato in corso...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ho verificato la mia email (Controlla)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Resend Email Button
                            OutlinedButton(
                                onClick = {
                                    isResending = true
                                    statusMessage = null
                                    onResendVerification { result ->
                                        isResending = false
                                        when (result) {
                                            is AuthResult.Success, is AuthResult.RequiresVerification -> {
                                                resendCooldownSeconds = 30
                                                isErrorMessage = false
                                                statusMessage = "Nuova email di verifica inviata a $email. Controlla la tua posta."
                                                Toast.makeText(context, "Email di verifica inviata!", Toast.LENGTH_SHORT).show()
                                            }
                                            is AuthResult.Error -> {
                                                isErrorMessage = true
                                                statusMessage = result.message
                                            }
                                        }
                                    }
                                },
                                enabled = !isChecking && !isResending && resendCooldownSeconds == 0,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("resend_verification_email_btn")
                            ) {
                                if (isResending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = TextPrimaryDark,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberGold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (resendCooldownSeconds > 0)
                                        "Reinvia email tra ${resendCooldownSeconds}s"
                                    else
                                        "Reinvia email di verifica",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Dev / Simulator Quick-Verify Action (Available for test / dev environment)
                            if (onSimulateVerification != null) {
                                TextButton(
                                    onClick = {
                                        onSimulateVerification()
                                        isVerifiedSuccess = true
                                        isErrorMessage = false
                                        statusMessage = "Email verificata tramite simulatore!"
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("simulate_verify_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Simula click link verifica (Dev/Test Mode)",
                                        color = CyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
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
