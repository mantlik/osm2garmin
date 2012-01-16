/*
 * #%L
 * Osm2garminAPI
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
package org.mantlik.osm2garmin;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Properties;
import jbittorrentapi.*;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class TorrentDownloader extends ThreadProcessor {

    private TorrentFile torrent;
    private File torrentFile;
    private File saveDir;
    private DownloadManager dm;
    private boolean stop = false;
    private boolean paused = false;
    private int startPiece = 0;
    private int noOfPieces;
    private DataVerifier verifier;
    private static ArrayList<DownloadManager> downloads = new ArrayList<DownloadManager>();

    /**
     *
     * @param parameters
     * @param torrentFile
     * @param saveDir
     */
    public TorrentDownloader(Properties parameters, File torrentFile, File saveDir) {
        super(parameters);
        this.torrentFile = torrentFile;
        this.saveDir = saveDir;
    }

    /**
     *
     * @param parameters
     * @param torrent
     * @param saveDir
     */
    public TorrentDownloader(Properties parameters, TorrentFile torrent, File saveDir) {
        super(parameters);
        this.torrent = torrent;
        this.saveDir = saveDir;
    }

    public TorrentDownloader(Properties parameters, TorrentFile torrent, File saveDir,
            int startPiece, int noOfPieces, DataVerifier verifier) {
        this(parameters, torrent, saveDir);
        this.startPiece = startPiece;
        this.noOfPieces = noOfPieces;
        this.verifier = verifier;
    }

    /**
     *
     * @return
     */
    @Override
    public float getProgress() {
        if (dm == null) {
            return 0;
        }
        if (dm.isComplete()) {
            return 100;
        }
        return dm.getCompleted();
    }

    /**
     *
     * @return
     */
    @Override
    public int getState() {
        if (dm == null) {
            return super.getState();
        }
        if (dm.isComplete()) {
            dm.closeTempFiles();
            return COMPLETED;
        }
        return RUNNING;
    }

    /**
     *
     * @return
     */
    @Override
    public String getStatus() {
        if (dm == null) {
            return "Initializing downloader.";
        }
        if (dm.isComplete()) {
            return "Torrent download completed. Seeding started.";
        }
        if (dm.init_progress() > -1) {
            if (torrent.name.size() > 1) {
                return "Checking files "
                        + " (" + dm.init_progress() + " %)";
            } else {
                return "Checking file " + torrent.saveAs
                        + " (" + dm.init_progress() + " %)";
            }
        }
        DecimalFormat df = new DecimalFormat("0.00");
        long dl = dm.downloaded() / 1024 / 1024;
        long ul = dm.uploaded() / 1024 / 1024;
        String webseed = WebseedTask.webseedActive ? "*" : "";
        return "Download " + torrent.saveAs + " " + df.format(getProgress())
                + " % (" + dm.noOfPeers() + webseed + " peers) D/U: " + dl + "mb("
                + df.format(dm.getDLRate()) + ")/" + ul + "mb(" + df.format(dm.getULRate()) + ")";
    }

    @Override
    public void run() {
        TorrentProcessor tp = new TorrentProcessor();
        if (torrent == null) {
            torrent = tp.getTorrentFile(tp.parseTorrent(torrentFile.getPath()));
        }
        if (saveDir != null) {
            String savePath = saveDir.getPath();
            if (!savePath.endsWith("/")) {
                savePath += "/";
            }
            Constants.SAVEPATH = savePath;
        }
        if (verifier != null) {
            dm = new DownloadManager(torrent, Utils.generateID(), startPiece, noOfPieces, verifier);
        } else {
            dm = new DownloadManager(torrent, Utils.generateID());
        }
        downloads.add(dm);
        dm.startListening(Integer.valueOf(parameters.getProperty("torrent_port_start", "6881")),
                Integer.valueOf(parameters.getProperty("torrent_port_end", "6889")));
        dm.startTrackerUpdate();
        while (!stop) {
            try {
                Thread.sleep(30000);
                if (!paused) {
                    dm.unchokePeers();
                }
            } catch (InterruptedException ex) {
                break;
            }
        }
        dm.stopTrackerUpdate();
        dm.closeTempFiles();
    }

    /**
     *
     */
    public void stop() {
        stop = true;
        downloads.remove(dm);
    }

    /**
     *
     */
    public void pause() {
        paused = true;
        dm.stopTrackerUpdate();
        dm.closeTempFiles();
    }

    /**
     *
     */
    public void resume() {
        dm.checkTempFiles();
        dm.startTrackerUpdate();
    }

    public static long downloaded() {
        long downloaded = 0;
        for (DownloadManager dm : downloads) {
            downloaded += dm.downloaded();
        }
        return downloaded;
    }

    public static long uploaded() {
        long uploaded = 0;
        for (DownloadManager dm : downloads) {
            uploaded += dm.uploaded();
        }
        return uploaded;
    }

    public int getNoOfPieces() {
        if (dm == null) {
            return 0;
        }
        return dm.noOfPieces();
    }

    public boolean isPieceComplete(int piece) {
        if (dm == null) {
            return false;
        }
        return dm.isPieceComplete(piece + dm.startPiece());
    }

    public static DownloadManager[] getDownloads() {
        return downloads.toArray(new DownloadManager[0]);
    }
}
