/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2017 The ZAP Development Team
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
package org.zaproxy.zap.view;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.model.Context;

/**
 * An {@link AbstractAction} that allows to delete a {@link Context}.
 *
 * <p>The (default) accelerator for the action is given by {@link View#getDefaultDeleteKeyStroke()}.
 *
 * @since 2.7.0
 */
public abstract class DeleteContextAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** The name of the action. */
    public static final String ACTION_NAME = "zap.delete.context";

    /** Constructs a {@code DeleteContextAction}. */
    public DeleteContextAction() {
        super(ACTION_NAME);

        putValue(ACCELERATOR_KEY, View.getSingleton().getDefaultDeleteKeyStroke());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ArrayList<Context> contexts = getContexts();
        if (contexts == null) {
            return;
        }

        String contextList =
                contexts.stream().map(c -> c.getName()).collect(Collectors.joining(", "));

        if (View.getSingleton()
                        .showConfirmDialog(
                                contexts.size() > 1
                                        ? Constant.messages.getString(
                                                "context.delete.warning.multiple", contextList)
                                        : Constant.messages.getString("context.delete.warning"))
                == JOptionPane.OK_OPTION) {
            for (Context context : contexts) {
                Model.getSingleton().getSession().deleteContext(context);
            }
        }
    }

    /**
     * Called when the action is performed to delete one or more selected contexts
     *
     * @return the {@code ArrayList} of {@code Context} to delete or {@code null} if none
     */
    protected abstract ArrayList<Context> getContexts();
}
