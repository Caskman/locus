package com.locus.core.data.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import com.locus.core.domain.usecase.ProvisioningUseCase
import com.locus.core.domain.usecase.RecoverAccountUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProvisioningWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        private val provisioningUseCase: ProvisioningUseCase,
        private val recoverAccountUseCase: RecoverAccountUseCase,
        private val authRepository: AuthRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            // Set Foreground immediately
            setForeground(getForegroundInfo())

            val mode = inputData.getString(KEY_MODE) ?: return Result.failure()
            val param = inputData.getString(KEY_PARAM) ?: return Result.failure()

            val credsResult = authRepository.getBootstrapCredentials()
            val creds =
                if (credsResult is LocusResult.Success && credsResult.data != null) {
                    credsResult.data!!
                } else {
                    Log.e(TAG, "Failed to retrieve bootstrap credentials from storage")
                    val failureResult =
                        Result.failure(
                            androidx.work.workDataOf(
                                "error_message" to "Bootstrap credentials missing",
                            ),
                        )
                    // Ensure repository state is updated if it wasn't already
                    authRepository.updateProvisioningState(
                        com.locus.core.domain.model.auth.ProvisioningState.Failure(
                            DomainException.AuthError.InvalidCredentials,
                        ),
                    )
                    return failureResult
                }

            val result =
                when (mode) {
                    MODE_NEW_DEVICE -> provisioningUseCase(creds, param)
                    MODE_RECOVERY -> recoverAccountUseCase(creds, param)
                    else -> LocusResult.Failure(DomainException.AuthError.InvalidCredentials) // Should not happen
                }

            return when (result) {
                is LocusResult.Success -> Result.success()
                is LocusResult.Failure -> {
                    // Extract error message for WorkManager output if needed
                    val msg = result.error.message ?: "Unknown provisioning error"
                    Result.failure(
                        androidx.work.workDataOf("error_message" to msg),
                    )
                }
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            val title = "Setting up Locus"
            val notification = createNotification(title)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        private fun createNotification(title: String): Notification {
            val channelId = "setup_status"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        channelId,
                        "Setup Status",
                        NotificationManager.IMPORTANCE_LOW,
                    )
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            return NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        companion object {
            private const val TAG = "ProvisioningWorker"
            const val KEY_MODE = "mode"
            const val KEY_PARAM = "param" // Device Name or Bucket Name

            const val MODE_NEW_DEVICE = "new_device"
            const val MODE_RECOVERY = "recovery"

            const val NOTIFICATION_ID = 1001
        }
    }
