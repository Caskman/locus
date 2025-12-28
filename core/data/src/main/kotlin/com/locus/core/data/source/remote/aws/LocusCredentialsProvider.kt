package com.locus.core.data.source.remote.aws

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

/**
 * Dynamic Credentials Provider that fetches credentials from SecureStorageDataSource.
 * This allows S3Client to automatically use the latest credentials.
 */
class LocusCredentialsProvider
    @Inject
    constructor(
        private val secureStorage: SecureStorageDataSource,
    ) : CredentialsProvider {
        override suspend fun resolve(attributes: aws.smithy.kotlin.runtime.collections.Attributes): Credentials {
            val credentialsResult = secureStorage.getRuntimeCredentials()

            return when (credentialsResult) {
                is LocusResult.Success -> {
                    // Explicitly handle potential nullability if T is nullable, though it shouldn't be based on interface.
                    // However, defensive coding solves the compiler ambiguity.
                    val creds: RuntimeCredentials? = credentialsResult.data
                    if (creds == null) {
                        throw IllegalStateException("Runtime credentials data is null.")
                    }
                    Credentials(
                        accessKeyId = creds.accessKeyId,
                        secretAccessKey = creds.secretAccessKey,
                        // Runtime credentials for IAM Users do not use session tokens.
                        sessionToken = null,
                    )
                }
                is LocusResult.Failure -> {
                    throw IllegalStateException("Failed to retrieve runtime credentials from secure storage.", credentialsResult.error)
                }
            }
        }
    }
