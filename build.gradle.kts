import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
	kotlin("jvm") version "2.1.10"
	application
}

group = "me.emaryllis"
val mainClass = "MainKt"
version = "1.0"

repositories {
	mavenCentral()
}

dependencies {
	// Junit 5 (Included with Kotlin)
	testImplementation(kotlin("test"))
	testImplementation("org.junit.jupiter:junit-jupiter-params:${project.property("junit_version")}")

	// Junit 5 Suite (Groups tests together)
	testImplementation("org.junit.platform:junit-platform-suite-api:${project.property("junit_version")}")
	testRuntimeOnly("org.junit.platform:junit-platform-suite-engine:${project.property("junit_version")}")
}

tasks {
	named<JavaCompile>("compileJava") { enabled = false }
	named<JavaCompile>("compileTestJava") { enabled = false }

	build {
		outputs.cacheIf { true }
		inputs.files(fileTree(sourceSets.main.get().allSource))
	}

	test {
		outputs.cacheIf { false }
		exclude("**/*Suite*")
		useJUnitPlatform {
			excludeTags("manual")
		}
		testLogging {
			events("skipped", "failed", "standardOut", "standardError")
			exceptionFormat = TestExceptionFormat.FULL
			showStandardStreams = true
		}
		maxParallelForks = Runtime.getRuntime().availableProcessors()
		maxHeapSize = "5G"
		jvmArgs = listOf("-XX:+UseG1GC")
	}

	jar {
		manifest {
			attributes["Main-Class"] = "$group.$mainClass"
		}
		from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}
}

kotlin {
	jvmToolchain(21)
}

application {
	mainClass.set("$group.$mainClass")
}