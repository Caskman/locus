package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.locus.android.features.onboarding.work.ProvisioningWorker
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDeviceUiState(
    val deviceName: String = "",
    val isNameValid: Boolean = false,
    val error: String? = null,
    val isChecking: Boolean = false,
    val availabilityMessage: String? = null,
)

@HiltViewModel
class NewDeviceViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewDeviceUiState())
        val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

        val provisioningState: StateFlow<ProvisioningState> =
            authRepository.getProvisioningState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = ProvisioningState.Idle,
                )

        private val deviceNameRegex = Regex("^[a-z0-9-]+$")

        fun onDeviceNameChanged(name: String) {
            val isValid = name.isNotBlank() && deviceNameRegex.matches(name)
            _uiState.update {
                it.copy(
                    deviceName = name,
                    isNameValid = isValid,
                    error =
                        if (!isValid && name.isNotEmpty()) {
                            "Only lowercase letters, numbers, and hyphens allowed"
                        } else {
                            null
                        },
                )
            }
        }

        fun checkAvailability() {
            // Mock/Stub implementation for now as per plan
            val name = _uiState.value.deviceName
            if (!deviceNameRegex.matches(name)) return

            viewModelScope.launch {
                _uiState.update { it.copy(isChecking = true) }
                // Simulate network delay
                delay(SIMULATED_DELAY_MS)

                // Basic mock logic: reject if "existing" is in the name for testing
                if (name.contains("existing")) {
                    _uiState.update { it.copy(isChecking = false, error = "Device name unavailable") }
                } else {
                    _uiState.update { it.copy(isChecking = false, availabilityMessage = "Available!") }
                }
            }
        }

        fun deployStack() {
            val name = _uiState.value.deviceName
            // Enqueue WorkManager job to ensure process survival
            val workRequest =
                OneTimeWorkRequest.Builder(ProvisioningWorker::class.java)
                    .setInputData(
                        workDataOf(
                            ProvisioningWorker.KEY_MODE to ProvisioningWorker.MODE_PROVISION,
                            ProvisioningWorker.KEY_DEVICE_NAME to name,
                        ),
                    )
                    .build()

            workManager.enqueueUniqueWork(
                ProvisioningWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
        }

        fun reset() {
            viewModelScope.launch {
                authRepository.updateProvisioningState(ProvisioningState.Idle)
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }
    }
