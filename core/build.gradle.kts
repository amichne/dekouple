plugins {
    kotlin("jvm")
}

group = "io.amichne"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.moshiKotlin)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
