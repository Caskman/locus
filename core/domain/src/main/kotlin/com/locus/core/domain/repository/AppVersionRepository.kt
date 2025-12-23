package com.locus.core.domain.repository

import com.locus.core.domain.AppVersion
import com.locus.core.domain.LocusResult

interface AppVersionRepository {
    suspend fun getAppVersion(): LocusResult<AppVersion>
}
