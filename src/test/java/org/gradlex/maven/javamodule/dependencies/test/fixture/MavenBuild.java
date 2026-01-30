// SPDX-License-Identifier: Apache-2.0
package org.gradlex.maven.javamodule.dependencies.test.fixture;

import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MavenBuild {
    private final MavenRuntime.MavenRuntimeBuilder builder;

    public final Directory projectDir;
    public final WritableFile rootPom;
    public final WritableFile appPomFile;
    public final WritableFile libPomFile;
    public final WritableFile appModuleInfoFile;
    public final WritableFile libModuleInfoFile;

    public MavenBuild() {
        this.builder = MavenRuntime.forkedBuilder(new File(System.getenv("MAVEN_HOME")));

        this.projectDir = new Directory(createBuildTmpDir());
        this.rootPom = new WritableFile(projectDir, "pom.xml");
        this.appPomFile = new WritableFile(projectDir.dir("app"), "pom.xml");
        this.libPomFile = new WritableFile(projectDir.dir("lib"), "pom.xml");
        this.appModuleInfoFile = new WritableFile(projectDir.dir("app/src/main/java"), "module-info.java");
        this.libModuleInfoFile = new WritableFile(projectDir.dir("lib/src/main/java"), "module-info.java");

        var appClass = new WritableFile(projectDir.dir("app/src/main/java"), "org/example/app/App.java");

        appClass.writeText("""
                package org.example.app;
                import java.io.File;
                import java.util.Arrays;
                import java.util.stream.Collectors;
                public class App {
                    public static void main(String[] args) {
                        String path = Arrays.stream(System.getProperty("jdk.module.path").split(File.pathSeparator))
                                 .map(File::new).map(File::getName).collect(Collectors.joining(","));
                        System.out.println("Module Path: " + path);
                    }
                }
                """);

        rootPom.writeText("""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <artifactId>test-project</artifactId>
                <packaging>pom</packaging>
                <groupId>org.example</groupId>
                <version>1.0</version>
                <modules>
                    <module>app</module>
                    <module>lib</module>
                </modules>
                <build>
                    <pluginManagement>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.14.0</version>
                                <configuration>
                                    <release>17</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </pluginManagement>
                    <extensions>
                        <extension>
                            <groupId>org.gradlex</groupId>
                            <artifactId>java-module-dependencies-maven-extension</artifactId>
                            <version>0.1</version>
                        </extension>
                     </extensions>
                </build>
            </project>""");

        appPomFile.writeText("""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <artifactId>test-project</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0</version>
                </parent>
                <artifactId>app</artifactId>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.codehaus.mojo</groupId>
                                    <artifactId>exec-maven-plugin</artifactId>
                                    <version>3.5.0</version>
                                    <configuration>
                                        <executable>java</executable>
                                            <arguments>
                                                <argument>--module-path</argument>
                                                <modulepath/>
                                                <argument>--module</argument>
                                                <argument>org.example.app/org.example.app.App</argument>
                                          </arguments>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
            </project>""");
        libPomFile.writeText("""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <artifactId>test-project</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0</version>
                </parent>
                <artifactId>lib</artifactId>
            </project>""");
    }

    public WritableFile file(String path) {
        return new WritableFile(projectDir, path);
    }

    public MavenExecutionResult verify() {
        try {
            return runner().execute("verify");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MavenExecutionResult exec() {
        try {
            return runner().execute("exec:exec", "-pl", "app");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MavenExecutionResult dependencyTree() {
        try {
            return runner().execute("dependency:tree", "-pl", "app");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MavenExecution runner() {
        try {
            return builder.withCliOptions("-B", "-U", "-ntp")
                    .build()
                    .forProject(projectDir.getAsPath().toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Path createBuildTmpDir() {
        try {
            return Files.createTempDirectory("gradle-build");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
