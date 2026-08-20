package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.ConnectivityObserver
import com.example.util.ConnectivityStatus
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SyncOutboxManager private constructor(
    private val context: Context,
    private val syncOutboxDao: SyncOutboxDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val pendingCountFlow: Flow<Int> = syncOutboxDao.getPendingCountFlow()

    init {
        // Automatically monitor network connectivity and flush outbox when online
        scope.launch {
            connectivityObserver.observe().collect { status ->
                if (status == ConnectivityStatus.AVAILABLE) {
                    Log.d("SyncOutboxManager", "Network AVAILABLE: Attempting to flush pending sync outbox...")
                    flushPendingActions()
                }
            }
        }
    }

    suspend fun enqueueAction(
        actionType: SyncActionType,
        targetEntityId: Long,
        targetEntityType: String = "PROPERTY_DEAL",
        payloadJson: String = ""
    ): Long {
        val action = SyncOutboxAction(
            actionType = actionType.name,
            targetEntityId = targetEntityId,
            targetEntityType = targetEntityType,
            payloadJson = payloadJson,
            createdAtTimestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val id = syncOutboxDao.insertAction(action)
        Log.d("SyncOutboxManager", "Enqueued sync action #$id ($actionType for $targetEntityType #$targetEntityId)")

        // If online, immediately trigger sync
        if (connectivityObserver.isConnected()) {
            scope.launch { flushPendingActions() }
        }

        return id
    }

    suspend fun flushPendingActions(): Int {
        if (_isSyncing.value) return 0
        val pendingList = syncOutboxDao.getPendingActionsList()
        if (pendingList.isEmpty()) return 0

        _isSyncing.value = true
        var processedCount = 0
        try {
            Log.d("SyncOutboxManager", "Flushing ${pendingList.size} pending actions...")
            for (action in pendingList) {
                try {
                    // Process each action idempotently
                    val success = processSingleAction(action)
                    if (success) {
                        syncOutboxDao.deleteAction(action)
                        processedCount++
                    } else {
                        val updated = action.copy(
                            retryCount = action.retryCount + 1,
                            syncStatus = if (action.retryCount >= 3) SyncStatus.FAILED.name else SyncStatus.PENDING.name,
                            errorMessage = "Sync retry limit or server rejection"
                        )
                        syncOutboxDao.updateAction(updated)
                    }
                } catch (e: Exception) {
                    Log.e("SyncOutboxManager", "Error syncing action #${action.id}: ${e.message}")
                }
            }
            if (processedCount > 0) {
                userPreferencesDataStore.updateLastSyncedTimestamp(System.currentTimeMillis())
            }
        } finally {
            _isSyncing.value = false
        }
        return processedCount
    }

    private suspend fun processSingleAction(action: SyncOutboxAction): Boolean {
        delay(80) // Simulate minimal network roundtrip
        Log.d("SyncOutboxManager", "Synchronized action: ${action.actionType} on ${action.targetEntityType} #${action.targetEntityId}")
        return true
    }

    companion object {
        @Volatile
        private var INSTANCE: SyncOutboxManager? = null

        fun getInstance(context: Context): SyncOutboxManager {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val ds = UserPreferencesDataStore.getInstance(context)
                INSTANCE ?: SyncOutboxManager(
                    context.applicationContext,
                    db.syncOutboxDao(),
                    ds
                ).also { INSTANCE = it }
            }
        }
    }
}
