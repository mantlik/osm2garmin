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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jbittorrentapi.DataVerifier;
import jbittorrentapi.TorrentFile;
import jbittorrentapi.Utils;
import org.openide.util.Exceptions;
import org.xml.sax.SAXException;

/**
 *
 * @author fm
 */
public class PlanetUpdateDownloader extends ThreadProcessor {

    private static final DecimalFormat DF = new DecimalFormat("0");
    private static final DecimalFormat D9 = new DecimalFormat("000000000");
    public TorrentDownloader torrentDownloader = null;
    int startPiece = 0;
    int noOfPieces = 0;
    int firstPieceToProcess = 0;

    /**
     *
     * @param parameters
     */
    public PlanetUpdateDownloader(Properties parameters) {
        super(parameters);
    }

    @Override
    public void run() {
        String osmosiswork = parameters.getProperty("osmosiswork");
        File osmosisState = new File(osmosiswork, "state.txt");
        File osmosisStateBackup = new File(osmosiswork, "state_old.txt");

        // recover or make planet file backup
        File planetFile = new File(parameters.getProperty("planet_file"));
        File oldPlanetFile = new File(parameters.getProperty("old_planet_file"));
        if (oldPlanetFile.exists() && (!planetFile.exists() || (planetFile.length() <= oldPlanetFile.length()))) {
            // recover backup
            if (planetFile.exists()) {
                planetFile.delete();
            }
            if (osmosisStateBackup.exists()) {
                if (osmosisState.exists()) {
                    osmosisState.delete();
                }
                try {
                    Osm2garmin.copyFile(osmosisStateBackup, osmosisState);
                } catch (IOException ex) {
                    Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE,
                            "Error recovering Osmosis status backup.", ex);
                    setState(ERROR);
                    return;
                }
            }
        } else {
            // backup planet
            if (oldPlanetFile.exists() && planetFile.exists()) {
                if (parameters.containsKey("planet_backup")) {
                    File bkp = new File(parameters.getProperty("planet_backup"));
                    if (bkp.exists()) {
                        bkp.delete();
                    }
                    oldPlanetFile.renameTo(bkp);
                } else {
                    oldPlanetFile.delete();
                }
            }
            // make backup
            planetFile.renameTo(oldPlanetFile);
            if (osmosisStateBackup.exists()) {
                osmosisStateBackup.delete();
            }
            if (osmosisState.exists()) {
                try {
                    Osm2garmin.copyFile(osmosisState, osmosisStateBackup);
                } catch (IOException ex) {
                    Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE,
                            "Error creating Osmosis status backup.", ex);
                    setState(ERROR);
                    return;
                }
            }
        }

        // Download planet updates
        int i = 0;
        long planet_timestamp = getPlanetTimestamp(osmosisState);
        long startTime = planet_timestamp;

        int minAge = Integer.parseInt(parameters.getProperty("planet_minimum_age"));
        if (parameters.getProperty("download_method", "http").equals("http")) {
            while (System.currentTimeMillis() - planet_timestamp > 1000 * 60 * 60 * minAge) {
                setProgress((float) ((planet_timestamp - startTime) * 100.0 / (System.currentTimeMillis() - startTime)));
                setStatus("Downloading planet updates (" + new SimpleDateFormat("yyyy-MM-dd").format(new Date(planet_timestamp)) + ") - "
                        + DF.format(getProgress()) + " % completed.");
                String[] osargs = new String[]{
                    "--rri", "workingDirectory=" + osmosiswork,
                    "--wxc", "file=" + Osm2garmin.userdir + "update" + i + ".osc.gz"
                };

                try {
                    Osm2garmin.runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                            Osm2garmin.libClassLoader("osmosis", getClass().getClassLoader()), osargs, this);
                } catch (Exception ex) {
                    Logger.getLogger(PlanetUpdateDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    setState(ERROR);
                    return;
                }
                long oldPlanetTimestamp = planet_timestamp;
                planet_timestamp = getPlanetTimestamp(osmosisState);
                if (planet_timestamp > oldPlanetTimestamp) {
                    i++;
                }
            }
        } else {  // pseudo-torrent download
            setStatus("Searching for updates.");
            int doDownload = 0;
            TorrentFile torrent = null;
            while ((noOfPieces < 1) && (doDownload < 3)) {
                torrent = createUpdatesTorrent(osmosisState);
                if (torrent == null) {
                    setStatus("Error creating updates pseudo-torrent.");
                    setState(ERROR);
                    return;
                }
                doDownload++;
            }
            if (noOfPieces < 1) {
                setStatus("Nothing to download.");
                setState(COMPLETED);
                return;
            }
            torrentDownloader =
                    new TorrentDownloader(parameters, torrent, new File(Osm2garmin.userdir),
                    startPiece, noOfPieces, new UpdateFileVerifier(torrent));
            while (torrentDownloader.getState() != TorrentDownloader.COMPLETED) {
                if (torrentDownloader.getState() == Downloader.ERROR) {
                    setState(ERROR);
                    return;
                }
                try {
                    setStatus(torrentDownloader.getStatus());
                    setProgress(torrentDownloader.getProgress());
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    setState(ERROR);
                    return;
                }
            }
            setStatus("Processing updates...");
            int sequence = 0;
            String[] args;
            ArrayList<String> l = new ArrayList<String>();
            int ii = 0;
            int fileno;
            // process downloads with Osmosis - 24 files in a single pass
            for (fileno = firstPieceToProcess; fileno < (startPiece + noOfPieces); fileno++) {
                setProgress((float) (100.0 * (fileno - firstPieceToProcess)
                        / (startPiece + noOfPieces - firstPieceToProcess + 24)));
                ii++;
                l.add("--rxc");
                l.add("file=" + Osm2garmin.userdir + updateName(fileno));
                l.add("--buffer-change");
                l.add("bufferCapacity=10000");
                if (ii == 24) {
                    l.add("--apc");
                    l.add("sourceCount=24");
                    l.add("--sc");
                    l.add("--simc");
                    l.add("--wxc");
                    l.add("file=" + Osm2garmin.userdir + "update" + sequence + ".osc.gz");
                    args = l.toArray(new String[0]);
                    try {
                        Osm2garmin.runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                                Osm2garmin.libClassLoader("osmosis", getClass().getClassLoader()), args, this);
                    } catch (Exception ex) {
                        setStatus(ex.getMessage());
                        setState(ERROR);
                        return;
                    }
                    sequence++;
                    l.clear();
                    ii = 0;
                }
            }
            if (!l.isEmpty()) {
                setProgress((float) (100.0 * (fileno - firstPieceToProcess)
                        / (startPiece + noOfPieces - firstPieceToProcess + 24)));
                l.add("--apc");
                l.add("sourceCount=" + ii);
                l.add("--sc");
                l.add("--simc");
                l.add("--wxc");
                l.add("file=" + Osm2garmin.userdir + "update" + sequence + ".osc.gz");
                args = l.toArray(new String[0]);
                try {
                    Osm2garmin.runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                            Osm2garmin.libClassLoader("osmosis", getClass().getClassLoader()), args, this);
                } catch (Exception ex) {
                    setStatus(ex.getMessage());
                    setState(ERROR);
                    return;
                }
            }
            // download latest osmosiswork/state.txt
            setStatus("Downloading current state.txt");
            String[] mirrors = parameters.getProperty("planet_file_update_urls").split(",");
            boolean ok = false;
            while (!ok) {
                String mirror = mirrors[((int) (Math.random() * 1.0 * mirrors.length))];
                if (!mirror.endsWith("/")) {
                    mirror += "/";
                }
                String stateUrl = (mirror + updateName(startPiece + noOfPieces - 1)).replace(".osc.gz", ".state.txt");
                URL surl;
                try {
                    surl = new URL(stateUrl);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
                    setStatus(ex.getMessage());
                    setState(ERROR);
                    return;
                }
                String state = osmosiswork + "state.txt";
                new File(state).delete();
                Downloader downloader = new Downloader(surl, state);
                while (downloader.getStatus() != Downloader.COMPLETE) {
                    if (downloader.getStatus() == Downloader.ERROR) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        //Logger.getLogger(PlanetUpdateDownloader.class.getName()).log(Level.SEVERE, null, ex);
                        //setState(ERROR);
                        setStatus("Interrupted.");
                        setState(ERROR);
                        return;
                    }
                }
                if (downloader.getStatus() == Downloader.COMPLETE) {
                    ok = true;
                }
            }

        }
        setProgress(100);
        setStatus("Completed.");
        setState(COMPLETED);
    }

    private static long getPlanetTimestamp(File timestampFile) {
        Long time = -1l;
        try {
            Scanner scanner = new Scanner(new FileInputStream(timestampFile));
            while (scanner.hasNext()) {
                String tstamp = scanner.nextLine();
                if (tstamp.startsWith("timestamp=")) {
                    Date tdate = new SimpleDateFormat("yyyy-MM-dd'T'HH\\:mm\\:ss'Z'").parse(tstamp, new ParsePosition(10));
                    time = tdate.getTime();
                }
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return time;
    }

    /*
     * name of the sequence file relative to planet directory
     */
    private String updateName(int fileno) {
        String d = D9.format(fileno);
        return "hour-replicate/" + d.substring(0, 3) + "/" + d.substring(3, 6) + "/" + d.substring(6) + ".osc.gz";
    }

    private int getSequenceNo(File osmosisstate) throws IOException {
        Scanner scanner = new Scanner(osmosisstate);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("sequenceNumber=")) {
                int sequenceNo = Integer.parseInt(line.replace("sequenceNumber=", ""));
                scanner.close();
                return sequenceNo;
            }
        }
        return -1;
    }

    private class UpdateFileVerifier implements DataVerifier {

        public UpdateFileVerifier(TorrentFile torrent) {
        }

        /*
         * Verify gzip file readibility
         */
        @Override
        public boolean verify(int index, byte[] data) {
            InputStream is = null;
            File hashfile = new File(Osm2garmin.userdir + updateName(index) + ".sha1");
            if (hashfile.exists()) {
                int l = (int)hashfile.length();
                byte[] hexhash = new byte[l];
                try {
                    is = new FileInputStream(hashfile);
                    is.read(hexhash);
                    is.close();
                } catch (IOException ex) {
                    hashfile.delete();
                    return false;
                }
                return Utils.byteArrayToByteString(Utils.hash(data)).
                        matches(Utils.byteArrayToByteString(Utils.hexStringToByteArray(new String(hexhash))));
            }
            try {
                is = new GZIPInputStream(new ByteArrayInputStream(data));
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                db.parse(is);
                is.close();
            } catch (SAXException ex) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex1) {
                        return false;
                    }
                }
                return false;
            } catch (ParserConfigurationException ex) {
                setStatus(ex.getMessage());
                setState(ERROR);
                try {
                    is.close();
                } catch (IOException ex1) {
                    return false;
                }
                return false;
            } catch (IOException ex) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex1) {
                        return false;
                    }
                }
                return false;
            }
            try {
                OutputStream os = new FileOutputStream(hashfile);
                os.write(Utils.bytesToHex(Utils.hash(data)).getBytes());
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
    }

    /*
     * Source:
     * http://stackoverflow.com/questions/263013/java-urlconnection-how-could-i-find-out-a-files-size
     */
    private long tryGetFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

    /*
     * Create pseudo-torrent structure for download of updates
     * each hourly update file serves as a (variable-length) piece
     * and downloads latest state.txt to osmosiswork.
     * In addition, sets startPiece and noOfPieces according to the
     * Planet database and local planet state.
     */
    private TorrentFile createUpdatesTorrent(File osmosisstate) {
        TorrentFile torrent = new TorrentFile();
        torrent.announceURL = "http://tracker.ipv6tracker.org:80/announce";
        torrent.comment = "Original planets from http://planet.openstreetmap.org/ "
                + "licensed under CC-BY-SA 2.0 by OpenStreetMap and contributors";
        torrent.createdBy = "Osm2garmin 1.0";
        torrent.creationDate = System.currentTimeMillis() / 1000;
        ArrayList<String> tier = new ArrayList<String>();
        tier.add("http://tracker.ipv6tracker.org:80/announce");
        tier.add("udp://tracker.ipv6tracker.org:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("udp://tracker.publicbt.com:80/announce");
        tier.add("http://tracker.publicbt.com:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("udp://tracker.openbittorrent.com:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("http://open-tracker.appspot.com/announce");
        torrent.announceList.add(tier);
        torrent.changeAnnounce();
        String[] mirrors = parameters.getProperty("planet_file_update_urls").split(",");
        torrent.urlList.addAll(Arrays.asList(mirrors));
        torrent.info_hash_as_binary = Utils.hash("Osm2Garmin planet update pseudo-torrent".getBytes());
        torrent.info_hash_as_hex = Utils.byteArrayToByteString(
                torrent.info_hash_as_binary);
        torrent.info_hash_as_url = Utils.byteArrayToURLString(
                torrent.info_hash_as_binary);
        int sequence = -1;
        try {
            sequence = getSequenceNo(osmosisstate) + 1;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (sequence < 0) {
            return null;
        }
        startPiece = sequence;
        String mirror = mirrors[((int) (Math.random() * 1.0 * mirrors.length))];
        if (!mirror.endsWith("/")) {
            mirror += "/";
        }
        torrent.total_length = 0;
        torrent.setPieceLength(sequence, 0);
        int size = 1;
        firstPieceToProcess = sequence;
        // search for existing older files to seed
        while (size > 0) {
            sequence--;
            String fname = Osm2garmin.userdir + updateName(sequence);
            size = (int) (new File(fname).length());
        }
        sequence++;
        startPiece = sequence;
        while (sequence < firstPieceToProcess) {
            String fname = Osm2garmin.userdir + updateName(sequence);
            String hashname = fname + ".sha1";
            if (!new File(hashname).exists()) {
                setStatus("Checking existing " + updateName(sequence));
                String url = mirror + updateName(sequence);
                try {
                    size = (int) tryGetFileSize(new URL(url));
                } catch (MalformedURLException ex) {
                    startPiece = sequence + 1;
                }
            } else {
                size = (int) (new File(fname).length());
            }
            torrent.setPieceLength(sequence, size);
            byte[] temp = Utils.hash(("Piece " + sequence + " length " + size).getBytes());
            torrent.piece_hash_values_as_binary.put(sequence, temp);
            torrent.piece_hash_values_as_hex.put(sequence, Utils.byteArrayToByteString(
                    temp));
            torrent.piece_hash_values_as_url.put(sequence, Utils.byteArrayToURLString(
                    temp));
            torrent.length.add(((long) size));
            torrent.total_length += size;
            torrent.name.add(updateName(sequence));
            sequence++;
        }
        sequence = firstPieceToProcess;
        size = 1;
        while (size > 0) {
            setStatus("Searching for updates - " + updateName(sequence));
            String url = mirror + updateName(sequence);
            try {
                size = (int) tryGetFileSize(new URL(url));
            } catch (MalformedURLException ex) {
                return null;
            }
            if (size < 0) {
                break;
            }
            torrent.setPieceLength(sequence, size);
            byte[] temp = Utils.hash(("Piece " + sequence + " length " + size).getBytes());
            torrent.piece_hash_values_as_binary.put(sequence, temp);
            torrent.piece_hash_values_as_hex.put(sequence, Utils.byteArrayToByteString(
                    temp));
            torrent.piece_hash_values_as_url.put(sequence, Utils.byteArrayToURLString(
                    temp));
            torrent.length.add(((long) size));
            torrent.total_length += size;
            torrent.name.add(updateName(sequence));
            sequence++;
        }
        noOfPieces = torrent.name.size();

        return torrent;
    }
}
