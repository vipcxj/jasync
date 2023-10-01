package io.github.vipcxj.plugin.mrjars

import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.*
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import javax.inject.Inject

abstract class MultiReleaseJarExtension @Inject constructor(private val project: Project, private val javaToolchains: JavaToolchainService) {

    private val javaPluginExtension: JavaPluginExtension
        get() =  project.extensions.getByType(JavaPluginExtension::class.java)

    fun addLanguageVersion(version: Int, toolchainVersion: Int, mainSourceDirectory: String = "src/main/", testSourceDirectory: String = "src/test/") {
        with(project) {
            val javaX = "java$version"
            val sourceSets = javaPluginExtension.sourceSets
            val mainSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)!!
            val mainTestSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME)
            // First, let's create a source set for this language version
            val langSourceSet = sourceSets.create(javaX) {
                compileClasspath += mainSourceSet.compileClasspath
                compileClasspath += mainSourceSet.output
                java.srcDir(mainSourceDirectory + javaX)
                java.filter.exclude("module-info.java")
            }
            val moduleSourceSet = sourceSets.create("${javaX}module") {
                println("original class path printing ...")
//                mainSourceSet.compileClasspath.files.forEach {
//                    println(it)
//                }
                println("original class path printed.")
                compileClasspath += mainSourceSet.compileClasspath
                java.srcDir(mainSourceDirectory + javaX)
                java.srcDirs(mainSourceSet.java.srcDirs)
                java.filter.include("module-info.java")
            }
            val testSourceSet = sourceSets.create(javaX + "Test") {
                java.srcDir(testSourceDirectory + javaX)
            }

            // This is only necessary because in real life, we have dependencies between classes
            // and what you're likely to want to do, is to provide a JDK 9 specific class, which depends on common
            // classes of the main source set. In other words, you want to override some specific classes, but they
            // still have dependencies onto other classes.
            // We want to avoid recompiling all those classes, so we're just saying that the Java 9 specific classes
            // "depend on" the main ones.
            val mainClasses: FileCollection = objects.fileCollection().from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output.classesDirs)
            dependencies.add(javaX + "Implementation", mainClasses)

            // then configure the compile task so that it uses the expected Gradle version
            val targetCompiler = javaToolchains.compilerFor {
                languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
            }
            val compileMainJava = tasks.findByName(mainSourceSet.compileJavaTaskName)
            val compileJava = tasks.named(langSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                javaCompiler.convention(targetCompiler)
                dependsOn(compileMainJava)
                sourceCompatibility = "$version"
                targetCompatibility = "$version"
                if (JavaLanguageVersion.of(version).asInt() > 8) {
                    options.release.convention(version)
                }
                modularity.inferModulePath.set(false)
            }.get()
            val compileModule = tasks.named(moduleSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                javaCompiler.convention(targetCompiler)
                dependsOn(compileMainJava)
                sourceCompatibility = "$version"
                targetCompatibility = "$version"
                if (JavaLanguageVersion.of(version).asInt() > 8) {
                    options.release.convention(version)
                }
                modularity.inferModulePath.set(true)
            }.get()
            tasks.named(testSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                javaCompiler.convention(targetCompiler)
                sourceCompatibility = "$version"
                targetCompatibility = "$version"
                if (JavaLanguageVersion.of(version).asInt() > 8) {
                    options.release.convention(version)
                }
            }

            // let's make sure to create a "test" task
            val targetLauncher = javaToolchains.launcherFor {
                languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
            }
            val testImplementation = configurations.getByName(testSourceSet.implementationConfigurationName)
            testImplementation.extendsFrom(configurations.getByName(mainTestSourceSet!!.implementationConfigurationName))
            val testCompileOnly = configurations.getByName(testSourceSet.compileOnlyConfigurationName)
            testCompileOnly.extendsFrom(configurations.getByName(mainTestSourceSet.compileOnlyConfigurationName))
            testCompileOnly.dependencies.add(dependencies.create(langSourceSet.output.classesDirs))
            testCompileOnly.dependencies.add(dependencies.create(mainSourceSet.output.classesDirs))
            val testRuntimeClasspath = configurations.getByName(testSourceSet.runtimeClasspathConfigurationName)
            // so here's the deal. MRjars are JARs! Which means that to execute tests, we need
            // the JAR on classpath, not just classes + resources as Gradle usually does
            testRuntimeClasspath.attributes
                    .attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))
            val testTask = tasks.register("java" + version + "Test", Test::class.java) {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                javaLauncher.convention(targetLauncher)
                val testClassesDirs = objects.fileCollection()
                testClassesDirs.from(testSourceSet.output)
                testClassesDirs.from(mainTestSourceSet.output)
                this.testClassesDirs = testClassesDirs
                val classpath = objects.fileCollection()
                // must put the MRJar first on classpath
                classpath.from(tasks.named("jar"))
                // then we put the specific test sourceset tests, so that we can override
                // the shared versions
                classpath.from(testSourceSet.output)

                // then we add the shared tests
                classpath.from(mainTestSourceSet.runtimeClasspath)
                this.classpath = classpath
            }
            tasks.named("check") {
                dependsOn(testTask)
            }
            configJarTask("jar", compileJava, compileModule, langSourceSet, moduleSourceSet, version)
            configJarTask("shadowJar", compileJava, compileModule, langSourceSet, moduleSourceSet, version)
            pluginManager.withPlugin("application") {
                val javaApp = extensions.getByType(JavaApplication::class.java)
                tasks.register("java" + version + "Run", JavaExec::class.java) {
                    group = ApplicationPlugin.APPLICATION_GROUP
                    javaLauncher.convention(targetLauncher)
                    mainClass.convention(javaApp.mainClass)
                    setClasspath(langSourceSet.runtimeClasspath)
                }
            }
        }
    }

    private fun configJarTask(
            taskName: String,
            compileJava: JavaCompile,
            compileModule: JavaCompile,
            langSourceSet: SourceSet,
            moduleSourceSet: SourceSet,
            version: Int,
    ) {
        try {
            project.tasks.named(taskName, Jar::class.java) {
                dependsOn(compileJava)
                dependsOn(compileModule)
                into("META-INF/versions/$version") {
                    from(langSourceSet.output)
                }
                into("META-INF/versions/$version") {
                    from(moduleSourceSet.output)
                    include("module-info.class")
                }
                manifest {
                    attributes["Multi-Release"] = true
                }
            }
        } catch (_: org.gradle.api.UnknownDomainObjectException) {}
    }

    fun defaultLanguageVersion(version: Int) {
        defaultLanguageVersion(version, version)
    }

    fun defaultLanguageVersion(version: Int, toolchainVersion: Int) {
        val sourceSets = javaPluginExtension.sourceSets
        val mainSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)!!
        val mainTestSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME)!!
        javaPluginExtension.toolchain.languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
        with(project) {
            tasks.findByName(mainSourceSet.compileJavaTaskName)!!.apply {
                val task = this as JavaCompile
                task.sourceCompatibility = "$version"
                task.targetCompatibility = "$version"
                if (JavaLanguageVersion.of(version).asInt() > 8) {
                    options.release.convention(version)
                }
            }
            tasks.findByName(mainTestSourceSet.compileJavaTaskName)!!.apply {
                val task = this as JavaCompile
                task.sourceCompatibility = "$version"
                task.targetCompatibility = "$version"
                if (JavaLanguageVersion.of(version).asInt() > 8) {
                    options.release.convention(version)
                }
            }
        }
    }
}
