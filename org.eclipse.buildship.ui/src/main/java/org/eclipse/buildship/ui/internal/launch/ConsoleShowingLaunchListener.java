/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.ui.internal.launch;

import com.google.common.base.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.buildship.core.internal.CorePlugin;
import org.eclipse.buildship.core.internal.configuration.RunConfiguration;
import org.eclipse.buildship.core.internal.launch.GradleRunConfigurationDelegate;
import org.eclipse.buildship.ui.internal.UiPlugin;
import org.eclipse.buildship.ui.internal.UiPluginConstants;
import org.eclipse.buildship.ui.internal.util.workbench.WorkbenchUtils;

/**
 * {@link ILaunchListener} implementation showing/activating the Console View when a new Gradle build has launched and the {@link
 * RunConfiguration#isShowConsoleView()} setting is enabled.
 * <p/>
 * The listener implementation is necessary since opening a view is a UI-related task and the launching is performed in the core component.
 */
public final class ConsoleShowingLaunchListener implements ILaunchListener {

    @Override
    public void launchAdded(ILaunch launch) {
        final Optional<RunConfiguration> attributes = convertToGradleRunConfigurationAttributes(launch);
        if (attributes.isPresent() && attributes.get().isShowConsoleView()) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    WorkbenchUtils.showView(UiPluginConstants.CONSOLE_VIEW_ID, null, IWorkbenchPage.VIEW_ACTIVATE);
                }
            });
        }
    }

    private Optional<RunConfiguration> convertToGradleRunConfigurationAttributes(ILaunch launch) {
        ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
        if (launchConfiguration == null) {
            return Optional.absent();
        }
        ILaunchConfigurationType type;
        try {
            type = launchConfiguration.getType();
        } catch (CoreException e) {
            UiPlugin.logger().error("Unable to determine launch configuration type", e);
            return Optional.absent();
        }
        if (GradleRunConfigurationDelegate.ID.equals(type.getIdentifier())) {
            return Optional.of(CorePlugin.configurationManager().loadRunConfiguration(launchConfiguration));
        }
        return Optional.absent();
    }

    @Override
    public void launchRemoved(ILaunch launch) {
    }

    @Override
    public void launchChanged(ILaunch launch) {
    }

    /**
     * Applies the logic of this listener to all already running Gradle consoles. This is needed since in case the ui plugin is started <i>after</i> a
     * run configuration has been launched, this listener will be registered too late to be notified about the launched console.
     */
    public void handleAlreadyRunningLaunches() {
        for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
            if (!launch.isTerminated()) {
                launchAdded(launch);
            }
        }
    }

}
