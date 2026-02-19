package org.efehan.skillmatcherbackend.fixtures.requests

import org.efehan.skillmatcherbackend.core.availability.CreateAvailabilityRequest
import org.efehan.skillmatcherbackend.core.availability.UpdateAvailabilityRequest
import java.time.LocalDate

object UserAvailabilityFixtures {
    fun buildCreateAvailabilityRequest(
        availableFrom: LocalDate = LocalDate.of(2026, 3, 1),
        availableTo: LocalDate = LocalDate.of(2026, 6, 1),
    ): CreateAvailabilityRequest =
        CreateAvailabilityRequest(
            availableFrom = availableFrom,
            availableTo = availableTo,
        )

    fun buildUpdateAvailabilityRequest(
        availableFrom: LocalDate = LocalDate.of(2026, 4, 1),
        availableTo: LocalDate = LocalDate.of(2026, 7, 1),
    ): UpdateAvailabilityRequest =
        UpdateAvailabilityRequest(
            availableFrom = availableFrom,
            availableTo = availableTo,
        )
}
