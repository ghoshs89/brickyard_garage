plugins {
    `java-library`
}

// The engine runs on Android down to API 22 (Fire OS 5), so it is compiled at
// Java 11 language level and must avoid java.util.stream, Optional and java.time.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Tests load real level files from content/.
    systemProperty("brickyard.content", rootProject.file("content").absolutePath)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
