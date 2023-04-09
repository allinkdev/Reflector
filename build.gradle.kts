plugins {
    id("java-library")
    id("checkstyle")
    id("maven-publish")
}

group = "com.github.allinkdev"
version = "1.1.1"

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
}

tasks {
    assemble {
        dependsOn(checkstyleMain)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}