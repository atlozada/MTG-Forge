import org.gradle.kotlin.dsl.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.docker.DockerExtension

buildscript {
	val kotlinVer by extra { "1.5.21" }
	val springBootVersion by extra { "2.5.3" }

	val versionPluginVer = "0.15.0"
	val shadowPluginVer = "2.0.1"
	val dockerPluginVer = "0.13.0"


	repositories {
		mavenCentral()
		maven { setUrl("https://plugins.gradle.org/m2/") }
	}

	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVer")
		classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVer")
		classpath("com.github.jengelman.gradle.plugins:shadow:$shadowPluginVer")
		// gradle dependencyUpdates -Drevision=release
		classpath("com.github.ben-manes:gradle-versions-plugin:$versionPluginVer")
		classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:$dockerPluginVer")
		classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
	}
}

repositories {
	maven {
		setUrl("https://repo.spring.io/libs-snapshot")
	}
	mavenCentral()
}

val kotlinVer: String by extra
val springBootVersion: String by extra

val kotlinLoggingVer = "1.4.6"
val logbackVer = "1.2.3"
val jAnsiVer = "1.16"

val junitJupiterVer = "5.0.1"

plugins {
	java
	application
	idea
	id("org.springframework.boot") version "2.5.3"
	id("org.jetbrains.kotlin.jvm") version "1.5.21"
}

apply {
	plugin("com.github.johnrengelman.shadow")
	plugin("com.github.ben-manes.versions")
	plugin("com.palantir.docker")
	plugin("org.jetbrains.kotlin.jvm")
	plugin("io.spring.dependency-management")
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.11"
}

application {
	mainClassName = "com.exile.MtgForgekt"
	applicationName = "app"
	version = "1.0-SNAPSHOT"
	group = "li.barlog.template.kotlin"
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
	implementation(kotlin("stdlib", kotlinVer))
	implementation(kotlin("reflect", kotlinVer))

	// Spring
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Database
	implementation("org.jooq:jooq:3.15.1")
	implementation("org.postgresql:postgresql:42.2.23")

	implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVer")

	implementation("ch.qos.logback:logback-classic:$logbackVer")
	implementation("org.fusesource.jansi:jansi:$jAnsiVer")

	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVer")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVer")
}

configure<IdeaModel> {
	project {
		languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
	}
	module {
		isDownloadJavadoc = true
		isDownloadSources = true
	}
}

val build: DefaultTask by tasks
val shadowJar = tasks["shadowJar"] as ShadowJar
build.dependsOn(shadowJar)

configure<DockerExtension> {
	name = "app"
	files(shadowJar.outputs)
	setDockerfile(file("src/main/docker/Dockerfile"))
	buildArgs(mapOf(
		"PORT"   to  "8080",
		"JAVA_OPTS" to "-Xms64m -Xmx128m"
	))
	pull(true)
	dependsOn(shadowJar, tasks["jar"])
}

tasks.withType<ShadowJar> {
	baseName = "mtg-forge"
	classifier = null
	version = null
}

tasks.withType<Test> {
	maxParallelForks = Runtime.getRuntime().availableProcessors()
}

val clean: Delete by tasks
task("stage") {
	dependsOn(build, clean)
}
