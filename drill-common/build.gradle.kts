plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(project.sourceSets["main"].allSource)
}

publishing {
    publications {
        this.register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}