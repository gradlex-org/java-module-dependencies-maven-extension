/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.maven.javamodule.dependencies.test;

import org.gradlex.maven.javamodule.dependencies.test.fixture.MavenBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
