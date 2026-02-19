package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.RoleModel

class RoleBuilder {
    fun build(
        name: String = "PROJECTMANAGER",
        description: String? = null,
    ): RoleModel =
        RoleModel(
            name = name,
            description = description,
        )
}
