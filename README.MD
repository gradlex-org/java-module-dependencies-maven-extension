# Java Module Dependencies Maven Extension

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fgradlex-org%2Fjava-module-dependencies-maven-extension%2Fbadge%3Fref%3Dmain&style=flat)](https://actions-badge.atrox.dev/gradlex-org/java-module-dependencies-maven-extension/goto?ref=main)
[![Maven Central](https://img.shields.io/maven-metadata/v?label=Maven%20Central&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fgradlex%2Fjava-module-dependencies-maven-extension%2Fmaven-metadata.xml)](https://repo1.maven.org/maven2/org/gradlex/java-module-dependencies-maven-extension)

A [Maven Extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) to make Maven use dependencies from _module-info.java_ files automatically.
If you have a project that fully uses Java Modules, you do **not** need to declare dependencies in the `<dependencies>` section anymore.
Maven will use the information from your `module-info.java` directly.

To manage the versions of Java Modules, you can use the `<dependencyManagement>` section in a (parent) POM and/or importe BOMs.

This Maven Extension – which is part of the [GradleX](https://gradlex.org) project – is maintained by me, [Jendrik Johannes](https://github.com/jjohannes).
I offer consulting and training for Gradle, Maven and/or the Java Module System - please [reach out](mailto:jendrik.johannes@gmail.com) if you are interested.
There is also my [YouTube channel](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE) on Java modularity and build topics.

If you have a suggestion or a question, please [open an issue](https://github.com/gradlex-org/java-module-dependencies-maven-extension/issues/new).

There is a [CHANGELOG.md](CHANGELOG.md).

> [!NOTE]
> This Maven Extension is derived from its [Gradle counterpart](https://github.com/gradlex-org/java-module-dependencies).
> It offers the same fundamental functionality, but does not have all the convenience features of the Gradle plugin.
> Some of these are not directly transferable to Maven.
> However, if you think something from the Gradle plugin could be transferred here in a useful way for your Maven use case,
> [please open an issue](https://github.com/gradlex-org/java-module-dependencies-maven-extension/issues/new).

# How to use?

Working example projects to inspect:
- [gradle-project-setup-howto](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system_mavenized) is a full-fledged Java Module System project setup

## Register the extension

Add this to one of your parent POMs:

```xml
<build>
  <extensions>
    <extension>
      <groupId>org.gradlex</groupId>
      <artifactId>java-module-dependencies-maven-extension</artifactId>
      <version>0.2</version>
    </extension>
  </extensions>
</build>
```

## Effect of the extension

Once the extension is registered, dependencies are automatically determined based on the `requires` directives in your `module-info.java` files. For example:

```java
module org.example.mymodule {
  requires transitive org.slf4j; // translates to:
    // <dependency>
    //   <groupId>org.slf4j</groupId>
    //   <artifactId>slf4j-api</artifactId>
    //   <scope>compile</scope>
    // </dependency>
  requires com.fasterxml.jackson.core; // translates to:
    // <dependency>
    //   <groupId>com.fasterxml.jackson.core</groupId>
    //   <artifactId>jackson-core</artifactId>
    //   <scope>compile</scope> <!-- non-transitive -->
    // </dependency>
  requires static jakarta.servlet; // translates to:
    // <dependency>
    //   <groupId>jakarta.servlet</groupId>
    //   <artifactId>jakarta.servlet-api</artifactId>
    //   <scope>provided</scope>
    // </dependency>
}
```

## Add Module Name mapping information (if needed)

You may define additional mappings from _Module Name_ to _group:name (GA) coordinates_.

The plugin already knows about Modules available on Maven Central. The information is stored in:

- [modules.properties](src/main/resources/org/gradlex/maven/javamodule/dependencies/modules.properties) - [please open a PR](https://github.com/gradlex-org/java-module-dependencies-maven-extension/pulls) if you miss an entry
- [unique_modules.properties](src/main/resources/org/gradlex/maven/javamodule/dependencies/unique_modules.properties) - this information is extracted from [modules.properties](https://github.com/sormuras/modules/blob/main/com.github.sormuras.modules/com/github/sormuras/modules/modules.properties) by [@sormuras](https://github.com/sormuras)

You define additional entries (or overwrite entries from the plugin) in a `.mvn/modules.properties` file in your project:

```
org.apache.commons.lang3=org.apache.commons:commons-lang3
```

## Define Module versions in dependency management sections

Use `<dependencyManagement>` entries to define versions for the modules you depend on.
In a multi-project it is recommended to do that in a parent POM.
You may also manage them separately in a BOM that you import (or use an existing BOM). 

```xml
<dependencyManagement>
  <dependencies>
    <!-- individual version, e.g. for module 'org.slf4j' -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>1.7.36</version>
    </dependency>
    <!-- import versions from BOM, e.g. spring-boot-dependencies -->
    <!-- has versions for many widely used modules -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>2.3.0.RELEASE</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

# Disclaimer

Gradle and the Gradle logo are trademarks of Gradle, Inc.
The GradleX project is not endorsed by, affiliated with, or associated with Gradle or Gradle, Inc. in any way.
