package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.StackDetails
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProvisioningUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val cloudFormationClient = mockk<CloudFormationClient>()
    private val resourceProvider = mockk<ResourceProvider>()

    private val useCase =
        ProvisioningUseCase(
            authRepository,
            configRepository,
            cloudFormationClient,
            resourceProvider,
        )

    @Test
    fun `successful provisioning flow`() =
        runBlocking {
            // Given
            val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
            val deviceName = "my-device"
            val template = "template-body"
            val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-my-device/uuid"

            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

            // Mock polling: first In Progress, then Complete
            coEvery { cloudFormationClient.describeStack(any(), any()) } returnsMany
                listOf(
                    LocusResult.Success(StackDetails(stackId, "CREATE_IN_PROGRESS", null)),
                    LocusResult.Success(
                        StackDetails(
                            stackId,
                            "CREATE_COMPLETE",
                            mapOf(
                                "RuntimeAccessKeyId" to "rk",
                                "RuntimeSecretAccessKey" to "rs",
                                "BucketName" to "rb",
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, deviceName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            coVerify {
                cloudFormationClient.createStack(
                    creds,
                    "locus-user-$deviceName",
                    template,
                    mapOf("StackName" to deviceName),
                )
            }
            coVerify(atLeast = 2) { cloudFormationClient.describeStack(creds, "locus-user-$deviceName") }
            coVerify { configRepository.initializeIdentity(any(), any()) }

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo("rb")
            assertThat(slot.captured.accountId).isEqualTo("123456789012")
        }
}
