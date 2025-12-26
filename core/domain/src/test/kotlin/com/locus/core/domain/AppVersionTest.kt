package com.locus.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {
    @Test
    fun `has correct properties`() {
        val version = AppVersion("1.0.0", 1)
        assertThat(version.versionName).isEqualTo("1.0.0")
        assertThat(version.versionCode).isEqualTo(1)
    }
}
