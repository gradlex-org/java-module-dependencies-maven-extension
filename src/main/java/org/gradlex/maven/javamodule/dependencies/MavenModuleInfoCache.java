// SPDX-License-Identifier: Apache-2.0
package org.gradlex.maven.javamodule.dependencies;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.project.MavenProject;
import org.gradlex.maven.javamodule.dependencies.internal.utils.ModuleInfo;

public class MavenModuleInfoCache {
    private final Map<String, ModuleInfo> mainModuleInfos = new HashMap<>();
    private final Map<String, ModuleInfo> testModuleInfos = new HashMap<>();

    private final Map<String, String> mainModuleNameToCoordinates = new HashMap<>();
    private final Map<String, String> testModuleNameToCoordinates = new HashMap<>();

    public void put(MavenProject project) throws IOException {
        List<String> compileSourceRoots = project.getCompileSourceRoots();
        List<String> testCompileSourceRoots = project.getTestCompileSourceRoots();
        List<String> altTestCompileSourceRoots = project.getTestCompileSourceRoots().stream()
                .map(location -> location + "9")
                .collect(Collectors.toList());

        Optional<File> mainModuleInfo = compileSourceRoots.stream()
                .map(src -> new File(src, "module-info.java"))
                .filter(File::exists)
                .findFirst();
        Optional<File> testModuleInfo = Stream.concat(
                        testCompileSourceRoots.stream(), altTestCompileSourceRoots.stream())
                .map(src -> new File(src, "module-info.java"))
                .filter(File::exists)
                .findFirst();

        if (mainModuleInfo.isPresent()) {
            String content = new String(Files.readAllBytes(mainModuleInfo.get().toPath()), UTF_8);
            ModuleInfo moduleInfo = new ModuleInfo(content);
            mainModuleInfos.put(toCoordinates(project), moduleInfo);
            mainModuleNameToCoordinates.put(
                    moduleInfo.getModuleName(), toCoordinates(project) + ":" + project.getVersion());
        }
        if (testModuleInfo.isPresent()) {
            String content = new String(Files.readAllBytes(testModuleInfo.get().toPath()), UTF_8);
            ModuleInfo moduleInfo = new ModuleInfo(content);
            testModuleInfos.put(toCoordinates(project), moduleInfo);
            testModuleNameToCoordinates.put(
                    moduleInfo.getModuleName(), toCoordinates(project) + ":" + project.getVersion());
        }
    }

    public Map<String, String> getMainModuleNameToCoordinates() {
        return mainModuleNameToCoordinates;
    }

    public Map<String, String> getTestModuleNameToCoordinates() {
        return testModuleNameToCoordinates;
    }

    public ModuleInfo getMain(MavenProject project) {
        return mainModuleInfos.getOrDefault(toCoordinates(project), ModuleInfo.EMPTY);
    }

    public ModuleInfo getTest(MavenProject project) {
        return testModuleInfos.getOrDefault(toCoordinates(project), ModuleInfo.EMPTY);
    }

    private String toCoordinates(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }
}
