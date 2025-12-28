package com.locus.core.data.source.remote.aws

import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.StackOutputs
import com.locus.core.domain.result.LocusResult

/**
 * Interface for provisioning AWS infrastructure using CloudFormation.
 */
interface InfrastructureProvisioner {
    /**
     * Creates or updates the CloudFormation stack.
     * This method is responsible for waiting/polling until the stack creation is complete.
     */
    suspend fun createStack(
        name: String,
        parameters: Map<String, String>,
        credentials: BootstrapCredentials,
    ): LocusResult<StackOutputs>

    /**
     * Finds the Locus data bucket associated with the account.
     * Uses the provided bootstrap credentials to perform discovery.
     */
    suspend fun findLocusBucket(credentials: BootstrapCredentials): LocusResult<List<String>>
}
