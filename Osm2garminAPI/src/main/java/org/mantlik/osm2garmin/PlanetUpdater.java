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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.mantlik.osm2garmin.ThreadProcessor.ERROR;

/**
 *
 * @author fm
 */
public class PlanetUpdater extends ThreadProcessor {

    private static int maxRegionsPass;
    private int pass = 0;
    File planetFile;
    File oldPlanetFile;
    private long uflen, planetlen, oldPlanetLen;
    ArrayList<Region> regions;
    private String regions_in_progress = "";

    /**
     *
     * @param parameters
     * @param regions
     */
    public PlanetUpdater(Properties parameters, ArrayList<Region> regions) {
        super(parameters, false);
        planetFile = new File(parameters.getProperty("planet_file"));
        oldPlanetFile = new File(parameters.getProperty("old_planet_file"));
        maxRegionsPass = Integer.parseInt(parameters.getProperty("max_regions_pass", "3"));
        uflen = 0;
        this.regions = regions;
        start();
    }

    /**
     *
     * @return
     */
    @Override
    public String getStatus() {
        if (this.getState() == COMPLETED) {
            setStatus("Completed.");
            setProgress(100);
            return super.getStatus();
        } else if (getState() == ERROR) {
            return super.getStatus();
        }
        int npasses = 4;
        double pass_progress = 0;
        File uf = new File(Utilities.getUserdir(this) + "update.osc.gz");
        if (uf.exists()) {
            uflen = uf.length();
        }
        if (oldPlanetFile.exists()) {
            oldPlanetLen = oldPlanetFile.length();
        }
        if (planetFile.exists() && pass <= 1) {
            planetlen = planetFile.length();
            setStatus("Updating planet file - " + planetlen + " bytes written" + regions_in_progress);
            if ((oldPlanetLen + uflen > 0) && (pass == 1)) {
                pass_progress = Math.min(1, 1.0 * planetlen / (oldPlanetLen + uflen));
            }
        } else if (uf.exists() && pass == 0) {
            setStatus("Processing updates - " + uf.length() + " bytes written");
        } else if (pass == 0) {
            setStatus("Preparing updates.");
        } else {
            setStatus(regions_in_progress);
        }
        setProgress((int) (100.0 * ((pass + pass_progress) / (npasses))));
        return super.getStatus();
    }

    @Override
    public void run() {
        boolean skipPlanetUpdate = parameters.getProperty("skip_planet_update", "false").equals("true");
        boolean updateRegions = parameters.getProperty("update_regions", "false").equals("true");
        if (skipPlanetUpdate && (!updateRegions)) {
            setProgress(100);
            setStatus("Skipped.");
            setState(COMPLETED);
            synchronized (this) {
                notify();
            }
            return;
        }
        try {
            File f = new File(Utilities.getUserdir(this) + "update.osc.gz");
            if (f.exists()) {
                f.delete();
            }
            ArrayList<String> l = new ArrayList<String>();
            int i = 0;
            File upd = new File(Utilities.getUserdir(this) + "update" + i + ".osc.gz");
            int nchanges = 0;
            while (upd.exists() && (!skipPlanetUpdate)) {
                l.add("--rxc");
                l.add("file=" + upd.getPath());
                l.add("--buffer-change");
                l.add("bufferCapacity=10000");
                nchanges++;
                i++;
                upd = new File(Utilities.getUserdir(this) + "update" + i + ".osc.gz");
            }
            String[] args;
            if (nchanges > 0) {
                l.add("--apc");
                l.add("sourceCount=" + nchanges);
                l.add("--sc");
                l.add("--simc");
                l.add("--wxc");
                l.add("file=" + Utilities.getUserdir(this) + "update.osc.gz");
                args = l.toArray(new String[0]);
                Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                        args, this);
            }

            File torrentFile = new File(parameters.getProperty("planet_file").replace(".osm.pbf", ".osm.bz2"));

            String readSource = "--rb";
            String inputFile = parameters.getProperty("old_planet_file");

            if (torrentFile.exists()) {
                readSource = "--fast-read-xml";
                inputFile = torrentFile.getPath();
            }

            pass++;
            if (!skipPlanetUpdate) {
                ArrayList<String> largs = new ArrayList<String>();
                largs.add(readSource);
                largs.add("file=" + inputFile);
                largs.add("outPipe.0=ac0");
                String[] args1;
                if (nchanges > 0) {
                    args1 = new String[]{
                        "--rxc", "file=" + Utilities.getUserdir(this) + "update.osc.gz", //"--buffer-change", "bufferCapacity=10000",
                        //"--buffer", "bufferCapacity=10000",
                        "outPipe.0=ac1",
                        "--ac", //"--buffer", "bufferCapacity=10000",
                        "inPipe.0=ac0", "inPipe.1=ac1",
                        "--log-progress", "interval=120",
                        "--t", "outputCount=1",
                        "--wb", "file=" + parameters.getProperty("planet_file")
                    };
                } else {
                    args1 = new String[]{
                        "--log-progress", "interval=120", "inPipe.0=ac0",
                        "--t", "outputCount=1",
                        "--wb", "file=" + parameters.getProperty("planet_file")
                    };
                }
                largs.addAll(Arrays.asList(args1));
                args = largs.toArray(new String[0]);
                regions_in_progress = "";

                Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                        args, this);
            }

            pass++;
            // Split planet to regions
            regions_in_progress = "Splitting planet file to "+ regions.size() +" regions.";
            String regfile = Utilities.getUserdir(this) + "/" + "planet.list";
            try {
                Writer os = new FileWriter(regfile);
                long id = 80000000;
                for (Region region : regions) {
                    id++;
                    os.write("#" + id + ": " + " " + region.lat1 + "," + region.lon1
                            + " to " + region.lat2 + "," + region.lon2 + " " + region.name + "\n");
                    os.write(id + ": " + " " + Utilities.coordToMap(region.lat1) + ","
                            + Utilities.coordToMap(region.lon1)
                            + " to " + Utilities.coordToMap(region.lat2) + ","
                            + Utilities.coordToMap(region.lon2) + "\n");
                }
                os.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                setStatus(ex.getMessage());
                synchronized (this) {
                    notify();
                }
                return;
            } catch (IOException ex) {
                Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                setStatus(ex.getMessage());
                synchronized (this) {
                    notify();
                }
                return;
            }
            args = new String[]{
                "--output-dir=" + Utilities.getUserdir(this), "--max-areas=" + maxRegionsPass,
                "--output=pbf",
                "--split-file=" + Utilities.getUserdir(this) + "/" + "planet.list",
                parameters.getProperty("planet_file")
            };
            try {

                Utilities.getInstance().runExternal("uk.me.parabola.splitter.Main", "main", "splitter",
                        args, this);
            } catch (Exception ex) {
                Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                setStatus(ex.getMessage());
                synchronized (this) {
                    notify();
                }
                return;
            }
            pass++;
            // Move regions to maps
            regions_in_progress = "Moving region files.";
            long id = 80000000;
            for (Region region : regions) {
                id++;
                File oldFile = new File(Utilities.getUserdir(this), id + ".osm.pbf");
                File newFile = new File(region.dir, region.name + ".osm.pbf");
                if (!oldFile.exists()) {
                    continue;
                }
                if (newFile.exists()) {
                    Utilities.deleteFile(newFile);
                }
                Utilities.copyFile(oldFile, newFile);
                Utilities.deleteFile(oldFile);
            }

            if ((!planetFile.exists()) || (planetFile.length() < oldPlanetFile.length())) {
                //if (planetFile.exists()) {
                //    planetFile.delete();
                //}
                setStatus("Planet file has invalid size after update.");
                Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, "Planet file has invalid size after update.");
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }

            if (torrentFile.exists()) {
                if (!torrentFile.delete()) {
                    torrentFile.deleteOnExit();
                }
            }

            // delete diff files
            i = 0;
            upd = new File(Utilities.getUserdir(this) + "update" + i + ".osc.gz");
            while (upd.exists()) {
                if (upd.exists()) {
                    upd.delete();
                }
                i++;
                upd = new File(Utilities.getUserdir(this) + "update" + i + ".osc.gz");
            }
            
            pass++;
            setStatus("Completed.");
            setProgress(100);
            setState(COMPLETED);
        } catch (Exception ex) {
            Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, "Planet update failed. " + ex.getMessage(), ex);
            setState(ERROR);
        }
        synchronized (this) {
            notify();
        }
    }
}
