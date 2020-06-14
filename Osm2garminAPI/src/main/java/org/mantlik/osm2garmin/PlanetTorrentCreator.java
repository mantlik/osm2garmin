/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 - 2014 Frantisek Mantlik <frantisek at mantlik.cz>
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
/*
 * Copyright (C) 2014 frantisek
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.mantlik.osm2garmin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import jbittorrentapi.TorrentFile;
import jbittorrentapi.TorrentProcessor;

/**
 * Create Planet torrent file from the remote
 *
 * @author frantisek
 */
public class PlanetTorrentCreator {

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyMMdd");
    private static final String[] MIRRORS = {
        "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org//pbf",
        "https://planet.openstreetmap.org/pbf"
    };
    private static final String[][] ANNOUNCE = {
        {"http://www.mantlik.cz:80/tracker/announce.php"},
        {"http://tracker.ipv6tracker.org:80/announce",
         "udp://tracker.ipv6tracker.org:80/announce"},
        {"udp://tracker.publicbt.com:80/announce",
         "http://tracker.publicbt.com:80/announce"},
        {"udp://tracker.openbittorrent.com:80/announce"},
        {"http://open-tracker.appspot.com/announce"}
    };
    private static final int PIECE_LENGTH = 4 * 1024 * 1024;
    private static final long MAX_PLANET_AGE = 31;

    private String planetName;
    private long planetLength;
    private String md5;
    private final String mirror = MIRRORS[(int)(Math.random() * MIRRORS.length)];

    private void findLastPlanetFile() {
        long planetTime = System.currentTimeMillis();
        planetLength = -1;
        for (int i = 0; i < MAX_PLANET_AGE; i++) {
            planetTime -= 1000l * 60 * 60 * 24;
            String dd = DF.format(new Date(planetTime));
            try {
                URL url = new URL(mirror + "/" + "planet-" + dd + ".osm.pbf");
                planetLength = url.openConnection().getContentLengthLong();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            if (planetLength > 1000000000) {
                planetName = "planet-" + dd + ".osm.pbf";
                return;
            }
        }
    }

    private void downloadMD5() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(mirror + "/" + planetName + ".md5").openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            md5 = scanner.nextLine().substring(0, 32);
            scanner.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void createTorrent() {
        TorrentFile torrent = new TorrentFile();
        torrent.announceURL = ANNOUNCE[0][0];
        torrent.comment = "Original data from https://planet.openstreetmap.org/ "
                + "(C) OpenStreetMap contributors";
        torrent.createdBy = "Osm2garmin 1.2";
        torrent.creationDate = System.currentTimeMillis() / 1000;
        for (String[] tierarr : ANNOUNCE) {
            ArrayList<String> tier = new ArrayList<String>();
            tier.addAll(Arrays.asList(tierarr));
            torrent.announceList.add(tier);
        }
        torrent.changeAnnounce();
        ArrayList<String> urlList = new ArrayList<String>();
        for (String amirror : MIRRORS) {
            urlList.add(amirror + "/" + planetName);
        }
        torrent.urlList.addAll(urlList);
        torrent.setPieceLength(0, PIECE_LENGTH);
        torrent.length.add(planetLength);
        torrent.name.add(planetName);
        TorrentProcessor tp = new TorrentProcessor(torrent);
        ArrayList<String> md5list = new ArrayList<String>();
        md5list.add(md5);
        if (tp.generatePieceHashesFromRemote(MIRRORS, md5list.toArray(new String[0]))) {
            try {
                FileOutputStream os = new FileOutputStream(planetName + ".torrent");
                os.write(tp.generateTorrent());
                os.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                System.exit(9);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(9);
            }
        } else {
            System.out.println("Torrent creation failed.");
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PlanetTorrentCreator instance = new PlanetTorrentCreator();
        instance.findLastPlanetFile();
        if (instance.planetLength < 1000000000) {
            System.out.println("Cannot find planet file not older than " + MAX_PLANET_AGE + 
                    " days at " + instance.mirror + ".");
            System.exit(9);
        }
        System.out.println("Found " + instance.planetName + " (" + instance.planetLength + " bytes)");
        if (new File(instance.planetName + ".torrent").exists()) {
            System.out.println(instance.planetName + ".torrent already exists. Exiting.");
            System.exit(0);
        }
        System.out.println("Downloading MD5 checksum.");
        instance.downloadMD5();
        System.out.println("Creating torrent file.");
        instance.createTorrent();
        System.out.println("Processing finished.");
    }

}
