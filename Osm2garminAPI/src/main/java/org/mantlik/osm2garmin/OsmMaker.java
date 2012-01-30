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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fm
 */
public class OsmMaker extends ThreadProcessor {

    private static final long MAPID = 70000001;
    /**
     *
     */
    public static final int SPLITTER_OUT_OF_MEMORY = ERROR + 1;
    private Region region;
    private boolean isSplitting;
    private ClassLoader splitterLoader;
    private int max_areas = 50;

    /**
     *
     * @param region
     * @param parameters
     * @param max_areas
     */
    public OsmMaker(Region region, Properties parameters, int max_areas) {
        super(parameters);
        this.region = region;
        this.max_areas = max_areas;
    }

    /**
     *
     * @return
     */
    @Override
    public float getProgress() {
        if (isSplitting && !(splitterLoader == null)) {
            try {
                Class splitter = splitterLoader.loadClass("uk.me.parabola.splitter.Main");
                Field pr = splitter.getField("progress");
                float progress = pr.getFloat(null) / 2;
                return progress;
            } catch (Exception ex) {
                Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                return super.getProgress();
            }
        }
        return super.getProgress();
    }

    /**
     *
     * @return
     */
    @Override
    public String getStatus() {
        if (isSplitting && !(splitterLoader == null)) {
            String astatus;
            try {
                Class splitter = splitterLoader.loadClass("uk.me.parabola.splitter.Main");
                Field st = splitter.getField("status");
                astatus = (String) st.get(null);
                String status = region.name + " splitting " + astatus;
                setStatus(status);
                setProgress(getProgress());
                return status + waitingString();
            } catch (Exception ex) {
                Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                return super.getStatus();
            }
        }
        return super.getStatus();
    }

    @Override
    public void run() {
        String args[];
        setProgress(0);
        setStatus(region.name + " Splitting planet file.");
        String splitFile = region.dir.getPath() + "/" + "areas.list";
        if (!new File(splitFile).exists()) {
            args = new String[]{
                "--output-dir=" + region.dir.getPath(), "--max-areas=" + max_areas, "--mapid=" + MAPID, "--output=pbf",
                "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip", "--bottom=" + region.lat1,
                "--top=" + region.lat2, "--left=" + region.lon1, "--right=" + region.lon2, "--status-freq=0",
                "--max-threads=1", "--max-nodes=1200000", region.dir.getPath() + "/" + region.name + ".osm.pbf"
            };
        } else {
            args = new String[]{
                "--output-dir=" + region.dir.getPath(), "--max-areas=" + max_areas, "--mapid=" + MAPID,
                "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip", "--status-freq=0",
                "--split-file=" + splitFile, "--output=pbf",
                "--max-threads=1", region.dir.getPath() + "/" + region.name + ".osm.pbf"
            };
        }
        isSplitting = true;
        //uk.me.parabola.splitter.Main.main(args);
        try {

            Utilities.getInstance().runExternal("uk.me.parabola.splitter.Main", "main", "splitter",
                    args, this);
        } catch (InvocationTargetException ex) {
            Throwable exx = ex.getTargetException();
            region.splitterMaxAreas = Math.max(1, region.splitterMaxAreas / 2);
            setStatus("Splitter out of memory. Restarting with max_areas=" + region.splitterMaxAreas);
            System.err.println(exx.getMessage());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex1) {
                setState(ERROR);
                setStatus("Interrupted.");
                return;
            }
            region.setState(Region.CONTOURS_READY);
            return;
        } catch (Exception ex) {
            Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
            setState(ERROR);
            setStatus(ex.getMessage());
            synchronized (this) {
                notify();
            }
            return;
        }
        isSplitting = false;
        File sFile = new File(splitFile);
        if (!sFile.delete()) {
            sFile.deleteOnExit();
        }

        // Create list of splitted files
        long maxid = MAPID;
        while (new File(region.dir.getPath() + "/" + maxid + ".osm.pbf").exists()) {
            maxid++;
        }
        maxid--;

        // convert to Garmin
        setProgress(50);
        setStatus(region.name + " converting to Garmin format");
        args = new String[]{
            "--output-dir=" + region.dir.getPath(), "--draw-priority=20",
            "--net", "--route", "--add-pois-to-areas", "--series-name=" + region.name,
            "-c", region.dir.getPath() + "/" + "template.args",
            "--remove-short-arcs"
        };
        if (parameters.getProperty("cycling_features", "false").equals("true")) {
            List<String> l = Arrays.asList(args);
            l.add("--make-all-cycleways");
            args = l.toArray(new String[0]);
        }
        try {
            //uk.me.parabola.mkgmap.main.Main.main(args);
            Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
        } catch (Exception ex) {
            Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (long id = MAPID; id <= maxid; id++) {
            setProgress((float) (75.0 + 20.0 * (id - MAPID) / (maxid - MAPID)));
            setStatus(region.name + " checking converted files (" + id + ".img) - " + getProgress() + " %");
            File imgFile = new File(region.dir.getPath() + "/" + id + ".img");
            if ((!imgFile.exists()) || imgFile.length() < 10) {
                // split OSM
                setStatus(region.name + " splitting area (" + id + ".img) - " + getProgress() + " %");
                args = new String[]{
                    "--output-dir=" + region.dir.getPath(), "--max-areas=20", "--mapid=" + (maxid + 1),
                    "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip", "--output=pbf",
                    "--max-nodes=800000", "--status-freq=0",
                    region.dir.getPath() + "/" + id + ".osm.pbf"
                };
                try {
                    //uk.me.parabola.splitter.Main.main(args);
                    Utilities.getInstance().runExternal("uk.me.parabola.splitter.Main", "main", "splitter",
                            args, this);
                } catch (Exception ex) {
                    Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (imgFile.exists()) {
                    imgFile.delete();
                }
                // convert splitted to Garmin in next iterations
                while (new File(region.dir.getPath() + "/" + (maxid + 1) + ".osm.pbf").exists()) {
                    maxid++;
                }
                new File(region.dir.getPath() + "/" + id + ".osm.pbf").delete();
                // convert to Garmin
                setStatus(region.name + " converting to Garmin format");
                args = new String[]{
                    "--output-dir=" + region.dir.getPath(), "--draw-priority=20",
                    "--net", "--route", "--add-pois-to-areas", "--series-name=" + region.name,
                    "-c", region.dir.getPath() + "/" + "template.args",
                    "--remove-short-arcs"
                };
                if (parameters.getProperty("cycling_features", "false").equals("true")) {
                    List<String> l = Arrays.asList(args);
                    l.add("--make-all-cycleways");
                    args = l.toArray(new String[0]);
                }
                try {
                    //uk.me.parabola.mkgmap.main.Main.main(args);
                    Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
                } catch (Exception ex) {
                    Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        System.gc();

        // make maps lists
        ArrayList<String> contourMaps = new ArrayList<String>();
        ArrayList<String> osmMaps = new ArrayList<String>();
        String[] files = new File(region.dir.getPath()).list();
        for (String name : files) {
            if (!name.matches("[0-9]{8}.img")) {
                continue;
            }
            long id = Long.parseLong(name.replace(".img", ""));
            if (id >= MAPID) {
                osmMaps.add(region.dir.getPath() + "/" + name);
            } else {
                contourMaps.add(region.dir.getPath() + "/" + name);
            }
        }

        // create gmapsupp.img
        setStatus(region.name + " adding " + contourMaps.size() + " contour maps and "
                + osmMaps.size() + " OSM maps to gmapsupp.img.");
        ArrayList<String> aa = new ArrayList<String>();
        aa.add("--gmapsupp");
        aa.add("--output-dir=" + region.dir.getPath());
        boolean withIndexes = false;
        if (Runtime.getRuntime().maxMemory() > 1800000000l) {
            aa.add("--index");
            aa.add("--location-autofill=nearest");
            withIndexes = true;
        }
        aa.add("--family-id=" + region.familyID);
        aa.add("--family-name=" + region.name);
        aa.add("--series-name=" + region.name);
        aa.add("--product-id=1");
        aa.addAll(osmMaps);
        aa.add("--show-profiles");
        aa.addAll(contourMaps);
        args = aa.toArray(new String[0]);
        try {
            Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
        } catch (Exception ex) {
            Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
            setState(ERROR);
            setStatus(ex.getMessage());
            synchronized (this) {
                notify();
            }
            return;
        }

        setStatus(region.name + " creating files for MapSource.");
        setProgress(98);
        System.gc();

        // create MapSource registration files
        aa.set(0, "--tdbfile"); // replace --gmapsupp with --tdbfile
        aa.add("--nsis");
        args = aa.toArray(new String[0]);
        try {
            Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
        } catch (Exception ex) {
            Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
            setState(ERROR);
            setStatus(ex.getMessage());
            synchronized (this) {
                notify();
            }
            return;
        }
        region.makeInstallers(withIndexes);

        // delete splitted pbf maps
        for (long id = MAPID; id <= maxid; id++) {
            File osmFile = new File(region.dir.getPath() + "/" + id + ".osm.pbf");
            if (!osmFile.delete()) {
                try {
                    Thread.sleep(500);
                    if (!osmFile.delete()) {
                        osmFile.deleteOnExit();
                    }
                } catch (InterruptedException ex) {
                    osmFile.deleteOnExit();
                }
            }
        }

        // delete temp files
        for (File f : region.dir.listFiles()) {
            if (f.getName().endsWith(".tmp")) {
                if (!f.delete()) {
                    f.deleteOnExit();
                }
            }
        }

        setStatus(" completed.");
        setProgress(100);
        setState(COMPLETED);
        synchronized (this) {
            notify();
        }
    }

    public void setSplitterLoader(ClassLoader splitterLoader) {
        this.splitterLoader = splitterLoader;
    }
}
