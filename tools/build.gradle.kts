plugins {
    application
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    implementation(project(":engine"))
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.brickyard.tools.Main")
    applicationDefaultJvmArgs = listOf("-Xmx2g")
}

val contentDir = rootProject.file("content").absolutePath

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

// The release gate: schema, bot difficulty bands, blueprint proofs, gear proofs, economy invariants.
tasks.register<JavaExec>("verifyAllContent") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.brickyard.tools.Main")
    args("verify", contentDir)
    workingDir = rootProject.projectDir
    inputs.dir(contentDir)
    outputs.file(layout.buildDirectory.file("verify-report.txt"))
    outputs.cacheIf { true }
    doFirst { layout.buildDirectory.get().asFile.mkdirs() }
}

tasks.test { useJUnitPlatform() }
