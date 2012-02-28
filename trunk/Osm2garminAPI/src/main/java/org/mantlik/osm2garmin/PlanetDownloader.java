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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fm
 */
public class PlanetDownloader extends ThreadProcessor {

    private SimpleDateFormat sdf;
    private DecimalFormat df;
    private File planetFile, oldPlanetFile;
    private Properties downloadParameters;
    private String mirror;
    public TorrentDownloader torrentDownloader = null;

    /**
     *
     * @param parameters
     */
    public PlanetDownloader(Properties parameters) {
        super(parameters);
    }

    @Override
    public void run() {
        setState(RUNNING);
        setStatus("Preparing download");
        downloadParameters = new Properties();
        sdf = new SimpleDateFormat("yyMMdd");
        df = new DecimalFormat("0.00");
        String[] mirrors = parameters.getProperty("planet_file_download_urls").split(",");
        mirror = mirrors[(int) (Math.floor(Math.random() * mirrors.length))].trim();  // select random mirror
        if (!mirror.endsWith("/")) {
            mirror = mirror + "/";
        }
        planetFile = new File(parameters.getProperty("planet_file"));
        oldPlanetFile = new File(parameters.getProperty("old_planet_file"));
        File downloadParmsFile = new File(Utilities.getUserdir(this) + "planetdownload.properties");
        if (downloadParmsFile.exists()) {
            try {
                downloadParameters.load(new FileInputStream(downloadParmsFile));
            } catch (IOException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Can't read download parameters.");
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
        }
        if (!(oldPlanetFile.exists() || planetFile.exists() || downloadParameters.containsKey("planet_name"))) {
            System.out.println("Planet file does not exist. Trying to download the newest one. This can take long time.");
            if (!downloadPlanetFile(planetFile)) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Planet file download failed.");
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            } else {
                setProgress(100);
                setStatus("Completed.");
                setState(COMPLETED);
            }
        }
        String parent = planetFile.getParent();
        if (parent != null) {
            parent = parent + "/";
        } else {
            parent = "";
        }
        if (downloadParameters.containsKey("planet_name")) {  // planet download not finished
            File planetDownload = new File(parent + downloadParameters.getProperty("planet_name"));
            System.out.println("Resuming broken download of " + planetDownload.getName().replace(".osm.pbf", ""));
            if (!resumePlanetFileDownload(planetFile)) {
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
        }

        setProgress(100);
        setStatus("Completed.");
        setState(COMPLETED);
        synchronized (this) {
            notify();
        }
    }

    private boolean downloadPlanetFile(File planetFile) {
        URL url = null;
        boolean planet_found;
        Date planetDate = new Date(System.currentTimeMillis());
        try {
            int i = 0;
            while (i < 14) {
                planet_found = true;
                String planetUrl = mirror + "planet-" + sdf.format(planetDate) + ".osm.pbf";
                if (!"http".equals(parameters.getProperty("download_method", "http"))) {
                    planetUrl = parameters.getProperty("torrent_download_url")
                            + "planet-" + sdf.format(planetDate) + ".osm.bz2" + ".torrent";
                }
                url = new URL(planetUrl);
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Range",
                            "bytes=0-10");
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) {
                        planet_found = false;
                    }
                } catch (IOException ex) {
                    planet_found = false;
                }
                int contentLength = connection.getContentLength();
                connection.disconnect();
                if (contentLength < 1) {
                    planet_found = false;
                }
                if (planet_found) {
                    break;
                } else {
                    i++;
                    planetDate = new Date(planetDate.getTime() - 1000 * 60 * 60 * 24);
                }
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }
        downloadParameters.put("planet_name", "planet-" + sdf.format(planetDate) + ".osm.pbf");
        downloadParameters.put("planet_date", sdf.format(planetDate));
        downloadParameters.put("start_date_osmosis", new SimpleDateFormat("yyyy-MM-dd").format(planetDate));
        try {
            downloadParameters.store(new FileOutputStream(new File(Utilities.getUserdir(this) + "planetdownload.properties")), "");
        } catch (IOException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Can not save download parameters.", ex);
            return false;
        }

        System.out.println("downloading " + url.toExternalForm());
        return resumePlanetFileDownload(planetFile);
    }

    private boolean resumePlanetFileDownload(File planetFile) {
        URL url;
        try {
            url = new URL(mirror + downloadParameters.getProperty("planet_name"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        String parent = planetFile.getParent();
        if (parent != null) {
            parent = parent + "/";
        } else {
            parent = "";
        }
        if ("http".equals(parameters.getProperty("download_method", "http"))) {
            File planetDownload = new File(parent + downloadParameters.getProperty("planet_name"));
            Downloader downloader = new Downloader(url, planetDownload.getPath());
            while (downloader.getStatus() != Downloader.COMPLETE) {
                if (downloader.getStatus() == Downloader.ERROR) {
                    return false;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
                if (downloader.getSize() > 0) {
                    setStatus(("Downloading " + planetFile.getName() + " (" + downloader.getSize() + " bytes) - "
                            + df.format(downloader.getProgress()) + " % completed."));
                    setProgress(downloader.getProgress());
                }
            }
            // check MD5 digest

            try {
                url = new URL(downloadParameters.getProperty("planet_name") + ".md5");
            } catch (MalformedURLException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Interrupted.", ex);
                return false;
            }
            File md5file = new File(downloadParameters.getProperty("planet_name") + ".md5");
            downloader = new Downloader(url, md5file.getPath());
            while (downloader.getStatus() != Downloader.COMPLETE) {
                if (downloader.getStatus() == Downloader.ERROR) {
                    return false;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, "Interrupted.", ex);
                    return false;
                }
            }

            setStatus("Computing MD5 hash.");
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            FileInputStream fis;
            try {
                fis = new FileInputStream(planetDownload);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }

            byte[] dataBytes = new byte[1024];

            int nread;
            long totread = 0l;
            long pl = planetDownload.length();
            try {
                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                    totread += nread;
                    setStatus("Computing MD5 hash - " + df.format(100.0 * totread / pl) + " % completed.");
                }
            } catch (IOException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            try {
                Scanner scanner = new Scanner(new FileInputStream(md5file));
                String md5_server = scanner.nextLine();
                scanner.close();
                if (!md5_server.contains(sb)) {
                    setStatus("Broken download. MD5 hash mismatch - computed hash: " + sb + ". Delete planet file and try again.");
                    return false;
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            if (!planetDownload.renameTo(planetFile)) {
                setStatus("Can not rename " + planetDownload.getName() + " to "
                        + planetFile.getName());
                return false;
            }
        } else {  // torrent download
            String torrentName = downloadParameters.getProperty("planet_name").replace(".osm.pbf", ".osm.bz2");
            try {
                url = new URL(parameters.getProperty("torrent_download_url") + torrentName + ".torrent");
            } catch (MalformedURLException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            File torrentFile = new File(parent
                    + torrentName + ".torrent");
            Downloader downloader = new Downloader(url, torrentFile.getPath());
            while (downloader.getStatus() != Downloader.COMPLETE) {
                if (downloader.getStatus() == Downloader.ERROR) {
                    return false;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
                if (downloader.getSize() > 0) {
                    setStatus(("Downloading " + torrentFile.getName() + " (" + downloader.getSize() + " bytes) - "
                            + df.format(downloader.getProgress()) + " % completed."));
                }
            }
            torrentDownloader =
                    new TorrentDownloader(parameters, torrentFile, torrentFile.getAbsoluteFile().getParentFile());
            Utilities.getInstance().addProcessToMonitor(torrentDownloader);
            torrentDownloader.changeSupport.addPropertyChangeListener(this);
            while (torrentDownloader.getState() != TorrentDownloader.COMPLETED) {
                if (torrentDownloader.getState() == ERROR) {
                    setStatus(torrentDownloader.getStatus());
                    setState(ERROR);
                    return false;
                }
                try {
                    setStatus(torrentDownloader.getStatus());
                    setProgress(torrentDownloader.getProgress());
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            }
            File torrentDownloadFile = new File(parent + torrentName);
            File planetBzFile = new File(planetFile.getPath().replace(".osm.pbf", ".osm.bz2"));
            try {
                Utilities.copyFile(new File(parent + torrentName), planetBzFile);
            } catch (IOException ex) {
                setStatus("Can not copy " + torrentDownloadFile.getName() + " to "
                        + planetBzFile.getName());
                return false;
            }
        }

        String osmosiswork = parameters.getProperty("osmosiswork");

        if (!osmosiswork.endsWith("/")) {
            osmosiswork = osmosiswork + "/";
        }
        File osmosisState = new File(osmosiswork + "state.txt");

        if (!new File(osmosiswork).isDirectory()) {
            // initialize Osmosis working directory
            new File(osmosiswork).mkdirs();
            String[] osargs = new String[]{"--rrii", "-v 9", "workingDirectory=" + osmosiswork};
            new File(osmosiswork + "configuration.txt").delete();
            try {
                Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                        osargs, this);
            } catch (Exception ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Osmosis error.", ex);
                setState(ERROR);
                return false;
            }
            try {
                //org.openstreetmap.osmosis.core.Osmosis.run(osargs);
                Utilities.copyFile(this.getClass().getResourceAsStream("configuration.txt"), new File(osmosiswork + "configuration.txt"));
            } catch (Exception ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Error while copying osmosis configuration.", ex);
                setState(ERROR);
                return false;
            }
        }
        // create osmosiswork
        if (!osmosisState.exists()) { // download state.txt
            String[] mirrors = parameters.getProperty("planet_file_update_urls").split(",");
            String upmirror = mirrors[(int) (Math.floor(Math.random() * mirrors.length))].trim();  // select random mirror
            if (!Utilities.downloadOsmosisStateFile(new SimpleDateFormat("yyyy-MM-dd").parse(
                    downloadParameters.getProperty("start_date_osmosis"), new ParsePosition(0)).getTime(),
                    upmirror, osmosisState.getPath())) {
                setState(ERROR);
                return false;
            }
        }

        // remove download properties
        new File(Utilities.getUserdir(this) + "planetdownload.properties").delete();
        return true;
    }
}
