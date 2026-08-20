package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CircuitState {
    CLOSED,    // Operating normally
    OPEN,      // Tripped, rejecting calls immediately to protect resources
    HALF_OPEN  // Testing if source has recovered
}

data class CircuitBreakerHealth(
    val sourceKey: String,
    val state: CircuitState,
    val failureCount: Int,
    val lastFailureTimestamp: Long = 0L,
    val lastSuccessTimestamp: Long = 0L,
    val totalRequests: Int = 0,
    val totalFailures: Int = 0
)

object ScraperCircuitBreaker {
    private const val MAX_CONSECUTIVE_FAILURES = 3
    private const val RECOVERY_TIMEOUT_MS = 45_000L // 45 seconds cooldown

    private val mutex = Mutex()
    private val healthMap = mutableMapOf<String, CircuitBreakerHealth>()

    private val _circuitStatusFlow = MutableStateFlow<Map<String, CircuitBreakerHealth>>(emptyMap())
    val circuitStatusFlow: StateFlow<Map<String, CircuitBreakerHealth>> = _circuitStatusFlow.asStateFlow()

    suspend fun <T> execute(
        sourceKey: String,
        fallback: suspend () -> T,
        block: suspend () -> T
    ): Result<T> {
        val currentState = getOrUpdateState(sourceKey)

        if (currentState == CircuitState.OPEN) {
            Log.w("ScraperCircuitBreaker", "Circuit OPEN for source '$sourceKey' - returning cached fallback directly")
            return try {
                Result.success(fallback())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val result = block()
            recordSuccess(sourceKey)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("ScraperCircuitBreaker", "Execution failed for source '$sourceKey': ${e.message}")
            recordFailure(sourceKey)
            try {
                Result.success(fallback())
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun getOrUpdateState(sourceKey: String): CircuitState {
        mutex.withLock {
            val entry = healthMap.getOrPut(sourceKey) {
                CircuitBreakerHealth(sourceKey = sourceKey, state = CircuitState.CLOSED, failureCount = 0)
            }

            if (entry.state == CircuitState.OPEN) {
                val now = System.currentTimeMillis()
                if (now - entry.lastFailureTimestamp > RECOVERY_TIMEOUT_MS) {
                    val halfOpen = entry.copy(state = CircuitState.HALF_OPEN)
                    healthMap[sourceKey] = halfOpen
                    _circuitStatusFlow.value = healthMap.toMap()
                    return CircuitState.HALF_OPEN
                }
            }
            return entry.state
        }
    }

    private suspend fun recordSuccess(sourceKey: String) {
        mutex.withLock {
            val entry = healthMap.getOrPut(sourceKey) {
                CircuitBreakerHealth(sourceKey = sourceKey, state = CircuitState.CLOSED, failureCount = 0)
            }
            val updated = entry.copy(
                state = CircuitState.CLOSED,
                failureCount = 0,
                lastSuccessTimestamp = System.currentTimeMillis(),
                totalRequests = entry.totalRequests + 1
            )
            healthMap[sourceKey] = updated
            _circuitStatusFlow.value = healthMap.toMap()
        }
    }

    private suspend fun recordFailure(sourceKey: String) {
        mutex.withLock {
            val entry = healthMap.getOrPut(sourceKey) {
                CircuitBreakerHealth(sourceKey = sourceKey, state = CircuitState.CLOSED, failureCount = 0)
            }
            val newFailureCount = entry.failureCount + 1
            val newState = if (newFailureCount >= MAX_CONSECUTIVE_FAILURES) CircuitState.OPEN else entry.state

            val updated = entry.copy(
                state = newState,
                failureCount = newFailureCount,
                lastFailureTimestamp = System.currentTimeMillis(),
                totalRequests = entry.totalRequests + 1,
                totalFailures = entry.totalFailures + 1
            )
            healthMap[sourceKey] = updated
            _circuitStatusFlow.value = healthMap.toMap()

            if (newState == CircuitState.OPEN) {
                Log.w("ScraperCircuitBreaker", "⚠️ Circuit Breaker TRIPPED to OPEN for source '$sourceKey' after $newFailureCount failures.")
            }
        }
    }

    suspend fun resetCircuit(sourceKey: String) {
        mutex.withLock {
            healthMap[sourceKey] = CircuitBreakerHealth(
                sourceKey = sourceKey,
                state = CircuitState.CLOSED,
                failureCount = 0,
                lastSuccessTimestamp = System.currentTimeMillis()
            )
            _circuitStatusFlow.value = healthMap.toMap()
        }
    }
}
