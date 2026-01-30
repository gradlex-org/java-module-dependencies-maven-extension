// SPDX-License-Identifier: Apache-2.0
package org.gradlex.maven.javamodule.dependencies.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.maven.javamodule.dependencies.test.fixture.MavenBuild;
import org.junit.jupiter.api.Test;

class WarningsTest {

    MavenBuild build = new MavenBuild();

    @Test
    void prints_warning_for_missing_mapping() {
        build.appModuleInfoFile.writeText("""
            module org.my.app {
                requires commons.math3;
            }""");

        // Manually add dependency so that build does not fail
        build.appPomFile.replaceText("</project>", """
                <dependencies>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-math3</artifactId>
                        <version>3.6.1</version>
                    </dependency>
                </dependencies>
            </project>
            """);

        var result = build.verify();

        assertThat(result.getLog()).contains("[WARNING] Mapping missing in '.mvn/modules.properties': commons.math3");
    }
}
