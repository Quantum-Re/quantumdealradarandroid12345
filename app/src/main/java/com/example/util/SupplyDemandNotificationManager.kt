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
import com.example.data.ShiftSeverity
import com.example.data.SupplyDemandAlertRecord
import com.example.data.SupplyDemandShiftType

object SupplyDemandNotificationManager {
    private const val TAG = "SupplyDemandNotif"
    const val CHANNEL_ID = "supply_demand_ratio_alerts_channel"
    const val CHANNEL_NAME = "🚨 Allarmi Shift Domanda/Offerta Immobiliare.it"
    const val CHANNEL_DESC = "Notifiche immediate per variazioni drastiche di offerta, domanda, saturazione e giorni sul mercato nelle zone dei tuoi immobili"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel $CHANNEL_ID initialized")
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

    fun sendShiftAlertNotification(
        context: Context,
        alert: SupplyDemandAlertRecord
    ) {
        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "SUPPLY_DEMAND_MONITOR")
            putExtra("ALERT_ID", alert.id)
            if (alert.affectedPropertyIds.isNotEmpty()) {
                putExtra("AFFECTED_PROPERTY_ID", alert.affectedPropertyIds.first())
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationId = (alert.id.hashCode() and 0x7FFFFFFF) % 10000 + 4000
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val iconRes = when (alert.shiftType) {
            SupplyDemandShiftType.SUPPLY_SQUEEZE -> android.R.drawable.stat_notify_more
            SupplyDemandShiftType.SUPPLY_GLUT -> android.R.drawable.stat_notify_error
            SupplyDemandShiftType.RENTAL_YIELD_DIVERGENCE -> android.R.drawable.stat_notify_sync
            SupplyDemandShiftType.MICRO_ZONE_HEATWAVE -> android.R.drawable.ic_dialog_alert
        }

        val sign = if (alert.ratioDeltaPercent >= 0) "+" else ""
        val deltaFormatted = "$sign${String.format(java.util.Locale.US, "%.1f", alert.ratioDeltaPercent)}%"

        val bigText = buildString {
            append("📍 Zona: ${alert.location}\n")
            append("⚡ Rapporto Domanda/Offerta: ${String.format(java.util.Locale.US, "%.1f", alert.previousRatio)} ➔ ${String.format(java.util.Locale.US, "%.1f", alert.currentRatio)} ($deltaFormatted)\n")
            append("⏱️ Giorni sul Mercato (DOM): ${alert.previousDom}gg ➔ ${alert.currentDom}gg\n")
            append("📊 Indice Saturazione: ${alert.previousSaturation}/100 ➔ ${alert.currentSaturation}/100\n\n")
            if (alert.affectedPropertyTitles.isNotEmpty()) {
                append("🏢 Immobili Interessati: ${alert.affectedPropertyTitles.joinToString(", ")}\n\n")
            }
            append("💡 Azione Consigliata:\n${alert.strategicRecommendation}")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("${alert.severity.labelIt} • ${alert.shiftType.shortLabel} in ${alert.location}")
            .setContentText("${alert.headline} (Variazione: $deltaFormatted)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(
                if (alert.severity == ShiftSeverity.CRITICAL) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(notificationId, builder.build())
                Log.d(TAG, "Push notification sent for alert ${alert.id} in ${alert.location}")
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        } else {
            Log.d(TAG, "Notification skipped because permission is not granted")
        }
    }
}
