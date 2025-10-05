plugins {
    val nmcpVersion = "1.2.0"

    id("com.gradleup.nmcp") version nmcpVersion
    id("com.gradleup.nmcp.aggregation") version nmcpVersion
    id("java-library")
    id("maven-publish")
    id("signing")
    id("checkstyle")
    id("gradlexbuild.module-mappings")
}

group = "org.gradlex"
version = "0.2"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
    withJavadocJar()
}

tasks.compileJava {
    options.release = 8
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        addStringOption("Xwerror", "-quiet")
    }
}

dependencies {
    compileOnly("org.apache.maven:maven-core:3.9.11")

    nmcpAggregation(project(path))

    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.takari.maven.plugins:takari-plugin-integration-testing:3.1.1")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "Java Module Dependencies Maven Extension"
            description = "A Maven extension to use dependencies from module-info.java files."
            url = "https://github.com/gradlex-org/java-module-dependencies-maven-extension"
            licenses {
                license {
                    name = "Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            scm {
                connection = "scm:git:git://github.com/gradlex-org/java-module-dependencies-maven-extension.git"
                developerConnection = "scm:git:git://github.com/gradlex-org/java-module-dependencies-maven-extension.git"
                url = "https://github.com/gradlex-org/java-module-dependencies-maven-extension"
            }
            developers {
                developer {
                    name = "Jendrik Johannes"
                    email = "jendrik@gradlex.org"
                }
            }
        }
    }
    repositories.maven(layout.buildDirectory.dir("test-repo")) {
        name = "testRepo"
    }
}

signing {
    if (providers.gradleProperty("sign").getOrElse("false").toBoolean()) {
        useInMemoryPgpKeys(
            providers.environmentVariable("SIGNING_KEY").getOrNull(),
            providers.environmentVariable("SIGNING_PASSPHRASE").getOrNull()
        )
        sign(publishing.publications["maven"])
    }
}


nmcpAggregation {
    centralPortal {
        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME")
        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD")
        publishingType = "AUTOMATIC" // "USER_MANAGED"
    }
}

testing.suites.named<JvmTestSuite>("test") {
    useJUnitJupiter()
    targets.configureEach {
        testTask {
            dependsOn(tasks.named("publishAllPublicationsToTestRepoRepository"))
        }
    }
}

checkstyle {
    configDirectory = layout.projectDirectory.dir("gradle/checkstyle")
}
