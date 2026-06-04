// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.gradle.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.kotlin.ksp) apply false

    //Hilt
    alias(libs.plugins.dagger.hilt) apply false

    //Firebase service
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.google.services) apply false

    //Room Database
    alias(libs.plugins.android.room) apply false

}

val jitpackGroup = "com.github.nguyenvuong0308.CodeBase"
val jitpackVersion = "1.0.0"
val jitpackPublishVariants = mapOf(
    ":core:ads" to "prodRelease",
    ":core:baseui" to "prodRelease",
    ":core:config" to "prodRelease",
    ":core:rate" to "prodRelease",
)

subprojects {
    group = System.getenv("GROUP")
        ?.let { group ->
            val artifact = System.getenv("ARTIFACT")
            if (artifact.isNullOrBlank() || group.endsWith(".$artifact")) group else "$group.$artifact"
        }
        ?: jitpackGroup
    version = System.getenv("VERSION") ?: jitpackVersion

    pluginManager.withPlugin("com.android.library") {
        apply(plugin = "maven-publish")

        val publishVariant = jitpackPublishVariants[path] ?: "release"

        extensions.configure<LibraryExtension>("android") {
            publishing {
                singleVariant(publishVariant) {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            val component = components.findByName(publishVariant) ?: return@afterEvaluate

            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    val publication = findByName("release") as? MavenPublication
                    if (publication == null) {
                        register<MavenPublication>("release") {
                            from(component)
                            groupId = project.group.toString()
                            artifactId = project.name
                            version = project.version.toString()
                        }
                    } else {
                        publication.groupId = project.group.toString()
                        publication.artifactId = project.name
                        publication.version = project.version.toString()
                    }
                }
            }
        }
    }
}
