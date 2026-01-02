package com.locus.android.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecoveryUiState(
    val buckets: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RecoveryViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryUiState())
        val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

        init {
            loadBuckets()
        }

        fun loadBuckets() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Mock list for now to satisfy the "Logic" part of the plan without breaking compilation
                // Will be implemented properly when AuthRepository supports bucket listing
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        // Mock data
                        buckets = listOf("locus-user-backup-bucket", "locus-user-pixel7"),
                        // "Not implemented yet"
                        error = null,
                    )
            }
        }
    }
