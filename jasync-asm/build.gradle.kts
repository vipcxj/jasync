import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.vipcxj.plugin.multiRelease")
}

multiRelease {
    defaultLanguageVersion(8)
    addLanguageVersion(9, 17)
    apiProject(":jasync-utils")
}

dependencies {
    api("org.ow2.asm:asm:9.5")
    api("org.ow2.asm:asm-tree:9.5")
    api("org.ow2.asm:asm-analysis:9.5")
    api("org.ow2.asm:asm-util:9.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.jgrapht:jgrapht-core:1.5.1")
}

description = "JAsync ASM"

java {
    withJavadocJar()
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
val renameJar: Copy = tasks.create("renameJar", Copy::class.java) {
    from(shadowJar.get().outputs.files.singleFile)
    into(shadowJar.get().outputs.files.singleFile.parentFile)
    rename { fileName ->
        fileName.replace("-all.jar", ".jar")
    }
}
tasks.named("jar", Jar::class.java) {
    enabled = false
    finalizedBy(renameJar)
}