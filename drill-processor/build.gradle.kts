plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":drill-common"))
    implementation("com.google.auto:auto-common:0.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.72")
    implementation("com.squareup:kotlinpoet:1.4.0")
    implementation("com.squareup:kotlinpoet-metadata:1.4.0")
    implementation("com.squareup:kotlinpoet-metadata-specs:1.4.0")
    implementation("net.ltgt.gradle.incap:incap-processor:0.2")
    implementation("net.ltgt.gradle.incap:incap:0.2")
    testImplementation("junit:junit:4.12")
    testImplementation("com.google.testing.compile:compile-testing:0.15")
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