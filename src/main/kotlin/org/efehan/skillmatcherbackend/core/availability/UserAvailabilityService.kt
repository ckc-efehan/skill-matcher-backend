package org.efehan.skillmatcherbackend.core.availability

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class UserAvailabilityService(
    private val availabilityRepo: UserAvailabilityRepository,
) {
    fun create(
        user: UserModel,
        availableFrom: LocalDate,
        availableTo: LocalDate,
    ): UserAvailabilityModel {
        require(!availableTo.isBefore(availableFrom)) { "availableTo must not be before availableFrom" }

        val overlaps =
            availabilityRepo
                .findByUser(user)
                .any { entry -> availableFrom.isBefore(entry.availableTo) && availableTo.isAfter(entry.availableFrom) }
        if (overlaps) {
            throw DuplicateEntryException(
                resource = "UserAvailability",
                field = "period",
                value = "$availableFrom–$availableTo",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_OVERLAP,
                status = HttpStatus.CONFLICT,
            )
        }

        return availabilityRepo.save(
            UserAvailabilityModel(
                user = user,
                availableFrom = availableFrom,
                availableTo = availableTo,
            ),
        )
    }

    fun getAll(user: UserModel): List<UserAvailabilityModel> =
        availabilityRepo
            .findByUser(user)
            .sortedBy { it.availableFrom }

    fun update(
        user: UserModel,
        id: String,
        availableFrom: LocalDate,
        availableTo: LocalDate,
    ): UserAvailabilityModel {
        val entry =
            availabilityRepo.findByIdOrNull(id)
                ?: throw EntryNotFoundException(
                    resource = "UserAvailability",
                    field = "id",
                    value = id,
                    errorCode = GlobalErrorCode.USER_AVAILABILITY_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
        if (entry.user.id != user.id) {
            throw AccessDeniedException(
                resource = "UserAvailability",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_ACCESS_DENIED,
                status = HttpStatus.FORBIDDEN,
            )
        }

        require(!availableTo.isBefore(availableFrom)) { "availableTo must not be before availableFrom" }

        val overlaps =
            availabilityRepo
                .findByUser(user)
                .filter { it.id != id }
                .any { entry -> availableFrom.isBefore(entry.availableTo) && availableTo.isAfter(entry.availableFrom) }
        if (overlaps) {
            throw DuplicateEntryException(
                resource = "UserAvailability",
                field = "period",
                value = "$availableFrom–$availableTo",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_OVERLAP,
                status = HttpStatus.CONFLICT,
            )
        }

        entry.availableFrom = availableFrom
        entry.availableTo = availableTo
        return availabilityRepo.save(entry)
    }

    fun delete(
        user: UserModel,
        id: String,
    ) {
        val entry =
            availabilityRepo.findByIdOrNull(id)
                ?: throw EntryNotFoundException(
                    resource = "UserAvailability",
                    field = "id",
                    value = id,
                    errorCode = GlobalErrorCode.USER_AVAILABILITY_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
        if (entry.user.id != user.id) {
            throw AccessDeniedException(
                resource = "UserAvailability",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_ACCESS_DENIED,
                status = HttpStatus.FORBIDDEN,
            )
        }

        availabilityRepo.delete(entry)
    }
}
