/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.ui.internal.util.predicate;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.buildship.core.internal.configuration.GradleProjectNature;

/**
 * Supplies some useful {@link Predicate} instances. May also be used used in PropertyTesters.
 */
@SuppressWarnings("restriction")
public final class Predicates {

    private Predicates() {
    }

    public static Predicate<Object> hasGradleNature() {
        return new Predicate<Object>() {

            @Override
            public boolean apply(Object adaptable) {
                @SuppressWarnings({ "cast", "RedundantCast" })
                IProject project = (IProject) Platform.getAdapterManager().getAdapter(adaptable, IProject.class);
                if (project != null) {
                    return hasGradleNature(project);
                } else if (adaptable instanceof PackageFragmentRootContainer) {
                    project = ((PackageFragmentRootContainer) adaptable).getJavaProject().getProject();
                    return hasGradleNature(project);
                } else if (adaptable instanceof IStructuredSelection) {
                    List<?> selections = ((IStructuredSelection) adaptable).toList();
                    return FluentIterable.from(selections).anyMatch(hasGradleNature());
                }
                return false;
            }
        };
    }

    private static boolean hasGradleNature(IProject project) {
        return GradleProjectNature.isPresentOn(project);
    }

}
