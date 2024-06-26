/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.ui.internal.view.task.adapter;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.buildship.ui.internal.view.task.ProjectNode;

/**
 * Adapts {@link ProjectNode} instances to {@link IPropertySource} instances.
 * <p/>
 * By adapting to {@link IPropertySource} instances, the Eclipse Properties View can automatically
 * display the properties of a project node upon selection.
 * <p/>
 * The adapter is registered with the Eclipse platform via the {@code org.eclipse.core.runtime.adapters} extension point.
 * <p/>
 * Description on the adapters is available at <a href= "http://www.programcreek.com/2012/01/decipher-eclipse-architecture-iadaptable-part-1-brief-introduction">
 * http://www.programcreek.com/2012/01/decipher-eclipse-architecture-iadaptable-part-1-brief-introduction</a>.
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) // Eclipse Mars M6 introduced type parameters on the IAdapterFactory interface
public final class ProjectNodeAdapterFactory implements IAdapterFactory {

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IPropertySource.class && adaptableObject instanceof ProjectNode) {
            return createProjectNodeAdapter((ProjectNode) adaptableObject);
        } else {
            return null;
        }
    }

    private IPropertySource createProjectNodeAdapter(ProjectNode adaptableObject) {
        return new ProjectNodeAdapter(adaptableObject);
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[]{IPropertySource.class};
    }

}
