import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.vipcxj.plugin.mrjars.MultiReleaseJarExtension

val mrJarVersion = 9
val myJarTaskName = "myJar"

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.vipcxj.plugin.multiRelease")
}

multiRelease {
    defaultLanguageVersion(8)
    addLanguageVersion(mrJarVersion, 17)
    apiProject(":jasync-utils")
}

dependencies {
    api("org.ow2.asm:asm:9.5")
    api("org.ow2.asm:asm-tree:9.5")
    api("org.ow2.asm:asm-analysis:9.5")
    api("org.ow2.asm:asm-util:9.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.jgrapht:jgrapht-core:1.4.0")
}

description = "JAsync ASM"

java {
    withJavadocJar()
}

tasks.withType(Test::class.java) {
    minHeapSize = "1g"
    maxHeapSize = "2g"
}

val shadowJar: TaskProvider<ShadowJar> = tasks.named("shadowJar", ShadowJar::class.java) {
    dependencies {
        include(dependency("io.github.vipcxj:jasync-utils:.*"))
        include(dependency("org.ow2.asm:asm:.*"))
        include(dependency("org.ow2.asm:asm-tree:.*"))
        include(dependency("org.ow2.asm:asm-analysis:.*"))
        include(dependency("org.ow2.asm:asm-util:.*"))
    }
    relocate("org.objectweb", "io.github.vipcxj.jasync.ng.asm.shaded.org.objectweb")
    relocate("io.github.vipcxj.jasync.ng.utils", "io.github.vipcxj.jasync.ng.asm.shaded.utils")
}
val jar: TaskProvider<Jar> = tasks.named("jar", Jar::class.java)
val myJar: Jar = tasks.create(myJarTaskName, Jar::class.java) {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().outputs.files.singleFile))
    MultiReleaseJarExtension.injectJar(project, this, mrJarVersion)
    archiveBaseName.set(jar.get().archiveFileName)
}
jar {
    enabled = false
    finalizedBy(myJar)
}