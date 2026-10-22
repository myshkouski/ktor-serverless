plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    implementation("org.jetbrains.kotlin:js-plain-objects:2.4.21")
}
