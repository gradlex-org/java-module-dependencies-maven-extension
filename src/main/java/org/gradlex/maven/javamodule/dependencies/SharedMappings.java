// SPDX-License-Identifier: Apache-2.0
package org.gradlex.maven.javamodule.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Class copied without modification.
 */
public final class SharedMappings {
    public static Map<String, String> mappings = loadModuleNameToGAProperties();

    static Map<String, String> loadModuleNameToGAProperties() {
        Properties properties = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                if (get(key) != null) {
                    throw new IllegalArgumentException(key + " already present.");
                }
                return super.put(key, value);
            }
        };
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, String> propertiesAsMap = (Map) properties;

        try (InputStream coordinatesFile =
                JavaModuleDependenciesLifecycleParticipant.class.getResourceAsStream("unique_modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream coordinatesFile =
                JavaModuleDependenciesLifecycleParticipant.class.getResourceAsStream("modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return propertiesAsMap;
    }

    private SharedMappings() {}
}
