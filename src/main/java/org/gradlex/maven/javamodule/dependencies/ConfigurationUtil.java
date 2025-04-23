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

package org.gradlex.maven.javamodule.dependencies;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.gradlex.maven.javamodule.dependencies.internal.utils.ModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class ConfigurationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUtil.class);

    static void addDependenciesForModuleInfo(String from, List<Dependency> dependencies, ModuleInfo mainModuleInfo, ModuleInfo testModuleInfo, List<Dependency> managed, Map<String, String> moduleNameToLocal, Map<String, String> localTestMappings, Map<String, String> localMappings) {
        mainModuleInfo.get(ModuleInfo.Directive.REQUIRES).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "compile", d)); // should be "runtime" in consumer BOM
        mainModuleInfo.get(ModuleInfo.Directive.REQUIRES_TRANSITIVE).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "compile", d));
        mainModuleInfo.get(ModuleInfo.Directive.REQUIRES_STATIC).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "provided", d));
        mainModuleInfo.get(ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "compile", d)); // should be "???" in consumer BOM

        testModuleInfo.get(ModuleInfo.Directive.REQUIRES).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "test", d));
        testModuleInfo.get(ModuleInfo.Directive.REQUIRES_TRANSITIVE).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "test", d));
        testModuleInfo.get(ModuleInfo.Directive.REQUIRES_STATIC).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "test", d));
        testModuleInfo.get(ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE).forEach(d -> addDependency(from, dependencies, managed, moduleNameToLocal, localTestMappings, localMappings, "test", d));
    }

    private static void addDependency(String from, List<Dependency> dependencies, List<Dependency> managed, Map<String, String> moduleNameToLocal, Map<String, String> moduleNameToLocalTest, Map<String, String> localMappings, String scope, String moduleName) {
        if (JDKInfo.MODULES.contains(moduleName)) {
            return;
        }

        String localModule = moduleNameToLocal.get(moduleName);
        if (from.equals(localModule)) {
            return; // do not add dependency on self (test -> main)
        }
        if (localModule != null) {
            String[] gav = localModule.split(":");
            defineDependency(dependencies, scope, gav[0], gav[1], gav[2], false);
            return;
        }
        String localTestModule = moduleNameToLocalTest.get(moduleName);
        if (localTestModule != null) {
            String[] gav = localTestModule.split(":");
            defineDependency(dependencies, scope, gav[0], gav[1], gav[2], true);
            return;
        }

        String externalModule = localMappings.get(moduleName);
        if (externalModule == null) {
            externalModule = SharedMappings.mappings.get(moduleName);
        }
        if (externalModule == null) {
            LOGGER.warn("Mapping missing in '.mvn/modules.properties': {}", moduleName);
            return;
        }

        String[] ga = externalModule.split(":");
        Optional<Dependency> version =
                managed.stream().filter(v -> v.getGroupId().equals(ga[0]) && v.getArtifactId().equals(ga[1])).findFirst();
        if (!version.isPresent()) {
            LOGGER.warn("Version missing: {}", externalModule);
            return;
        }
        defineDependency(dependencies, scope, ga[0], ga[1], version.get().getVersion(), false);
    }

    private static void defineDependency(List<Dependency> dependencies, String scope, String group, String artifactId, String version, boolean testsClassifier) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(group);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        if (testsClassifier) {
            dependency.setClassifier("tests");
        }
        dependencies.add(dependency);
    }

    static void collectManagedDependencies(MavenProject project, List<Dependency> managedDependencies) {
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
            managedDependencies.addAll(dependencyManagement.getDependencies());
        }
        if (project.getParent() != null) {
            collectManagedDependencies(project.getParent(), managedDependencies);
        }
    }
}
