package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.result.DomainException
import org.junit.Test

class ProvisioningStateTest {
    @Test
    fun `Idle state exists`() {
        assertThat(ProvisioningState.Idle).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `ValidatingInput state exists`() {
        assertThat(ProvisioningState.ValidatingInput).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `VerifyingBootstrapKeys state exists`() {
        assertThat(ProvisioningState.VerifyingBootstrapKeys).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `DeployingStack state holds stack name`() {
        val state = ProvisioningState.DeployingStack("stack-name")
        assertThat(state.stackName).isEqualTo("stack-name")
    }

    @Test
    fun `WaitingForCompletion state holds details`() {
        val state = ProvisioningState.WaitingForCompletion("stack-name", "CREATE_IN_PROGRESS")
        assertThat(state.stackName).isEqualTo("stack-name")
        assertThat(state.status).isEqualTo("CREATE_IN_PROGRESS")
    }

    @Test
    fun `FinalizingSetup state exists`() {
        assertThat(ProvisioningState.FinalizingSetup).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `Success state exists`() {
        assertThat(ProvisioningState.Success).isInstanceOf(ProvisioningState::class.java)
    }

    @Test
    fun `Failure state holds error`() {
        val error = DomainException.NetworkError.Offline
        val state = ProvisioningState.Failure(error)
        assertThat(state.error).isEqualTo(error)
    }
}
