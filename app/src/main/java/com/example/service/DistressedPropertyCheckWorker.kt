package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DistressedProperty
import com.example.data.InvestorProfile
import com.example.data.PropertyDeal
import com.example.util.BriefMatcher
import java.text.NumberFormat
import java.util.Locale

class DistressedPropertyCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "PropertyCriteriaWorker"
        const val CHANNEL_ID = "distressed_property_alerts"
        const val CHANNEL_NAME = "Property & Deal Criteria Alerts"
        const val PREFS_NAME = "distressed_worker_prefs"
        const val KEY_NOTIFIED_IDS = "notified_property_ids"
        const val KEY_NOTIFIED_DEAL_IDS = "notified_deal_ids"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Property criteria WorkManager started background check...")
        try {
            val database = AppDatabase.getDatabase(appContext)
            val distressedDao = database.distressedPropertyDao()
            val dealDao = database.propertyDealDao()
            val profileDao = database.investorProfileDao()

            createNotificationChannel()

            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val notifiedDistressedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            val notifiedDealIds = prefs.getStringSet(KEY_NOTIFIED_DEAL_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

            // 1. Check Investor Brief Criteria against Room PropertyDeals
            val investorProfile: InvestorProfile? = profileDao.getProfile(1L)
            val allDeals: List<PropertyDeal> = dealDao.getAllDealsList()

            if (investorProfile != null && investorProfile.briefActive && investorProfile.briefAlertsEnabled) {
                val matchingDeals = allDeals.filter { deal ->
                    val matchResult = BriefMatcher.evaluate(deal, investorProfile)
                    val priceAlertHit = deal.priceAlertThreshold != null && deal.askingPrice <= deal.priceAlertThreshold!!
                    matchResult.isTargetMatch || priceAlertHit
                }

                val newMatchingDeals = matchingDeals.filter { it.id.toString() !in notifiedDealIds }
                Log.d(TAG, "Found ${newMatchingDeals.size} NEW deals matching user Investor Brief criteria")

                newMatchingDeals.forEach { deal ->
                    val matchResult = BriefMatcher.evaluate(deal, investorProfile)
                    val isPriceAlert = deal.priceAlertThreshold != null && deal.askingPrice <= deal.priceAlertThreshold!!
                    sendDealPushNotification(
                        deal = deal,
                        matchScore = matchResult.score,
                        matchReason = if (isPriceAlert) "Prezzo sceso sotto la soglia target" else matchResult.reasons.firstOrNull() ?: "Target Brief Match",
                        notificationId = (deal.id % 10000).toInt() + 2000
                    )
                    notifiedDealIds.add(deal.id.toString())
                }
            }

            // 2. Check Saved Criteria against Room DistressedProperties
            val alertPrefs = appContext.getSharedPreferences("distressed_alert_prefs", Context.MODE_PRIVATE)
            val criteriaQuery = alertPrefs.getString("saved_query", "") ?: ""
            val criteriaLevel = alertPrefs.getString("saved_distress_level", "ALL") ?: "ALL"
            val criteriaMaxPrice = if (alertPrefs.contains("saved_max_price")) alertPrefs.getFloat("saved_max_price", 0f).toDouble() else null
            val alertsEnabled = alertPrefs.getBoolean("alerts_enabled", true)

            if (alertsEnabled) {
                val allDistressed = distressedDao.getDistressedPropertiesList()
                val matchingDistressed = allDistressed.filter { property ->
                    val matchesQuery = criteriaQuery.isBlank() ||
                            property.address.contains(criteriaQuery, ignoreCase = true) ||
                            property.distressLevel.contains(criteriaQuery, ignoreCase = true)

                    val matchesLevel = criteriaLevel.equals("ALL", ignoreCase = true) ||
                            property.distressLevel.equals(criteriaLevel, ignoreCase = true) ||
                            property.distressLevel.contains(criteriaLevel, ignoreCase = true)

                    val matchesPrice = criteriaMaxPrice == null || property.price <= criteriaMaxPrice

                    matchesQuery && matchesLevel && matchesPrice
                }

                val newMatchingDistressed = matchingDistressed.filter { it.id.toString() !in notifiedDistressedIds }
                Log.d(TAG, "Found ${newMatchingDistressed.size} NEW distressed properties matching alert criteria")

                newMatchingDistressed.forEach { property ->
                    sendDistressedPushNotification(
                        property = property,
                        notificationId = (property.id % 10000).toInt() + 1000
                    )
                    notifiedDistressedIds.add(property.id.toString())
                }
            }

            // Save updated notified IDs
            prefs.edit()
                .putStringSet(KEY_NOTIFIED_IDS, notifiedDistressedIds)
                .putStringSet(KEY_NOTIFIED_DEAL_IDS, notifiedDealIds)
                .apply()

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing PropertyCriteriaWorker: ${e.message}", e)
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifiche per nuovi immobili e opportunità che soddisfano i tuoi criteri"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            }
        }
    }

    private fun sendDealPushNotification(
        deal: PropertyDeal,
        matchScore: Int,
        matchReason: String,
        notificationId: Int
    ) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "RADAR_FEED")
            putExtra("DEAL_ID", deal.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val numberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
        val priceStr = numberFormat.format(deal.askingPrice)
        val valueStr = numberFormat.format(deal.estimatedMarketValue)

        val notificationTitle = "🎯 Nuovo Affare nel Tuo Target Brief ($matchScore/100)"
        val notificationText = "${deal.title} • $priceStr (-${deal.discountPercent}%) [${deal.location}]"

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${deal.title}\n📍 ${deal.location}\n💶 Prezzo: $priceStr (Valore stimato: $valueStr)\n📉 Sconto: -${deal.discountPercent}% | Rendimento: ${deal.estimatedCapRate}%\n💡 Criterio: $matchReason")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Push notification sent for Deal ID ${deal.id}: ${deal.title}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending notification (missing POST_NOTIFICATIONS permission?): ${e.message}")
        }
    }

    private fun sendDistressedPushNotification(property: DistressedProperty, notificationId: Int) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "DISTRESSED_PROPERTIES")
            putExtra("PROPERTY_ID", property.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val numberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
            maximumFractionDigits = 0
        }
        val priceStr = numberFormat.format(property.price)
        val valueStr = numberFormat.format(property.estimatedValue)

        val notificationTitle = "🚨 Immobile Distressed nei Criteri!"
        val notificationText = "${property.address} • $priceStr (Est. $valueStr) [${property.distressLevel}]"

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${property.address}\n💶 Prezzo Richiesto: $priceStr\n📈 Valore di Mercato: $valueStr\n⚠️ Livello Distress: ${property.distressLevel}\n📝 Note: ${property.notes ?: "Nessuna nota aggiuntiva"}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Push notification sent for Distressed Property ID ${property.id}: ${property.address}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending notification (missing POST_NOTIFICATIONS permission?): ${e.message}")
        }
    }
}

