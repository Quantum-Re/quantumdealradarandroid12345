package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DistressedProperty
import com.example.ui.theme.*
import com.example.auth.FirebaseAuthManager
import com.example.auth.FirestoreSyncState
import com.example.auth.UserAuthState
import com.example.util.GeminiAiHubService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFeaturesHubDialog(
    selectedProperty: DistressedProperty?,
    allProperties: List<DistressedProperty>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val authState by FirebaseAuthManager.authState.collectAsStateWithLifecycle()
    val syncState by FirebaseAuthManager.syncState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Auth & Cloud, 1: Search & Maps, 2: Voice, 3: Vision & Video, 4: High Thinking & Lite

    var searchQuery by remember { mutableStateOf("Court foreclosure auction calendar & OMI average prices") }
    var searchResultText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    var mapsResultText by remember { mutableStateOf("") }
    var isMapsAnalyzing by remember { mutableStateOf(false) }

    var voiceInputText by remember { mutableStateOf("What is the maximum auction bid strategy for this property?") }
    var voiceResultText by remember { mutableStateOf("") }
    var isVoiceActive by remember { mutableStateOf(false) }

    var visionResultText by remember { mutableStateOf("") }
    var isVisionAnalyzing by remember { mutableStateOf(false) }

    var videoResultText by remember { mutableStateOf("") }
    var isVideoAnalyzing by remember { mutableStateOf(false) }

    var fastLiteResultText by remember { mutableStateOf("") }
    var isFastLiteAnalyzing by remember { mutableStateOf(false) }

    var thinkingResultText by remember { mutableStateOf("") }
    var isThinkingAnalyzing by remember { mutableStateOf(false) }

    val defaultProp = selectedProperty ?: allProperties.firstOrNull() ?: DistressedProperty(
        id = 1,
        address = "Via Monte Napoleone 12, Milan",
        price = 280000.0,
        estimatedValue = 390000.0,
        distressLevel = "Auction",
        status = "Active",
        latitude = 45.4681,
        longitude = 9.1952,
        notes = "CTU Appraisal available. Minor dampness."
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.88f)
            .testTag("dialog_ai_features_hub")
    ) {
        Surface(
            color = DarkSlateBg,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Intelligence Hub",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI & Cloud Intelligence Hub",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Firebase Auth, Firestore, Search/Maps & Gemini 3.1 Suite",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_ai_hub")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMutedDark
                        )
                    }
                }

                Divider(color = SurfaceCardBorder)

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = CyanAccent,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("☁️ Auth & Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_auth_cloud")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🔍 Search & Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_search_maps")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("🎙️ Voice Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_voice")
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("📸 Vision & Video", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_vision_video")
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        text = { Text("🧠 Thinking & Lite", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_thinking_lite")
                    )
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        0 -> AuthAndCloudTab(
                            authState = authState,
                            syncState = syncState,
                            allProperties = allProperties,
                            onSignIn = {
                                coroutineScope.launch {
                                    FirebaseAuthManager.signInWithGoogle(context)
                                }
                            },
                            onSignOut = { FirebaseAuthManager.signOut() },
                            onSyncFirestore = {
                                coroutineScope.launch {
                                    FirebaseAuthManager.syncPropertiesToFirestore(context, allProperties)
                                }
                            }
                        )

                        1 -> SearchAndMapsTab(
                            property = defaultProp,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            searchResult = searchResultText,
                            isSearching = isSearching,
                            onExecuteSearch = {
                                isSearching = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.performSearchGroundedResearch(searchQuery, defaultProp.address)
                                    searchResultText = res.getOrDefault("Search grounding complete.")
                                    isSearching = false
                                }
                            },
                            mapsResult = mapsResultText,
                            isMapsAnalyzing = isMapsAnalyzing,
                            onExecuteMaps = {
                                isMapsAnalyzing = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.performMapsGroundedInspection(
                                        defaultProp.address,
                                        defaultProp.latitude ?: 41.9028,
                                        defaultProp.longitude ?: 12.4964
                                    )
                                    mapsResultText = res.getOrDefault("Maps grounding complete.")
                                    isMapsAnalyzing = false
                                }
                            }
                        )

                        2 -> VoiceAssistantTab(
                            property = defaultProp,
                            voiceInputText = voiceInputText,
                            onVoiceInputChange = { voiceInputText = it },
                            voiceResult = voiceResultText,
                            isVoiceActive = isVoiceActive,
                            onSendVoice = {
                                isVoiceActive = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.sendVoiceAssistantTurn(voiceInputText, defaultProp.address)
                                    voiceResultText = res.getOrDefault("Voice turn complete.")
                                    isVoiceActive = false
                                }
                            }
                        )

                        3 -> VisionAndVideoTab(
                            property = defaultProp,
                            visionResult = visionResultText,
                            isVisionAnalyzing = isVisionAnalyzing,
                            onAnalyzePhoto = {
                                isVisionAnalyzing = true
                                coroutineScope.launch {
                                    // Generate dummy bitmap representing property photo
                                    val bmp = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bmp)
                                    canvas.drawColor(AndroidColor.DKGRAY)
                                    val paint = Paint().apply {
                                        color = AndroidColor.CYAN
                                        textSize = 24f
                                    }
                                    canvas.drawText("Property Facade Photo", 30f, 100f, paint)

                                    val res = GeminiAiHubService.analyzePropertyPhoto(bmp)
                                    visionResultText = res.getOrDefault("Photo analysis completed.")
                                    isVisionAnalyzing = false
                                }
                            },
                            videoResult = videoResultText,
                            isVideoAnalyzing = isVideoAnalyzing,
                            onAnalyzeVideo = {
                                isVideoAnalyzing = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.analyzeWalkthroughVideo(
                                        "Interior video showing 3 bedrooms, water dampness on bathroom ceiling, outdated kitchen plumbing."
                                    )
                                    videoResultText = res.getOrDefault("Video walkthrough analysis completed.")
                                    isVideoAnalyzing = false
                                }
                            }
                        )

                        4 -> ThinkingAndLiteTab(
                            property = defaultProp,
                            fastLiteResult = fastLiteResultText,
                            isFastLiteAnalyzing = isFastLiteAnalyzing,
                            onRunFastLite = {
                                isFastLiteAnalyzing = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.performLowLatencyQuickScreen(defaultProp)
                                    fastLiteResultText = res.getOrDefault("Low-latency screener complete.")
                                    isFastLiteAnalyzing = false
                                }
                            },
                            thinkingResult = thinkingResultText,
                            isThinkingAnalyzing = isThinkingAnalyzing,
                            onRunHighThinking = {
                                isThinkingAnalyzing = true
                                coroutineScope.launch {
                                    val res = GeminiAiHubService.performHighThinkingAnalysis(
                                        defaultProp,
                                        "Evaluate Fix&Flip vs BRRRR exit strategy with 75% LTV refi and capital gains tax impact."
                                    )
                                    thinkingResultText = res.getOrDefault("High thinking analysis complete.")
                                    isThinkingAnalyzing = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Sub-Tab 0: Auth & Cloud
@Composable
private fun AuthAndCloudTab(
    authState: UserAuthState,
    syncState: FirestoreSyncState,
    allProperties: List<DistressedProperty>,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncFirestore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "GOOGLE SIGN-IN & FIREBASE AUTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.8.sp
                    )

                    if (!FirebaseAuthManager.isFirebaseConfigured) {
                        Surface(
                            color = Color(0xFF334155).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Funzione non disponibile: Firebase non è configurato in questa build (modalità simulata)",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    when (authState) {
                        is UserAuthState.SignedOut -> {
                            Text(
                                text = "Sign in with Google to enable secure Firestore sync and backup user deal rosters.",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )

                            Button(
                                onClick = onSignIn,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_google_signin")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Sign In",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Google", fontWeight = FontWeight.Bold)
                            }
                        }

                        is UserAuthState.SigningIn -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Authenticating with Google Identity...", fontSize = 12.sp, color = TextPrimaryDark)
                            }
                        }

                        is UserAuthState.SignedIn -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Authenticated",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(authState.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text(authState.email, fontSize = 11.sp, color = TextMutedDark)
                                    }
                                }

                                OutlinedButton(
                                    onClick = onSignOut,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_google_signout")
                                ) {
                                    Text("Sign Out", fontSize = 11.sp, color = TextSecondaryDark)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CLOUD FIRESTORE PERSISTENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold,
                        letterSpacing = 0.8.sp
                    )

                    if (!FirebaseAuthManager.isFirebaseConfigured) {
                        Surface(
                            color = Color(0xFF334155).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Funzione non disponibile: Firebase non è configurato in questa build",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    Text(
                        text = "Persist ${allProperties.size} tracked properties and room observations to Firebase Cloud Firestore.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    Button(
                        onClick = onSyncFirestore,
                        enabled = authState is UserAuthState.SignedIn && syncState !is FirestoreSyncState.Syncing,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_sync_firestore")
                    ) {
                        if (syncState is FirestoreSyncState.Syncing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing to Firestore...", color = Color.Black)
                        } else {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Sync", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync ${allProperties.size} Deals to Firestore", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (val s = syncState) {
                        is FirestoreSyncState.Synced -> {
                            Text("✅ Synced ${s.count} items to Firestore cloud document store.", fontSize = 11.sp, color = EmeraldGreen)
                        }
                        is FirestoreSyncState.Error -> {
                            Text("ℹ️ Sync status: ${s.message}", fontSize = 11.sp, color = TextMutedDark)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

// Sub-Tab 1: Search & Maps
@Composable
private fun SearchAndMapsTab(
    property: DistressedProperty,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResult: String,
    isSearching: Boolean,
    onExecuteSearch: () -> Unit,
    mapsResult: String,
    isMapsAnalyzing: Boolean,
    onExecuteMaps: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. GOOGLE SEARCH GROUNDED RESEARCH (gemini-3.5-flash)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = { Text("Search Query", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_grounding")
                    )

                    Button(
                        onClick = onExecuteSearch,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_run_search_grounding")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Search Grounding (gemini-3.5-flash)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (searchResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(searchResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. GOOGLE MAPS GROUNDED LOCATION INSPECTOR (gemini-3.5-flash)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )

                    Text("Target Property: ${property.address}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)

                    Button(
                        onClick = onExecuteMaps,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_run_maps_grounding")
                    ) {
                        if (isMapsAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Maps Location Grounding", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (mapsResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(mapsResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// Sub-Tab 2: Voice Assistant
@Composable
private fun VoiceAssistantTab(
    property: DistressedProperty,
    voiceInputText: String,
    onVoiceInputChange: (String) -> Unit,
    voiceResult: String,
    isVoiceActive: Boolean,
    onSendVoice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "LIVE VOICE CONVERSATION ASSISTANT (gemini-3.1-flash-live-preview)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseRed
                )

                Text("Context: ${property.address} (€${property.price.toInt()})", fontSize = 11.sp, color = TextMutedDark)

                OutlinedTextField(
                    value = voiceInputText,
                    onValueChange = onVoiceInputChange,
                    label = { Text("Spoken Voice Message Simulation", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_voice_message")
                )

                Button(
                    onClick = onSendVoice,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_send_voice_turn")
                ) {
                    if (isVoiceActive) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Voice Turn to Live API", fontWeight = FontWeight.Bold)
                    }
                }

                if (voiceResult.isNotBlank()) {
                    Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(voiceResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}

// Sub-Tab 3: Vision & Video
@Composable
private fun VisionAndVideoTab(
    property: DistressedProperty,
    visionResult: String,
    isVisionAnalyzing: Boolean,
    onAnalyzePhoto: () -> Unit,
    videoResult: String,
    isVideoAnalyzing: Boolean,
    onAnalyzeVideo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PHOTO DAMAGE & REHAB ANALYZER (gemini-3.1-pro-preview)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPurpleOnContainer
                    )

                    Button(
                        onClick = onAnalyzePhoto,
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_analyze_photo_pro")
                    ) {
                        if (isVisionAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze Property Photo with Gemini Pro", fontWeight = FontWeight.Bold, color = BentoPurpleOnContainer)
                        }
                    }

                    if (visionResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(visionResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VIDEO WALKTHROUGH INSPECTION (gemini-3.1-pro-preview)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )

                    Button(
                        onClick = onAnalyzeVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_analyze_video_pro")
                    ) {
                        if (isVideoAnalyzing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze Video Walkthrough Footage", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    if (videoResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(videoResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// Sub-Tab 4: Thinking & Lite
@Composable
private fun ThinkingAndLiteTab(
    property: DistressedProperty,
    fastLiteResult: String,
    isFastLiteAnalyzing: Boolean,
    onRunFastLite: () -> Unit,
    thinkingResult: String,
    isThinkingAnalyzing: Boolean,
    onRunHighThinking: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SUB-SECOND QUICK DEAL SCREENER (gemini-3.1-flash-lite)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )

                    Button(
                        onClick = onRunFastLite,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_run_flash_lite")
                    ) {
                        if (isFastLiteAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sub-Second Triage (gemini-3.1-flash-lite)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (fastLiteResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(fastLiteResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DEEP HIGH THINKING MODE (gemini-3.1-pro-preview, thinkingLevel = HIGH)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseRed
                    )

                    Button(
                        onClick = onRunHighThinking,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_run_high_thinking")
                    ) {
                        if (isThinkingAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Deep Multi-Scenario Thinking Mode", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (thinkingResult.isNotBlank()) {
                        Surface(color = DarkSlateBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(thinkingResult, fontSize = 12.sp, color = TextPrimaryDark, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}
