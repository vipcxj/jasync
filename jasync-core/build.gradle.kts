import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/*
 * This file was generated by the Gradle 'init' task.
 */

val autoServiceVersion = rootProject.extra["auto_service_version"]

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.vipcxj.plugin.multiRelease")
}

multiRelease {
    println("jasync-core multiRelease")
    defaultLanguageVersion(8)
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs.add("-XDenableSunApiLintControl")
}

dependencies {
    api(project(":jasync-spec"))
    api(project(":jasync-utils"))
    api("org.ow2.asm:asm:9.5")
    api("org.ow2.asm:asm-tree:9.5")
    api("net.bytebuddy:byte-buddy-dep:1.14.7")
    api("net.bytebuddy:byte-buddy-agent:1.14.7")
    api("com.google.auto.service:auto-service-annotations:$autoServiceVersion")
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.core") {
        version {
            strictly("3.26.0")
        }
    }
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.apt.core") {
        version {
            strictly("3.6.900")
        }
    }
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.compiler.tool") {
        version {
            strictly("1.3.50")
        }
    }
    compileOnly("org.eclipse.platform:org.eclipse.core.runtime") {
        version {
            strictly("3.13.0")
        }
    }
    compileOnly("org.eclipse.platform:org.eclipse.core.resources") {
        version {
            strictly("3.13.900")
        }
    }
    compileOnly("org.eclipse.platform:org.eclipse.equinox.common") {
        version {
            strictly("3.14.100")
        }
    }
    compileOnly("org.eclipse.platform:org.eclipse.core.jobs") {
        version {
            strictly("3.10.0")
        }
    }
    compileOnly("org.eclipse.platform:org.eclipse.equinox.preferences") {
        version {
            strictly("3.9.100")
        }
    }
    annotationProcessor("com.google.auto.service:auto-service:$autoServiceVersion")
}

description = "JAsync Core"

java {
    withJavadocJar()
}

val shadowJar: TaskProvider<ShadowJar> = tasks.named("shadowJar", ShadowJar::class.java) {
    dependencies {
        include(dependency("net.bytebuddy:byte-buddy-dep:.*"))
        include(dependency("org.ow2.asm:.*:.*"))
    }
    relocate("org.objectweb.asm", "io.github.vipcxj.jasync.ng.core.shaded.org.objectweb.asm")
}

val jar: TaskProvider<Jar> = tasks.named("jar", Jar::class.java)
val deleteTarget: Delete = tasks.create("deleteTarget", Delete::class.java) {
    delete(jar)
    shouldRunAfter(shadowJar)
}
val addAsm: Jar = tasks.create("addAsm", Jar::class.java) {
    dependsOn(shadowJar)
    dependsOn(deleteTarget)
    from(zipTree(shadowJar.get().outputs.files.singleFile))
    val asmJar = rootProject.project(":jasync-asm").tasks.named("jar")
    dependsOn(asmJar)
    from(asmJar) {
        filesMatching("jasync-asm.jar") {
            name = "asm.jar"
        }
    }
    archiveBaseName.convention(jar.get().archiveBaseName)
}
 jar {
     finalizedBy(addAsm)
 }
