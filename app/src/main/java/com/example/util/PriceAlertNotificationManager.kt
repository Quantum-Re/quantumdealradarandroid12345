package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.DistressedProperty
import com.example.data.PropertyDeal
import java.text.NumberFormat
import java.util.Locale

object PriceAlertNotificationManager {
    const val CHANNEL_ID = "property_price_alerts_channel"
    const val CHANNEL_NAME = "Allarmi Prezzo Immobili Salvati"
    const val CHANNEL_DESC = "Notifiche automatiche quando un immobile salvato scende sotto la tua soglia prezzo"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

    fun sendPriceDropAlertNotification(
        context: Context,
        deal: PropertyDeal,
        oldPrice: Double,
        newPrice: Double,
        thresholdPrice: Double
    ) {
        ensureNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
        val formattedNewPrice = currencyFormat.format(newPrice)
        val formattedThreshold = currencyFormat.format(thresholdPrice)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DEAL_ID", deal.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            deal.id.toInt(),
            intent,
            pendingIntentFlags
        )

        val title = "🚨 Allerta Prezzo: ${deal.title}"
        val contentText = "Il prezzo è sceso a $formattedNewPrice! (Soglia impostata: $formattedThreshold)"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Opportunità in Saved Deals!\n${deal.title} (${deal.location})\nPrezzo Attuale: $formattedNewPrice\nSoglia Utente: $formattedThreshold\n\nTocca per aprire la scheda immobile e calcolare il nuovo ROI.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun sendTestAlertNotification(
        context: Context,
        dealTitle: String,
        targetPrice: Double
    ) {
        ensureNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
        val formattedPrice = currencyFormat.format(targetPrice)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Allerta Prezzo Configurato: $dealTitle")
            .setContentText("Notifica di prova inviata con successo! Soglia impostata a $formattedPrice.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    const val DISTRESSED_CHANNEL_ID = "distressed_property_alerts_channel"
    const val DISTRESSED_CHANNEL_NAME = "Notifiche Nuovi Immobili Distressed"
    const val DISTRESSED_CHANNEL_DESC = "Avvisi automatici all'inserimento di immobili in sofferenza corrispondenti ai criteri salvati"

    fun ensureDistressedNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(DISTRESSED_CHANNEL_ID, DISTRESSED_CHANNEL_NAME, importance).apply {
                description = DISTRESSED_CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNewDistressedPropertyAlertNotification(
        context: Context,
        property: DistressedProperty,
        matchedCriteria: String
    ) {
        ensureDistressedNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
        val formattedPrice = currencyFormat.format(property.price)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_DISTRESSED_ID", property.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (property.id + 20000).toInt(),
            intent,
            pendingIntentFlags
        )

        val title = "🚨 Nuovo Immobile Distressed ($matchedCriteria)"
        val contentText = "${property.address} - $formattedPrice [${property.distressLevel}]"

        val builder = NotificationCompat.Builder(context, DISTRESSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Nuovo immobile aggiunto al Database!\n\nIndirizzo: ${property.address}\nPrezzo Asta: $formattedPrice\nLivello Distress: ${property.distressLevel}\nCriterio Match: $matchedCriteria\n\nTocca per aprire la mappa e analizzare la scheda.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (hasNotificationPermission(context)) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
