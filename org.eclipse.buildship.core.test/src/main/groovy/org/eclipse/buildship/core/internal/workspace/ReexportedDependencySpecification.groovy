/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.core.internal.workspace

import org.gradle.api.JavaVersion
import spock.lang.IgnoreIf

import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IWorkspaceRunnable
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore

import org.eclipse.buildship.core.internal.test.fixtures.ProjectSynchronizationSpecification
import org.eclipse.buildship.core.GradleDistribution

@IgnoreIf({ JavaVersion.current().isJava9Compatible() })
class ReexportedDependencySpecification extends ProjectSynchronizationSpecification {

    def "Transitive dependencies are acessible from local project classpath when using Gradle 2.5+"() {
        setup:
        File location = multiProjectWithSpringTransitiveDependency(dependencyScope)

        when:
        importAndWait(location, distribution)

        then:
        def moduleA = findProject('moduleA')
        def moduleB = findProject('moduleB')
        resolvedClasspath(moduleA).any { IClasspathEntry entry -> entry.path.lastSegment() == 'spring-beans-1.2.8.jar' }
        resolvedClasspath(moduleA).any { IClasspathEntry entry -> entry.path.lastSegment() == 'moduleB' && entry.entryKind == IClasspathEntry.CPE_PROJECT && !entry.isExported() }
        resolvedClasspath(moduleB).any { IClasspathEntry entry -> entry.path.lastSegment() == 'spring-beans-1.2.8.jar' && !entry.isExported() }

        where:
        distribution                         | dependencyScope
        GradleDistribution.forVersion('2.6') | "compile"
        GradleDistribution.fromBuild()       | "implementation"
    }

    def "Excluded dependencies are not resolved when using Gradle 2.5+"() {
        setup:
        File location = springExampleProjectFromBug473348(dependencyScope)

        when:
        importAndWait(location, distribution)

        then:
        def moduleA = findProject('moduleA')
        def moduleB = findProject('moduleB')

        resolvedClasspath(moduleA).any { IClasspathEntry entry -> entry.path.lastSegment() == 'spring-core-3.1.4.RELEASE.jar' }
        !resolvedClasspath(moduleA).any { IClasspathEntry entry -> entry.path.lastSegment() == 'spring-core-1.2.8.jar' }
        resolvedClasspath(moduleA).any { IClasspathEntry entry -> entry.path.lastSegment() == 'moduleB' && entry.entryKind == IClasspathEntry.CPE_PROJECT && !entry.isExported() }
        resolvedClasspath(moduleB).any { IClasspathEntry entry -> entry.path.lastSegment() == 'spring-core-1.2.8.jar' && !entry.isExported() }

        where:
        distribution                         | dependencyScope
        GradleDistribution.forVersion('2.6') | "compile"
        GradleDistribution.fromBuild()       | "implementation"
    }

    def "Sample with transitive dependency exclusion should compile when imported by Buildship"() {
        setup:
        File location = springExampleProjectFromBug473348(dependencyScope)

        when:
        importAndWait(location, distribution)
        waitForBuild()

        then:
        errorMarkers(findProject("moduleA")) == []
        errorMarkers(findProject("moduleB")) == []

        where:
        distribution                         | dependencyScope
        GradleDistribution.forVersion('2.6') | "compile"
        GradleDistribution.fromBuild()       | "implementation"
    }

    private List<String> errorMarkers(IProject project) {
        def errors
        workspace.run({
            IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)
            errors = markers.findAll { it.getAttribute(IMarker.SEVERITY) == IMarker.SEVERITY_ERROR }.collect { it.getAttribute(IMarker.MESSAGE) }
        } as IWorkspaceRunnable, null)
        return errors
    }

    private def waitForBuild() {
        workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null)
    }

    private File multiProjectWithSpringTransitiveDependency(scope = "compile") {
        dir('spring-example') {
            file 'build.gradle', """
                allprojects {
                    ${jcenterRepositoryBlock}
                    apply plugin: 'java'
                }
            """
            file 'settings.gradle', '''
                include "moduleA"
                include "moduleB"
            '''

            moduleA {
                dir 'src/main/java'
                file 'build.gradle', """
                    dependencies {
                        $scope (project(":moduleB"))
                    }
                """
            }
            moduleB {
                dir 'src/main/java'
                file 'build.gradle', """
                    dependencies {
                        $scope "org.springframework:spring-beans:1.2.8"
                    }
                """
            }
        }
    }

    private File springExampleProjectFromBug473348(configuration = 'implementation') {
        dir('Bug473348') {
            file 'build.gradle', """
                allprojects {
                   ${jcenterRepositoryBlock}
                   apply plugin: 'java'
                }
            """
            file 'settings.gradle', '''
                include "moduleA"
                include "moduleB"
            '''
            moduleA {
                file 'build.gradle', """
                    dependencies {
                        $configuration "org.springframework:spring-beans:3.1.4.RELEASE"
                        $configuration (project(":moduleB")) {
                            exclude group: "org.springframework"
                        }
                    }
                """
                dir ('src/main/java') {
                    file 'ApplicationA.java', '''
                        import org.springframework.beans.BeansException;
                        import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
                        import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
                        import java.beans.PropertyEditor;

                        public class ApplicationA implements BeanFactoryPostProcessor {
                            @Override
                            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                                try {
                                    Class<?> classA = Class.forName("any");
                                    beanFactory.registerCustomEditor(classA, classA.asSubclass(PropertyEditor.class));
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    '''
                }

            }

            moduleB {
                file 'build.gradle', """
                    dependencies {
                        $configuration "org.springframework:spring-beans:1.2.8"
                    }
                """
                dir("src/main/java") {
                    file 'ApplicationB.java', '''
                        import org.springframework.beans.factory.FactoryBean;
                        public class ApplicationB {
                            public void methodA(){
                                FactoryBean factoryBean;
                            }
                        }
                    '''
                }

            }
        }
    }

    private def resolvedClasspath(IProject project) {
        JavaCore.create(project).getResolvedClasspath(false)
    }

    private def rawClasspath(IProject project) {
        JavaCore.create(project).rawClasspath
    }
}
