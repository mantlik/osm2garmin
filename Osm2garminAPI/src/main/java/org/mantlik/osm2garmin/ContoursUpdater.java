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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.mantlik.osm2garmin.srtm2osm.Srtm;

/**
 *
 * @author fm
 */
public class ContoursUpdater extends ThreadProcessor {

    private final static DecimalFormat d000 = new DecimalFormat("000");
    private final static DecimalFormat d00 = new DecimalFormat("00");
    private final static DecimalFormat d8 = new DecimalFormat("00000000");
    private Region region;
    private ThreadProcessor srtm2osm = null;
    private byte[] buf = new byte[1024];

    /**
     *
     * @param region
     * @param parameters
     */
    public ContoursUpdater(Region region, Properties parameters) {
        super(parameters);
        this.region = region;
    }

    @Override
    public void run() {
        setStatus("Starting contours update - " + region.name);
        setProgress(0);
        String contoursDir = parameters.getProperty("contours_dir");
        if (!contoursDir.endsWith("/")) {
            contoursDir = contoursDir + "/";
        }
        int step = 0;
        int srtmStep = Integer.parseInt(parameters.getProperty("srtm_step", "2"));
        int nlon = (int) Math.ceil((region.lon2 - region.lon1) / srtmStep);
        int nlat = (int) Math.ceil((region.lat2 - region.lat1) / srtmStep);
        for (float lon = region.lon1 - (region.lon1 % srtmStep); lon < region.lon2; lon += srtmStep) {
            for (float lat = region.lat1 - (region.lat1 % srtmStep); lat < region.lat2; lat += srtmStep) {
                step++;
                int perc = (100 * step) / nlat / nlon;
                setProgress(perc);
                int la = (int) Math.floor(lat);
                int lo = (int) Math.floor(lon);
                String name = d8.format(((la + 90) * 360 + lo + 180) * 1000); // 0 to 64800000
                String srtmName = Srtm.getName(lo, la).substring(0, 8);
                String coords = "Contours " + (int) Math.abs(lat) + (lat > 0 ? "N " : "S ")
                        + (int) Math.abs(lon) + (lon > 0 ? "E" : "W") + ": ";

                // check existence of contours file
                setStatus(coords + "Unpacking contours data "
                        + " - " + region.name + " " + perc + " % completed.");

                File zipfile = new File(contoursDir + srtmName + ".zip");
                File zipFile2 = new File(contoursDir + name + ".zip");
                if (zipfile.exists() && (zipfile.length() > 0)) {
                    unpackFiles(zipfile);
                    continue;
                } else if (zipFile2.exists()) {
                    if (zipFile2.length() > 0) {
                        unpackFiles(zipFile2);
                    }
                    zipFile2.renameTo(zipfile);
                    continue;
                }

                /*
                 * if (!Srtm.exists(lo, la, parameters)) {
                 * // System.out.println(coords+" no srtm data.");
                 * continue;
                 * }
                 */


                //String name = d000.format(Math.abs(lo)) + (lo >= 0 ? "E" : "W") + d00.format(Math.abs(la)) + (la >= 0 ? "N" : "S");
                String outputName = contoursDir + name + ".osm.gz";
                OutputStream os;
                try {
                    os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(
                            new File(outputName))));
                } catch (IOException ex) {
                    Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    setStatus("Error creating contours output.");
                    setState(ERROR);
                    region.setState(Region.ERROR);
                    return;
                }
                srtm2osm = new Srtm2Osm(parameters, la, lo, os);
                while (srtm2osm.getState() == Srtm2Osm.RUNNING) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        setState(ERROR);
                        setStatus("Interrupted.");
                        region.setState(Region.ERROR);
                        return;
                    }
                    setStatus(srtm2osm.getStatus() + " - " + region.name + " " + perc + " % completed.");
                }
                File f = new File(outputName);
                //if (f.exists() && f.length() < 30) {
                //    f.delete();  // gzip header only
                //    continue;
                //}
                if (!f.exists()) {
                    continue;
                }
                ArrayList<String> outputFiles = new ArrayList<String>();
                outputFiles.add(contoursDir + name + ".osm.gz");

                long osmlen = f.length();
                // split big contours file
                if (osmlen > 30000000) {  // 30 MB

                    // run splitter
                    String[] args = new String[]{
                        "--max-areas=1", "--max-nodes=1000000", "--mixed",
                        "--status-freq=0", "--output-dir=" + contoursDir,
                        "--mapid=" + d8.format(Long.parseLong(name) + 1), contoursDir + name + ".osm.gz"
                    };
                    Runtime.getRuntime().gc();
                    try {
                        while (Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory()
                                - Runtime.getRuntime().totalMemory() < 500 * 1024 * 1024) {
                            setStatus(coords + "Not enough memory - waiting"
                                    + " - " + region.name + " " + perc + " % completed.");
                            Thread.sleep(10000);
                            Runtime.getRuntime().gc();
                        }
                        setStatus(coords + "Splitting contour data "
                                + " - " + region.name + " " + perc + " % completed.");
                        //uk.me.parabola.splitter.Main.main(args);
                        Osm2garmin.runExternal("uk.me.parabola.splitter.Main", "main", "splitter", null, args, this);
                    } catch (Exception ex) {
                        Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
                        setState(ERROR);
                        setStatus(ex.getMessage());
                        region.setState(Region.ERROR);
                        return;
                    }
                    if (new File(contoursDir + d8.format(Long.parseLong(name) + 1) + ".osm.gz").exists()) {
                        f.delete();
                        outputFiles.clear();
                        for (int i = 1; i < 1000; i++) {
                            String fname = contoursDir + d8.format(Long.parseLong(name) + i) + ".osm.gz";
                            if (!new File(fname).exists()) {
                                break;
                            }
                            outputFiles.add(fname);
                        }
                    }

                    new File(contoursDir + name + ".osm.gz").delete();
                    new File(contoursDir + "areas.list").delete();
                    new File(contoursDir + "template.args").delete();
                }
                String[] osmFiles = outputFiles.toArray(new String[0]);

                if (osmlen > 20) {
                    // transform splitted contours files to Garmin format
                    setStatus(coords + "Convert contours to Garmin "
                            + " - " + region.name + " " + perc + " % completed.");
                    String args[] = new String[]{
                        "--draw-priority=10000", "--transparent",
                        "--merge-lines", "--output-dir=" + contoursDir
                    };
                    ArrayList<String> aa = new ArrayList<String>();
                    aa.addAll(Arrays.asList(args));
                    aa.addAll(Arrays.asList(osmFiles));
                    args = aa.toArray(new String[0]);
                    try {
                        //uk.me.parabola.mkgmap.main.Main.main(args);
                        Osm2garmin.runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", null, args, this);
                    } catch (Exception ex) {
                        Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
                        setState(ERROR);
                        setStatus(ex.getMessage());
                        region.setState(Region.ERROR);
                        return;
                    }
                }

                for (int i = 0; i < osmFiles.length; i++) {
                    new File(osmFiles[i]).delete();
                }
                boolean noFile = true;
                for (String file : osmFiles) {
                    if (new File(file.replace(".osm.gz", ".img")).exists()) {
                        noFile = false;
                        break;
                    }
                }

                if (!noFile) {
                    // pack files
                    setStatus(coords + "Packing files for later use "
                            + " - " + region.name + " " + perc + " % completed.");
                    try {
                        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                                new FileOutputStream(new File(contoursDir + srtmName + ".zip"))));
                        for (int i = 0; i < osmFiles.length; i++) {
                            String fname = osmFiles[i].replace(".osm.gz", ".img");
                            File file = new File(fname);
                            if (!file.exists()) {
                                continue;
                            }
                            InputStream is = new BufferedInputStream(new FileInputStream(file));
                            out.putNextEntry(new ZipEntry("/" + file.getName()));
                            int len;
                            while ((len = is.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.closeEntry();
                            is.close();
                            file.delete();
                        }
                        out.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    // if data ready, unpack file to region dir
                    setStatus(coords + "Copying contours data "
                            + " - " + region.name + " " + perc + " % completed.");
                    File zipFile = new File(contoursDir + srtmName + ".zip");
                    if (zipFile.length() > 0) {
                        unpackFiles(zipFile);
                    }
                }
            }
        }
        setStatus(region.name + " contours update completed.");
        setState(COMPLETED);
    }

    private void unpackFiles(File zipFile) {
        String destDir = parameters.getProperty("maps_dir");
        if (destDir.equals("")) {
            destDir = region.name;
        } else {
            if (!destDir.endsWith("/")) {
                destDir = destDir + "/";
            }
            destDir = destDir + region.name;
        }
        try {
            ZipInputStream zip = new ZipInputStream(new BufferedInputStream(
                    new FileInputStream(zipFile)));
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                String destname = entry.getName();
                File destFile = new File(destDir + destname);
                if (destFile.exists()) {
                    destFile.delete();
                }
                OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile));
                int len;
                while ((len = zip.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                os.close();
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
            zip.close();
        } catch (IOException ex) {
            Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
