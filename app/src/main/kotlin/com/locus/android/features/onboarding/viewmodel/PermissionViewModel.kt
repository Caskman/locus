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
            _uiState.value = PermissionUiState.ForegroundPending
            return
        }

        // If we have Fine location...

        // Notifications are optional (API 33+), so we don't block on them,
        // but we request them alongside Foreground usually.
        // We proceed to Background check regardless of notification status.

        if (!background) {
            _uiState.value = PermissionUiState.BackgroundPending
            return
        }

        // If Fine and Background are granted
        _uiState.value = PermissionUiState.Granted
    }

    fun onPermissionDenied(isPermanentlyDenied: Boolean) {
        if (isPermanentlyDenied) {
            _uiState.value = PermissionUiState.DeniedForever
        } else {
            // If just denied (but not forever), we revert to pending so they can try again.
            // Logic in updatePermissions will likely set it to ForegroundPending/BackgroundPending anyway
            // based on what is missing.
            // So strictly speaking, we might not need to do anything here if updatePermissions is called right after.
            // But if we want to show a specific "Rationale" UI state, we could add one.
            // For now, let's trust updatePermissions to reset to Pending,
            // and we only override if it's DeniedForever.
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            authRepository.setOnboardingStage(OnboardingStage.COMPLETE)
        }
    }
}
