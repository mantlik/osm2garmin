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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
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
        maxRegionsPass = Integer.parseInt(parameters.getProperty("max_regions_pass", "8"));
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
        boolean autoSplitPlanet = parameters.getProperty("auto_split_planet", "false").equals("true");
        int nodesPerRegion = Integer.parseInt(parameters.getProperty("nodes_per_region", "400000000"));
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
            String regfile = Utilities.getUserdir(this) + "/" + "regions.list";
            if (autoSplitPlanet) {
                regions_in_progress = "Splitting planet file.";
            } else {
                int noOfRegions = 0;
                try {
                    Scanner s = new Scanner(new File(parameters.getProperty("regions")));
                    Writer os = new FileWriter(regfile);
                    long id = 80000000;
                    while (s.hasNextLine()) {
                        String[] data = s.nextLine().split(" +");
                        // Comment starts with #
                        // GUI temporarily excluded region starts with x
                        if (data.length >= 5 && !(data[0].startsWith("#") || data[0].startsWith("x"))) {
                            float lon1 = Float.parseFloat(data[0]);
                            float lat1 = Float.parseFloat(data[1]);
                            float lon2 = Float.parseFloat(data[2]);
                            float lat2 = Float.parseFloat(data[3]);
                            String name = data[4];
                            id++;
                            noOfRegions++;
                            os.write("#" + id + ": " + " " + lat1 + "," + lon1
                                    + " to " + lat2 + "," + lon2 + " " + name + "\n");
                            os.write(id + ": " + " " + Utilities.coordToMap(lat1) + ","
                                    + Utilities.coordToMap(lon1)
                                    + " to " + Utilities.coordToMap(lat2) + ","
                                    + Utilities.coordToMap(lon2) + "\n");
                        }
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
                regions_in_progress = "Splitting planet file to " + noOfRegions + " regions.";
            }
            if (autoSplitPlanet) {
                args = new String[]{
                    "--output-dir=" + Utilities.getUserdir(this), "--max-areas=" + maxRegionsPass,
                    "--output=pbf",
                    "--max-nodes=" + nodesPerRegion,
                    "--mapid=80000001",
                    "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip",
                    parameters.getProperty("planet_file")
                };
            } else {
                args = new String[]{
                    "--output-dir=" + Utilities.getUserdir(this), "--max-areas=" + maxRegionsPass,
                    "--output=pbf",
                    "--split-file=" + Utilities.getUserdir(this) + "/" + "regions.list",
                    parameters.getProperty("planet_file")
                };
            }
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

            // Create regions file from splitted regions
            if (autoSplitPlanet) {
                File areasList = new File(Utilities.getUserdir(this), "areas.list");
                if (!areasList.exists()) {
                    setState(ERROR);
                    setStatus("Cannot find areas.list file.");
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
                File autoregfile = new File(Utilities.getUserdir(this), "autoregions.txt");
                if (autoregfile.exists()) {
                    Utilities.deleteFile(autoregfile);
                }
                File template = new File(Utilities.getUserdir(this), "template.args");
                Map names = new TreeMap();
                try {
                    if (template.exists()) {
                        int id = 1;
                        Scanner tmpl = new Scanner(new BufferedReader(new FileReader(template)));
                        while (tmpl.hasNextLine()) {
                            String line = tmpl.nextLine();
                            if (!line.startsWith("mapname: ")) {
                                continue;
                            }
                            String key = (line.split(":")[1]).trim();
                            line = tmpl.nextLine();
                            String name = id + "_" + (line.split(":")[1]).trim().replaceAll(" ", "_").
                                    replaceAll("-", "_");
                            id++;
                            names.put(key, name);
                        }
                        tmpl.close();
                    }
                    Scanner rd = new Scanner(new BufferedReader(new FileReader(areasList)));
                    Writer os = new FileWriter(autoregfile);
                    String id = "";
                    while (rd.hasNextLine()) {
                        String line = rd.nextLine();
                        if (line.startsWith("#       :")) { // extract coords and write region record
                            String[] coords = (line.split(":")[1]).trim().split(" to ");
                            String[] from = coords[0].split(",");
                            String[] to = coords[1].split(",");
                            String lat1 = from[0];
                            String lon1 = from[1];
                            String lat2 = to[0];
                            String lon2 = to[1];
                            os.write(lon1 + " " + lat1 + " " + lon2 + " " + lat2 + " " + 
                                    (names.containsKey(id) ? names.get(id) : id) + "\n");
                        } else {
                            if (line.trim().isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            // extract ID
                            id = line.split(":")[0];
                        }
                    }
                    os.close();
                    rd.close();
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
            }
            pass++;

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
