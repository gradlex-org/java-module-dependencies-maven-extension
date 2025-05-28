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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.gradlex.maven.javamodule.dependencies.ConfigurationUtil.addDependenciesForModuleInfo;
import static org.gradlex.maven.javamodule.dependencies.ConfigurationUtil.collectManagedDependencies;

@Singleton
@Named("java-module-dependencies")
public class JavaModuleDependenciesLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private final MavenModuleInfoCache moduleInfoCache = new MavenModuleInfoCache();

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        List<MavenProject> projects = session.getAllProjects();
        Map<String, String> localMappings = loadLocalMappings(session.getRequest().getMultiModuleProjectDirectory());

        for (MavenProject project : projects) {
            try {
                moduleInfoCache.put(project);
            } catch (IOException e) {
                throw new MavenExecutionException("Error reading module-info.java", e);
            }
        }

        for (MavenProject project : projects) {
            List<Dependency> managedDependencies = new ArrayList<>();
            collectManagedDependencies(project, managedDependencies);

            addDependenciesForModuleInfo(
                    project.getGroupId() + ":" + project.getArtifactId() + ":" +project.getVersion(),
                    project.getDependencies(),
                    moduleInfoCache.getMain(project),
                    moduleInfoCache.getTest(project),
                    managedDependencies,
                    moduleInfoCache.getMainModuleNameToCoordinates(),
                    moduleInfoCache.getTestModuleNameToCoordinates(),
                    localMappings,
                    interpolator(project));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, String> loadLocalMappings(File root) {
        Map<String, String> localMappings = new LinkedHashMap<>();
        File mappingFile = new File(root, ".mvn/modules.properties");
        if (mappingFile.exists()) {
            try {
                Properties p = new Properties();
                p.load(Files.newInputStream(mappingFile.toPath()));
                localMappings.putAll((Map) p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return localMappings;
    }

    private Interpolator interpolator(MavenProject project) {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        ValueSource allProperties = new PropertiesBasedValueSource(project.getModel().getProperties());
        interpolator.addValueSource(allProperties);
        return interpolator;
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
    }
}
