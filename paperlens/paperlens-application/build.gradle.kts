plugins {
	kotlin("jvm")
	kotlin("plugin.spring")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
	}
}

dependencies {
	implementation(project(":paperlens-domain"))
	implementation("org.springframework:spring-context")
	implementation("org.springframework:spring-tx")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("jakarta.validation:jakarta.validation-api")
}
