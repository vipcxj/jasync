package io.github.vipcxj.plugin.mrjars

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.base.plugins.LifecycleBasePlugin
import spoon.Launcher
import spoon.reflect.reference.CtPackageReference
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject


abstract class MultiReleaseJarExtension @Inject constructor(private val project: Project, private val javaToolchains: JavaToolchainService) {

    private val javaPluginExtension: JavaPluginExtension
        get() =  project.extensions.getByType(JavaPluginExtension::class.java)

    private val sourceSets: SourceSetContainer
        get() = javaPluginExtension.sourceSets

    var mainSourceDirectory: String = "src/main/"
    var testSourceDirectory: String = "src/test/"
    private val configureTasks: MutableMap<Int, ConfigureTask> = HashMap()
    private val dummyMap: MutableMap<Int, Set<String>> = HashMap()
    private val dependencyProjects: MutableMap<String, ProjectMeta> = HashMap()
    private fun existInSource(sourceSet: SourceSet, qualifiedName: String, clazz: Boolean): Boolean {
        return sourceSet.java.srcDirTrees.any {
            val target = it.dir.resolve(
                    qualifiedName.replace('.', '/') + (if (clazz) ".java" else "")
            )
            return target.exists()
        }
    }

    private fun validPackage(sourceSet: SourceSet, qualifiedName: String): Boolean {
        return sourceSet.java.srcDirTrees.any {
            val target = it.dir.resolve(qualifiedName.replace('.', '/'))
            if (!target.exists() || !target.isDirectory) {
                return false
            }
            return target.listFiles { file -> file.isFile && file.name.endsWith(".java") }?.isNotEmpty() ?: false
        }
    }

    private fun createDummyClass(pkgDir: File, pkgName: String): String {
        val name = "Dummy___"
        val targetFile = pkgDir.resolve("${name}.java")
        val content = """
            package ${pkgName};
            
            class Dummy___ {}
        """.trimIndent()
        var shouldWriteContent = false
        if (targetFile.exists()) {
            if (targetFile.isFile) {
                val oldContent = targetFile.readText(StandardCharsets.UTF_8)
                if (content != oldContent) {
                    shouldWriteContent = true
                }
            } else {
                throw RuntimeException("The dummy java class file ${targetFile.canonicalPath} exists, but is not a file!")
            }
        } else {
            shouldWriteContent = true
        }
        if (shouldWriteContent) {
            targetFile.writeText("""
            package ${pkgName};
            
            class Dummy___ {}
        """.trimIndent(), StandardCharsets.UTF_8)
        }
        return "${pkgName}.${name}"
    }

    private fun createDirectory(file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (file.exists()) {
            return false
        }
        createDirectory(file.parentFile)
        return file.mkdir()
    }

    private fun dealWithModuleInfo(sourceSet: SourceSet, mainSourceSet: SourceSet, version: Int) {
        println("dealWith $sourceSet")
        val dummyClasses: MutableSet<String> = HashSet()
        sourceSet.java.find {
            it.isFile && it.name == "module-info.java"
        }?.let { file ->
            if (file.exists()) {
                val srcFile = project.layout.buildDirectory.dir("tmp/javaDummySource${version}").get().asFile
                createDirectory(srcFile)
                sourceSet.java.srcDir(srcFile)
                val launcher = Launcher()
                launcher.addInputResource(file.canonicalPath)
                launcher.environment.complianceLevel = version
                val model = launcher.buildModel()
                model.allModules.forEach { module ->
                    val packages: MutableSet<CtPackageReference> = HashSet()
                    packages.addAll(module.exportedPackages.map { ep -> ep.packageReference })
                    packages.addAll(module.openedPackages.map { ep -> ep.packageReference })
                    packages.forEach {pkg ->
                        val pkgDir = srcFile.resolve(pkg.qualifiedName.replace('.', '/'))
                        createDirectory(pkgDir)
                        if (!validPackage(sourceSet, pkg.qualifiedName)) {
                            dummyClasses.add(createDummyClass(pkgDir, pkg.qualifiedName))
                        }
                    }
                    module.providedServices.forEach { service ->
                        service.implementationTypes.forEach { impl ->
                            println(impl)
                        }
                    }
                }
            }
        }
        dummyMap[version] = dummyClasses
    }

    private val mainSourceSet: SourceSet
        get() = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)!!

    private fun doConfigure(configureTask: ConfigureTask) {
        with(project) {
            val version = configureTask.version
            val testVersion = configureTask.testVersion
            val toolchainVersion = configureTask.toolchainVersion
            val testToolchainVersion = configureTask.testToolchainVersion
            val mainTestSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME)!!
            val compiler: Provider<JavaCompiler> = javaToolchains.compilerFor {
                languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
            }
            val testCompiler: Provider<JavaCompiler> = javaToolchains.compilerFor {
                languageVersion.convention(JavaLanguageVersion.of(testToolchainVersion))
            }
            if (configureTask.isDefault) {
                javaPluginExtension.toolchain.languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
                tasks.named(mainSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                    javaCompiler.convention(compiler)
                    sourceCompatibility = "$version"
                    targetCompatibility = "$version"
                    if (JavaLanguageVersion.of(version).asInt() > 8) {
                        options.release.convention(version)
                        modularity.inferModulePath.convention(true)
                    }
                }
                tasks.named(mainTestSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                    javaCompiler.convention(testCompiler)
                    sourceCompatibility = "$testVersion"
                    targetCompatibility = "$testVersion"
                    if (JavaLanguageVersion.of(testVersion).asInt() > 8) {
                        options.release.convention(testVersion)
                        modularity.inferModulePath.convention(true)
                    }
                }
            } else {
                val javaX = "java$version"
                val testJavaX = "java${testVersion}"
                // First, let's create a source set for this language version
                // First, let's create a source set for this language version
                val langSourceSet: SourceSet = sourceSets.create(javaX) {
                    java.srcDir(mainSourceDirectory + javaX)
                    compileClasspath += mainSourceSet.compileClasspath
                }
                val testSourceSet: SourceSet = sourceSets.create("${testJavaX}Test") {
                    java.srcDir(testSourceDirectory + testJavaX)
                }
                dealWithModuleInfo(langSourceSet, mainSourceSet, version)

                // This is only necessary because in real life, we have dependencies between classes
                // and what you're likely to want to do, is to provide a JDK 9 specific class, which depends on common
                // classes of the main source set. In other words, you want to override some specific classes, but they
                // still have dependencies onto other classes.
                // We want to avoid recompiling all those classes, so we're just saying that the Java 9 specific classes
                // "depend on" the main ones.

                // This is only necessary because in real life, we have dependencies between classes
                // and what you're likely to want to do, is to provide a JDK 9 specific class, which depends on common
                // classes of the main source set. In other words, you want to override some specific classes, but they
                // still have dependencies onto other classes.
                // We want to avoid recompiling all those classes, so we're just saying that the Java 9 specific classes
                // "depend on" the main ones.
                val mainClasses: FileCollection = objects.fileCollection().from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output.classesDirs)
                dependencies.add(javaX + "Implementation", mainClasses)

                // then configure the compile task so that it uses the expected Gradle version

                // then configure the compile task so that it uses the expected Gradle version
                tasks.named(langSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                    javaCompiler.convention(compiler)
                    sourceCompatibility = "$version"
                    targetCompatibility = "$version"
                    if (version > 8) {
                        options.release.convention(version)
                        modularity.inferModulePath.convention(true)
                    }
                    for (pMeta in dependencyProjects) {
                        val dependencyProject = project(pMeta.key)
                        val javaPluginExtension = dependencyProject.extensions.getByType(JavaPluginExtension::class.java)
                        javaPluginExtension.sourceSets.forEach { s ->
                            println("remove project classes")
                            classpath -= s.output.classesDirs
                        }
                        classpath += objects.fileCollection().from(dependencyProject.tasks.named(pMeta.value.jarTaskName))
                    }
                }
                tasks.named(testSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                    javaCompiler.convention(testCompiler)
                    sourceCompatibility = "$testVersion"
                    targetCompatibility = "$testVersion"
                    if (testVersion > 8) {
                        options.release.convention(testVersion)
                        modularity.inferModulePath.convention(true)
                    }
                }

                // let's make sure to create a "test" task
                val launcher: Provider<JavaLauncher> = javaToolchains.launcherFor {
                    languageVersion.convention(JavaLanguageVersion.of(toolchainVersion))
                }
                val testLauncher: Provider<JavaLauncher> = javaToolchains.launcherFor {
                    languageVersion.convention(JavaLanguageVersion.of(testToolchainVersion))
                }

                val testImplementation: Configuration = configurations.getByName(testSourceSet.implementationConfigurationName)
                testImplementation.extendsFrom(configurations.getByName(mainTestSourceSet.implementationConfigurationName))
                val testCompileOnly: Configuration = configurations.getByName(testSourceSet.compileOnlyConfigurationName)
                testCompileOnly.extendsFrom(configurations.getByName(mainTestSourceSet.compileOnlyConfigurationName))
                testCompileOnly.dependencies.add(dependencies.create(langSourceSet.output.classesDirs))
                testCompileOnly.dependencies.add(dependencies.create(mainSourceSet.output.classesDirs))

                val testRuntimeClasspath: Configuration = configurations.getByName(testSourceSet.runtimeClasspathConfigurationName)
                // so here's the deal. MRjars are JARs! Which means that to execute tests, we need
                // the JAR on classpath, not just classes + resources as Gradle usually does
                // so here's the deal. MRjars are JARs! Which means that to execute tests, we need
                // the JAR on classpath, not just classes + resources as Gradle usually does
                testRuntimeClasspath.attributes
                        .attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class.java, LibraryElements.JAR))

                val testTask = tasks.register("java${version}Test", Test::class.java) {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    javaLauncher.convention(testLauncher)
                    val testClassesDirs = objects.fileCollection()
                    testClassesDirs.from(testSourceSet.output)
                    testClassesDirs.from(mainTestSourceSet.output)
                    setTestClassesDirs(testClassesDirs)
                    val classpath = objects.fileCollection()
                    // must put the MRJar first on classpath
                    classpath.from(tasks.named("jar"))
                    // then we put the specific test sourceset tests, so that we can override
                    // the shared versions
                    classpath.from(testSourceSet.output)

                    // then we add the shared tests
                    classpath.from(mainTestSourceSet.runtimeClasspath)
                    setClasspath(classpath)
                }

                tasks.named("check") {
                    dependsOn(testTask)
                }

                injectJar("jar", version)
                injectJar("shadowJar", version)

                pluginManager.withPlugin("application") {
                    val javaApp = extensions.getByType(JavaApplication::class.java)
                    tasks.register("java$version" + "Run", JavaExec::class.java) {
                        group = ApplicationPlugin.APPLICATION_GROUP
                        javaLauncher.convention(launcher)
                        mainClass.convention(javaApp.mainClass)
                        this.classpath = langSourceSet.runtimeClasspath
                    }
                }
            }
        }
    }

    private fun injectJar(
            taskName: String,
            version: Int,
    ) {
        try {
            project.tasks.named(taskName, Jar::class.java) {
                injectJar(project, this, version)
            }
        } catch (_: org.gradle.api.UnknownDomainObjectException) {}
    }

    fun addLanguageVersion(version: Int, toolchainVersion: Int, testVersion: Int, testToolchainVersion: Int) {
        if (configureTasks.containsKey(version)) {
            throw RuntimeException("Language version $version exists.")
        }
        val task = ConfigureTask(version, toolchainVersion, testVersion, testToolchainVersion, false)
        configureTasks[version] = task
        doConfigure(task)
    }

    fun addLanguageVersion(version: Int, toolchainVersion: Int) {
        addLanguageVersion(version, toolchainVersion, version, toolchainVersion)
    }

    fun addLanguageVersion(version: Int) {
        addLanguageVersion(version, version)
    }

    fun defaultLanguageVersion(version: Int, toolchainVersion: Int, testVersion: Int, testToolchainVersion: Int) {
        if (configureTasks.containsKey(version)) {
            throw RuntimeException("Language version $version exists.")
        }
        val task = ConfigureTask(version, toolchainVersion, testVersion, testToolchainVersion, true)
        doConfigure(task)
    }

    fun defaultLanguageVersion(version: Int, toolchainVersion: Int) {
        defaultLanguageVersion(version, toolchainVersion, version, toolchainVersion)
    }

    fun defaultLanguageVersion(version: Int) {
        defaultLanguageVersion(version, version)
    }

    fun apiProject(name: String, jarTaskName: String = "jar") {
        if (dependencyProjects.contains(name)) {
            throw RuntimeException("Project dependency $name exists.")
        }
        val depProject = project.project(name)
        project.dependencies.add("api", depProject)
        dependencyProjects.put(name, ProjectMeta(name, jarTaskName))
        val javaPluginExtension = depProject.extensions.getByType(JavaPluginExtension::class.java)
        for (version in configureTasks.keys) {
            val sourceSet = sourceSets.getByName("java$version")
            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                javaPluginExtension.sourceSets.forEach { s ->
                    classpath -= s.output.classesDirs
                }
                classpath += project.objects.fileCollection().from(depProject.tasks.named(jarTaskName))
            }
        }
    }

    class ConfigureTask(val version: Int, val toolchainVersion: Int, val testVersion: Int, val testToolchainVersion: Int, val isDefault: Boolean)

    class ProjectMeta(val name: String, val jarTaskName: String = "jar")

    companion object {
        fun injectJar(project: Project, jar: Jar, version: Int) {
            val extension = project.extensions.getByType(MultiReleaseJarExtension::class.java)
            val langSourceSet = project
                .extensions
                .getByType(JavaPluginExtension::class.java)
                .sourceSets
                .findByName("java$version")!!
            with(jar) {
                into("META-INF/versions/$version") {
                    from(langSourceSet.output) {
                        extension.dummyMap[version]?.forEach { dummy ->
                            println("exclude class $dummy")
                            exclude("**/" + dummy.replace('.', '/') + ".class")
                        }
                    }
                }
                manifest {
                    attributes["Multi-Release"] = true
                }
            }
        }
    }
}