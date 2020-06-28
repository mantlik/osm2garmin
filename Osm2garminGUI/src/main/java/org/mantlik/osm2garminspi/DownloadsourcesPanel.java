/*
 * #%L
 * Osm2garminSPI
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

import org.mantlik.osm2garmin.Osm2garmin;
import org.openide.util.NbPreferences;

final class DownloadsourcesPanel extends javax.swing.JPanel {

    private final DownloadsourcesOptionsPanelController controller;

    DownloadsourcesPanel(DownloadsourcesOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        torrentMethodItem = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jLabel21 = new javax.swing.JLabel();
        torrentPortStartItem = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        torrentPortEndItem = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        torrentDownloadLimitItem = new javax.swing.JTextField();
        torrentUploadLimitItem = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        planetmirrorsItem = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        torrentDownloadUrlItem = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        planetupdatesItem = new javax.swing.JTextArea();
        srtmdownloadItem = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jPanel2.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 12), new java.awt.Color(51, 153, 255))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel7.text")); // NOI18N

        buttonGroup1.add(torrentMethodItem);
        torrentMethodItem.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(torrentMethodItem, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentMethodItem.text")); // NOI18N
        torrentMethodItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentMethodItemPropertyChange(evt);
            }
        });

        buttonGroup1.add(jRadioButton2);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton2, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jRadioButton2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel21, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel21.text")); // NOI18N

        torrentPortStartItem.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        torrentPortStartItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentPortStartItem.text")); // NOI18N
        torrentPortStartItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentPortStartItemPropertyChange(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel1.text")); // NOI18N

        torrentPortEndItem.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        torrentPortEndItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentPortEndItem.text")); // NOI18N
        torrentPortEndItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentPortEndItemPropertyChange(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel3.text")); // NOI18N

        torrentDownloadLimitItem.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        torrentDownloadLimitItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentDownloadLimitItem.text")); // NOI18N
        torrentDownloadLimitItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentDownloadLimitItem.toolTipText")); // NOI18N
        torrentDownloadLimitItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentDownloadLimitItemPropertyChange(evt);
            }
        });

        torrentUploadLimitItem.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        torrentUploadLimitItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentUploadLimitItem.text")); // NOI18N
        torrentUploadLimitItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentUploadLimitItem.toolTipText")); // NOI18N
        torrentUploadLimitItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentUploadLimitItemPropertyChange(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel9.text")); // NOI18N

        planetmirrorsItem.setColumns(20);
        planetmirrorsItem.setRows(3);
        planetmirrorsItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.planetmirrorsItem.toolTipText")); // NOI18N
        planetmirrorsItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                planetmirrorsItemPropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(planetmirrorsItem);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel6.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel5.text")); // NOI18N

        torrentDownloadUrlItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentDownloadUrlItem.text")); // NOI18N
        torrentDownloadUrlItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.torrentDownloadUrlItem.toolTipText")); // NOI18N
        torrentDownloadUrlItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                torrentDownloadUrlItemPropertyChange(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel8.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel4.text")); // NOI18N

        planetupdatesItem.setColumns(20);
        planetupdatesItem.setRows(3);
        planetupdatesItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.planetupdatesItem.toolTipText")); // NOI18N
        planetupdatesItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                planetupdatesItemPropertyChange(evt);
            }
        });
        jScrollPane2.setViewportView(planetupdatesItem);

        srtmdownloadItem.setText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.srtmdownloadItem.text")); // NOI18N
        srtmdownloadItem.setToolTipText(org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.srtmdownloadItem.toolTipText")); // NOI18N
        srtmdownloadItem.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                srtmdownloadItemPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(srtmdownloadItem, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(torrentDownloadUrlItem, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(srtmdownloadItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(torrentDownloadUrlItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap())
        );

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel10.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(DownloadsourcesPanel.class, "DownloadsourcesPanel.jLabel11.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addComponent(jLabel2))
                        .addGap(103, 103, 103)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(torrentPortStartItem, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(torrentPortEndItem, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(torrentMethodItem)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jRadioButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(torrentDownloadLimitItem, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addGap(19, 19, 19)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(torrentUploadLimitItem, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {torrentPortEndItem, torrentPortStartItem});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(3, 3, 3)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(torrentMethodItem)
                        .addComponent(jLabel21))
                    .addComponent(jRadioButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(torrentPortStartItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(torrentPortEndItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(torrentDownloadLimitItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(torrentUploadLimitItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 566, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 337, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void srtmdownloadItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_srtmdownloadItemPropertyChange
        if (!srtmdownloadItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("srtm_url",
                "https://dds.cr.usgs.gov/srtm/version2_1/SRTM3/"))) {
            controller.changed();
        }
    }//GEN-LAST:event_srtmdownloadItemPropertyChange

    private void planetmirrorsItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_planetmirrorsItemPropertyChange
        if (!planetmirrorsItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("planet_file_download_urls",
                "https://ftp.heanet.ie/mirrors/openstreetmap.org/pbf,"
                + "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org//pbf,"
                + "https://planet.openstreetmap.org/").
                replace(",", "\n"))) {
            controller.changed();
        }
    }//GEN-LAST:event_planetmirrorsItemPropertyChange

    private void planetupdatesItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_planetupdatesItemPropertyChange
        if (!planetupdatesItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("planet_file_update_urls",
                "https://ftp.heanet.ie/mirrors/openstreetmap.org,"
                + "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/,"
                + "https://planet.openstreetmap.org/").replace(",", "\n"))) {
            controller.changed();
        }
    }//GEN-LAST:event_planetupdatesItemPropertyChange

    private void torrentDownloadUrlItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentDownloadUrlItemPropertyChange
        if (!torrentDownloadUrlItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("torrent_download_url",
                "http://www.mantlik.cz/tracker/torrents/"))) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentDownloadUrlItemPropertyChange

    private void torrentMethodItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentMethodItemPropertyChange
        String oldMethod = NbPreferences.forModule(Osm2garmin.class).get("download_method",
                "torrent");
        String newMethod = torrentMethodItem.isSelected() ? "torrent" : "http";
        if (!newMethod.equals(oldMethod)) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentMethodItemPropertyChange

    private void torrentPortStartItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentPortStartItemPropertyChange
        if (!torrentPortStartItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("torrent_port_start",
                "6881"))) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentPortStartItemPropertyChange

    private void torrentPortEndItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentPortEndItemPropertyChange
        if (!torrentPortEndItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("torrent_port_end",
                "6999"))) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentPortEndItemPropertyChange

    private void torrentDownloadLimitItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentDownloadLimitItemPropertyChange
        if (!torrentDownloadLimitItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("torrent_download_limit",
                "0.0"))) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentDownloadLimitItemPropertyChange

    private void torrentUploadLimitItemPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_torrentUploadLimitItemPropertyChange
        if (!torrentUploadLimitItem.getText().equals(NbPreferences.forModule(Osm2garmin.class).get("torrent_upload_limit",
                "0.0"))) {
            controller.changed();
        }
    }//GEN-LAST:event_torrentUploadLimitItemPropertyChange

    void load() {
        srtmdownloadItem.setText(NbPreferences.forModule(Osm2garmin.class).get("srtm_url",
                "https://dds.cr.usgs.gov/srtm/version2_1/SRTM3/"));
        planetmirrorsItem.setText(NbPreferences.forModule(Osm2garmin.class).get("planet_file_download_urls",
                "https://ftp.heanet.ie/mirrors/openstreetmap.org/pbf,"
                + "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org//pbf,"
                + "https://planet.openstreetmap.org/").
                replace(",", "\n"));
        planetupdatesItem.setText(NbPreferences.forModule(Osm2garmin.class).get("planet_file_update_urls",
                "https://ftp.heanet.ie/mirrors/openstreetmap.org,"
                + "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/,"
                + "https://planet.openstreetmap.org/").replace(",", "\n"));
        torrentDownloadUrlItem.setText(NbPreferences.forModule(Osm2garmin.class).get("torrent_download_url",
                "http://www.mantlik.cz/tracker/torrents/"));
        torrentMethodItem.setSelected(NbPreferences.forModule(Osm2garmin.class)
                .get("download_method", "torrent").equals("torrent"));
        torrentPortStartItem.setText(NbPreferences.forModule(Osm2garmin.class).get("torrent_port_start",
                "6881"));
        torrentPortEndItem.setText(NbPreferences.forModule(Osm2garmin.class).get("torrent_port_end",
                "6999"));
        torrentDownloadLimitItem.setText(NbPreferences.forModule(Osm2garmin.class).get("torrent_download_limit",
                "0.0"));
        torrentUploadLimitItem.setText(NbPreferences.forModule(Osm2garmin.class).get("torrent_upload_limit",
                "0.0"));
    }

    void store() {
        NbPreferences.forModule(Osm2garmin.class).put("srtm_url", srtmdownloadItem.getText());
        NbPreferences.forModule(Osm2garmin.class).put("planet_file_download_urls", planetmirrorsItem.getText().
                replace("\n", ",").replaceAll("\\s", "").replace(",,", ",").replaceFirst(",$", ""));
        NbPreferences.forModule(Osm2garmin.class).put("planet_file_update_urls", planetupdatesItem.getText().
                replace("\n", ",").replaceAll("\\s", "").replace(",,", ",").replaceFirst(",$", ""));
        NbPreferences.forModule(Osm2garmin.class).put("torrent_download_url", torrentDownloadUrlItem.getText());
        NbPreferences.forModule(Osm2garmin.class).put("download_method",
                torrentMethodItem.isSelected() ? "torrent" : "http");
        NbPreferences.forModule(Osm2garmin.class).put("torrent_port_start", torrentPortStartItem.getText());
        NbPreferences.forModule(Osm2garmin.class).put("torrent_port_end", torrentPortEndItem.getText());
        NbPreferences.forModule(Osm2garmin.class).put("torrent_download_limit", torrentDownloadLimitItem.getText());
        NbPreferences.forModule(Osm2garmin.class).put("torrent_upload_limit", torrentUploadLimitItem.getText());
    }

    boolean valid() {
        int startPort = Integer.parseInt(torrentPortStartItem.getText());
        int endPort = Integer.parseInt(torrentPortEndItem.getText());
        if (endPort < startPort) {
            torrentPortEndItem.setText(torrentPortStartItem.getText());
        }
        return true;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea planetmirrorsItem;
    private javax.swing.JTextArea planetupdatesItem;
    private javax.swing.JTextField srtmdownloadItem;
    private javax.swing.JTextField torrentDownloadLimitItem;
    private javax.swing.JTextField torrentDownloadUrlItem;
    private javax.swing.JRadioButton torrentMethodItem;
    private javax.swing.JTextField torrentPortEndItem;
    private javax.swing.JTextField torrentPortStartItem;
    private javax.swing.JTextField torrentUploadLimitItem;
    // End of variables declaration//GEN-END:variables
}
