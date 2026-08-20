package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class SyncActionType {
    UPDATE_DEAL_STAGE,
    TOGGLE_BOOKMARK,
    SAVE_DEAL_NOTE,
    SUBMIT_PROPERTY_OFFER,
    UPDATE_PROPERTY_STATUS,
    RECORD_PRICE_DROP
}

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

@Immutable
@Entity(tableName = "sync_outbox_actions")
data class SyncOutboxAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // from SyncActionType
    val targetEntityId: Long,
    val targetEntityType: String, // "PROPERTY_DEAL", "PROPERTY", etc.
    val payloadJson: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: String = SyncStatus.PENDING.name,
    val errorMessage: String? = null
)

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: SyncOutboxAction): Long

    @Query("SELECT * FROM sync_outbox_actions WHERE syncStatus = 'PENDING' ORDER BY createdAtTimestamp ASC")
    fun getPendingActions(): Flow<List<SyncOutboxAction>>

    @Query("SELECT * FROM sync_outbox_actions WHERE syncStatus = 'PENDING' ORDER BY createdAtTimestamp ASC")
    suspend fun getPendingActionsList(): List<SyncOutboxAction>

    @Query("SELECT COUNT(*) FROM sync_outbox_actions WHERE syncStatus = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Update
    suspend fun updateAction(action: SyncOutboxAction)

    @Delete
    suspend fun deleteAction(action: SyncOutboxAction)

    @Query("DELETE FROM sync_outbox_actions WHERE syncStatus = 'SYNCED'")
    suspend fun clearSyncedActions()

    @Query("DELETE FROM sync_outbox_actions")
    suspend fun clearAll()
}
