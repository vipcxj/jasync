subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    dependencies {

    }
}

allprojects {
    group = "io.github.vipcxj"
    version = "1.0.18-SNAPSHOT"
    tasks.withType(Javadoc::class.java) {
        isFailOnError = false
    }
    tasks.withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

buildscript {

    extra.apply {
        set("auto_service_version", "1.1.1")
    }
}