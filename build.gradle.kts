plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "mini"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
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