plugins {
    kotlin("jvm")
    kotlin("kapt")
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":drill-common"))
    implementation("com.squareup.moshi:moshi-kotlin:1.9.2")
    kapt(project(":drill-processor"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}