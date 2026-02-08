package org.efehan.skillmatcherbackend.core.admin

import jakarta.transaction.Transactional
import org.efehan.skillmatcherbackend.core.invitation.InvitationService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RoleRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
@Transactional
class AdminUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val invitationService: InvitationService,
) {
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEntryException(
                resource = "User",
                field = "email",
                value = request.email,
                errorCode = GlobalErrorCode.USER_ALREADY_EXISTS,
                status = HttpStatus.CONFLICT,
            )
        }

        val role =
            roleRepository.findByName(request.role.uppercase()) ?: throw EntryNotFoundException(
                resource = "Role",
                field = "name",
                value = request.role,
                errorCode = GlobalErrorCode.ROLE_NOT_FOUND,
                status = HttpStatus.NOT_FOUND,
            )

        val username = generateUniqueUsername(request.firstName, request.lastName)

        val user =
            UserModel(
                username = username,
                email = request.email,
                passwordHash = null,
                firstName = request.firstName,
                lastName = request.lastName,
                role = role,
            )
        user.isEnabled = false

        val savedUser = userRepository.save(user)

        invitationService.createAndSendInvitation(savedUser)

        return CreateUserResponse(
            id = savedUser.id,
            username = username,
            email = savedUser.email,
            firstName = savedUser.firstName!!,
            lastName = savedUser.lastName!!,
            role = role.name,
        )
    }

    internal fun generateUniqueUsername(
        firstName: String,
        lastName: String,
    ): String {
        val base = "${normalizeForUsername(firstName)}.${normalizeForUsername(lastName)}"

        if (!userRepository.existsByUsername(base)) {
            return base
        }

        var suffix = 2
        while (userRepository.existsByUsername("$base$suffix")) {
            suffix++
        }
        return "$base$suffix"
    }

    internal fun normalizeForUsername(input: String): String {
        val replaced =
            input
                .lowercase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")

        val normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{M}".toRegex(), "")
            .replace("[^a-z0-9]".toRegex(), "")
    }
}
