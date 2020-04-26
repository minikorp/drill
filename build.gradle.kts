plugins {
    java
    kotlin("jvm") version "1.3.72"
    `maven-publish`
}

fun runCommand(command: String): String {
    val stream = Runtime.getRuntime().exec(command)
        .apply { waitFor() }.inputStream
    return stream.reader().readText().trim()
}

allprojects {

    apply(plugin = "maven-publish")
    group = "mini.drill"
    version = runCommand("scripts/latest-version.sh")

    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}