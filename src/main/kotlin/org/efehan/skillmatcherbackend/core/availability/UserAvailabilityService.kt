package org.efehan.skillmatcherbackend.core.availability

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
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
        request: CreateAvailabilityRequest,
    ): UserAvailabilityDto {
        validateDateRange(request.availableFrom, request.availableTo)
        checkOverlap(user, request.availableFrom, request.availableTo, excludeId = null)

        val entry =
            availabilityRepo.save(
                UserAvailabilityModel(
                    user = user,
                    availableFrom = request.availableFrom,
                    availableTo = request.availableTo,
                ),
            )
        return entry.toDto()
    }

    fun getAll(user: UserModel): List<UserAvailabilityDto> =
        availabilityRepo
            .findByUser(user)
            .sortedBy { it.availableFrom }
            .map { it.toDto() }

    fun update(
        user: UserModel,
        id: String,
        request: UpdateAvailabilityRequest,
    ): UserAvailabilityDto {
        val entry = findAndCheckOwnership(user, id)
        validateDateRange(request.availableFrom, request.availableTo)
        checkOverlap(user, request.availableFrom, request.availableTo, excludeId = id)

        entry.availableFrom = request.availableFrom
        entry.availableTo = request.availableTo
        return availabilityRepo.save(entry).toDto()
    }

    fun delete(
        user: UserModel,
        id: String,
    ) {
        val entry = findAndCheckOwnership(user, id)
        availabilityRepo.delete(entry)
    }

    private fun findAndCheckOwnership(
        user: UserModel,
        id: String,
    ): UserAvailabilityModel {
        val entry =
            availabilityRepo.findById(id).orElseThrow {
                EntryNotFoundException(
                    resource = "UserAvailability",
                    field = "id",
                    value = id,
                    errorCode = GlobalErrorCode.USER_AVAILABILITY_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )
            }

        if (entry.user.id != user.id) {
            throw AccessDeniedException(
                resource = "UserAvailability",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_ACCESS_DENIED,
                status = HttpStatus.FORBIDDEN,
            )
        }
        return entry
    }

    private fun validateDateRange(
        from: LocalDate,
        to: LocalDate,
    ) {
        require(!to.isBefore(from)) { "availableTo must not be before availableFrom" }
    }

    private fun checkOverlap(
        user: UserModel,
        from: LocalDate,
        to: LocalDate,
        excludeId: String?,
    ) {
        val overlaps =
            availabilityRepo
                .findByUser(user)
                .filter { it.id != excludeId }
                .any { entry -> from.isBefore(entry.availableTo) && to.isAfter(entry.availableFrom) }

        if (overlaps) {
            throw DuplicateEntryException(
                resource = "UserAvailability",
                field = "period",
                value = "$from–$to",
                errorCode = GlobalErrorCode.USER_AVAILABILITY_OVERLAP,
                status = HttpStatus.CONFLICT,
            )
        }
    }

    private fun UserAvailabilityModel.toDto() =
        UserAvailabilityDto(
            id = id,
            availableFrom = availableFrom,
            availableTo = availableTo,
        )
}
