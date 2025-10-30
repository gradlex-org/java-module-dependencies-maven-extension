import java.util.Properties
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.internal.VersionNumber

version = "0.2"

dependencies {
    compileOnly("org.apache.maven:maven-core:3.9.11") {
        exclude("com.google.guava", "failureaccess")
        exclude("com.google.guava", "guava")
        exclude("com.google.inject", "guice")
        exclude("javax.inject", "javax.inject")
        exclude("org.apache.maven.resolver", "maven-resolver-api")
        exclude("org.apache.maven.resolver", "maven-resolver-impl")
        exclude("org.apache.maven.resolver", "maven-resolver-spi")
        exclude("org.apache.maven.resolver", "maven-resolver-util")
        exclude("org.apache.maven.shared", "maven-shared-utils")
        exclude("org.apache.maven", "maven-artifact")
        exclude("org.apache.maven", "maven-builder-support")
        exclude("org.apache.maven", "maven-model-builder")
        exclude("org.apache.maven", "maven-plugin-api")
        exclude("org.apache.maven", "maven-repository-metadata")
        exclude("org.apache.maven", "maven-resolver-provider")
        exclude("org.apache.maven", "maven-settings-builder")
        exclude("org.apache.maven", "maven-settings")
        exclude("org.codehaus.plexus", "plexus-component-annotations")
        exclude("org.eclipse.sisu", "org.eclipse.sisu.inject")
        exclude("org.eclipse.sisu", "org.eclipse.sisu.plexus")
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation("io.takari.maven.plugins:takari-plugin-integration-testing:3.1.1") {
        exclude("com.google.guava", "failureaccess")
        exclude("com.google.guava", "guava")
        exclude("com.google.inject", "guice")
        exclude("javax.inject", "javax.inject")
        exclude("org.apache.maven.resolver", "maven-resolver-api")
        exclude("org.apache.maven.resolver", "maven-resolver-impl")
        exclude("org.apache.maven.resolver", "maven-resolver-spi")
        exclude("org.apache.maven.resolver", "maven-resolver-util")
        exclude("org.apache.maven.shared", "maven-shared-utils")
        exclude("org.apache.maven", "maven-artifact")
        exclude("org.apache.maven", "maven-builder-support")
        exclude("org.apache.maven", "maven-model-builder")
        exclude("org.apache.maven", "maven-plugin-api")
        exclude("org.apache.maven", "maven-repository-metadata")
        exclude("org.apache.maven", "maven-resolver-provider")
        exclude("org.apache.maven", "maven-settings-builder")
        exclude("org.apache.maven", "maven-settings")
        exclude("org.codehaus.plexus", "plexus-component-annotations")
        exclude("org.eclipse.sisu", "org.eclipse.sisu.inject")
        exclude("org.eclipse.sisu", "org.eclipse.sisu.plexus")
        exclude("org.slf4j", "slf4j-api")
        exclude("org.slf4j", "slf4j-simple")
        exclude("io.takari.m2e.workspace", "org.eclipse.m2e.workspace.cli")
    }
}

publishingConventions {
    mavenCentral {
        displayName("Java Module Dependencies Maven Extension")
        description("A Maven extension to use dependencies from module-info.java files.")
    }
    gitHub("https://github.com/gradlex-org/java-module-dependencies-maven-extension")
    developer {
        name = "Jendrik Johannes"
        email = "jendrik@gradlex.org"
    }
}

publishing { repositories.maven(layout.buildDirectory.dir("test-repo")) { name = "testRepo" } }

tasks.test { dependsOn(tasks.named("publishAllPublicationsToTestRepoRepository")) }

// update 'unique_modules.properties'
val detachedResolver: ProjectInternal.DetachedResolver = (project as ProjectInternal).newDetachedResolver()

detachedResolver.repositories.ivy {
    name = "Modules Properties Repository"
    url = project.uri("https://raw.githubusercontent.com/sormuras/modules/main/com.github.sormuras.modules")
    metadataSources.artifact()
    patternLayout {
        artifact("[organisation]/[module].properties")
        ivy("[module]/[revision]/ivy.xml")
        setM2compatible(true)
    }
}

val modulePropertiesScope = detachedResolver.configurations.dependencyScope("moduleProperties")
val modulePropertiesPath =
    detachedResolver.configurations.resolvable("modulePropertiesPath") { extendsFrom(modulePropertiesScope.get()) }
val dep =
    detachedResolver.dependencies.add(modulePropertiesScope.name, "com.github.sormuras.modules:modules:1@properties")

(dep as ExternalModuleDependency).isChanging = true

val updateUniqueModulesProperties =
    tasks.register<UniqueModulesPropertiesUpdate>("updateUniqueModulesProperties") {
        skipUpdate = providers.environmentVariable("CI").getOrElse("false").toBoolean()
        modulesProperties.from(modulePropertiesPath)
        uniqueModulesProperties =
            layout.projectDirectory.file(
                "src/main/resources/org/gradlex/maven/javamodule/dependencies/unique_modules.properties"
            )
    }

sourceSets.main {
    resources.setSrcDirs(
        listOf(
            updateUniqueModulesProperties.map {
                it.uniqueModulesProperties
                    .get()
                    .asFile
                    .parentFile
                    .parentFile
                    .parentFile
                    .parentFile
                    .parentFile
                    .parentFile
            }
        )
    )
}

abstract class UniqueModulesPropertiesUpdate : DefaultTask() {

    @get:Inject abstract val layout: ProjectLayout

    @get:Input abstract val skipUpdate: Property<Boolean>

    @get:InputFiles abstract val modulesProperties: ConfigurableFileCollection

    @get:OutputFile abstract val uniqueModulesProperties: RegularFileProperty

    @TaskAction
    fun convert() {
        if (skipUpdate.get()) {
            return
        }

        val modulesToRepoLocation = Properties()
        modulesToRepoLocation.load(modulesProperties.singleFile.inputStream())
        val modules =
            modulesToRepoLocation
                .toSortedMap { e1, e2 -> e1.toString().compareTo(e2.toString()) }
                .map { entry ->
                    val split = entry.value.toString().split("/")
                    val group = split.subList(4, split.size - 3).joinToString(".")
                    val name =
                        split[split.size - 3]
                            // Special handling of "wrong" entries
                            .replace("-debug-jdk18on", "-jdk18on")
                    val version = split[split.size - 2]
                    Module(entry.key.toString(), "$group:$name", version)
                }
                .groupBy { it.ga }
                .values
                .map { moduleList -> moduleList.maxBy { VersionNumber.parse(it.version) } }
                .sortedBy { it.name }

        val modulesToCoordinates = modules.map { "${it.name}=${it.ga}\n" }
        uniqueModulesProperties.get().asFile.writeText(modulesToCoordinates.joinToString("").trim())
    }

    data class Module(val name: String, val ga: String, val version: String)
}
