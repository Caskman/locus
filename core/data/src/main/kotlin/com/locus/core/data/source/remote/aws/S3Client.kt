package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject
import aws.sdk.kotlin.services.s3.S3Client as AwsS3Client

/**
 * Client for interacting with S3 using runtime credentials.
 */
class S3Client
    @Inject
    constructor(
        private val credentialsProvider: LocusCredentialsProvider,
    ) : RemoteStorageInterface {
        // Region should ideally be configurable, defaulting to us-east-1 if not specified in bucket metadata
        // For now, assuming standard region or handled by SDK resolution if bucket name implies it (which it doesn't always).
        // TODO: Inject region or retrieve from config.
        private val region = "us-east-1"

        override suspend fun uploadTrack(
            bucketName: String,
            key: String,
            fileContent: ByteArray,
        ): LocusResult<Unit> {
            return try {
                val client =
                    AwsS3Client {
                        this.region = this@S3Client.region
                        this.credentialsProvider = this@S3Client.credentialsProvider
                    }

                client.use { s3Client ->
                    val request =
                        PutObjectRequest {
                            this.bucket = bucketName
                            this.key = key
                            this.body = ByteStream.fromBytes(fileContent)
                        }

                    s3Client.putObject(request)
                    LocusResult.Success(Unit)
                }
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }
    }
