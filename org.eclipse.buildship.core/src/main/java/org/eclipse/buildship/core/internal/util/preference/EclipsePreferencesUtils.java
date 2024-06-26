/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.core.internal.util.preference;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.buildship.core.internal.GradlePluginsRuntimeException;

/**
 * Utility methods to work with Eclipse preferences.
 */
public final class EclipsePreferencesUtils {

    private EclipsePreferencesUtils() {
    }

    /**
     * Returns the instance scope preferences object.
     * <p/>
     * In older Eclipse versions the {@code InstanceScope.INSTANCE} object does not exist. On the
     * other hand in Eclipse 4.5 the constructor of the {@code EclipseScope} class became
     * deprecated. This method returns the {@code InstanceScope} reference in a backward-compatible
     * way.
     * 
     * @return the instance scope preferences object
     */
    public static InstanceScope getInstanceScope() {
        try {
            Field field = InstanceScope.class.getField("INSTANCE");
            return (InstanceScope) field.get(null);
        } catch (Exception e1) {
            try {
                return InstanceScope.class.newInstance();
            } catch (Exception e2) {
                throw new GradlePluginsRuntimeException(e2);
            }
        }
    }
}
