/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2024 The ZAP Development Team
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
package org.zaproxy.zap.extension.history;

import static org.apache.log4j.builders.appender.SocketAppenderBuilder.LOGGER;

import java.awt.Component;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.AbstractDialog;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.SiteNode;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.zaproxy.zap.utils.ZapTextArea;

public class SiteNotesAddDialog extends AbstractDialog {

    private static final long serialVersionUID = 1L;
    private ZapTextArea txtDisplay = null;
    private JButton btnOk = null;
    private JButton btnCancel = null;

    private SiteNode siteNode;

    private JScrollPane jScrollPane = null;

    private JList<HistoryReference> childSiteNodes = null;

    private JScrollPane jScrollPaneSelector = null;

    private JPanel hrefSelectorPanel = null;

    /**
     * @throws HeadlessException
     */
    public SiteNotesAddDialog() throws HeadlessException {
        super();
        initialize();
    }

    /**
     * @param arg0
     * @param arg1
     * @throws HeadlessException
     */
    public SiteNotesAddDialog(Frame arg0, boolean arg1) throws HeadlessException {
        super(arg0, arg1);
        initialize();
    }

    /** This method initializes this */
    private void initialize() {
        this.setTitle(Constant.messages.getString("history.addnote.title"));

        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        Component buttonsGlue = Box.createHorizontalGlue();

        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(
                                                getHrefSelectorListPanel(),
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(
                                                getJScrollPane(),
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE,
                                                Short.MAX_VALUE))
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(
                                                buttonsGlue,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE)
                                        .addComponent(getBtnCancel())
                                        .addComponent(getBtnOk())));

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(getHrefSelectorListPanel())
                                        .addComponent(getJScrollPane()))
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(buttonsGlue)
                                        .addComponent(getBtnCancel())
                                        .addComponent(getBtnOk())));

        setContentPane(panel);

        this.addWindowListener(
                new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        clearAndDispose();
                    }
                });

        pack();
    }

    private JPanel getHrefSelectorListPanel() {
        if (hrefSelectorPanel == null) {
            hrefSelectorPanel = new JPanel();
            GroupLayout layout = new GroupLayout(hrefSelectorPanel);
            hrefSelectorPanel.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            JLabel titleLabel = new JLabel(Constant.messages.getString("sites.notes.history.id"));
            layout.setHorizontalGroup(
                    layout.createParallelGroup()
                            .addComponent(titleLabel)
                            .addComponent(getNewJScrollPane(getHrefSelectorList())));
            layout.setVerticalGroup(
                    layout.createSequentialGroup()
                            .addComponent(titleLabel)
                            .addComponent(getNewJScrollPane(getHrefSelectorList())));
        }
        return hrefSelectorPanel;
    }

    private JScrollPane getNewJScrollPane(Component view) {
        if (view == null) {
            return new JScrollPane();
        }
        if (jScrollPaneSelector == null) {
            jScrollPaneSelector = new JScrollPane();
            jScrollPaneSelector.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPaneSelector.setVerticalScrollBarPolicy(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            jScrollPaneSelector.setViewportView(view);
        }
        return jScrollPaneSelector;
    }

    private JList<HistoryReference> getHrefSelectorList() {
        if (childSiteNodes == null) {
            childSiteNodes = new JList<>();
            childSiteNodes.setCellRenderer(new HistoryReferenceRenderer());
            childSiteNodes.addListSelectionListener(
                    e -> {
                        if (!e.getValueIsAdjusting()) {
                            HistoryReference historyReference = childSiteNodes.getSelectedValue();
                            if (historyReference != null) {
                                try {
                                    setNote(historyReference.getHttpMessage().getNote());
                                } catch (HttpMalformedHeaderException | DatabaseException ex) {
                                    LOGGER.error(ex.getMessage(), ex);
                                }
                            }
                        }
                    });
        }

        return childSiteNodes;
    }

    private void updateHrefSelector() {
        if (siteNode == null) {
            return;
        }

        Set<HistoryReference> historyReferencesSet =
                new TreeSet<>(Comparator.comparingInt(HistoryReference::getHistoryId));
        fillJSet(historyReferencesSet, siteNode);

        DefaultListModel<HistoryReference> listModel = new DefaultListModel<>();
        listModel.addAll(historyReferencesSet);

        childSiteNodes.setModel(listModel);
        if (!historyReferencesSet.isEmpty()) {
            childSiteNodes.setSelectedIndex(0);
        }
    }

    private void fillJSet(Set<HistoryReference> set, SiteNode curSiteNode) {
        set.add(curSiteNode.getHistoryReference());
        set.addAll(curSiteNode.getPastHistoryReference());

        for (Object o : Collections.list(curSiteNode.children())) {
            fillJSet(set, (SiteNode) o);
        }
    }

    private ZapTextArea getTxtDisplay() {
        if (txtDisplay == null) {
            txtDisplay = new ZapTextArea("", 15, 25);
        }
        return txtDisplay;
    }

    /**
     * This method initializes btnStart
     *
     * @return javax.swing.JButton
     */
    private JButton getBtnOk() {
        if (btnOk == null) {
            btnOk = new JButton();
            btnOk.setText(Constant.messages.getString("all.button.save"));
            btnOk.addActionListener(
                    e -> {
                        HistoryReference historyRef = childSiteNodes.getSelectedValue();
                        if (historyRef != null) {
                            historyRef.setNote(getTxtDisplay().getText());
                        }
                        clearAndDispose();
                    });
        }
        return btnOk;
    }

    private void clearAndDispose() {
        setNote("");
        siteNode = null;
        dispose();
    }

    /**
     * This method initializes btnStop
     *
     * @return javax.swing.JButton
     */
    private JButton getBtnCancel() {
        if (btnCancel == null) {
            btnCancel = new JButton();
            btnCancel.setText(Constant.messages.getString("all.button.cancel"));
            btnCancel.addActionListener(e -> clearAndDispose());
        }
        return btnCancel;
    }

    /**
     * @param plugin unused.
     * @deprecated (2.7.0) No longer used/needed.
     */
    @Deprecated
    public void setPlugin(ExtensionHistory plugin) {}

    /**
     * This method initializes jScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setVerticalScrollBarPolicy(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setViewportView(getTxtDisplay());
        }
        return jScrollPane;
    }

    public SiteNode getSiteNode() {
        return siteNode;
    }

    public void setSiteNode(SiteNode siteNode) {
        this.siteNode = siteNode;
        updateHrefSelector();
    }

    public void setNote(String note) {
        getTxtDisplay().setText(note);
        getTxtDisplay().discardAllEdits();
    }

    @SuppressWarnings("serial")
    public static class HistoryReferenceRenderer extends JLabel
            implements ListCellRenderer<HistoryReference> {

        public HistoryReferenceRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(5, 10, 0, 10));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends HistoryReference> list,
                HistoryReference historyReference,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            setText(Integer.toString(historyReference.getHistoryId()));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
