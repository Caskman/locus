package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDeviceUiState(
    val deviceName: String = "",
    val isChecking: Boolean = false,
    val isAvailable: Boolean? = null,
    val error: String? = null,
)

@HiltViewModel
class NewDeviceViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(NewDeviceUiState())
        val uiState: StateFlow<NewDeviceUiState> = _uiState.asStateFlow()

        private val nameRegex = Regex("^[a-z0-9-]+$")

        fun updateDeviceName(name: String) {
            val isValidFormat = nameRegex.matches(name) || name.isEmpty()
            if (isValidFormat) {
                _uiState.value = _uiState.value.copy(deviceName = name, error = null, isAvailable = null)
            } else {
                _uiState.value =
                    _uiState.value.copy(
                        deviceName = name,
                        error = "Use lowercase, numbers, and hyphens only",
                    )
            }
        }

        fun checkAvailability() {
            val name = _uiState.value.deviceName
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Device name cannot be empty")
                return
            }

            if (!nameRegex.matches(name)) {
                _uiState.value = _uiState.value.copy(error = "Invalid format")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isChecking = true, error = null)

                // Simulating network delay for mock availability check
                kotlinx.coroutines.delay(SIMULATED_DELAY_MS)

                // Mocked success
                _uiState.value =
                    _uiState.value.copy(
                        isChecking = false,
                        isAvailable = true,
                    )
            }
        }

        companion object {
            private const val SIMULATED_DELAY_MS = 500L
        }
    }
