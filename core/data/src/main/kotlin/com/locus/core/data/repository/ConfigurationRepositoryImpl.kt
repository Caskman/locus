package com.locus.core.data.repository

import android.content.SharedPreferences
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepositoryImpl
    @Inject
    constructor(
        private val secureStorage: SecureStorageDataSource,
        private val prefs: SharedPreferences,
    ) : ConfigurationRepository {
        override suspend fun initializeIdentity(
            deviceId: String,
            salt: String,
        ): LocusResult<Unit> {
            try {
                // Store Device ID in plain prefs
                prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
                // Store Salt (usually stored alongside runtime credentials, but we can store it here for now if needed,
                // however SecureStorageDataSource manages salt with credentials.
                // The interface requires us to pass it. If we are setting up, we assume we might save it.
                // But wait, SecureStorageDataSource saves salt with saveRuntimeCredentials.
                // Here we might just want to persist it to the fallback plain prefs if needed, or rely on runtime creds flow.
                // The plan says "Implement a basic version... Can use SharedPreferences or DataStore to persist deviceId and salt."

                // Let's store salt in plain prefs as a backup/fallback as SecureStorage does.
                prefs.edit().putString(SecureStorageDataSource.KEY_SALT, salt).apply()

                return LocusResult.Success(Unit)
            } catch (e: Exception) {
                return LocusResult.Failure(e)
            }
        }

        override suspend fun getDeviceId(): String? {
            return prefs.getString(KEY_DEVICE_ID, null)
        }

        override suspend fun getTelemetrySalt(): String? {
            return secureStorage.getTelemetrySalt()
        }

        companion object {
            private const val KEY_DEVICE_ID = "device_id"
        }
    }
