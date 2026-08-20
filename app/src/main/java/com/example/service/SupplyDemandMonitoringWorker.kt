package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SupplyDemandMonitoringWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SupplyDemandWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SupplyDemandMonitoringWorker executing proactive scraper scan...")
        return try {
            val alerts = SupplyDemandMonitoringEngine.performProactiveMonitoringScan(appContext)
            Log.d(TAG, "SupplyDemandMonitoringWorker completed with ${alerts.size} alerts triggered")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SupplyDemandMonitoringWorker encountered error", e)
            Result.retry()
        }
    }
}
