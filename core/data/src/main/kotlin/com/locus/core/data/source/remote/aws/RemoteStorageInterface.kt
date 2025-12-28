package com.locus.core.data.source.remote.aws

import com.locus.core.domain.result.LocusResult

/**
 * Interface for runtime remote storage operations (S3).
 */
interface RemoteStorageInterface {
    /**
     * Uploads a track file to S3.
     * @param bucketName The name of the S3 bucket.
     * @param key The S3 object key.
     * @param fileContent The Gzipped content to upload.
     */
    suspend fun uploadTrack(
        bucketName: String,
        key: String,
        fileContent: ByteArray,
    ): LocusResult<Unit>
}
