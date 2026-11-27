plugins {
    id("com.android.application") version "8.7.3" apply false
}

// verifyAllContent: schema + bot bands + blueprint proofs + gear proofs + economy invariants.
// Wired into :app:assembleRelease (see app/build.gradle.kts).
tasks.register("verifyAllContent") {
    group = "verification"
    description = "Runs the full content verification gate from :tools."
    dependsOn(":tools:verifyAllContent")
}
