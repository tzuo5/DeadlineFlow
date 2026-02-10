plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.deadlineflow"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.13")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.deadlineflow.app.DeadlineFlowApp")
    applicationDefaultJvmArgs = listOf(
        // Required on newer JDKs so JavaFX and sqlite-jdbc native loading remains allowed.
        "--enable-native-access=javafx.graphics,ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
