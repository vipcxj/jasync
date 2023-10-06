package io.github.vipcxj.plugin.confgure

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.get

class SharedConfigurePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply(MavenPublishPlugin::class.java)
            extensions.configure(PublishingExtension::class.java) {
                publications {
                    register("mavenJava", MavenPublication::class.java) {
                        from(components["java"])
                    }
                }
            }
        }
    }
}