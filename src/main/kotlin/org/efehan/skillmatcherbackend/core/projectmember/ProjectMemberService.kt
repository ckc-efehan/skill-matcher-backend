package org.efehan.skillmatcherbackend.core.projectmember

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberModel
import org.efehan.skillmatcherbackend.persistence.ProjectMemberRepository
import org.efehan.skillmatcherbackend.persistence.ProjectMemberStatus
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.persistence.UserRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class ProjectMemberService(
    private val projectRepo: ProjectRepository,
    private val userRepo: UserRepository,
    private val memberRepo: ProjectMemberRepository,
) {
    fun addMember(
        owner: UserModel,
        projectId: String,
        request: AddProjectMemberRequest,
    ): ProjectMemberDto {
        val project = findProjectOrThrow(projectId)
        checkOwnership(project, owner)

        val user = findUserOrThrow(request.userId)

        // bereits aktives Mitglied?
        memberRepo.findByProjectAndUser(project, user)?.let { existing ->
            if (existing.status == ProjectMemberStatus.ACTIVE) {
                throw DuplicateEntryException(
                    resource = "ProjectMember",
                    field = "userId",
                    value = user.id,
                    errorCode = GlobalErrorCode.PROJECT_MEMBER_DUPLICATE,
                    status = HttpStatus.CONFLICT,
                )
            }
            // LEFT → reaktivieren
            existing.status = ProjectMemberStatus.ACTIVE
            return memberRepo.save(existing).toDto()
        }

        // maxMembers prüfen
        val activeCount = memberRepo.countByProjectAndStatus(project, ProjectMemberStatus.ACTIVE)
        if (activeCount >= project.maxMembers) {
            throw DuplicateEntryException(
                resource = "ProjectMember",
                field = "projectId",
                value = projectId,
                errorCode = GlobalErrorCode.PROJECT_FULL,
                status = HttpStatus.CONFLICT,
            )
        }

        val member =
            memberRepo.save(
                ProjectMemberModel(
                    project = project,
                    user = user,
                    status = ProjectMemberStatus.ACTIVE,
                    joinedDate = Instant.now(),
                ),
            )
        return member.toDto()
    }

    fun getMembers(projectId: String): List<ProjectMemberDto> {
        val project = findProjectOrThrow(projectId)
        return memberRepo
            .findByProject(project)
            .filter { it.status == ProjectMemberStatus.ACTIVE }
            .map { it.toDto() }
    }

    fun removeMember(
        owner: UserModel,
        projectId: String,
        userId: String,
    ) {
        val project = findProjectOrThrow(projectId)
        checkOwnership(project, owner)

        val user = findUserOrThrow(userId)
        val member =
            memberRepo.findByProjectAndUser(project, user)
                ?: throw EntryNotFoundException(
                    resource = "ProjectMember",
                    field = "userId",
                    value = userId,
                    errorCode = GlobalErrorCode.PROJECT_MEMBER_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )

        member.status = ProjectMemberStatus.LEFT
        memberRepo.save(member)
    }

    fun leaveProject(
        user: UserModel,
        projectId: String,
    ) {
        val project = findProjectOrThrow(projectId)
        val member =
            memberRepo.findByProjectAndUser(project, user)
                ?: throw EntryNotFoundException(
                    resource = "ProjectMember",
                    field = "userId",
                    value = user.id,
                    errorCode = GlobalErrorCode.PROJECT_MEMBER_NOT_FOUND,
                    status = HttpStatus.NOT_FOUND,
                )

        member.status = ProjectMemberStatus.LEFT
        memberRepo.save(member)
    }

    private fun findProjectOrThrow(projectId: String): ProjectModel =
        projectRepo.findById(projectId).orElseThrow {
            EntryNotFoundException(
                resource = "Project",
                field = "id",
                value = projectId,
                errorCode = GlobalErrorCode.PROJECT_NOT_FOUND,
                status = HttpStatus.NOT_FOUND,
            )
        }

    private fun findUserOrThrow(userId: String): UserModel =
        userRepo.findById(userId).orElseThrow {
            EntryNotFoundException(
                resource = "User",
                field = "id",
                value = userId,
                errorCode = GlobalErrorCode.USER_NOT_FOUND,
                status = HttpStatus.NOT_FOUND,
            )
        }

    private fun checkOwnership(
        project: ProjectModel,
        user: UserModel,
    ) {
        if (project.owner.id != user.id) {
            throw AccessDeniedException(
                resource = "Project",
                errorCode = GlobalErrorCode.PROJECT_ACCESS_DENIED,
                status = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun ProjectMemberModel.toDto() =
        ProjectMemberDto(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            email = user.email,
            status = status.name,
            joinedDate = joinedDate,
        )
}
