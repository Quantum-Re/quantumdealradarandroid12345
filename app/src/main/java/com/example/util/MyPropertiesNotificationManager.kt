package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

object MyPropertiesNotificationManager {
    private const val TAG = "MyPropertiesNotification"
    const val CHANNEL_ID = "my_properties_portfolio_alerts_channel"
    const val CHANNEL_NAME = "Allarmi Portafoglio 'I Miei Immobili'"
    const val CHANNEL_DESC = "Notifiche per ribassi di prezzo significativi e cambi di stato degli immobili salvati in portafoglio"

    private const val PREFS_NAME = "my_properties_alert_prefs"
    private const val KEY_ALERT_HISTORY = "portfolio_alert_history_json"
    private const val KEY_ENABLED = "alerts_enabled"
    private const val KEY_PRICE_DROP_ENABLED = "price_drop_enabled"
    private const val KEY_THRESHOLD_PERCENT = "threshold_percent"
    private const val KEY_MIN_ABSOLUTE_EUROS = "min_absolute_euros"
    private const val KEY_STATUS_CHANGE_ENABLED = "status_change_enabled"
    private const val KEY_RENOVATION_ENABLED = "renovation_enabled"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel $CHANNEL_ID created successfully")
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

    fun loadPreferences(context: Context): MyPropertiesAlertPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MyPropertiesAlertPreferences(
            notificationsEnabled = prefs.getBoolean(KEY_ENABLED, true),
            priceDropAlertsEnabled = prefs.getBoolean(KEY_PRICE_DROP_ENABLED, true),
            priceDropThresholdPercent = prefs.getFloat(KEY_THRESHOLD_PERCENT, 5.0f).toDouble(),
            priceDropMinAbsoluteEuros = prefs.getFloat(KEY_MIN_ABSOLUTE_EUROS, 2000.0f).toDouble(),
            statusChangeAlertsEnabled = prefs.getBoolean(KEY_STATUS_CHANGE_ENABLED, true),
            renovationMilestoneAlertsEnabled = prefs.getBoolean(KEY_RENOVATION_ENABLED, true)
        )
    }

    fun savePreferences(context: Context, preferences: MyPropertiesAlertPreferences) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, preferences.notificationsEnabled)
            .putBoolean(KEY_PRICE_DROP_ENABLED, preferences.priceDropAlertsEnabled)
            .putFloat(KEY_THRESHOLD_PERCENT, preferences.priceDropThresholdPercent.toFloat())
            .putFloat(KEY_MIN_ABSOLUTE_EUROS, preferences.priceDropMinAbsoluteEuros.toFloat())
            .putBoolean(KEY_STATUS_CHANGE_ENABLED, preferences.statusChangeAlertsEnabled)
            .putBoolean(KEY_RENOVATION_ENABLED, preferences.renovationMilestoneAlertsEnabled)
            .apply()
    }

    fun loadAlertHistory(context: Context): List<PropertyAlertRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ALERT_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<PropertyAlertRecord>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeKey = obj.optString("alertType", PropertyAlertType.PRICE_DROP.key)
                val alertType = PropertyAlertType.values().find { it.key == typeKey } ?: PropertyAlertType.PRICE_DROP
                val sevKey = obj.optString("severity", AlertSeverity.HIGH.name)
                val severity = try { AlertSeverity.valueOf(sevKey) } catch (e: Exception) { AlertSeverity.HIGH }

                list.add(
                    PropertyAlertRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        propertyId = obj.optLong("propertyId", 0L),
                        propertyTitle = obj.optString("propertyTitle", ""),
                        propertyAddress = obj.optString("propertyAddress", ""),
                        alertType = alertType,
                        oldValue = obj.optString("oldValue", ""),
                        newValue = obj.optString("newValue", ""),
                        changeSummary = obj.optString("changeSummary", ""),
                        dropAmount = obj.optDouble("dropAmount", 0.0),
                        dropPercent = obj.optDouble("dropPercent", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        severity = severity
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing alert history JSON: ${e.message}", e)
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun saveAlertHistory(context: Context, history: List<PropertyAlertRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            // Keep top 100 most recent records
            history.take(100).forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("propertyId", item.propertyId)
                    put("propertyTitle", item.propertyTitle)
                    put("propertyAddress", item.propertyAddress)
                    put("alertType", item.alertType.key)
                    put("oldValue", item.oldValue)
                    put("newValue", item.newValue)
                    put("changeSummary", item.changeSummary)
                    put("dropAmount", item.dropAmount)
                    put("dropPercent", item.dropPercent)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                    put("severity", item.severity.name)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_ALERT_HISTORY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alert history JSON: ${e.message}", e)
        }
    }

    fun addAlertRecord(context: Context, record: PropertyAlertRecord): List<PropertyAlertRecord> {
        val existing = loadAlertHistory(context).toMutableList()
        existing.add(0, record)
        val trimmed = existing.take(100)
        saveAlertHistory(context, trimmed)
        return trimmed
    }

    fun markAlertAsRead(context: Context, alertId: String): List<PropertyAlertRecord> {
        val existing = loadAlertHistory(context).map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
        saveAlertHistory(context, existing)
        return existing
    }

    fun clearAlertHistory(context: Context): List<PropertyAlertRecord> {
        saveAlertHistory(context, emptyList())
        return emptyList()
    }

    /**
     * Inspects changes between old and updated Property entities.
     * Fires high-priority system notifications and saves history records if significant change occurs.
     */
    fun checkAndNotifyPropertyChange(
        context: Context,
        oldProperty: Property,
        newProperty: Property,
        prefs: MyPropertiesAlertPreferences = loadPreferences(context)
    ): List<PropertyAlertRecord> {
        if (!prefs.notificationsEnabled) return emptyList()

        val generatedRecords = mutableListOf<PropertyAlertRecord>()
        val euroFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }

        // 1. Check for SIGNIFICANT PRICE DROP
        if (prefs.priceDropAlertsEnabled && newProperty.price < oldProperty.price) {
            val dropAmount = oldProperty.price - newProperty.price
            val dropPercent = (dropAmount / oldProperty.price) * 100.0

            val isSignificantPercent = dropPercent >= prefs.priceDropThresholdPercent
            val isSignificantAbsolute = dropAmount >= prefs.priceDropMinAbsoluteEuros

            if (isSignificantPercent || isSignificantAbsolute) {
                val record = sendPriceDropNotification(
                    context = context,
                    property = newProperty,
                    oldPrice = oldProperty.price,
                    newPrice = newProperty.price,
                    dropPercent = dropPercent,
                    dropAmount = dropAmount
                )
                generatedRecords.add(record)
            }
        }

        // 2. Check for PIPELINE STATUS CHANGE
        if (prefs.statusChangeAlertsEnabled && !oldProperty.pipelineStatus.equals(newProperty.pipelineStatus, ignoreCase = true)) {
            val record = sendStatusChangeNotification(
                context = context,
                property = newProperty,
                oldStatusKey = oldProperty.pipelineStatus,
                newStatusKey = newProperty.pipelineStatus
            )
            generatedRecords.add(record)
        }

        // 3. Check for DISTRESS STATUS CHANGE
        if (prefs.statusChangeAlertsEnabled && !oldProperty.distressStatus.equals(newProperty.distressStatus, ignoreCase = true)) {
            val record = sendDistressChangeNotification(
                context = context,
                property = newProperty,
                oldDistress = oldProperty.distressStatus,
                newDistress = newProperty.distressStatus
            )
            generatedRecords.add(record)
        }

        // 4. Check for RENOVATION MILESTONE (e.g. crossing 50%, 75%, 100%)
        if (prefs.renovationMilestoneAlertsEnabled && newProperty.renovationProgressPercent > oldProperty.renovationProgressPercent) {
            val oldP = oldProperty.renovationProgressPercent
            val newP = newProperty.renovationProgressPercent
            if ((oldP < 50 && newP >= 50) || (oldP < 100 && newP >= 100) || (newP - oldP >= 25)) {
                val record = sendRenovationMilestoneNotification(
                    context = context,
                    property = newProperty,
                    oldProgress = oldP,
                    newProgress = newP
                )
                generatedRecords.add(record)
            }
        }

        return generatedRecords
    }

    fun sendPriceDropNotification(
        context: Context,
        property: Property,
        oldPrice: Double,
        newPrice: Double,
        dropPercent: Double,
        dropAmount: Double
    ): PropertyAlertRecord {
        ensureNotificationChannel(context)

        val euroFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
        val oldPriceStr = euroFormat.format(oldPrice)
        val newPriceStr = euroFormat.format(newPrice)
        val dropAmountStr = euroFormat.format(dropAmount)
        val percentFormatted = String.format(Locale.ITALY, "%.1f", dropPercent)

        val notificationTitle = "📉 Ribasso Prezzo Rilevato (-$percentFormatted%)"
        val notificationText = "${property.title.ifBlank { property.address }}: da $oldPriceStr a $newPriceStr (-$dropAmountStr)"
        val detailSummary = "Ribasso di $dropAmountStr (-$percentFormatted%) • Nuovo prezzo: $newPriceStr"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "MY_PROPERTIES")
            putExtra("PROPERTY_ID", property.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (property.id % 10000).toInt() + 3000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Opportunità in 'I Miei Immobili'!\n\n🏢 ${property.title.ifBlank { "Immobile salvato" }}\n📍 ${property.address}\n\n📉 Prezzo precedente: $oldPriceStr\n💶 Nuovo prezzo: $newPriceStr\n🔥 Risparmio: -$dropAmountStr (-$percentFormatted%)\n🎯 ROI Stimato ricalcolato: ${String.format(Locale.ITALY, "%.1f", property.projectedRoiPercent)}%\n\nTocca per aprire la scheda immobile e aggiornare il piano d'investimento.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasNotificationPermission(context)) {
            try {
                notificationManager.notify((property.id % 10000).toInt() + 3000, builder.build())
                Log.d(TAG, "Sent price drop notification for property ${property.id}: -$dropAmountStr")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending notification: ${e.message}")
            }
        }

        val record = PropertyAlertRecord(
            propertyId = property.id,
            propertyTitle = property.title.ifBlank { property.address },
            propertyAddress = property.address,
            alertType = PropertyAlertType.PRICE_DROP,
            oldValue = oldPriceStr,
            newValue = newPriceStr,
            changeSummary = detailSummary,
            dropAmount = dropAmount,
            dropPercent = dropPercent,
            timestamp = System.currentTimeMillis(),
            severity = if (dropPercent >= 10.0) AlertSeverity.HIGH else AlertSeverity.MEDIUM
        )

        addAlertRecord(context, record)
        return record
    }

    fun sendStatusChangeNotification(
        context: Context,
        property: Property,
        oldStatusKey: String,
        newStatusKey: String
    ): PropertyAlertRecord {
        ensureNotificationChannel(context)

        val oldStatus = PipelineStatus.fromKey(oldStatusKey)
        val newStatus = PipelineStatus.fromKey(newStatusKey)

        val notificationTitle = "🔄 Cambio Stato Pipeline Portafoglio"
        val notificationText = "${property.title.ifBlank { property.address }}: ${oldStatus.labelIt} ➔ ${newStatus.labelIt}"
        val detailSummary = "Avanzamento fase: da '${oldStatus.labelIt}' a '${newStatus.labelIt}'"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "MY_PROPERTIES")
            putExtra("PROPERTY_ID", property.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (property.id % 10000).toInt() + 4000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Aggiornamento Pipeline 'I Miei Immobili':\n\n🏢 ${property.title.ifBlank { property.address }}\n📍 ${property.address}\n\n🚀 Nuovo stato: ${newStatus.labelIt}\n📋 Descrizione: ${newStatus.description}\n\nTocca per visualizzare la Kanban board e i dettagli dell'immobile.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasNotificationPermission(context)) {
            try {
                notificationManager.notify((property.id % 10000).toInt() + 4000, builder.build())
                Log.d(TAG, "Sent status change notification for property ${property.id}: ${newStatus.labelIt}")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending notification: ${e.message}")
            }
        }

        val record = PropertyAlertRecord(
            propertyId = property.id,
            propertyTitle = property.title.ifBlank { property.address },
            propertyAddress = property.address,
            alertType = PropertyAlertType.STATUS_CHANGE,
            oldValue = oldStatus.labelIt,
            newValue = newStatus.labelIt,
            changeSummary = detailSummary,
            timestamp = System.currentTimeMillis(),
            severity = if (newStatus == PipelineStatus.SOLD || newStatus == PipelineStatus.IN_ESCROW) AlertSeverity.HIGH else AlertSeverity.MEDIUM
        )

        addAlertRecord(context, record)
        return record
    }

    fun sendDistressChangeNotification(
        context: Context,
        property: Property,
        oldDistress: String,
        newDistress: String
    ): PropertyAlertRecord {
        ensureNotificationChannel(context)

        val notificationTitle = "⚖️ Variazione Procedura Giudiziaria / Distress"
        val notificationText = "${property.title.ifBlank { property.address }}: $oldDistress ➔ $newDistress"
        val detailSummary = "Stato procedura modificato da '$oldDistress' a '$newDistress'"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "MY_PROPERTIES")
            putExtra("PROPERTY_ID", property.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (property.id % 10000).toInt() + 5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Attenzione: Procedura Immobile Aggiornata\n\n🏢 ${property.title.ifBlank { property.address }}\n📍 ${property.address}\n\n⚠️ Precedente: $oldDistress\n📌 Nuovo Stato: $newDistress\n\nTocca per verificare la documentazione e i rilievi tecnici.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasNotificationPermission(context)) {
            try {
                notificationManager.notify((property.id % 10000).toInt() + 5000, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending notification: ${e.message}")
            }
        }

        val record = PropertyAlertRecord(
            propertyId = property.id,
            propertyTitle = property.title.ifBlank { property.address },
            propertyAddress = property.address,
            alertType = PropertyAlertType.DISTRESS_STATUS_CHANGE,
            oldValue = oldDistress,
            newValue = newDistress,
            changeSummary = detailSummary,
            timestamp = System.currentTimeMillis(),
            severity = AlertSeverity.HIGH
        )

        addAlertRecord(context, record)
        return record
    }

    fun sendRenovationMilestoneNotification(
        context: Context,
        property: Property,
        oldProgress: Int,
        newProgress: Int
    ): PropertyAlertRecord {
        ensureNotificationChannel(context)

        val notificationTitle = "🔨 Cantiere Avanzato al $newProgress%"
        val notificationText = "${property.title.ifBlank { property.address }}: avanzamento lavori salito da $oldProgress% a $newProgress%"
        val detailSummary = "Avanzamento lavori: $oldProgress% ➔ $newProgress%"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "MY_PROPERTIES")
            putExtra("PROPERTY_ID", property.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (property.id % 10000).toInt() + 6000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Stato di Avanzamento Lavori (SAL):\n\n🏢 ${property.title.ifBlank { property.address }}\n📍 ${property.address}\n\n📊 Progresso: $newProgress% (in precedenza $oldProgress%)\n${if (newProgress >= 100) "🎉 Ristrutturazione completata con successo! Pronto per la messa a reddito o vendita." else "Lavori in corso secondo cronoprogramma."}\n\nTocca per visualizzare la scheda di cantiere.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasNotificationPermission(context)) {
            try {
                notificationManager.notify((property.id % 10000).toInt() + 6000, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending notification: ${e.message}")
            }
        }

        val record = PropertyAlertRecord(
            propertyId = property.id,
            propertyTitle = property.title.ifBlank { property.address },
            propertyAddress = property.address,
            alertType = PropertyAlertType.RENOVATION_MILESTONE,
            oldValue = "$oldProgress%",
            newValue = "$newProgress%",
            changeSummary = detailSummary,
            timestamp = System.currentTimeMillis(),
            severity = if (newProgress >= 100) AlertSeverity.HIGH else AlertSeverity.INFO
        )

        addAlertRecord(context, record)
        return record
    }

    fun sendTestPriceDropNotification(context: Context, sampleProperty: Property? = null): PropertyAlertRecord {
        val target = sampleProperty ?: Property(
            id = 999L,
            title = "Appartamento Via Manzoni",
            address = "Via Alessandro Manzoni 14, Milano",
            price = 220000.0,
            distressStatus = "ASTA",
            pipelineStatus = "ANALYZED"
        )

        val oldPrice = target.price
        val dropPercent = 8.5
        val dropAmount = oldPrice * (dropPercent / 100.0)
        val newPrice = oldPrice - dropAmount

        return sendPriceDropNotification(
            context = context,
            property = target.copy(price = newPrice),
            oldPrice = oldPrice,
            newPrice = newPrice,
            dropPercent = dropPercent,
            dropAmount = dropAmount
        )
    }

    fun sendTestStatusChangeNotification(context: Context, sampleProperty: Property? = null): PropertyAlertRecord {
        val target = sampleProperty ?: Property(
            id = 998L,
            title = "Quadrilocale Corso Buenos Aires",
            address = "Corso Buenos Aires 45, Milano",
            price = 310000.0,
            distressStatus = "PRE_ASTA",
            pipelineStatus = "ANALYZED"
        )

        return sendStatusChangeNotification(
            context = context,
            property = target.copy(pipelineStatus = PipelineStatus.IN_ESCROW.key),
            oldStatusKey = PipelineStatus.ANALYZED.key,
            newStatusKey = PipelineStatus.IN_ESCROW.key
        )
    }
}
