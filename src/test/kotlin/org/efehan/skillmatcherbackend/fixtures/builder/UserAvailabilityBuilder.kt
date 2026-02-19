package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserModel
import java.time.LocalDate

class UserAvailabilityBuilder {
    fun build(
        user: UserModel = UserBuilder().build(),
        availableFrom: LocalDate = LocalDate.of(2026, 3, 1),
        availableTo: LocalDate = LocalDate.of(2026, 6, 1),
    ): UserAvailabilityModel =
        UserAvailabilityModel(
            user = user,
            availableFrom = availableFrom,
            availableTo = availableTo,
        )
}
