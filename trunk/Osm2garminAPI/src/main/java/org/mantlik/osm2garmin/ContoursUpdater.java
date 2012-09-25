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
import java.util.zip.ZipEntry;
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
        super(parameters, false);
        this.region = region;
        start();
    }

    @Override
    public void run() {
        setStatus("Starting contours update - " + region.name);
        setProgress(0);
        String contoursDir = parameters.getProperty("contours_dir");
        Utilities.checkArgFiles(Utilities.getUserdir(this));
        String argsFileName = Utilities.getUserdir(this) + "contours.args";
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
                String srtmName = Srtm.getName(lo, la).substring(0, 7);
                String coords = "Contours " + (int) Math.abs(lat) + (lat > 0 ? "N " : "S ")
                        + (int) Math.abs(lon) + (lon > 0 ? "E" : "W") + ": ";

                // check existence of contours file
                setStatus(coords + "Unpacking contours data "
                        + " - " + region.name + " " + perc + " % completed.");

                File zipFile = new File(contoursDir + srtmName + ".zip");
                // typo in file name up to r34
                File zipFile3 = new File(contoursDir + srtmName + "..zip");
                // file created but not yet checked
                File zipFile2 = new File(contoursDir + srtmName + "_.zip");
                if (zipFile.exists() && (zipFile.length() > 0)) {
                    unpackFiles(zipFile);
                    if (zipFile2.exists()) {
                        if (Utilities.zipGetTotalLength(zipFile) == Utilities.zipGetTotalLength(zipFile2)) {
                            zipFile2.delete();
                        }
                    }
                    continue;
                } else if (zipFile3.exists()) {
                    if (zipFile3.length() > 0) {
                        unpackFiles(zipFile3);
                    }
                    zipFile3.renameTo(zipFile);
                    continue;
                }

                Utilities utils = Utilities.getInstance();
                try {
                    utils.waitExclusive(Srtm2Osm.class.getName(), this);
                } catch (InterruptedException ex) {
                    setState(ERROR);
                    setStatus("Interrupted.");
                    region.setState(Region.ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
                String outputName = contoursDir + name + ".osm.gz";
                srtm2osm = new Srtm2Osm(parameters, la, lo, outputName);
                while (srtm2osm.getState() == Srtm2Osm.RUNNING) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        utils.endExclusive(Srtm2Osm.class.getName());
                        setState(ERROR);
                        setStatus("Interrupted.");
                        region.setState(Region.ERROR);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                    setStatus(srtm2osm.getStatus() + " - " + region.name + " " + perc + " % completed.");
                }
                utils.endExclusive(Srtm2Osm.class.getName());
                if (srtm2osm.getState() == Srtm2Osm.ERROR) {
                    setState(ERROR);
                    setStatus(srtm2osm.getStatus());
                    region.setState(Region.ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
                File f = new File(outputName);
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
                        "--max-areas=10", "--max-nodes=1000000", "--output=pbf",
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
                        Utilities.getInstance().runExternal("uk.me.parabola.splitter.Main", "main", "splitter", args, this);
                    } catch (Exception ex) {
                        Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
                        setState(ERROR);
                        setStatus(ex.getMessage());
                        region.setState(Region.ERROR);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                    if (new File(contoursDir + d8.format(Long.parseLong(name) + 1) + ".osm.pbf").exists()) {
                        f.delete();
                        outputFiles.clear();
                        for (int i = 1; i < 1000; i++) {
                            String fname = contoursDir + d8.format(Long.parseLong(name) + i) + ".osm.pbf";
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
                        "--output-dir=" + contoursDir,
                        "-c", argsFileName};
                    ArrayList<String> aa = new ArrayList<String>();
                    aa.addAll(Arrays.asList(args));
                    aa.addAll(Arrays.asList(osmFiles));
                    args = aa.toArray(new String[0]);
                    try {
                        //uk.me.parabola.mkgmap.main.Main.main(args);
                        Utilities.getInstance().runExternal("uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", args, this);
                    } catch (Exception ex) {
                        Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
                        setState(ERROR);
                        setStatus(ex.getMessage());
                        region.setState(Region.ERROR);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                }
                System.gc();

                ArrayList<String> imgFiles = new ArrayList<String>();
                String nn;
                for (String file : osmFiles) {
                    nn = file.replace(".osm.gz", ".img");
                    nn = nn.replace(".osm.pbf", ".img");
                    if (new File(nn).exists()) {
                        imgFiles.add(nn);
                    }
                }

                if (!imgFiles.isEmpty()) {
                    // pack files
                    setStatus(coords + "Packing files for later use "
                            + " - " + region.name + " " + perc + " % completed.");
                    try {
                        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                                new FileOutputStream(new File(contoursDir + srtmName + ".zip"))));
                        for (String fname : imgFiles) {
                            File file = new File(fname);
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
                    long zipLength = Utilities.zipGetTotalLength(zipFile);
                    if (zipLength > 0) {
                        if (zipFile2.exists() && zipFile2.length() > 0) {
                            // check pass
                            long zip2length = Utilities.zipGetTotalLength(zipFile2);
                            if (zipLength == zip2length) {
                                // checked, contours OK
                                unpackFiles(zipFile);
                                if (!zipFile2.delete()) {
                                    try {
                                        Thread.sleep(100);
                                        if (!zipFile2.delete()) {
                                            zipFile2.deleteOnExit();
                                        }
                                    } catch (InterruptedException ex) {
                                    }
                                }
                            } else if (zipLength > zip2length) {
                                // use new contours and allow one more check
                                unpackFiles(zipFile);
                                if (!zipFile2.delete()) {
                                    try {
                                        Thread.sleep(100);
                                        if (!zipFile2.delete()) {
                                            zipFile2.deleteOnExit();
                                        }
                                    } catch (InterruptedException ex) {
                                    }
                                }
                                if (!((Srtm2Osm) srtm2osm).allSrtms) {
                                    zipFile.renameTo(zipFile2);
                                }
                            } else {
                                // things go wrong, discard current zipFile
                                // and do one more check
                                unpackFiles(zipFile2);
                                if (!zipFile.delete()) {
                                    try {
                                        Thread.sleep(100);
                                        if (!zipFile.delete()) {
                                            zipFile.deleteOnExit();
                                        }
                                    } catch (InterruptedException ex) {
                                    }
                                }
                            }
                        } else {
                            unpackFiles(zipFile);
                            if (!((Srtm2Osm) srtm2osm).allSrtms) {
                                // not all srtms included
                                zipFile.renameTo(zipFile2);
                            }
                        }
                    }
                }
                for (int i = 0; i < osmFiles.length; i++) {
                    File osmFile = new File(osmFiles[i]);
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
            }
        }
        setStatus(region.name + " contours update completed.");
        setProgress(100);
        setState(COMPLETED);
        synchronized (this) {
            notify();
        }
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
        Utilities.unpackZipFiles(zipFile, destDir);
    }
}
