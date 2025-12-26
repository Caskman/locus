package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthStateTest {
    @Test
    fun `Uninitialized state exists`() {
        val state = AuthState.Uninitialized
        assertThat(state).isInstanceOf(AuthState::class.java)
    }

    @Test
    fun `SetupPending state exists`() {
        val state = AuthState.SetupPending
        assertThat(state).isInstanceOf(AuthState::class.java)
    }

    @Test
    fun `Authenticated state exists`() {
        val state = AuthState.Authenticated
        assertThat(state).isInstanceOf(AuthState::class.java)
    }
}
