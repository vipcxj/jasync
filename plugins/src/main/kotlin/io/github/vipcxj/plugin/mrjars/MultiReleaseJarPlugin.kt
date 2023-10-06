package io.github.vipcxj.plugin.mrjars

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.toolchain.JavaToolchainService
import javax.inject.Inject


open class MultiReleaseJarPlugin : Plugin<Project> {

    @Inject
    open fun getToolchains(): JavaToolchainService {
        throw UnsupportedOperationException()
    }

    override fun apply(target: Project) {
        target.plugins.apply(JavaPlugin::class.java)
        target.extensions.create(
                "multiRelease", MultiReleaseJarExtension::class.java,
                target,
                getToolchains(),
        )
    }
}