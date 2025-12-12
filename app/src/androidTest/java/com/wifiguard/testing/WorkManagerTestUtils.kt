package com.wifiguard.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException

/**
 * Utility class for WorkManager testing with proper synchronization
 */
object WorkManagerTestUtils {
    
    /**
     * Waits for a specific work to reach the expected state with timeout
     * @param workId The ID of the work request to monitor
     * @param expectedState The expected WorkInfo.State
     * @param timeoutMs Maximum time to wait in milliseconds (default 10000ms)
     * @param pollIntervalMs Interval between state checks (default 100ms)
     * @throws TimeoutException if the work doesn't reach the expected state within timeout
     */
    suspend fun waitForWorkToFinish(
        workId: java.util.UUID,
        expectedState: WorkInfo.State,
        timeoutMs: Long = 10000L,
        pollIntervalMs: Long = 100L
    ): WorkInfo {
        val startTime = System.currentTimeMillis()
        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val workInfo = workManager.getWorkInfoById(workId).get()
            
            if (workInfo.state == expectedState) {
                return workInfo
            }
            
            if (workInfo.state == WorkInfo.State.FAILED || workInfo.state == WorkInfo.State.CANCELLED) {
                return workInfo
            }
            
            delay(pollIntervalMs)
        }
        
        throw TimeoutException(
            "Work with ID $workId did not reach state $expectedState within ${timeoutMs}ms. " +
            "Final state: ${workManager.getWorkInfoById(workId).get().state}"
        )
    }
    
    /**
     * Waits for a unique work to reach the expected state with timeout
     * @param uniqueWorkName The name of the unique work to monitor
     * @param expectedState The expected WorkInfo.State
     * @param timeoutMs Maximum time to wait in milliseconds (default 10000ms)
     * @param pollIntervalMs Interval between state checks (default 100ms)
     * @throws TimeoutException if the work doesn't reach the expected state within timeout
     */
    suspend fun waitForUniqueWorkToFinish(
        uniqueWorkName: String,
        expectedState: WorkInfo.State,
        timeoutMs: Long = 10000L,
        pollIntervalMs: Long = 100L
    ): WorkInfo {
        val startTime = System.currentTimeMillis()
        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val workInfos = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
            
            if (workInfos.isNotEmpty()) {
                val workInfo = workInfos.first()
                
                if (workInfo.state == expectedState) {
                    return workInfo
                }
                
                if (workInfo.state == WorkInfo.State.FAILED || workInfo.state == WorkInfo.State.CANCELLED) {
                    return workInfo
                }
            }
            
            delay(pollIntervalMs)
        }
        
        val finalWorkInfos = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
        val finalState = if (finalWorkInfos.isNotEmpty()) finalWorkInfos.first().state else "NOT_FOUND"
        
        throw TimeoutException(
            "Unique work '$uniqueWorkName' did not reach state $expectedState within ${timeoutMs}ms. " +
            "Final state: $finalState"
        )
    }
    
    /**
     * Cancels all work in the WorkManager - useful for test cleanup
     */
    fun cancelAllWork() {
        runBlocking {
            val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
            workManager.cancelAllWork().result.get()
            
            // Additional delay to ensure cancellation is processed
            delay(500)
        }
    }
    
    /**
     * Initializes WorkManager for testing with proper configuration
     */
    fun initializeTestWorkManager() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }
}