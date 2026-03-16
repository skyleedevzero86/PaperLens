package com.sleekydz86.paperlens.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.sleekydz86.paperlens.infrastructure"])
@EntityScan("com.sleekydz86.paperlens.infrastructure.persistence.entity")
@EnableJpaRepositories("com.sleekydz86.paperlens.infrastructure.persistence.repository")
@EnableAsync
@EnableScheduling
class PaperLensApplication

fun main(args: Array<String>) {
	runApplication<PaperLensApplication>(*args)
}
