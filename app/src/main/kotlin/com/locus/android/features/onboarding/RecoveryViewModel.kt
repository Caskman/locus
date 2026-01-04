package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.locus.android.features.onboarding.work.ProvisioningWorker
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.LocusResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class RecoveryUiState(
    val buckets: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RecoveryViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                when (val result = authRepository.getRecoveryBuckets()) {
                    is LocusResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                buckets = result.data,
                            )
                        }
                    }
                    is LocusResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error.message ?: "Failed to list buckets",
                            )
                        }
                    }
                }
            }
        }

        fun recover(bucketName: String) {
            viewModelScope.launch {
                // 1. Set Stage to PROVISIONING (Setup Trap)
                authRepository.setOnboardingStage(com.locus.core.domain.model.auth.OnboardingStage.PROVISIONING)

                // 2. Enqueue Work
                val inputData =
                    workDataOf(
                        ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_RECOVER,
                        ProvisioningWorker.KEY_BUCKET_NAME to bucketName,
                    )

                val workRequest =
                    OneTimeWorkRequest.Builder(ProvisioningWorker::class.java)
                        .setInputData(inputData)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            WorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS,
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    ProvisioningWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
            }
        }
    }
