// SPDX-License-Identifier: Apache-2.0
package org.gradlex.maven.javamodule.dependencies.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.maven.javamodule.dependencies.test.fixture.MavenBuild;
import org.junit.jupiter.api.Test;

class ModuleNameMappingTest {

    MavenBuild build = new MavenBuild();

    @Test
    void can_require_other_local_module() {
        build.libModuleInfoFile.writeText("module org.example.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires org.example.lib;
            }""");

        build.verify();

        var result = build.dependencyTree();
        assertThat(result.getLog())
                .contains("[INFO] org.example:app:jar:1.0", "[INFO] \\- org.example:lib:jar:1.0:compile");
    }

    @Test
    void can_add_custom_mapping_via_properties_file_in_default_location() {
        var modulesPropertiesFile = build.file(".mvn/modules.properties");
        modulesPropertiesFile.writeText("jakarta.mail=com.sun.mail:jakarta.mail");
        build.rootPom.replaceText("</project>", """
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.sun.mail</groupId>
                            <artifactId>jakarta.mail</artifactId>
                            <version>2.0.1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """);

        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires jakarta.mail;
            }""");

        build.verify();

        var result = build.dependencyTree();
        assertThat(result.getLog())
                .contains(
                        "[INFO] org.example:app:jar:1.0",
                        "[INFO] \\- com.sun.mail:jakarta.mail:jar:2.0.1:compile",
                        "[INFO]    \\- com.sun.activation:jakarta.activation:jar:2.0.1:compile");

        result = build.exec();
        assertThat(result.getLog())
                .contains("Module Path: classes,jakarta.mail-2.0.1.jar,jakarta.activation-2.0.1.jar");
    }

    @Test
    void versions_can_be_imported() {
        build.rootPom.replaceText("</project>", """
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-dependencies</artifactId>
                            <version>2.3.0.RELEASE</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """);
        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires org.slf4j;
            }""");

        build.verify();

        var result = build.dependencyTree();
        assertThat(result.getLog())
                .contains("[INFO] org.example:app:jar:1.0", "[INFO] \\- org.slf4j:slf4j-api:jar:1.7.30:compile");

        result = build.exec();
        assertThat(result.getLog()).contains("Module Path: classes,slf4j-api-1.7.30.jar");
    }

    @Test
    void ignores_jdk_modules() {
        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires java.desktop;
            }""");

        var result = build.verify();

        assertThat(result.getLog())
                .doesNotContain("[WARNING] Mapping missing in '.mvn/modules.properties': java.desktop");
    }
}
