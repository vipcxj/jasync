buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}

apply(plugin = "kotlin")
apply(plugin = "java-gradle-plugin")
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
}

gradlePlugin {
    plugins {
        create("multiReleaseJarPlugin") {
            id = "io.github.vipcxj.plugin.multiRelease"
            implementationClass = "io.github.vipcxj.plugin.mrjars.MultiReleaseJarPlugin"
        }
    }
}