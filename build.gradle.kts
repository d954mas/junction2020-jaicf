plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    kotlin("plugin.serialization") version "1.3.61"
}

group = "monstrarium_dev"
version = "1.0.0"

val jaicf = "0.4.1"
val slf4j = "1.7.30"
val ktor = "1.3.1"

application {
    mainClassName = "com.justai.monstrarium.ServerKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")

    implementation(kotlin("stdlib-jdk8"))


    implementation("org.slf4j:slf4j-simple:$slf4j")
    implementation("org.slf4j:slf4j-log4j12:$slf4j")

    implementation("com.justai.jaicf:core:$jaicf")
    implementation("com.justai.jaicf:google-actions:$jaicf")
    implementation("com.justai.jaicf:caila:$jaicf")
    implementation("com.justai.jaicf:mongo:$jaicf")

    implementation("io.ktor:ktor-server-netty:$ktor")

    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    // https://mvnrepository.com/artifact/com.google.auth/google-auth-library-oauth2-http
    implementation("com.google.auth:google-auth-library-oauth2-http:0.15.0")

    // https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient
    implementation("org.apache.httpcomponents:httpclient:4.5.8")

    // https://mvnrepository.com/artifact/org.apache.httpcomponents/fluent-hc
    implementation("org.apache.httpcomponents:fluent-hc:4.5.8")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf("Main-Class" to application.mainClassName))
    }
}

tasks.create("stage") {
    dependsOn("shadowJar")

}
