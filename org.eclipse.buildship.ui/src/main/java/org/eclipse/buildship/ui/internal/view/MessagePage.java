/*******************************************************************************
 * Copyright (c) 2023 Gradle Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.buildship.ui.internal.view;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A {@link MultiPageView} page displaying a simple message.
 */
@SuppressWarnings("unchecked")
public final class MessagePage extends BasePage<Composite> {

    private final String message;

    /**
     * Creates a new page. The message is the empty string.
     */
    public MessagePage(String message) {
        this.message = message;
    }

    @Override
    public String getDisplayName() {
        return "DEFAULT";  // never shown in the UI
    }

    @Override
    public Composite createPageWithResult(Composite parent) {
        // message in default page of outline should have margins
        Composite root = new Composite(parent, SWT.NULL);
        root.setLayout(new FillLayout());

        Label messageLabel = new Label(root, SWT.LEFT | SWT.TOP | SWT.WRAP);
        messageLabel.setText(this.message);

        return root;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

}
