package com.locus.core.domain.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DomainExceptionTest {
    @Test
    fun `NetworkError Offline exists`() {
        val error = DomainException.NetworkError.Offline
        assertThat(error.message).isEqualTo("Network is offline")
    }

    @Test
    fun `NetworkError Timeout exists`() {
        val error = DomainException.NetworkError.Timeout()
        assertThat(error.message).isEqualTo("Request timed out")
    }

    @Test
    fun `NetworkError ServerError exists`() {
        val error = DomainException.NetworkError.ServerError(500, "Internal Server Error")
        assertThat(error.code).isEqualTo(500)
        assertThat(error.message).isEqualTo("Internal Server Error")
    }

    @Test
    fun `NetworkError Generic exists`() {
        val cause = RuntimeException("Unknown")
        val error = DomainException.NetworkError.Generic(cause)
        assertThat(error.cause).isEqualTo(cause)
    }

    @Test
    fun `AuthError InvalidCredentials exists`() {
        assertThat(DomainException.AuthError.InvalidCredentials.message).isEqualTo("Invalid credentials provided")
    }

    @Test
    fun `AuthError Expired exists`() {
        assertThat(DomainException.AuthError.Expired.message).isEqualTo("Credentials have expired")
    }

    @Test
    fun `AuthError AccessDenied exists`() {
        assertThat(DomainException.AuthError.AccessDenied.message).isEqualTo("Access denied")
    }

    @Test
    fun `AuthError Generic exists`() {
        val cause = RuntimeException("Auth fail")
        val error = DomainException.AuthError.Generic(cause)
        assertThat(error.cause).isEqualTo(cause)
    }

    @Test
    fun `S3Error BucketNotFound exists`() {
        val error = DomainException.S3Error.BucketNotFound("my-bucket")
        assertThat(error.message).isEqualTo("Bucket 'my-bucket' not found")
        assertThat(error.bucketName).isEqualTo("my-bucket")
    }

    @Test
    fun `S3Error Generic exists`() {
        val cause = RuntimeException("S3 fail")
        val error = DomainException.S3Error.Generic(cause)
        assertThat(error.cause).isEqualTo(cause)
    }

    @Test
    fun `BatteryCriticalException exists`() {
        assertThat(DomainException.BatteryCriticalException.message).isEqualTo("Battery level is critical")
    }

    @Test
    fun `ProvisioningError StackExists exists`() {
        val error = DomainException.ProvisioningError.StackExists("my-stack")
        assertThat(error.message).isEqualTo("Stack 'my-stack' already exists")
    }

    @Test
    fun `ProvisioningError Permissions exists`() {
        val error = DomainException.ProvisioningError.Permissions("No perms")
        assertThat(error.message).isEqualTo("No perms")
    }

    @Test
    fun `ProvisioningError Quota exists`() {
        val error = DomainException.ProvisioningError.Quota("Quota exceeded")
        assertThat(error.message).isEqualTo("Quota exceeded")
    }

    @Test
    fun `ProvisioningError DeploymentFailed exists`() {
        val error = DomainException.ProvisioningError.DeploymentFailed("Failed")
        assertThat(error.message).isEqualTo("Failed")
    }

    @Test
    fun `ProvisioningError Wait exists`() {
        val error = DomainException.ProvisioningError.Wait("Waiting")
        assertThat(error.message).isEqualTo("Waiting")
    }
}
