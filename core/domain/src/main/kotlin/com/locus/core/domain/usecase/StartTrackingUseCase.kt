package com.locus.core.domain.usecase

import javax.inject.Inject

/**
 * UseCase responsible for starting all necessary background tracking services.
 *
 * This implementation uses the [TrackingManager] abstraction to interact with the Android Service layer,
 * ensuring strict dependency separation between the pure Kotlin Domain layer and the Android App layer.
 */
interface TrackingManager {
    fun startTracking()
}

class StartTrackingUseCase
    @Inject
    constructor(
        private val trackingManager: TrackingManager,
    ) {
        operator fun invoke() {
            trackingManager.startTracking()
        }
    }
