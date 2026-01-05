package com.locus.android.features.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.core.domain.model.auth.OnboardingStage
import com.locus.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PermissionUiState {
    data object ForegroundPending : PermissionUiState()
    data object BackgroundPending : PermissionUiState()
    data object DeniedForever : PermissionUiState()
    data object Granted : PermissionUiState()
    data object CoarseLocationError : PermissionUiState()
}

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionUiState>(PermissionUiState.ForegroundPending)
    val uiState = _uiState.asStateFlow()

    fun updatePermissions(
        fine: Boolean,
        coarse: Boolean,
        background: Boolean,
        notifications: Boolean
    ) {
        // Precise Location (Fine) is mandatory.
        if (coarse && !fine) {
            _uiState.value = PermissionUiState.CoarseLocationError
            return
        }

        if (!fine) {
            // If neither fine nor coarse is granted, or if we are in a state where we haven't asked yet.
            // But this method receives the CURRENT state.
            // If fine is false, we need foreground permissions.
            // Check if we were denied or just haven't asked.
            // Simplified logic: If we don't have fine, we show the foreground prompt.
            // Handling "DeniedForever" requires knowing rationale status which we don't have here easily
            // without context. But the UI calls this update.
            // For now, if !fine, we assume we need to ask or it's pending.
            // We can rely on the UI to decide if it should show rationale or open settings if the
            // request fails repeatedly.

            // Note: If previously set to DeniedForever, we should keep it unless permissions change.
            // However, this method is usually called onResume or after permission result.
            // If state was DeniedForever and fine is still false, we probably stay DeniedForever until
            // the user fixes it in settings (which triggers onResume -> updatePermissions).
            // But we don't know here if the user just returned from settings without fixing it.
            // So we default to ForegroundPending, UNLESS we have explicit "Denied" signal which comes via onPermissionDenied.

            if (_uiState.value !is PermissionUiState.DeniedForever) {
                _uiState.value = PermissionUiState.ForegroundPending
            }
            return
        }

        // If we have Fine location...

        // Notifications are optional (API 33+), so we don't block on them,
        // but we request them alongside Foreground usually.
        // We proceed to Background check regardless of notification status.

        if (!background) {
             if (_uiState.value !is PermissionUiState.DeniedForever) {
                _uiState.value = PermissionUiState.BackgroundPending
             }
            return
        }

        // If Fine and Background are granted
        _uiState.value = PermissionUiState.Granted
    }

    fun onPermissionDenied(showRationale: Boolean) {
        if (!showRationale) {
            _uiState.value = PermissionUiState.DeniedForever
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
        }
    }
}
