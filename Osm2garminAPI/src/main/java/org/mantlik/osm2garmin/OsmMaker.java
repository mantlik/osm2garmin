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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.mantlik.osm2garmin.ThreadProcessor.ERROR;
import org.openide.util.Exceptions;

/**
 *
 * @author fm
 */
public class OsmMaker extends ThreadProcessor {

    /**
     *
     */
    public static final int SPLITTER_OUT_OF_MEMORY = ERROR + 1;
    private Region region;
    private boolean isSplitting;
    private ClassLoader splitterLoader;
    private int max_areas = 200;
    private long map_start_id = 70000001;

    /**
     *
     * @param region
     * @param parameters
     * @param max_areas
     */
    public OsmMaker(Region region, Properties parameters, int max_areas) {
        super(parameters, false);
        this.region = region;
        this.max_areas = max_areas;
        this.map_start_id = map_start_id + (region.familyID - 5000) * 100000;
        start();
    }

    @Override
    public void run() {
        String args[];
        setProgress(0);
        Utilities.checkArgFiles(Utilities.getUserdir(this));
        // since Splitter r279 replace --overlap with --keep-complete=true for low memory systems
        long maxMemory = Runtime.getRuntime().maxMemory();
        String keepComplete = "--keep-complete=true";
        if (maxMemory <= 1800000000l) {
            keepComplete = "--keep-complete=false";
        }
        String osm2imgArgsFileName = Utilities.getUserdir(this) + "osm2img.args";
        String gmapsuppArgsFileName = Utilities.getUserdir(this) + "gmapsupp.args";
        String gmapsuppContoursArgsFileName = Utilities.getUserdir(this) + "gmapsupp_contours.args";
        setStatus(region.name + " Splitting planet file.");
        String splitFile = region.dir.getPath() + "/" + "areas.list";
        if (!new File(splitFile).exists()) {
            String polyfile = region.dir.getPath() + "/" + "boundary.poly";
            try {
                Writer os = new FileWriter(polyfile);
                os.write(region.name + "\n");
                os.write("1" + "\n");
                os.write(" " + region.lon1 + " " + region.lat1 + "\n");
                os.write(" " + region.lon1 + " " + region.lat2 + "\n");
                os.write(" " + region.lon2 + " " + region.lat2 + "\n");
                os.write(" " + region.lon2 + " " + region.lat1 + "\n");
                os.write(" " + region.lon1 + " " + region.lat1 + "\n");
                os.write("END" + "\n");
                os.write("END" + "\n");
                os.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                setStatus(ex.getMessage());
                synchronized (this) {
                    notify();
                }
                return;
            } catch (IOException ex) {
                Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                setStatus(ex.getMessage());
                synchronized (this) {
                    notify();
                }
                return;
            }
            args = new String[]{
                "--output-dir=" + region.dir.getPath(), "--max-areas=" + max_areas,
                keepComplete, "--mapid=" + map_start_id, "--output=pbf",
                "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip", "--status-freq=0",
                "--polygon-file=" + polyfile,
                "--max-threads=1", "--max-nodes=1200000", region.dir.getPath() + "/" + region.name + ".osm.pbf"
            };
        } else {
            args = new String[]{
                "--output-dir=" + region.dir.getPath(), "--max-areas=" + max_areas, "--mapid=" + map_start_id,
                "--geonames-file=" + Utilities.getUserdir(this) + "cities15000.zip", "--status-freq=0",
                keepComplete, "--split-file=" + splitFile, "--output=pbf",
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
                synchronized (this) {
                    notify();
                }
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
        long maxid = map_start_id;
        while (new File(region.dir.getPath() + "/" + maxid + ".osm.pbf").exists()) {
            maxid++;
        }
        maxid--;

        // convert to Garmin
        setProgress(50);
        setStatus(region.name + " converting to Garmin format");
        args = new String[]{
            "--output-dir=" + region.dir.getPath(), "--series-name=" + region.name,
            "-c", osm2imgArgsFileName,
            "-c", region.dir.getPath() + "/" + "template.args"
        };
        try {
            //uk.me.parabola.mkgmap.main.Main.main(args);
            Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
        } catch (Exception ex) {
            Logger.getLogger(OsmMaker.class.getName()).log(Level.SEVERE, null, ex);
            Utilities.getInstance().endExclusive("uk.me.parabola.mkgmap.main.Main");
        }

        for (long id = map_start_id; id <= maxid; id++) {
            setProgress((float) (75.0 + 20.0 * (id - map_start_id) / (maxid - map_start_id)));
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
                    setState(ERROR);
                    setStatus(ex.getMessage());
                    synchronized (this) {
                        notify();
                    }
                    return;
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
                    "--output-dir=" + region.dir.getPath(),
                    "-c", osm2imgArgsFileName,
                    "-c", region.dir.getPath() + "/" + "template.args"
                };
                try {
                    //uk.me.parabola.mkgmap.main.Main.main(args);
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
            File imgFile = new File(region.dir, name);
            if (imgFile.length() < 10) {
                imgFile.deleteOnExit();
                continue;
            }
            long id = Long.parseLong(name.replace(".img", ""));
            if (id >= map_start_id) {
                osmMaps.add(region.dir.getPath() + "/" + name);
            } else {
                contourMaps.add(region.dir.getPath() + "/" + name);
            }
        }

        // create gmapsupp.img
        setStatus(region.name + " adding " + contourMaps.size() + " contour maps and "
                + osmMaps.size() + " OSM maps to gmapsupp.img.");
        ArrayList<String> aa = new ArrayList<>();
        aa.add("--gmapsupp");
        aa.add("--output-dir=" + region.dir.getPath());
        boolean withIndexes = false;
        if (Runtime.getRuntime().maxMemory() > 1800000000l) {
            aa.add("--index");
            aa.add("--location-autofill=nearest");
            aa.add("--add-pois-to-areas");
            if (new File("bounds").exists()) {
                aa.add("--bounds=bounds");
            } else {
                if (new File("bounds.zip").exists()) {
                    aa.add("--bounds=bounds.zip");
                }
            }
            withIndexes = true;
        }
        aa.add("--family-id=" + region.familyID);
        aa.add("--family-name=" + region.name);
        aa.add("--series-name=" + region.name);
        aa.add("--area-name=" + region.name);
        aa.add("-c");
        aa.add(gmapsuppArgsFileName);
        aa.addAll(osmMaps);
        aa.add("-c");
        aa.add(gmapsuppContoursArgsFileName);
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
        aa.add(1, "--nsis");
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
        for (long id = map_start_id; id <= maxid; id++) {
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
