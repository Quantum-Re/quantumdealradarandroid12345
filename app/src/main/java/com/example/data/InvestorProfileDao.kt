package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestorProfileDao {
    @Query("SELECT * FROM investor_profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: Long = 1L): Flow<InvestorProfile?>

    @Query("SELECT * FROM investor_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: Long = 1L): InvestorProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: InvestorProfile)
}
