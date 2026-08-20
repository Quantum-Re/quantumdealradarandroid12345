package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FcmPushAlert
import com.example.data.FcmPushType
import com.example.data.FcmTopicItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FcmPushManager {
    private const val TAG = "FcmPushManager"

    // Preferences & Keys
    private const val PREFS_NAME = "fcm_push_preferences_v1"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"
    private const val KEY_FCM_TOKEN_TIMESTAMP = "fcm_token_timestamp"
    private const val KEY_SUBSCRIBED_TOPICS = "fcm_subscribed_topics_set"
    private const val KEY_PUSH_HISTORY = "fcm_push_history_json"
    private const val KEY_MASTER_PUSH_ENABLED = "fcm_master_push_enabled"

    // Channels
    const val CHANNEL_PRICE_DROPS = "fcm_price_drops_channel_v1"
    const val CHANNEL_STATUS_CHANGES = "fcm_status_changes_channel_v1"
    const val CHANNEL_GRAVE_DANCER = "fcm_distress_channel_v1"
    const val CHANNEL_BRIEF_MATCH = "fcm_brief_match_channel_v1"
    const val CHANNEL_GENERAL = "fcm_deal_alerts_channel_v1"

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    // Default Topics
    val DEFAULT_TOPICS = listOf(
        FcmTopicItem(
            topicId = "price_drops",
            title = "⚡ Ribassi di Prezzo Immediati",
            subtitle = "Sconti d'asta & tagli di prezzo > 5%",
            description = "Ricevi notifiche push istantanee non appena un immobile subisce una riduzione di prezzo significativa o entra in sconto d'asta.",
            isSubscribed = true,
            channelId = CHANNEL_PRICE_DROPS
        ),
        FcmTopicItem(
            topicId = "status_changes",
            title = "⚖️ Cambi Stato & Aste Deserte",
            subtitle = "Aste deserte, aggiudicazioni e nuovi incanti",
            description = "Avviso in tempo reale quando un'asta va deserta (predisponendo il ribasso del 25%) o quando lo stato procedurale si aggiorna.",
            isSubscribed = true,
            channelId = CHANNEL_STATUS_CHANGES
        ),
        FcmTopicItem(
            topicId = "grave_dancer_distress",
            title = "🔥 Grave Dancer™ Distress Intelligence",
            subtitle = "Opportunità estreme & venditori motivati",
            description = "Segnalazione prioritaria per immobili con oltre il 35% di sconto rispetto ai benchmark OMI e indicatori di distress procedurale.",
            isSubscribed = true,
            channelId = CHANNEL_GRAVE_DANCER
        ),
        FcmTopicItem(
            topicId = "high_yield_roi",
            title = "💎 Opportunità High Yield (ROI > 12%)",
            subtitle = "Net Cap Rate elevato & Score > 90",
            description = "Filtraggio in tempo reale per opportunità ad altissima redditività locativa o plusvalenza Fix & Flip certificata.",
            isSubscribed = true,
            channelId = CHANNEL_PRICE_DROPS
        ),
        FcmTopicItem(
            topicId = "deals_all",
            title = "📡 Tutte le Nuove Aste Telematiche PVP",
            subtitle = "Feed completo annunci e bandi PVP",
            description = "Ricevi un ping istantaneo per ogni nuova pubblicazione sul Portale Vendite Pubbliche e circuiti fallimentari.",
            isSubscribed = false,
            channelId = CHANNEL_GENERAL
        )
    )

    /**
     * Initializes notification channels for FCM.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_PRICE_DROPS,
                    "⚡ FCM - Ribassi Prezzo Immediati",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifiche push in tempo reale per riduzioni di prezzo e sconti d'asta su immobili"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_STATUS_CHANGES,
                    "⚖️ FCM - Cambi Stato & Aste Deserte",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifiche push in tempo reale per aste deserte, aggiudicazioni e variazioni procedurali"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_GRAVE_DANCER,
                    "🔥 FCM - Grave Dancer™ Distress",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Allarmi di massima urgenza per asimmetrie di prezzo, venditori motivati e ribassi successivi"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_BRIEF_MATCH,
                    "🎯 FCM - Match Investor Brief",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifiche per deal perfettamente allineati ai parametri dell'Investor Brief personale"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "📡 FCM - Quantum Radar Feed",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifiche generali e aggiornamenti di mercato Quantum Deal Radar"
                    enableVibration(true)
                    setShowBadge(true)
                }
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
            Log.d(TAG, "Initialized all FCM notification channels")
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+)
     */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // =========================================================
    // TOKEN MANAGEMENT & FIREBASE SAFETY
    // =========================================================

    /**
     * Checks if Firebase is initialized and available in the current runtime.
     */
    fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext ?: context) != null
            } else {
                true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase unavailable or not configured: ${e.message}")
            false
        }
    }

    /**
     * Retrieves the current cached or fresh FCM token safely.
     */
    fun fetchToken(context: Context, onComplete: ((String?) -> Unit)? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_FCM_TOKEN, null)

        if (!isFirebaseAvailable(context)) {
            Log.d(TAG, "Firebase not initialized, using local fallback cached token: $cached")
            onComplete?.invoke(cached)
            return
        }

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "Fetched FCM Registration Token: $token")
                    saveTokenLocally(context, token)
                    syncTokenToFirestore(context, token)
                    onComplete?.invoke(token)
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    onComplete?.invoke(cached)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error accessing FirebaseMessaging: ${e.message}")
            onComplete?.invoke(cached)
        }
    }

    fun getCachedToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun onNewToken(context: Context, token: String) {
        Log.d(TAG, "FCM onNewToken called with: $token")
        saveTokenLocally(context, token)
        syncTokenToFirestore(context, token)
        resubscribeStoredTopics(context)
    }

    private fun saveTokenLocally(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .putLong(KEY_FCM_TOKEN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    private fun syncTokenToFirestore(context: Context, token: String) {
        if (!isFirebaseAvailable(context)) return
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val db = FirebaseFirestore.getInstance()
            val tokenData = mapOf(
                "fcmToken" to token,
                "updatedAt" to System.currentTimeMillis(),
                "devicePlatform" to "ANDROID",
                "appVersion" to "1.0-quantum"
            )
            db.collection("users").document(user.uid)
                .set(mapOf("fcmTokens" to mapOf(token.hashCode().toString() to tokenData)), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Synced FCM token to Firestore user ${user.uid}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed syncing FCM token to Firestore", e)
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore sync skipped: ${e.message}")
        }
    }

    // =========================================================
    // TOPIC SUBSCRIPTIONS
    // =========================================================

    fun loadSubscribedTopics(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultSet = setOf("price_drops", "status_changes", "grave_dancer_distress", "high_yield_roi")
        return prefs.getStringSet(KEY_SUBSCRIBED_TOPICS, defaultSet) ?: defaultSet
    }

    fun setTopicSubscribed(context: Context, topicId: String, isSubscribed: Boolean) {
        val currentSet = loadSubscribedTopics(context).toMutableSet()
        val firebaseReady = isFirebaseAvailable(context)
        if (isSubscribed) {
            currentSet.add(topicId)
            if (firebaseReady) {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(topicId)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) Log.d(TAG, "Subscribed to FCM topic: $topicId")
                        }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to subscribe to topic: $topicId", e)
                }
            }
        } else {
            currentSet.remove(topicId)
            if (firebaseReady) {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topicId)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) Log.d(TAG, "Unsubscribed from FCM topic: $topicId")
                        }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to unsubscribe from topic: $topicId", e)
                }
            }
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SUBSCRIBED_TOPICS, currentSet).apply()
    }

    fun resubscribeStoredTopics(context: Context) {
        if (!isFirebaseAvailable(context)) return
        val topics = loadSubscribedTopics(context)
        topics.forEach { topic ->
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
            } catch (e: Throwable) {
                Log.w(TAG, "Resubscribe failed for $topic: ${e.message}")
            }
        }
    }

    fun isMasterPushEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MASTER_PUSH_ENABLED, true)
    }

    fun setMasterPushEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MASTER_PUSH_ENABLED, enabled).apply()
    }

    // =========================================================
    // REMOTE MESSAGE HANDLING & NOTIFICATION BUILDING
    // =========================================================

    /**
     * Handles incoming FCM RemoteMessage payloads from the background service.
     */
    fun handleRemoteMessage(context: Context, remoteMessage: RemoteMessage) {
        Log.d(TAG, "Received FCM RemoteMessage from: ${remoteMessage.from}")

        if (!isMasterPushEnabled(context)) {
            Log.d(TAG, "FCM push ignored: Master push disabled by user")
            return
        }

        ensureChannels(context)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val typeStr = data["type"] ?: data["alert_type"] ?: "PRICE_DROP"
        val type = parsePushType(typeStr)

        val dealId = data["deal_id"]?.toLongOrNull() ?: data["dealId"]?.toLongOrNull()
        val propertyTitle = data["property_title"] ?: data["title"] ?: notification?.title ?: "Opportunità Quantum Radar"
        val rawBody = data["body"] ?: data["message"] ?: notification?.body ?: "Nuovo aggiornamento disponibile"
        val address = data["address"] ?: ""
        val city = data["city"] ?: ""
        val oldPrice = data["old_price"]?.toDoubleOrNull() ?: data["oldPrice"]?.toDoubleOrNull()
        val newPrice = data["new_price"]?.toDoubleOrNull() ?: data["newPrice"]?.toDoubleOrNull()
        val discountPercent = data["discount_percent"]?.toDoubleOrNull() ?: data["discount"]?.toDoubleOrNull()
        val oldStatus = data["old_status"]
        val newStatus = data["new_status"]
        val deepLink = data["deep_link"]

        val title = when (type) {
            FcmPushType.PRICE_DROP -> "⚡ Ribasso di Prezzo: -$discountPercent%!"
            FcmPushType.STATUS_CHANGE -> "⚖️ Aggiornamento Procedura Asta"
            FcmPushType.GRAVE_DANCER_DISTRESS -> "🔥 Allarme Grave Dancer™ Distress"
            FcmPushType.NEW_AUCTION -> "🏛️ Nuovo Bando Asta PVP Disponibile"
            FcmPushType.BRIEF_MATCH -> "🎯 Match Perfetto con il tuo Brief"
            FcmPushType.SYSTEM_ANNOUNCEMENT -> "📡 Aggiornamento Quantum Radar"
        }.let { defaultTitle ->
            notification?.title ?: data["custom_title"] ?: defaultTitle
        }

        val alert = FcmPushAlert(
            id = remoteMessage.messageId ?: UUID.randomUUID().toString(),
            dealId = dealId,
            type = type,
            title = title,
            body = rawBody,
            propertyTitle = propertyTitle,
            address = address,
            city = city,
            oldPrice = oldPrice,
            newPrice = newPrice,
            discountPercent = discountPercent,
            oldStatus = oldStatus,
            newStatus = newStatus,
            receivedTimestamp = System.currentTimeMillis(),
            deepLink = deepLink
        )

        // Save to push history
        savePushAlertToHistory(context, alert)

        // Show system notification
        dispatchSystemNotification(context, alert)
    }

    /**
     * Dispatches the Android system notification with rich BigTextStyle and Action Intents.
     */
    fun dispatchSystemNotification(context: Context, alert: FcmPushAlert) {
        if (!hasPermission(context)) {
            Log.w(TAG, "Cannot dispatch notification: POST_NOTIFICATIONS permission missing")
            return
        }

        val channelId = when (alert.type) {
            FcmPushType.PRICE_DROP -> CHANNEL_PRICE_DROPS
            FcmPushType.STATUS_CHANGE -> CHANNEL_STATUS_CHANGES
            FcmPushType.GRAVE_DANCER_DISTRESS -> CHANNEL_GRAVE_DANCER
            FcmPushType.BRIEF_MATCH -> CHANNEL_BRIEF_MATCH
            else -> CHANNEL_GENERAL
        }

        val notificationId = (alert.dealId?.toInt() ?: 0) + alert.type.ordinal * 1000 + (System.currentTimeMillis() % 1000).toInt()

        // Main Intent: open MainActivity and target deal
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "RADAR_FEED")
            alert.dealId?.let { putExtra("DEAL_ID", it) }
            putExtra("FCM_ALERT_ID", alert.id)
            putExtra("FCM_ALERT_TYPE", alert.type.name)
        }

        val pendingMainIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intent: Open Financial Simulator
        val roiIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "ROI_CALCULATOR")
            alert.dealId?.let { putExtra("DEAL_ID", it) }
        }

        val pendingRoiIntent = PendingIntent.getActivity(
            context,
            notificationId + 10000,
            roiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build rich description text
        val bigTextBuilder = StringBuilder()
        if (alert.propertyTitle.isNotBlank()) {
            bigTextBuilder.append("📍 ${alert.propertyTitle}\n")
        }
        if (alert.address.isNotBlank() || alert.city.isNotBlank()) {
            bigTextBuilder.append("🗺️ ${listOf(alert.address, alert.city).filter { it.isNotBlank() }.joinToString(", ")}\n")
        }

        if (alert.oldPrice != null && alert.newPrice != null) {
            val oldFmt = currencyFormat.format(alert.oldPrice)
            val newFmt = currencyFormat.format(alert.newPrice)
            val dropPct = alert.discountPercent ?: ((alert.oldPrice - alert.newPrice) / alert.oldPrice * 100.0)
            bigTextBuilder.append("💰 Prezzo: $oldFmt ➔ $newFmt (-${String.format(Locale.US, "%.1f", dropPct)}%)\n")
        } else if (alert.newPrice != null) {
            bigTextBuilder.append("💰 Prezzo Attuale: ${currencyFormat.format(alert.newPrice)}\n")
        }

        if (alert.newStatus != null) {
            bigTextBuilder.append("⚖️ Stato Procedura: ${alert.newStatus}\n")
        }

        bigTextBuilder.append("\n${alert.body}")

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(if (alert.propertyTitle.isNotBlank()) "${alert.propertyTitle} - ${alert.body}" else alert.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigTextBuilder.toString())
                    .setBigContentTitle(alert.title)
                    .setSummaryText(when (alert.type) {
                        FcmPushType.PRICE_DROP -> "⚡ Alert Prezzo FCM"
                        FcmPushType.STATUS_CHANGE -> "⚖️ Alert Aste FCM"
                        FcmPushType.GRAVE_DANCER_DISTRESS -> "🔥 Distress FCM"
                        FcmPushType.BRIEF_MATCH -> "🎯 Brief Match FCM"
                        else -> "📡 Quantum Radar FCM"
                    })
            )
            .setColor(ContextCompat.getColor(context, R.color.cyan_accent))
            .setContentIntent(pendingMainIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .addAction(
                android.R.drawable.ic_menu_view,
                "Visualizza Scheda",
                pendingMainIntent
            )

        if (alert.dealId != null) {
            builder.addAction(
                android.R.drawable.ic_menu_agenda,
                "Simula ROI",
                pendingRoiIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(TAG, "Dispatched notification #$notificationId for deal ${alert.dealId}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while dispatching notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching notification", e)
        }
    }

    // =========================================================
    // LOCAL SIMULATION / TEST DISPATCHER
    // =========================================================

    /**
     * Simulates an incoming FCM push notification locally for testing and verification.
     */
    fun simulatePushAlert(
        context: Context,
        type: FcmPushType,
        dealId: Long? = 101L,
        propertyTitle: String = "Quadrilocale Panoramico con Terrazzo",
        address: String = "Via Tortona 35",
        city: String = "Milano",
        oldPrice: Double = 320000.0,
        newPrice: Double = 240000.0,
        oldStatus: String = "In Corso",
        newStatus: String = "2° Incanto Deserto - Ribasso -25%",
        customNote: String = "Algoritmo Quantum Radar: Sottocosto del 38% rispetto al valore medio di zona OMI (€4.200/mq)."
    ): FcmPushAlert {
        ensureChannels(context)

        val discountPercent = if (oldPrice > 0) ((oldPrice - newPrice) / oldPrice * 100.0) else 25.0

        val (title, body) = when (type) {
            FcmPushType.PRICE_DROP -> Pair(
                "⚡ Ribasso Immediato: -$discountPercent% su $propertyTitle",
                "Prezzo ribassato da ${currencyFormat.format(oldPrice)} a ${currencyFormat.format(newPrice)}. $customNote"
            )
            FcmPushType.STATUS_CHANGE -> Pair(
                "⚖️ Asta Deserta Rilevata: $propertyTitle",
                "Il 2° incanto è andato deserto. Prezzo base ribassato per il prossimo round a ${currencyFormat.format(newPrice)}."
            )
            FcmPushType.GRAVE_DANCER_DISTRESS -> Pair(
                "🔥 Grave Dancer™ Allarme Distress: $city",
                "Individuata forte asimmetria di prezzo (-$discountPercent%). Margine operativo stimato: +€ 85.000."
            )
            FcmPushType.BRIEF_MATCH -> Pair(
                "🎯 Match Investor Brief: Nuovo Deal a $city",
                "Nuovo immobile idoneo con Cap Rate stimato al 9.4% e prezzo ${currencyFormat.format(newPrice)}."
            )
            FcmPushType.NEW_AUCTION -> Pair(
                "🏛️ Nuovo Bando Telematico PVP: $city",
                "Pubblicata nuova esecuzione immobiliare in $address, $city. Base d'asta: ${currencyFormat.format(newPrice)}."
            )
            FcmPushType.SYSTEM_ANNOUNCEMENT -> Pair(
                "📡 Quantum Radar: Scansione Mercato Completata",
                "Aggiornati 48 benchmark OMI e 12 nuovi bandi d'asta telematici."
            )
        }

        val alert = FcmPushAlert(
            id = "sim_" + System.currentTimeMillis(),
            dealId = dealId,
            type = type,
            title = title,
            body = body,
            propertyTitle = propertyTitle,
            address = address,
            city = city,
            oldPrice = oldPrice,
            newPrice = newPrice,
            discountPercent = discountPercent,
            oldStatus = oldStatus,
            newStatus = newStatus,
            receivedTimestamp = System.currentTimeMillis()
        )

        savePushAlertToHistory(context, alert)
        dispatchSystemNotification(context, alert)
        return alert
    }

    // =========================================================
    // HISTORY PERSISTENCE
    // =========================================================

    fun loadPushHistory(context: Context): List<FcmPushAlert> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PUSH_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<FcmPushAlert>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", FcmPushType.PRICE_DROP.name)
                val type = parsePushType(typeStr)

                list.add(
                    FcmPushAlert(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        dealId = if (obj.has("dealId")) obj.getLong("dealId") else null,
                        type = type,
                        title = obj.optString("title", ""),
                        body = obj.optString("body", ""),
                        propertyTitle = obj.optString("propertyTitle", ""),
                        address = obj.optString("address", ""),
                        city = obj.optString("city", ""),
                        oldPrice = if (obj.has("oldPrice")) obj.getDouble("oldPrice") else null,
                        newPrice = if (obj.has("newPrice")) obj.getDouble("newPrice") else null,
                        discountPercent = if (obj.has("discountPercent")) obj.getDouble("discountPercent") else null,
                        oldStatus = if (obj.has("oldStatus")) obj.getString("oldStatus") else null,
                        newStatus = if (obj.has("newStatus")) obj.getString("newStatus") else null,
                        receivedTimestamp = obj.optLong("receivedTimestamp", System.currentTimeMillis()),
                        deepLink = if (obj.has("deepLink")) obj.getString("deepLink") else null,
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading push history", e)
        }
        return list.sortedByDescending { it.receivedTimestamp }
    }

    fun savePushAlertToHistory(context: Context, alert: FcmPushAlert) {
        val current = loadPushHistory(context).toMutableList()
        current.removeAll { it.id == alert.id }
        current.add(0, alert)
        val trimmed = current.take(50) // Keep latest 50 alerts

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val array = JSONArray()
            trimmed.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    item.dealId?.let { put("dealId", it) }
                    put("type", item.type.name)
                    put("title", item.title)
                    put("body", item.body)
                    put("propertyTitle", item.propertyTitle)
                    put("address", item.address)
                    put("city", item.city)
                    item.oldPrice?.let { put("oldPrice", it) }
                    item.newPrice?.let { put("newPrice", it) }
                    item.discountPercent?.let { put("discountPercent", it) }
                    item.oldStatus?.let { put("oldStatus", it) }
                    item.newStatus?.let { put("newStatus", it) }
                    put("receivedTimestamp", item.receivedTimestamp)
                    item.deepLink?.let { put("deepLink", it) }
                    put("isRead", item.isRead)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_PUSH_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving push history", e)
        }
    }

    fun markPushAsRead(context: Context, alertId: String) {
        val current = loadPushHistory(context).map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val array = JSONArray()
            current.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    item.dealId?.let { put("dealId", it) }
                    put("type", item.type.name)
                    put("title", item.title)
                    put("body", item.body)
                    put("propertyTitle", item.propertyTitle)
                    put("address", item.address)
                    put("city", item.city)
                    item.oldPrice?.let { put("oldPrice", it) }
                    item.newPrice?.let { put("newPrice", it) }
                    item.discountPercent?.let { put("discountPercent", it) }
                    item.oldStatus?.let { put("oldStatus", it) }
                    item.newStatus?.let { put("newStatus", it) }
                    put("receivedTimestamp", item.receivedTimestamp)
                    item.deepLink?.let { put("deepLink", it) }
                    put("isRead", item.isRead)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_PUSH_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking push as read", e)
        }
    }

    fun clearPushHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PUSH_HISTORY).apply()
    }

    private fun parsePushType(typeStr: String): FcmPushType {
        return try {
            FcmPushType.valueOf(typeStr.uppercase())
        } catch (e: Exception) {
            when {
                typeStr.contains("price", ignoreCase = true) || typeStr.contains("drop", ignoreCase = true) -> FcmPushType.PRICE_DROP
                typeStr.contains("status", ignoreCase = true) || typeStr.contains("deserta", ignoreCase = true) -> FcmPushType.STATUS_CHANGE
                typeStr.contains("distress", ignoreCase = true) || typeStr.contains("grave", ignoreCase = true) -> FcmPushType.GRAVE_DANCER_DISTRESS
                typeStr.contains("brief", ignoreCase = true) || typeStr.contains("match", ignoreCase = true) -> FcmPushType.BRIEF_MATCH
                typeStr.contains("new", ignoreCase = true) || typeStr.contains("auction", ignoreCase = true) -> FcmPushType.NEW_AUCTION
                else -> FcmPushType.PRICE_DROP
            }
        }
    }
}
