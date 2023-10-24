/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("io.github.vipcxj.plugin.sharedConfigure")
    id("io.github.vipcxj.plugin.multiRelease")
}

multiRelease {
    defaultLanguageVersion(8)
    addLanguageVersion(9, 17)
}

description = "JAsync Spec"

java {
    withJavadocJar()
}