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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class PlanetUpdateDownloader extends ThreadProcessor {

    private static final DecimalFormat df = new DecimalFormat("0");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
    public TorrentDownloader torrentDownloader = null;

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
        File osmosisState = new File(osmosiswork + "state.txt");
        File osmosisStateBackup = new File(osmosiswork + "state_old.txt");

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
        while (System.currentTimeMillis() - planet_timestamp > 1000 * 60 * 60 * minAge) {
            setProgress((float) ((planet_timestamp - startTime) * 100.0 / (System.currentTimeMillis() - startTime)));
            setStatus("Downloading planet updates (" + new SimpleDateFormat("yyyy-MM-dd").format(new Date(planet_timestamp)) + ") - "
                    + df.format(getProgress()) + " % completed.");
            String[] osargs = new String[]{
                "--rri", "workingDirectory=" + osmosiswork,
                "--wxc", "file=" + Osm2garmin.userdir + "update" + i + ".osc.gz"
            };

            try {
                Osm2garmin.runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                        Osm2garmin.libClassLoader("osmosis", getClass().getClassLoader()), osargs);
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
}
