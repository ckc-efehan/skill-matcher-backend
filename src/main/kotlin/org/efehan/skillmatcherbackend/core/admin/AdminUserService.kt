package org.efehan.skillmatcherbackend.core.admin

import org.efehan.skillmatcherbackend.core.invitation.InvitationService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RefreshTokenRepository
import org.efehan.skillmatcherbackend.persistence.RoleRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer

@Service
@Transactional
class AdminUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val invitationService: InvitationService,
    private val refreshTokenRepository: RefreshTokenRepository,
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

    fun updateUserStatus(
        userId: String,
        enabled: Boolean,
    ) {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow {
                    EntryNotFoundException(
                        resource = "User",
                        field = "id",
                        value = userId,
                        errorCode = GlobalErrorCode.USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }
        user.isEnabled = enabled
        userRepository.save(user)

        if (!enabled) {
            refreshTokenRepository.revokeAllUserTokens(userId)
        }
    }

    fun listUsers(): List<AdminUserListResponse> =
        userRepository.findAll().map { user ->
            AdminUserListResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role = user.role.name,
                isEnabled = user.isEnabled,
                createdDate = user.createdDate,
            )
        }

    fun updateUserRole(
        userId: String,
        roleName: String,
    ) {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow {
                    EntryNotFoundException(
                        resource = "User",
                        field = "id",
                        value = userId,
                        errorCode = GlobalErrorCode.USER_NOT_FOUND,
                        status = HttpStatus.NOT_FOUND,
                    )
                }

        val role =
            roleRepository.findByName(roleName.uppercase())
                ?: throw EntryNotFoundException(
                    resource = "Role",
                    field = "name",
                    value = roleName,
                    errorCode = GlobalErrorCode.ROLE_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )

        user.role = role
        userRepository.save(user)
        refreshTokenRepository.revokeAllUserTokens(user.id)
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
