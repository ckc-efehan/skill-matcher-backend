package org.efehan.skillmatcherbackend.core.skill

import jakarta.validation.Valid
import org.efehan.skillmatcherbackend.core.auth.SecurityUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me/skills")
class MySkillController(
    private val service: UserSkillService,
) {
    @PostMapping
    fun addSkill(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @Valid @RequestBody req: AddSkillRequest,
    ): ResponseEntity<UserSkillDto> {
        val (dto, created) = service.addOrUpdateSkill(securityUser.user, req.name, req.level)
        val status = if (created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(dto)
    }

    @GetMapping
    fun getMySkills(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): ResponseEntity<List<UserSkillDto>> = ResponseEntity.ok(service.getUserSkills(securityUser.user))

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal securityUser: SecurityUser,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        service.deleteSkill(securityUser.user, id)
        return ResponseEntity.noContent().build()
    }
}
