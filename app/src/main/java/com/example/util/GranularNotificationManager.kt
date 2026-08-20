package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object GranularNotificationManager {
    private const val TAG = "GranularNotificationMgr"

    const val CHANNEL_GRANULAR_ALERTS_ID = "granular_property_price_alerts_v2"
    const val CHANNEL_GRANULAR_ALERTS_NAME = "Allarmi Prezzo e Soglie Personalizzate"
    const val CHANNEL_GRANULAR_ALERTS_DESC = "Notifiche per ribassi percentuali, soglie prezzo target e allarmi asta su singoli immobili tracciati"

    private const val PREFS_NAME = "granular_notification_prefs_v2"
    private const val KEY_GLOBAL_SETTINGS = "global_settings_json"
    private const val KEY_PROPERTY_ALERTS_MAP = "property_alerts_map_json"
    private const val KEY_ALERT_HISTORY = "granular_alert_history_json"

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(CHANNEL_GRANULAR_ALERTS_ID)
            if (existing == null) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_GRANULAR_ALERTS_ID, CHANNEL_GRANULAR_ALERTS_NAME, importance).apply {
                    description = CHANNEL_GRANULAR_ALERTS_DESC
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Created notification channel: $CHANNEL_GRANULAR_ALERTS_ID")
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ==========================================
    // PREFERENCES: Global Settings
    // ==========================================

    fun loadGlobalSettings(context: Context): GlobalNotificationSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_GLOBAL_SETTINGS, null) ?: return GlobalNotificationSettings()
        return try {
            val json = JSONObject(jsonStr)
            GlobalNotificationSettings(
                masterPushEnabled = json.optBoolean("masterPushEnabled", true),
                soundAndVibrationEnabled = json.optBoolean("soundAndVibrationEnabled", true),
                headsUpHighPriorityEnabled = json.optBoolean("headsUpHighPriorityEnabled", true),
                autoAuctionDesertaAlerts = json.optBoolean("autoAuctionDesertaAlerts", true),
                defaultPriceDropPercent = json.optDouble("defaultPriceDropPercent", 10.0),
                defaultMinAbsoluteDropEuros = json.optDouble("defaultMinAbsoluteDropEuros", 5000.0),
                quietHoursEnabled = json.optBoolean("quietHoursEnabled", false),
                quietHoursStartHour = json.optInt("quietHoursStartHour", 22),
                quietHoursEndHour = json.optInt("quietHoursEndHour", 7)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading global settings", e)
            GlobalNotificationSettings()
        }
    }

    fun saveGlobalSettings(context: Context, settings: GlobalNotificationSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val json = JSONObject().apply {
                put("masterPushEnabled", settings.masterPushEnabled)
                put("soundAndVibrationEnabled", settings.soundAndVibrationEnabled)
                put("headsUpHighPriorityEnabled", settings.headsUpHighPriorityEnabled)
                put("autoAuctionDesertaAlerts", settings.autoAuctionDesertaAlerts)
                put("defaultPriceDropPercent", settings.defaultPriceDropPercent)
                put("defaultMinAbsoluteDropEuros", settings.defaultMinAbsoluteDropEuros)
                put("quietHoursEnabled", settings.quietHoursEnabled)
                put("quietHoursStartHour", settings.quietHoursStartHour)
                put("quietHoursEndHour", settings.quietHoursEndHour)
            }
            prefs.edit().putString(KEY_GLOBAL_SETTINGS, json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving global settings", e)
        }
    }

    // ==========================================
    // PREFERENCES: Property Alerts Map
    // ==========================================

    fun loadPropertyAlerts(context: Context): Map<Long, GranularPropertyAlert> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PROPERTY_ALERTS_MAP, null) ?: return emptyMap()
        val result = mutableMapOf<Long, GranularPropertyAlert>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dealId = obj.getLong("dealId")
                val triggerKey = obj.optString("triggerMode", AlertTriggerMode.PERCENTAGE_DROP.key)
                val mode = AlertTriggerMode.values().find { it.key == triggerKey } ?: AlertTriggerMode.PERCENTAGE_DROP

                val freqKey = obj.optString("alertFrequency", AlertFrequencyPreference.INSTANT_PUSH.key)
                val freq = AlertFrequencyPreference.values().find { it.key == freqKey } ?: AlertFrequencyPreference.INSTANT_PUSH

                val alert = GranularPropertyAlert(
                    dealId = dealId,
                    dealTitle = obj.optString("dealTitle", "Immobile #$dealId"),
                    dealLocation = obj.optString("dealLocation", ""),
                    currentAskingPrice = obj.optDouble("currentAskingPrice", 100000.0),
                    originalAskingPrice = obj.optDouble("originalAskingPrice", 100000.0),
                    estimatedMarketValue = obj.optDouble("estimatedMarketValue", 120000.0),
                    propertyType = obj.optString("propertyType", "Residenziale"),
                    imageUrl = obj.optString("imageUrl", ""),
                    isAlertEnabled = obj.optBoolean("isAlertEnabled", true),
                    triggerMode = mode,
                    dropPercentThreshold = obj.optDouble("dropPercentThreshold", 10.0),
                    targetPriceThreshold = obj.optDouble("targetPriceThreshold", 90000.0),
                    minCapRateThreshold = obj.optDouble("minCapRateThreshold", 8.0),
                    targetDiscountOmiThreshold = obj.optInt("targetDiscountOmiThreshold", 30),
                    isHighPriority = obj.optBoolean("isHighPriority", true),
                    notifyOnAuctionDeserta = obj.optBoolean("notifyOnAuctionDeserta", true),
                    alertFrequency = freq,
                    customNotes = obj.optString("customNotes", ""),
                    totalAlertsTriggered = obj.optInt("totalAlertsTriggered", 0),
                    lastTriggeredAt = if (obj.has("lastTriggeredAt")) obj.getLong("lastTriggeredAt") else null,
                    lastTriggeredPrice = if (obj.has("lastTriggeredPrice")) obj.getDouble("lastTriggeredPrice") else null,
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                result[dealId] = alert
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property alerts map", e)
        }
        return result
    }

    fun savePropertyAlert(context: Context, alert: GranularPropertyAlert) {
        val map = loadPropertyAlerts(context).toMutableMap()
        map[alert.dealId] = alert
        saveAllPropertyAlerts(context, map)
    }

    fun saveAllPropertyAlerts(context: Context, map: Map<Long, GranularPropertyAlert>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            map.values.forEach { alert ->
                val obj = JSONObject().apply {
                    put("dealId", alert.dealId)
                    put("dealTitle", alert.dealTitle)
                    put("dealLocation", alert.dealLocation)
                    put("currentAskingPrice", alert.currentAskingPrice)
                    put("originalAskingPrice", alert.originalAskingPrice)
                    put("estimatedMarketValue", alert.estimatedMarketValue)
                    put("propertyType", alert.propertyType)
                    put("imageUrl", alert.imageUrl)
                    put("isAlertEnabled", alert.isAlertEnabled)
                    put("triggerMode", alert.triggerMode.key)
                    put("dropPercentThreshold", alert.dropPercentThreshold)
                    put("targetPriceThreshold", alert.targetPriceThreshold)
                    put("minCapRateThreshold", alert.minCapRateThreshold)
                    put("targetDiscountOmiThreshold", alert.targetDiscountOmiThreshold)
                    put("isHighPriority", alert.isHighPriority)
                    put("notifyOnAuctionDeserta", alert.notifyOnAuctionDeserta)
                    put("alertFrequency", alert.alertFrequency.key)
                    put("customNotes", alert.customNotes)
                    put("totalAlertsTriggered", alert.totalAlertsTriggered)
                    alert.lastTriggeredAt?.let { put("lastTriggeredAt", it) }
                    alert.lastTriggeredPrice?.let { put("lastTriggeredPrice", it) }
                    put("updatedAt", alert.updatedAt)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_PROPERTY_ALERTS_MAP, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving property alerts map", e)
        }
    }

    fun deletePropertyAlert(context: Context, dealId: Long) {
        val map = loadPropertyAlerts(context).toMutableMap()
        map.remove(dealId)
        saveAllPropertyAlerts(context, map)
    }

    // ==========================================
    // PREFERENCES: Alert History Log
    // ==========================================

    fun loadAlertHistory(context: Context): List<GranularAlertHistoryEvent> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ALERT_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<GranularAlertHistoryEvent>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val triggerKey = obj.optString("triggerMode", AlertTriggerMode.PERCENTAGE_DROP.key)
                val mode = AlertTriggerMode.values().find { it.key == triggerKey } ?: AlertTriggerMode.PERCENTAGE_DROP

                list.add(
                    GranularAlertHistoryEvent(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        dealId = obj.getLong("dealId"),
                        dealTitle = obj.optString("dealTitle", ""),
                        dealLocation = obj.optString("dealLocation", ""),
                        alertTypeTitle = obj.optString("alertTypeTitle", "Ribasso Prezzo"),
                        triggerMode = mode,
                        oldPrice = obj.optDouble("oldPrice", 0.0),
                        newPrice = obj.optDouble("newPrice", 0.0),
                        dropAmount = obj.optDouble("dropAmount", 0.0),
                        dropPercent = obj.optDouble("dropPercent", 0.0),
                        targetThresholdPrice = obj.optDouble("targetThresholdPrice", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading alert history", e)
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addHistoryEvent(context: Context, event: GranularAlertHistoryEvent) {
        val list = loadAlertHistory(context).toMutableList()
        list.add(0, event)
        // Keep up to 100 recent alerts
        val trimmed = list.take(100)
        saveAlertHistory(context, trimmed)
    }

    fun markHistoryEventAsRead(context: Context, eventId: String) {
        val list = loadAlertHistory(context).map {
            if (it.id == eventId) it.copy(isRead = true) else it
        }
        saveAlertHistory(context, list)
    }

    fun clearAlertHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ALERT_HISTORY).apply()
    }

    private fun saveAlertHistory(context: Context, list: List<GranularAlertHistoryEvent>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            list.forEach { event ->
                val obj = JSONObject().apply {
                    put("id", event.id)
                    put("dealId", event.dealId)
                    put("dealTitle", event.dealTitle)
                    put("dealLocation", event.dealLocation)
                    put("alertTypeTitle", event.alertTypeTitle)
                    put("triggerMode", event.triggerMode.key)
                    put("oldPrice", event.oldPrice)
                    put("newPrice", event.newPrice)
                    put("dropAmount", event.dropAmount)
                    put("dropPercent", event.dropPercent)
                    put("targetThresholdPrice", event.targetThresholdPrice)
                    put("timestamp", event.timestamp)
                    put("isRead", event.isRead)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_ALERT_HISTORY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alert history", e)
        }
    }

    // ==========================================
    // NOTIFICATION DISPATCH
    // ==========================================

    fun sendGranularPriceDropNotification(
        context: Context,
        alertConfig: GranularPropertyAlert,
        oldPrice: Double,
        newPrice: Double
    ) {
        ensureNotificationChannel(context)

        val globalSettings = loadGlobalSettings(context)
        if (!globalSettings.masterPushEnabled || !alertConfig.isAlertEnabled) {
            Log.d(TAG, "Notification skipped (disabled)")
            return
        }

        val dropAmount = (oldPrice - newPrice).coerceAtLeast(0.0)
        val dropPercent = if (oldPrice > 0) ((dropAmount / oldPrice) * 100.0) else 0.0

        val formattedOld = currencyFormat.format(oldPrice)
        val formattedNew = currencyFormat.format(newPrice)
        val formattedTarget = currencyFormat.format(alertConfig.effectiveTriggerPrice)
        val formattedSavings = currencyFormat.format(dropAmount)

        val title = "🚨 Ribasso Prezzo Rilevato (-${String.format(Locale.ITALY, "%.1f%%", dropPercent)})"
        val contentText = "${alertConfig.dealTitle}: da $formattedOld a $formattedNew (Soglia: $formattedTarget)"

        val bigText = """
            🏷️ Opportunità su Immobile Monitorato!
            📍 ${alertConfig.dealLocation}
            
            • Prezzo Precedente: $formattedOld
            • Nuovo Prezzo Richiesto: $formattedNew
            • Risparmio Immediato: $formattedSavings (-${String.format(Locale.ITALY, "%.1f%%", dropPercent)})
            • Soglia Configurato: $formattedTarget (${alertConfig.triggerMode.titleIt})
            
            Tocca per aprire l'analisi di rendimento e la scheda asta.
        """.trimIndent()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DEAL_ID", alertConfig.dealId)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alertConfig.dealId.toInt(),
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GRANULAR_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (alertConfig.isHighPriority && globalSettings.headsUpHighPriorityEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (globalSettings.soundAndVibrationEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                val notificationId = (alertConfig.dealId * 100 + (System.currentTimeMillis() % 99)).toInt()
                notificationManager.notify(notificationId, builder.build())

                // Record in history log
                val historyEvent = GranularAlertHistoryEvent(
                    dealId = alertConfig.dealId,
                    dealTitle = alertConfig.dealTitle,
                    dealLocation = alertConfig.dealLocation,
                    alertTypeTitle = "Ribasso -${String.format(Locale.ITALY, "%.1f%%", dropPercent)}",
                    triggerMode = alertConfig.triggerMode,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    dropAmount = dropAmount,
                    dropPercent = dropPercent,
                    targetThresholdPrice = alertConfig.effectiveTriggerPrice
                )
                addHistoryEvent(context, historyEvent)

                // Update alert stats
                val updatedAlert = alertConfig.copy(
                    totalAlertsTriggered = alertConfig.totalAlertsTriggered + 1,
                    lastTriggeredAt = System.currentTimeMillis(),
                    lastTriggeredPrice = newPrice
                )
                savePropertyAlert(context, updatedAlert)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending notification", e)
            }
        }
    }

    fun sendTestGranularNotification(
        context: Context,
        alertConfig: GranularPropertyAlert
    ) {
        ensureNotificationChannel(context)

        val simulatedNewPrice = alertConfig.effectiveTriggerPrice
        val simulatedOldPrice = alertConfig.currentAskingPrice
        val dropAmount = (simulatedOldPrice - simulatedNewPrice).coerceAtLeast(0.0)
        val dropPercent = if (simulatedOldPrice > 0) ((dropAmount / simulatedOldPrice) * 100.0) else 0.0

        val formattedOld = currencyFormat.format(simulatedOldPrice)
        val formattedNew = currencyFormat.format(simulatedNewPrice)
        val formattedTarget = currencyFormat.format(alertConfig.effectiveTriggerPrice)

        val title = "🔔 [TEST] Allerta Prezzo Configurato: ${alertConfig.dealTitle}"
        val contentText = "Soglia attiva: ≤ $formattedTarget (${alertConfig.triggerMode.titleIt})"

        val bigText = """
            🔔 Notifica di Test Allarme Prezzo
            L'allarme per l'immobile '${alertConfig.dealTitle}' è configurato correttamente!
            
            • Modalità Attiva: ${alertConfig.triggerMode.titleIt}
            • Prezzo Attuale di Partenza: $formattedOld
            • Soglia d'Innesco Notifica: ≤ $formattedNew (-${String.format(Locale.ITALY, "%.1f%%", dropPercent)})
            • Priorità: ${if (alertConfig.isHighPriority) "Alta (Heads-Up)" else "Standard"}
            
            Riceverai una notifica non appena il sistema rileva un ribasso nel portale d'asta o nel feed.
        """.trimIndent()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DEAL_ID", alertConfig.dealId)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (alertConfig.dealId + 50000).toInt(),
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GRANULAR_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                val notificationId = (alertConfig.dealId * 1000 + 77).toInt()
                notificationManager.notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending test notification", e)
            }
        }
    }
}
