package org.efehan.skillmatcherbackend.core.project

import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.ProjectMemberRepository
import org.efehan.skillmatcherbackend.persistence.ProjectModel
import org.efehan.skillmatcherbackend.persistence.ProjectRepository
import org.efehan.skillmatcherbackend.persistence.ProjectSkillRepository
import org.efehan.skillmatcherbackend.persistence.ProjectStatus
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProjectService(
    private val projectRepo: ProjectRepository,
    private val projectSkillRepo: ProjectSkillRepository,
    private val projectMemberRepo: ProjectMemberRepository,
) {
    fun createProject(
        owner: UserModel,
        request: CreateProjectRequest,
    ): ProjectDto {
        val project =
            projectRepo.save(
                ProjectModel(
                    name = request.name,
                    description = request.description,
                    status = ProjectStatus.PLANNED,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    maxMembers = request.maxMembers,
                    owner = owner,
                ),
            )
        return project.toDto()
    }

    fun getProject(projectId: String): ProjectDto {
        val project = findProjectOrThrow(projectId)
        return project.toDto()
    }

    fun getAllProjects(): List<ProjectDto> = projectRepo.findAll().map { it.toDto() }

    fun updateProject(
        user: UserModel,
        projectId: String,
        request: UpdateProjectRequest,
    ): ProjectDto {
        val project = findProjectOrThrow(projectId)
        checkOwnership(project, user)

        project.name = request.name
        project.description = request.description
        project.status = ProjectStatus.valueOf(request.status)
        project.startDate = request.startDate
        project.endDate = request.endDate
        project.maxMembers = request.maxMembers

        return projectRepo.save(project).toDto()
    }

    fun deleteProject(
        user: UserModel,
        projectId: String,
    ) {
        val project = findProjectOrThrow(projectId)
        checkOwnership(project, user)

        projectMemberRepo.deleteAll(projectMemberRepo.findByProject(project))
        projectSkillRepo.deleteAll(projectSkillRepo.findByProject(project))
        projectRepo.delete(project)
    }

    private fun findProjectOrThrow(projectId: String): ProjectModel =
        projectRepo
            .findById(projectId)
            .orElseThrow {
                EntryNotFoundException(
                    resource = "Project",
                    field = "id",
                    value = projectId,
                    errorCode = GlobalErrorCode.PROJECT_NOT_FOUND,
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

    private fun ProjectModel.toDto() =
        ProjectDto(
            id = id,
            name = name,
            description = description,
            status = status.name,
            startDate = startDate,
            endDate = endDate,
            maxMembers = maxMembers,
            ownerName = "${owner.firstName} ${owner.lastName}",
            createdDate = createdDate,
        )
}
