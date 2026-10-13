import org.gradle.api.artifacts.repositories.MavenArtifactRepository

// Regression test for CI environments where the KSP plugin marker cannot be resolved.
// Run: ./gradlew -I gradle/tests/ksp-without-marker.init.gradle.kts help
// A JVM unit test cannot cover this failure: plugin resolution happens before tests can run.
// Repository filtering also prevents an already cached marker from hiding the regression.
settingsEvaluated {
    pluginManagement.repositories.withType<MavenArtifactRepository>().configureEach {
        content {
            excludeModule("com.google.devtools.ksp", "com.google.devtools.ksp.gradle.plugin")
        }
    }
    logger.lifecycle("KSP regression test: plugin marker excluded from all plugin repositories")
}
