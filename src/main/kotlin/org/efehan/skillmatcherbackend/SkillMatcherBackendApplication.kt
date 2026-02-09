package org.efehan.skillmatcherbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SkillMatcherBackendApplication

fun main(args: Array<String>) {
    runApplication<SkillMatcherBackendApplication>(*args)
}
