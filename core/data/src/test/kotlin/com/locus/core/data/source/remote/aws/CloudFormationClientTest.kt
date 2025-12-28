package com.locus.core.data.source.remote.aws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloudFormationClientTest {
    private lateinit var context: Context
    private lateinit var client: CloudFormationClient

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        client = CloudFormationClient(context)
    }

    @Test
    fun `createStack returns failure when template loading fails or client fails`() =
        runTest {
            // Similarly, we can't easily mock the AWS SDK CloudFormationClient builder.
            // We just verify that the method is callable and returns a LocusResult.
            // In a real environment, this would try to load the asset and then build the client.
            // If the asset exists (which it should in core/data/src/main/assets), it proceeds to build the client.

            // We pass dummy credentials.
            val dummyCreds = com.locus.core.domain.model.auth.BootstrapCredentials("id", "secret", "token", "us-east-1")

            val result = client.createStack("test-stack", emptyMap(), dummyCreds)

            // Should be a Failure (likely due to client build or network).
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
        }
}
