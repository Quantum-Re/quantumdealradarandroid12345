package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object SupplyDemandWorkManagerScheduler {
    private const val TAG = "SupplyDemandScheduler"
    const val UNIQUE_PERIODIC_WORK_NAME = "periodic_supply_demand_ratio_check"
    const val UNIQUE_ONE_TIME_WORK_NAME = "immediate_supply_demand_ratio_check"

    fun schedulePeriodicCheck(context: Context, intervalMinutes: Long = 15) {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SupplyDemandMonitoringWorker>(
                intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d(TAG, "Enqueued periodic Supply-Demand check every $intervalMinutes min")
        }.onFailure { e ->
            Log.w(TAG, "Failed to schedule periodic supply-demand check: ${e.message}")
        }
    }

    fun triggerImmediateCheck(context: Context) {
        runCatching {
            val immediateRequest = OneTimeWorkRequestBuilder<SupplyDemandMonitoringWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Log.d(TAG, "Triggered immediate one-time supply-demand check via WorkManager")
        }.onFailure { e ->
            Log.w(TAG, "Failed to trigger immediate supply-demand check: ${e.message}")
        }
    }

    fun cancelMonitoring(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
            Log.d(TAG, "Cancelled periodic supply-demand check")
        }.onFailure { e ->
            Log.w(TAG, "Failed to cancel supply-demand check: ${e.message}")
        }
    }
}
