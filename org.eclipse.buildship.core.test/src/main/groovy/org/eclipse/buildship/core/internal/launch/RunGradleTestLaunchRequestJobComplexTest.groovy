/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.core.internal.launch

import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.test.TestOperationDescriptor

import com.google.common.collect.Lists

import org.eclipse.core.resources.IProject
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration

import org.eclipse.buildship.core.internal.CorePlugin
import org.eclipse.buildship.core.internal.configuration.RunConfiguration
import org.eclipse.buildship.core.internal.event.Event
import org.eclipse.buildship.core.internal.event.EventListener
import org.eclipse.buildship.core.internal.test.fixtures.ProjectSynchronizationSpecification;
import org.eclipse.buildship.core.GradleDistribution

class RunGradleTestLaunchRequestJobComplexTest extends ProjectSynchronizationSpecification {

    def "Can execute a test launch using test operation descriptors"() {
        setup:
        // import the project
        importAndWait(sampleProject())
        IProject project = findProject('sample-project')

        // setup project collection
        List descriptors = Lists.newCopyOnWriteArrayList()
        collectTestDescriptorsInto(descriptors)

        // execute a test build to obtain test operation descriptors
        GradleRunConfigurationAttributes attributes = new GradleRunConfigurationAttributes(
            ['clean', 'test'],
            project.getLocation().toFile().absolutePath,
            GradleDistribution.fromBuild().toString(),
            "",
            null,
            [],
            [],
            true,
            true,
            false,
            false,
            false)
        ILaunchConfiguration launchConfiguration = CorePlugin.gradleLaunchConfigurationManager().getOrCreateRunConfiguration(attributes)
        RunConfiguration runConfig = CorePlugin.configurationManager().loadRunConfiguration(launchConfiguration)
        executeCleanTestAndWait(runConfig)

        when:
        // execute only the tests containing the word 'test1'
        def testJob = new RunGradleTestLaunchRequestJob(descriptors.findAll { it.name.contains('test1') }, CorePlugin.configurationManager().loadTestRunConfiguration(runConfig))
        descriptors.clear()
        testJob.schedule()
        testJob.join()

        then:
        // the test descriptors from the second test run should contain only 'test1' tests
        !descriptors.findAll { it.name.contains('test1') }.isEmpty()
        descriptors.findAll { it.name.contains('test2') }.isEmpty()
    }

    private def sampleProject() {
        dir('sample-project') {
            file 'build.gradle',  """
                apply plugin: "java"
                ${jcenterRepositoryBlock}
                dependencies { testImplementation 'junit:junit:4.10' }
            """
            dir('src/test/java') {
                file 'SampleTest.java', '''
                    import org.junit.Test;
                    import static org.junit.Assert.*;
                    public class SampleTest {
                        @Test public void test1() { assertTrue(true); }
                        @Test public void test2() { assertTrue(true); }
                    }
                '''
            }

        }
    }

    private def collectTestDescriptorsInto(List descriptors) {
        ProgressListener progressListener = new ProgressListener() {
            public void statusChanged(ProgressEvent event) {
                if (event.descriptor instanceof TestOperationDescriptor) {
                    descriptors.add(event.getDescriptor())
                }
            }
        }
        CorePlugin.listenerRegistry().addEventListener(new EventListener() {
            void onEvent(Event event) {
                if (event instanceof ExecuteLaunchRequestEvent) {
                    ((ExecuteLaunchRequestEvent) event).operation.addProgressListener(progressListener)
                }
            }
        })
    }

    private def executeCleanTestAndWait(RunConfiguration runConfig) {
        GradleRunConfigurationAttributes attributes = new GradleRunConfigurationAttributes(
            runConfig.tasks,
            runConfig.projectConfiguration.buildConfiguration.rootProjectDirectory.absolutePath,
            runConfig.projectConfiguration.buildConfiguration.gradleDistribution.toString(),
            runConfig.gradleUserHome,
            runConfig.javaHome,
            runConfig.jvmArguments,
            runConfig.arguments,
            runConfig.showExecutionView,
            runConfig.showConsoleView,
            runConfig.projectConfiguration.buildConfiguration.overrideWorkspaceSettings,
            runConfig.projectConfiguration.buildConfiguration.offlineMode,
            runConfig.projectConfiguration.buildConfiguration.buildScansEnabled)
        ILaunch launch = Mock()
        launch.getLaunchConfiguration() >> CorePlugin.gradleLaunchConfigurationManager().getOrCreateRunConfiguration(attributes)
        RunGradleBuildLaunchRequestJob job = new RunGradleBuildLaunchRequestJob(launch)
        job.schedule()
        job.join()
    }
}
