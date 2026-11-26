plugins {
	java
	pmd
	id("com.diffplug.spotless") version "8.12.0"
	id("org.springframework.boot") version "4.2.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.nihar4154.supportflow"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "2.1.0-M1"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.ai:spring-ai-starter-mcp-client")
	// Include both macOS architectures so Netty selects the native DNS resolver
	// matching the JVM. Other operating systems continue using their own resolver.
	runtimeOnly("io.netty:netty-resolver-dns-native-macos::osx-aarch_64")
	runtimeOnly("io.netty:netty-resolver-dns-native-macos::osx-x86_64")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	developmentOnly("org.springframework.ai:spring-ai-spring-boot-docker-compose")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.bootRun {
	systemProperty("spring.docker.compose.file", project.file("compose.yaml").absolutePath)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

pmd {
    toolVersion = "7.29.0"
    ruleSets = emptyList()
    ruleSetFiles = files("../config/pmd/ruleset.xml")
    isIgnoreFailures = false
}

spotless {
    // Enforce formatting on files changed since the initial adoption baseline.
    ratchetFrom("e7139e259e6a5ff425307a96b91da58648b6ae36")
    java {
        googleJavaFormat("1.30.0")
    }
}
