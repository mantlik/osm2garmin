/*
 * #%L
 * Osm2garminGUI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.mantlik.osm2garminspi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import jbittorrentapi.DownloadManager;
import jbittorrentapi.WebseedTask;
import org.mantlik.osm2garmin.*;
import org.netbeans.api.javahelp.Help;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.util.*;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.mantlik.osm2garmin.gui//MainWindow//EN",
autostore = false)
@TopComponent.Description(preferredID = "MainWindowTopComponent",
iconBase = "org/mantlik/osm2garmin/gui/Globe16.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "org.mantlik.osm2garmin.gui.MainWindowTopComponent")
@ActionReference(path = "Menu/Window" /*
 * , position = 333
 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_MainWindowAction",
preferredID = "MainWindowTopComponent")
@Messages({
    "CTL_MainWindowAction=MainWindow",
    "CTL_MainWindowTopComponent=MainWindow",
    "HINT_MainWindowTopComponent="
})
public final class MainWindowTopComponent extends TopComponent implements PropertyChangeListener, Runnable {

    private final DecimalFormat df1 = new DecimalFormat("0.0");
    private String userdir = System.getProperty("netbeans.user") + "/";
    Osm2garmin osm2Garmin;
    RequestProcessor processor = new RequestProcessor("Osm2garmin", 1, true);
    RequestProcessor changeProcessor = new RequestProcessor("MainWindowChange");
    InputOutput io;
    long outputClearTime = 0;
    private static final Color[] BGCOLORS = new Color[]{ // none, running, completed, error 
        new Color(200, 200, 200), new Color(51, 153, 255), new Color(127, 255, 127),
        new Color(255, 127, 127)
    };
    private float[] ulsp = new float[100], dlsp = new float[100];
    private boolean stopped = false;

    public MainWindowTopComponent() {
        initComponents();
        setName(Bundle.CTL_MainWindowTopComponent());
        setToolTipText(Bundle.HINT_MainWindowTopComponent());
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);

        torrentStatusItem.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    torrentStatusEventDesc = e.getDescription();
                    startTorrentStatus = torrentStatusItem.getText();
                    updateTorrentStatus("Please wait...");
                    RequestProcessor.getDefault().post(new Runnable() {

                        @Override
                        public void run() {
                            String s = torrentStatusEventDesc;
                            if (s.equals("pause")) {
                                pauseDownloads(true);
                            } else if (s.equals("resume")) {
                                pauseDownloads(false);
                            } else if (s.equals("memoryHelp")) {
                                Help help = Lookup.getDefault().lookup(Help.class);
                                String id = "more_memory";
                                if (help != null && help.isValidID(id, true)) {
                                    help.showHelp(new HelpCtx(id));
                                    torrentStatusItem.setText(startTorrentStatus);
                                }
                            }
                        }
                    });
                }
            }
        });

        long maxMemory = Runtime.getRuntime().maxMemory();
        int mm = (int) (maxMemory / 1024 / 1024);
        if (maxMemory <= 1800000000l) {
            torrentStatusItem.setText("<html><font color=\"red\">Only " + mm
                    + " mb of memory available.</font> Address indexes will NOT be generated - at least 1800 mb would be needed. "
                    + "<a href=\"memoryHelp\">Help</a>");
        }
    }
    private String torrentStatusEventDesc;
    String startTorrentStatus = "<html>";

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("getting-started");
    }

    @Override
    public boolean canClose() {
        if (!startButton.isEnabled()) {
            if (DialogDisplayer.getDefault().notify(new NotifyDescriptor.Confirmation(
                    "Maps processing is running. Do you really want to cancel it?",
                    NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE)).equals(NotifyDescriptor.YES_OPTION)) {
                cancelButtonActionPerformed(null);
                return super.canClose();
            } else {
                return false;
            }
        }
        return super.canClose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel7 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        overallProgress = new javax.swing.JProgressBar();
        startButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        planetUpdateDownloadStatus = new JTextFieldImage();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        planetUpdateStatus = new JTextFieldImage();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        planetDownloadStatus = new JTextFieldImage();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        torrentStatusItem = new javax.swing.JTextPane();
        jPanel9 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        regionsStatus = new JTextFieldImage();
        jLabel4 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        regionLabel = new javax.swing.JLabel();
        contoursStatus = new JTextFieldImage();
        jLabel6 = new javax.swing.JLabel();

        setDisplayName(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.displayName")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel5.text")); // NOI18N

        overallProgress.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.overallProgress.toolTipText")); // NOI18N
        overallProgress.setStringPainted(true);

        org.openide.awt.Mnemonics.setLocalizedText(startButton, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.startButton.text")); // NOI18N
        startButton.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.startButton.toolTipText")); // NOI18N
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cancelButton, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.cancelButton.text")); // NOI18N
        cancelButton.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.cancelButton.toolTipText")); // NOI18N
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel6.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jPanel8.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(51, 153, 255))); // NOI18N

        planetUpdateDownloadStatus.setEditable(false);
        planetUpdateDownloadStatus.setText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetUpdateDownloadStatus.text")); // NOI18N
        planetUpdateDownloadStatus.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetUpdateDownloadStatus.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel2.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(planetUpdateDownloadStatus)
                .addGap(14, 14, 14))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(planetUpdateDownloadStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(0, 10, Short.MAX_VALUE))
        );

        planetUpdateStatus.setEditable(false);
        planetUpdateStatus.setText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetUpdateStatus.text")); // NOI18N
        planetUpdateStatus.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetUpdateStatus.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(planetUpdateStatus)
                .addGap(14, 14, 14))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(planetUpdateStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(0, 11, Short.MAX_VALUE))
        );

        planetDownloadStatus.setEditable(false);
        planetDownloadStatus.setText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetDownloadStatus.text")); // NOI18N
        planetDownloadStatus.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.planetDownloadStatus.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel1.text")); // NOI18N

        jScrollPane1.setBorder(null);

        torrentStatusItem.setBorder(null);
        torrentStatusItem.setContentType(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.torrentStatusItem.contentType_1")); // NOI18N
        torrentStatusItem.setEditable(false);
        torrentStatusItem.setText("<html>\r\n  <head>\r\n\r  <style>body {text-align: right}</style>\n  </head>\r\n  <body>\r\nNo active downloads.\n  </body>\r\n</html>\r\n"); // NOI18N
        torrentStatusItem.setMargin(new java.awt.Insets(0, 0, 0, 0));
        torrentStatusItem.setMinimumSize(new java.awt.Dimension(10, 10));
        torrentStatusItem.setName(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.torrentStatusItem.name")); // NOI18N
        torrentStatusItem.setOpaque(false);
        jScrollPane1.setViewportView(torrentStatusItem);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(planetDownloadStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(planetDownloadStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68))
        );

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jPanel9.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(51, 153, 255))); // NOI18N

        regionsStatus.setEditable(false);
        regionsStatus.setText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.regionsStatus.text")); // NOI18N
        regionsStatus.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.regionsStatus.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel4.text")); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(regionsStatus)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(regionsStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(0, 10, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(regionLabel, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.regionLabel.text")); // NOI18N

        contoursStatus.setEditable(false);
        contoursStatus.setText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.contoursStatus.text")); // NOI18N
        contoursStatus.setToolTipText(org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.contoursStatus.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(MainWindowTopComponent.class, "MainWindowTopComponent.jLabel6.text")); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(contoursStatus))
                    .addComponent(regionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(regionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(contoursStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(overallProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel7Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, startButton});

        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelButton)
                        .addComponent(startButton))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(overallProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (stopped) {  // exit the application
            LifecycleManager.getDefault().exit();
        } else {
            cancelButton.setEnabled(true);
            startButton.setEnabled(false);
            Installer.setCanClose(false);
            osm2Garmin = new Osm2garmin();
            osm2Garmin.changeSupport.addPropertyChangeListener(WeakListeners.propertyChange(this, this));
            clearOutput();
            clearStatus(planetDownloadStatus);
            clearStatus(planetUpdateDownloadStatus);
            clearStatus(planetUpdateStatus);
            clearStatus(regionsStatus);
            clearStatus(contoursStatus);
            overallProgress.setValue(0);
            regionLabel.setText("");
            redirectSystemStreams();
            processor.post(this);
        }

    }//GEN-LAST:event_startButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        if (stopped) {
            LifecycleManager.getDefault().markForRestart();
            LifecycleManager.getDefault().exit();
        } else {
            cancelButton.setEnabled(false);
            osm2Garmin.stop();
        }
        /*
         * processor.shutdownNow(); saveParameters(osm2Garmin.parameters);
         * startButton.setEnabled(true); Installer.setCanClose(true);
         * stopSystemStreamsRedirect();
         */

    }//GEN-LAST:event_cancelButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField contoursStatus;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JProgressBar overallProgress;
    private javax.swing.JTextField planetDownloadStatus;
    private javax.swing.JTextField planetUpdateDownloadStatus;
    private javax.swing.JTextField planetUpdateStatus;
    private javax.swing.JLabel regionLabel;
    private javax.swing.JTextField regionsStatus;
    private javax.swing.JButton startButton;
    private javax.swing.JTextPane torrentStatusItem;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    private ArrayList<PropertyChangeEvent> eventQueue = new ArrayList<PropertyChangeEvent>();

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        eventQueue.add(evt);
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

            @Override
            public void run() {
                while (!eventQueue.isEmpty()) {
                    PropertyChangeEvent evt = eventQueue.remove(0);
                    if (evt == null) {
                        continue;
                    }
                    if (evt.getSource().getClass().equals(PlanetDownloader.class)) {
                        PlanetDownloader downloader = (PlanetDownloader) evt.getSource();
                        planetDownloadStatus.setText(downloader.getStatus());
                        int width = planetDownloadStatus.getWidth();
                        int height = planetDownloadStatus.getHeight();
                        Image image = progressMap(width, height, downloader);
                        TorrentDownloader torrent = downloader.torrentDownloader;
                        if (torrent != null && torrent.getState() == TorrentDownloader.RUNNING) {
                            image = torrentMap(width, height, torrent);
                        }
                        ((JTextFieldImage) planetDownloadStatus).setImage(image);
                    } else if (evt.getSource().getClass().equals(PlanetUpdateDownloader.class)) {
                        PlanetUpdateDownloader downloader = (PlanetUpdateDownloader) evt.getSource();
                        planetUpdateDownloadStatus.setText(downloader.getStatus());
                        int width = planetUpdateDownloadStatus.getWidth();
                        int height = planetUpdateDownloadStatus.getHeight();
                        TorrentDownloader torrent = downloader.torrentDownloader;
                        Image image = progressMap(width, height, downloader);
                        if (torrent != null && torrent.getState() == TorrentDownloader.RUNNING) {
                            image = torrentMap(width, height, torrent);
                        }
                        ((JTextFieldImage) planetUpdateDownloadStatus).setImage(image);
                    } else if (evt.getSource().getClass().equals(PlanetUpdater.class)) {
                        PlanetUpdater updater = (PlanetUpdater) evt.getSource();
                        planetUpdateStatus.setText(updater.getStatus());
                        int width = planetUpdateStatus.getWidth();
                        int height = planetUpdateStatus.getHeight();
                        Image image = progressMap(width, height, updater);
                        ((JTextFieldImage) planetUpdateStatus).setImage(image);
                    } else if (evt.getSource().getClass().equals(ContoursUpdater.class)) {
                        ContoursUpdater updater = (ContoursUpdater) evt.getSource();
                        contoursStatus.setText(updater.getStatus());
                        int width = contoursStatus.getWidth();
                        int height = contoursStatus.getHeight();
                        Image image = progressMap(width, height, updater);
                        ((JTextFieldImage) contoursStatus).setImage(image);
                    } else if (evt.getSource().getClass().equals(OsmMaker.class)) {
                        OsmMaker updater = (OsmMaker) evt.getSource();
                        regionsStatus.setText(updater.getStatus());
                        int width = regionsStatus.getWidth();
                        int height = regionsStatus.getHeight();
                        Image image = progressMap(width, height, updater);
                        ((JTextFieldImage) regionsStatus).setImage(image);
                    } else if (evt.getSource().getClass().equals(TorrentDownloader.class)) {
                        if (evt.getPropertyName().equals("progress")) {
                            String text;
                            if (DownloadManager.getNoOfDownloads() > 0) {
                                long downloaded = DownloadManager.getTotalDownloaded();
                                long uploaded = DownloadManager.getTotalUploaded();
                                float downSpeed = DownloadManager.getDownSpeed();
                                float upSpeed = DownloadManager.getUpSpeed();
                                int peers = DownloadManager.getTotalPeers();
                                String action = "<a href=\"pause\">Pause</a>";
                                if (DownloadManager.getNoOfRunningDownloads() == 0) {
                                    action = "<a href=\"resume\">Resume</a>";
                                }
                                if (DownloadManager.initiating()) {
                                    action = "";
                                }
                                String webseed = WebseedTask.webseedActive ? "+webseed" : "";
                                text = peers + webseed + " peers, downloaded " + downloaded / 1024 / 1024
                                        + " mb / uploaded " + uploaded / 1024 / 1024 + " mb, D/U rate "
                                        + df1.format(downSpeed) + " / " + df1.format(upSpeed)
                                        + " kb/s " + action;
                            } else {
                                text = "No active downloads.";
                            }
                            updateTorrentStatus(text);
                        }
                    }
                    if (evt.getPropertyName().equals("state")) {
                        if ((Integer) evt.getNewValue() == ThreadProcessor.COMPLETED) {
                            clearOutput();
                        }
                    }
                    String inProgressText = "";
                    int regionsReady = 0;
                    int regionsInError = 0;
                    int inProgress = 0;
                    if (evt.getPropertyName().equals("progress")) { // count overall progress
                        float progress = 0;
                        if (osm2Garmin.planetDownloader != null) {
                            progress += osm2Garmin.planetDownloader.getProgress() / 4;
                        }
                        if (osm2Garmin.planetUpdateDownloader != null) {
                            progress += osm2Garmin.planetUpdateDownloader.getProgress() / 8;
                        }
                        if (osm2Garmin.planetUpdater != null) {
                            progress += osm2Garmin.planetUpdater.getProgress() / 8;
                        }
                        for (int regno = 0; regno < osm2Garmin.regions.size(); regno++) {
                            Region region = osm2Garmin.regions.get(regno);
                            if (region != null) {
                                if (region.getState() >= Region.READY) {
                                    progress += 50 / osm2Garmin.regions.size();
                                    regionsReady++;
                                    if (region.getState() == Region.ERROR) {
                                        regionsInError++;
                                    }
                                } else if (region.getState() == Region.MAKING_OSM) {
                                    progress += (25 + 0.25 * region.processor.getProgress()) / osm2Garmin.regions.size();
                                    inProgressText += " Processing map for region " + region.name
                                            + " (" + (regno + 1) + "/" + osm2Garmin.regions.size() + "). ";
                                    inProgress += 1;
                                    regionsStatus.setBackground(null); // clear bgcolor
                                } else if (region.getState() == Region.MAKING_CONTOURS) {
                                    progress += 0.25 * region.processor.getProgress() / osm2Garmin.regions.size();
                                    inProgressText += " Creating contours for region: " + region.name
                                            + " (" + (regno + 1) + "/" + osm2Garmin.regions.size() + "). ";
                                    inProgress += 1;
                                    contoursStatus.setBackground(null); // clear bgcolor
                                }
                            }
                        }
                        //inProgressText += " (" + (regionsReady + inProgress) + "/" + osm2Garmin.regions.size() + ")";
                        if (regionsInError == 1) {
                            inProgressText += " - " + regionsInError + " region with error.";
                        } else if (regionsInError > 1) {
                            inProgressText += " - " + regionsInError + " regions with error.";
                        }
                        overallProgress.setValue((int) (progress + 0));
                        regionLabel.setText(inProgressText);
                        if (regionsInError > 0) {
                            regionLabel.setForeground(Color.red);
                        } else {
                            regionLabel.setForeground(Color.black);
                        }
                    }
                    validate();
                }
            }
        });
    }
    private static Boolean updatingTorrentStatus = false;

    private synchronized void updateTorrentStatus(String text) {
        if (updatingTorrentStatus) {
            try {
                this.wait();
            } catch (InterruptedException ex) {
            }
        }
        updatingTorrentStatus = true;
        torrentStatusItem.setText("<html><head><style>body {text-align: right}</style></head><body>" + text + "</body></html>");
        updatingTorrentStatus = false;
        this.notify();
    }

    private void pauseDownloads(boolean pause) {
        DownloadManager.pauseAllDownloads(pause);
    }

    private Properties getParameters(Properties par) {
        userdir = NbPreferences.forModule(Osm2garmin.class).get("userdir", userdir);
        par.setProperty("regions", NbPreferences.forModule(Osm2garmin.class).get("regions", userdir + "regions.txt"));
        par.setProperty("maps_dir", NbPreferences.forModule(Osm2garmin.class).get("maps_dir", userdir + "maps/"));
        par.setProperty("delete_old_maps", NbPreferences.forModule(Osm2garmin.class).get("delete_old_maps", "true"));
        par.setProperty("contours_dir", NbPreferences.forModule(Osm2garmin.class).get("contours_dir", userdir + "contours/"));
        par.setProperty("planet_file", NbPreferences.forModule(Osm2garmin.class).get("planet_file", userdir + "planet.osm.pbf"));
        par.setProperty("planet_backup", NbPreferences.forModule(Osm2garmin.class).get("planet_backup", userdir + "planet_backup.osm.pbf"));
        par.setProperty("old_planet_file", NbPreferences.forModule(Osm2garmin.class).get("old_planet_file", userdir + "planet_old.osm.pbf"));
        par.setProperty("planet_minimum_age", NbPreferences.forModule(Osm2garmin.class).get("planet_minimum_age", "8"));
        par.setProperty("planet_file_download_urls", NbPreferences.forModule(Osm2garmin.class).get("planet_file_download_urls",
                "http://ftp.heanet.ie/mirrors/openstreetmap.org/pbf,"
                + "http://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org//pbf"));
        par.setProperty("planet_file_update_urls", NbPreferences.forModule(Osm2garmin.class).get("planet_file_update_urls",
                "http://ftp.heanet.ie/mirrors/openstreetmap.org,"
                + "http://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/,"
                + "http://planet.openstreetmap.org/"));
        par.setProperty("osmosiswork", NbPreferences.forModule(Osm2garmin.class).get("osmosiswork", userdir + "osmosiswork/"));
        par.setProperty("srtm_dir", NbPreferences.forModule(Osm2garmin.class).get("srtm_dir", userdir + "SRTM/"));
        par.setProperty("srtm_url", NbPreferences.forModule(Osm2garmin.class).get("srtm_url",
                "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/"));
        par.setProperty("srtm_offs_lat", NbPreferences.forModule(Osm2garmin.class).get("srtm_offs_lat", "0.000"));
        par.setProperty("srtm_offs_lon", NbPreferences.forModule(Osm2garmin.class).get("srtm_offs_lon", "0.000"));
        par.setProperty("contour_start_id", NbPreferences.forModule(Osm2garmin.class).get("contour_start_id", "2000000000"));
        par.setProperty("contour_start_way_id", NbPreferences.forModule(Osm2garmin.class).get("contour_start_way_id", "1000000000"));
        par.setProperty("contour_minor_interval", NbPreferences.forModule(Osm2garmin.class).get("contour_minor_interval", "25"));
        par.setProperty("contour_medium_interval", NbPreferences.forModule(Osm2garmin.class).get("contour_medium_interval", "50"));
        par.setProperty("contour_major_interval", NbPreferences.forModule(Osm2garmin.class).get("contour_major_interval", "100"));
        par.setProperty("plot_minor_threshold", NbPreferences.forModule(Osm2garmin.class).get("plot_minor_threshold", "500"));
        par.setProperty("plot_medium_threshold", NbPreferences.forModule(Osm2garmin.class).get("plot_medium_threshold", "2400"));
        par.setProperty("gui", NbPreferences.forModule(Osm2garmin.class).get("verbose", "1"));
        par.setProperty("userdir", userdir);
        par.setProperty("download_method", NbPreferences.forModule(Osm2garmin.class).get("download_method", "torrent"));
        par.setProperty("torrent_download_url", NbPreferences.forModule(Osm2garmin.class).get("torrent_download_url",
                "http://osm-torrent.torres.voyager.hr/files/"));
        par.setProperty("torrent_port_start", NbPreferences.forModule(Osm2garmin.class).get("torrent_port_start", "6881"));
        par.setProperty("torrent_port_end", NbPreferences.forModule(Osm2garmin.class).get("torrent_port_end", "6999"));
        par.setProperty("torrent_download_limit", NbPreferences.forModule(Osm2garmin.class).get("torrent_download_limit", "0.0"));
        par.setProperty("torrent_upload_limit", NbPreferences.forModule(Osm2garmin.class).get("torrent_upload_limit", "0.0"));
        par.setProperty("log_report", NbPreferences.forModule(Osm2garmin.class).get("log_report", "report.log"));
        par.setProperty("cycling_features", NbPreferences.forModule(Osm2garmin.class).get("cycling_features", "false"));
        par.setProperty("srtm_step", NbPreferences.forModule(Osm2garmin.class).get("srtm_step", "5"));
        par.setProperty("contours_density", NbPreferences.forModule(Osm2garmin.class).get("contours_density", "4"));
        par.setProperty("exclusive_utils", NbPreferences.forModule(Osm2garmin.class).get("exclusive_utils", "true"));
        //String settings = par.getProperty("userdir") + "/settings.properties";
        //if (new File(settings).exists()) {
        //    try {
        //        par.load(new FileInputStream(settings));
        //    } catch (IOException ex) {
        //        Exceptions.printStackTrace(ex);
        //    }
        //}
        return par;
    }

    private void saveParameters(Properties parameters) {
        try {
            NbPreferences.forModule(Osm2garmin.class).clear();
            Set<Entry<Object, Object>> set = parameters.entrySet();
            for (Entry<Object, Object> entry : set) {
                NbPreferences.forModule(Osm2garmin.class).put((String) entry.getKey(), (String) entry.getValue());
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void run() {
        io.select();
        osm2Garmin.start(getParameters(new Properties()));
        saveParameters(osm2Garmin.getParameters());
        Installer.setCanClose(true);
        stopSystemStreamsRedirect();
        stopped = true;
        startButton.setText("Exit");
        startButton.setToolTipText("Click to finish the application. Rerun later to update planet file and generate updated maps.");
        cancelButton.setText("Restart");
        cancelButton.setToolTipText("<html>Restart the application now.<br/>Restart is not possible on some systems.<br/>"
                + "If the application will not restart, start it manualy, please.");
        startButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }
    private PrintStream systemOut, systemErr, logreport;

    private void redirectSystemStreams() {
        io = IOProvider.getDefault().getIO("Osm2garmin", false);
        io.select();
        try {
            logreport = new PrintStream(new FileOutputStream(new File(
                    NbPreferences.forModule(Osm2garmin.class).get("userdir", userdir)
                    + NbPreferences.forModule(Osm2garmin.class).get("log_report", "report.log"))));
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        OutputStream out = new OutputStream() {

            @Override
            public void write(int i) throws IOException {
                io.getOut().print(String.valueOf((char) i));
                logreport.print(String.valueOf((char) i));
            }

            @Override
            public void write(byte[] bytes) throws IOException {
                io.getOut().print(new String(bytes));
                logreport.print(new String(bytes));
            }

            @Override
            public void write(byte[] bytes, int off, int len) throws IOException {
                io.getOut().print(new String(bytes, off, len));
                logreport.print(new String(bytes, off, len));
            }
        };
        systemOut = System.out;
        systemErr = System.err;
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void stopSystemStreamsRedirect() {
        System.setOut(systemOut);
        System.setErr(systemErr);
        io.getOut().close();
        logreport.close();
        outputClearTime = 0;
    }

    private void clearStatus(JTextField status) {
        status.setText("");
        status.setBackground(null);
    }

    private void clearOutput() {
        if ((System.currentTimeMillis() - outputClearTime) > 300000) {  // 5 minutes at least
            outputClearTime = System.currentTimeMillis();
            if (io != null) {
                try {
                    io.getOut().reset();
                } catch (IOException ex) {
                    io.getOut().println("Can't clear output.");
                }
            }
        }
    }

    private Image torrentMap(int width, int height, TorrentDownloader downloader) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        Color cc = graphics.getColor();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        int pieces = downloader.getNoOfPieces();
        graphics.setColor(BGCOLORS[downloader.getState()]);
        double factor = 1.0 * width / pieces;
        int d = (int) factor + 1;
        if (d == 0) {
            d = 1;
        }
        int h2 = height / 5;
        int noOfPieces = downloader.getNoOfPieces();
        if (noOfPieces < width * 2) {
            for (int i = 0; i < noOfPieces; i++) {
                if (downloader.isPieceComplete(i)) {
                    int x = (int) (factor * i);
                    graphics.fillRect(x, h2, d, height - h2);
                }
            }
        } else {
            int koef = (int) Math.ceil(1.0 * noOfPieces / width);
            int pieceHeight = (int) ((1.0 * height - height / 5) / koef);
            for (int i = 0; i < width; i++) {
                int piece = (int) (1.0 * i * noOfPieces / width);
                for (int h = height / 5; h < height; h = h + pieceHeight) {
                    if (downloader.isPieceComplete(piece)) {
                        graphics.fillRect(i, h, 1, pieceHeight);
                    }
                    piece++;
                }
            }
        }
        d = (int) (downloader.getProgress() * width / 100);
        graphics.fillRect(0, 0, d, height / 6);
        graphics.setColor(cc);
        return image;
    }

    private Image progressMap(int width, int height, ThreadProcessor tp) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        Color cc = graphics.getColor();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(BGCOLORS[tp.getState()]);
        int d = (int) (tp.getProgress() * width / 100);
        graphics.fillRect(0, 0, d, height);
        graphics.setColor(cc);
        return image;
    }

    private class JTextFieldImage extends JTextField {

        private Image image = null;

        public void setImage(Image img) {
            image = img;
        }

        @Override
        public void paint(Graphics g) {
            if (image != null) {
                setOpaque(false);
                g.drawImage(image, 0, 0, this);
            } else {
                setOpaque(true);
            }
            super.paint(g);
        }
    }
}
