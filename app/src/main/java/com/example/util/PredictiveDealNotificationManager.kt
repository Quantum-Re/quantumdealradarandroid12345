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
import java.text.NumberFormat
import java.util.Locale

/**
 * Manages Predictive Deal Alert notifications triggered when a property enters
 * the top 10th percentile of deal potential based on historical yield trends.
 */
object PredictiveDealNotificationManager {
    const val CHANNEL_ID = "predictive_deal_alerts_channel"
    const val CHANNEL_NAME = "Allarmi Predittivi Top 10% Deal"
    const val CHANNEL_DESC = "Notifiche quando un immobile entra nel 10% dei migliori deal per rendimento storico provinciale"

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

    fun sendTop10DealAlertNotification(
        context: Context,
        evaluation: PredictiveDealEvaluation
    ) {
        ensureNotificationChannel(context)

        val euroFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
        val formattedPrice = euroFormat.format(evaluation.currentListPrice)
        val formattedYield = String.format(Locale.ITALY, "%.1f%%", evaluation.propertyImpliedGrossYield)
        val formattedP90 = String.format(Locale.ITALY, "%.1f%%", evaluation.provinceHistoricalStats.p90HistoricalYield)
        val formattedPercentile = String.format(Locale.US, "%.0f°", evaluation.dealPercentile)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_PROPERTY_ID", evaluation.propertyId)
            putExtra("EXTRA_NAVIGATE_TAB", "MY_PROPERTIES")
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (evaluation.propertyId + 7000).toInt(),
            intent,
            pendingIntentFlags
        )

        val title = "🚀 Top 10% Deal Alert: ${evaluation.propertyTitle}"
        val contentText = "$formattedPercentile Percentile a ${evaluation.location} (${evaluation.provinceCode})! Rendimento: $formattedYield a $formattedPrice"

        val bigText = """
            🔥 OPPORTUNITÀ TOP 10% PROVINCIALE RILEVATA!
            
            📍 Immobile: ${evaluation.propertyTitle} (${evaluation.location}, ${evaluation.provinceCode})
            💶 Prezzo Attuale: $formattedPrice (${euroFormat.format(evaluation.pricePerSqm)}/m²)
            📈 Rendimento Storico Implicito: $formattedYield (Soglia Top 10% P90: $formattedP90)
            📊 Posizionamento: ${evaluation.topTierRankLabel} (${formattedPercentile} percentile)
            🔮 Previsione Valore 12M: ${euroFormat.format(evaluation.predicted12mMarketValue)}
            
            Tocca per aprire l'analisi approfondita e visualizzare la curva di resa storica.
        """.trimIndent()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
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
                val notificationId = (evaluation.propertyId % 10000 + 5000).toInt()
                notificationManager.notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
