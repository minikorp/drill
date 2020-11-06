import java.util.*

plugins {
    java
    kotlin("jvm") version "1.4.10"
    `maven-publish`
}

fun runCommand(command: String): String {
    val stream = Runtime.getRuntime().exec(command)
        .apply { waitFor() }.inputStream
    return stream.reader().readText().trim()
}

val localProperties = run {
    val props = Properties()
    val file = file("$rootDir/local.properties")
    if (file.exists()) props.load(file.inputStream())
    props
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}

subprojects {

    apply(plugin = "maven-publish")
    group = "com.minikorp"
    version = runCommand("$rootDir/scripts/latest-version.sh")

    val modules = arrayOf("drill-common", "drill-processor")
    if (this.name in modules) {
        afterEvaluate {

            val sourcesJar by tasks.creating(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets["main"].allSource)
            }

            this.publishing {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/minikorp/drill")
                        credentials {
                            username = "minikorp"
                            password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }

                publications {
                    register("gpr", MavenPublication::class) {
                        from(components["java"])
                        artifact(sourcesJar)
                    }
                }
            }
        }
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