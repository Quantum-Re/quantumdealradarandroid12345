package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

object DistressedWorkManagerScheduler {

    private const val TAG = "DistressedScheduler"
    const val UNIQUE_PERIODIC_WORK_NAME = "periodic_property_criteria_check"
    const val UNIQUE_ONE_TIME_WORK_NAME = "immediate_property_criteria_check"

    fun schedulePeriodicCheck(context: Context, intervalMinutes: Long = 15) {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DistressedPropertyCheckWorker>(
                intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            Log.d(TAG, "Enqueued periodic WorkManager check every $intervalMinutes minutes (Policy: KEEP)")
        }.onFailure { e ->
            Log.w(TAG, "WorkManager periodic check scheduling skipped/failed: ${e.message}")
        }
    }

    fun triggerImmediateCheck(context: Context) {
        runCatching {
            val immediateRequest = OneTimeWorkRequestBuilder<DistressedPropertyCheckWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Log.d(TAG, "Triggered immediate WorkManager one-time background check")
        }.onFailure { e ->
            Log.w(TAG, "WorkManager immediate check skipped/failed: ${e.message}")
        }
    }

    fun cancelPeriodicCheck(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
            Log.d(TAG, "Cancelled periodic WorkManager check")
        }.onFailure { e ->
            Log.w(TAG, "WorkManager cancel skipped/failed: ${e.message}")
        }
    }

    fun resetNotifiedCache(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(DistressedPropertyCheckWorker.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Reset notified property and deal IDs cache")
        }
    }

    fun getPeriodicWorkInfoFlow(context: Context): Flow<List<WorkInfo>> {
        return try {
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_WORK_NAME)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    fun getImmediateWorkInfoFlow(context: Context): Flow<List<WorkInfo>> {
        return try {
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_ONE_TIME_WORK_NAME)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
}

