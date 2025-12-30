package com.locus.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
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

class RecoverAccountUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigurationRepository>(relaxed = true)
    private val cloudFormationClient = mockk<CloudFormationClient>()
    private val s3Client = mockk<S3Client>()
    private val resourceProvider = mockk<ResourceProvider>()

    private val useCase =
        RecoverAccountUseCase(
            authRepository,
            configRepository,
            cloudFormationClient,
            s3Client,
            resourceProvider,
        )

    @Test
    fun `successful recovery flow`() =
        runBlocking {
            // Given
            val creds = BootstrapCredentials("access", "secret", "token", "us-east-1")
            val bucketName = "locus-bucket-old"
            val template = "template-body"
            val stackId = "arn:aws:cloudformation:us-east-1:123456789012:stack/locus-user-uuid/uuid"

            coEvery { s3Client.getBucketTags(creds, bucketName) } returns
                LocusResult.Success(mapOf("aws:cloudformation:stack-name" to "locus-user-old"))

            every { resourceProvider.getStackTemplate() } returns template
            coEvery { authRepository.updateProvisioningState(any()) } returns Unit
            coEvery { cloudFormationClient.createStack(any(), any(), any(), any()) } returns LocusResult.Success("stack-id")

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
                            ),
                        ),
                    ),
                )

            coEvery { configRepository.initializeIdentity(any(), any()) } returns LocusResult.Success(Unit)
            coEvery { authRepository.promoteToRuntimeCredentials(any()) } returns LocusResult.Success(Unit)

            // When
            val result = useCase(creds, bucketName)

            // Then
            assertThat(result).isInstanceOf(LocusResult.Success::class.java)

            // Verify we created a new stack (captured stack name will vary due to UUID)
            coVerify {
                cloudFormationClient.createStack(
                    creds,
                    match { it.startsWith("locus-user-") },
                    template,
                    match { it["BucketName"] == bucketName },
                )
            }

            val slot = slot<RuntimeCredentials>()
            coVerify { authRepository.promoteToRuntimeCredentials(capture(slot)) }
            assertThat(slot.captured.accessKeyId).isEqualTo("rk")
            assertThat(slot.captured.bucketName).isEqualTo(bucketName)
            assertThat(slot.captured.accountId).isEqualTo("123456789012")
        }
}
