/*
 * Copyright 2014 Netflix, Inc.
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
package nebula.plugin.dependencylock.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class LockDependenciesTask extends DefaultTask {
    @Input
    Set<String> configurationNames

    @OutputFile
    File dependenciesLock

    @TaskAction
    void lock() {
        def dependencyMap = readDependenciesFromConfigurations()
        writeLock(dependencyMap)
    }

    private readDependenciesFromConfigurations() {
        def deps = [:].withDefault { [:] }
        def confs = getConfigurationNames().collect { project.configurations.getByName(it) }

        confs.each { Configuration configuration ->
            def peerNames = configuration.allDependencies.withType(ProjectDependency).collect { it.name }
            configuration.allDependencies.withType(ExternalDependency).each { Dependency dependency ->
                deps["${dependency.group}:${dependency.name}"].requested = dependency.version
            }
            configuration.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency resolved ->
                if (!peerNames.contains(resolved.moduleName)) {
                    deps["${resolved.moduleGroup}:${resolved.moduleName}"].locked = resolved.moduleVersion
                }
            }
        }

        return deps
    }

    private void writeLock(deps) {
        def strings = deps.collect { k, v -> "  \"${k}\": { \"locked\": \"${v.locked}\", \"requested\": \"${v.requested}\" }"}
        strings = strings.sort()
        getDependenciesLock().withPrintWriter { out ->
            out.println '{'
            out.println strings.join(",${System.getProperty('line.separator')}")
            out.println '}'
        }
    }
}
