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
    implementation("fr.inria.gforge.spoon:spoon-core:10.4.1")
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")
}

gradlePlugin {
    plugins {
        create("sharedConfigurePlugin") {
            id = "io.github.vipcxj.plugin.sharedConfigure"
            implementationClass = "io.github.vipcxj.plugin.confgure.SharedConfigurePlugin"
        }
        create("multiReleaseJarPlugin") {
            id = "io.github.vipcxj.plugin.multiRelease"
            implementationClass = "io.github.vipcxj.plugin.mrjars.MultiReleaseJarPlugin"
        }
    }
}