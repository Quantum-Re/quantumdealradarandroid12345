package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AuthResult
import com.example.ui.theme.*

@Composable
fun ForgotPasswordDialog(
    initialEmail: String = "",
    isFirebaseConfigured: Boolean = true,
    onDismiss: () -> Unit,
    onSendResetEmail: (email: String, onResult: (AuthResult) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail.trim()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var confirmedEmail by remember { mutableStateOf("") }

    val isEmailValid = remember(email) {
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .testTag("forgot_password_dialog"),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon & Title
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSuccess) Color(0xFF064E3B) else Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.MarkEmailRead else Icons.Default.LockReset,
                                contentDescription = null,
                                tint = if (isSuccess) EmeraldGreen else AmberGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isSuccess) "Email Inviata" else "Recupero Password",
                                color = TextPrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firebase Authentication Security",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("cancel_reset_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                AnimatedContent(
                    targetState = isSuccess,
                    transitionSpec = {
                        (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                    },
                    label = "forgot_password_content_transition"
                ) { success ->
                    if (success) {
                        // Success Confirmation View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color(0xFF064E3B).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Link di reset inviato!",
                                            color = EmeraldGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "Abbiamo trasmesso un'email protetta con il link per reimpostare la tua password all'indirizzo:",
                                        color = TextPrimaryDark,
                                        fontSize = 12.sp
                                    )

                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = confirmedEmail,
                                            color = CyanAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Info Security Tips
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Il link di sicurezza scade automaticamente entro 60 minuti.",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Il tuo piano e lo stato dei token Investor PRO rimarranno intatti.",
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
                                        text = "Se non visualizzi l'email, controlla anche la cartella Spam o Promozioni.",
                                        fontSize = 11.sp,
                                        color = TextMutedDark
                                    )
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_success_dismiss_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Torna al Login", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Email Input & Trigger View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Inserisci l'indirizzo email associato al tuo profilo per ricevere le istruzioni di ripristino dell'accesso.",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            // Firebase Status Badge
                            Surface(
                                color = if (isFirebaseConfigured) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isFirebaseConfigured) EmeraldGreen.copy(alpha = 0.4f) else SurfaceCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isFirebaseConfigured) EmeraldGreen else AmberGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isFirebaseConfigured)
                                            "Firebase Auth Live: Invio email con crittografia TLS"
                                        else
                                            "Simulatore Locale: Invio token di ripristino simulato",
                                        fontSize = 11.sp,
                                        color = if (isFirebaseConfigured) EmeraldGreen else AmberGold,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Email Input Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email Registrata", color = TextSecondaryDark) },
                                placeholder = { Text("es. investitore@fondo.it", color = TextMutedDark) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = AmberGold)
                                },
                                trailingIcon = {
                                    if (email.isNotEmpty()) {
                                        IconButton(onClick = { email = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextMutedDark)
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = errorMessage != null || (email.isNotEmpty() && !isEmailValid),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (isEmailValid && !isLoading) {
                                            isLoading = true
                                            errorMessage = null
                                            onSendResetEmail(email.trim()) { result ->
                                                isLoading = false
                                                when (result) {
                                                    is AuthResult.Success, is AuthResult.RequiresVerification -> {
                                                        confirmedEmail = email.trim()
                                                        isSuccess = true
                                                    }
                                                    is AuthResult.Error -> {
                                                        errorMessage = result.message
                                                    }
                                                }
                                            }
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSlateBg,
                                    unfocusedContainerColor = DarkSlateBg,
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = SurfaceCardBorder,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_email_input")
                            )

                            // Error message banner
                            if (errorMessage != null) {
                                Surface(
                                    color = Color(0xFF7F1D1D).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_error_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SurfaceCardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(48.dp)
                                ) {
                                    Text("Annulla", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        if (isEmailValid && !isLoading) {
                                            isLoading = true
                                            errorMessage = null
                                            onSendResetEmail(email.trim()) { result ->
                                                isLoading = false
                                                when (result) {
                                                    is AuthResult.Success, is AuthResult.RequiresVerification -> {
                                                        confirmedEmail = email.trim()
                                                        isSuccess = true
                                                    }
                                                    is AuthResult.Error -> {
                                                        errorMessage = result.message
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = isEmailValid && !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberGold,
                                        contentColor = Color.Black,
                                        disabledContainerColor = AmberGold.copy(alpha = 0.3f),
                                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .testTag("send_reset_link_btn")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Invia Link", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
