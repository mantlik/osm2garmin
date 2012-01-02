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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mantlik.osm2garmin.srtm2osm.Contour;
import org.mantlik.osm2garmin.srtm2osm.Contours;
import org.mantlik.osm2garmin.srtm2osm.Point;
import org.mantlik.osm2garmin.srtm2osm.Srtm;

/**
 *
 * @author fm
 */
public class Srtm2Osm extends ThreadProcessor {

    private int lat, lon;
    private OutputStream osm;

    /**
     *
     * @param parameters
     * @param lat
     * @param lon
     * @param osm
     */
    public Srtm2Osm(Properties parameters, int lat, int lon, OutputStream osm) {
        super(parameters);
        super.parameters = parameters;
        this.lat = lat;
        this.lon = lon;
        this.osm = osm;
    }

    @Override
    public void run() {
        ArrayList<Contour> contours = new ArrayList<Contour>();
        int srtmStep = Integer.parseInt(parameters.getProperty("srtm_step", "2"));
        String coords;
        double offsLat = Double.parseDouble(parameters.getProperty("srtm_offs_lat"));
        double offsLon = Double.parseDouble(parameters.getProperty("srtm_offs_lon"));
        int minorInterval = Integer.parseInt(parameters.getProperty("contour_minor_interval"));
        int mediumInterval = Integer.parseInt(parameters.getProperty("contour_medium_interval"));
        int majorInterval = Integer.parseInt(parameters.getProperty("contour_major_interval"));
        int plotMinorThreshold = Integer.parseInt(parameters.getProperty("plot_minor_threshold"));
        int plotMediumThreshold = Integer.parseInt(parameters.getProperty("plot_medium_threshold"));
        int contoursDensity = Integer.parseInt(parameters.getProperty("contours_density", "1"));
        ArrayList<Contour> gridcont = new ArrayList<Contour>();
        for (int la = 0; la < srtmStep; la++) {
            for (int lo = 0; lo < srtmStep; lo++) {
                coords = Math.abs(lat + la) + (lat + la > 0 ? "N " : "S ") + Math.abs(lon + lo) + (lon + lo > 0 ? "E" : "W");
                setStatus("Contours " + coords + ": Downloading SRTM data.");
                Srtm srtm = Srtm.get(lon + lo, lat + la, parameters);
                if (srtm == null) {
                    setStatus("Contours " + coords + ": No SRTM data.");
                    continue;
                }
                setStatus("Contours " + coords + ": Preparing data.");
                float[][] data = new float[121][121];
                setStatus("Contours " + coords + ": Making contours.");
                try {
                    gridcont.clear();
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 10; j++) {
                            int mindata = Integer.MAX_VALUE;
                            int maxdata = Integer.MIN_VALUE;
                            for (int ii = 0; ii < 121; ii++) {
                                for (int jj = 0; jj < 121; jj++) {
                                    int dd = srtm.data[i * 120 + ii][j * 120 + jj];
                                    if ((dd > -10000) && (dd < 10000)) {
                                        mindata = Math.min(mindata, dd);
                                        maxdata = Math.max(maxdata, dd);
                                        data[ii][jj] = dd;
                                    } else {
                                        data[ii][jj] = 32768.0f;
                                    }
                                }
                            }
                            int extent = maxdata - mindata;
                            int interval = extent < plotMinorThreshold ? minorInterval : mediumInterval;
                            interval = extent < plotMediumThreshold ? interval : majorInterval;
                            if (extent < 2 * interval) {
                                if (extent > 15) {
                                    interval = 10;
                                } else if (extent > 10) {
                                    interval = 5;
                                } else if (extent > 5) {
                                    interval = 2;
                                } else {
                                    interval = 1;
                                }
                            }
                            Contours contoursMaker = new Contours(data, 121, 121, 1.0d * lat + la + i / 10d - offsLat,
                                    1.0d * lon + lo + j / 10d - offsLon, 1d / 1200d, interval, 32768.0d);
                            ArrayList c = contoursMaker.makeContours();
                            addContours(gridcont, c, null);
                            setStatus("Contours " + coords + ": Making contours - " + (10 * i + j) + " %");
                        }
                    }
                    String prefix = "Contours " + coords + ": Joining contours " + 
                            gridcont.size() + "->" + contours.size() + " ";
                    addContours(contours, gridcont, prefix);
                } catch (Exception ex) {
                    Logger.getLogger(Srtm2Osm.class.getName()).log(Level.SEVERE, "", ex);
                    setStatus("Contours " + coords + ": Contours creation failed.");
                    continue;
                }
            }
        }
        coords = Math.abs(lat) + (lat > 0 ? "N " : "S ") + Math.abs(lon) + (lon > 0 ? "E" : "W");
        setStatus("Contours " + coords + ": Checking contours density.");
        checkContoursDensity(contours, 1200 * srtmStep + 1, 1200 * srtmStep + 1, 1.0d * lat - offsLat,
                1.0d * lon - offsLon, 1d / 1200d, contoursDensity, majorInterval);
        if (contours == null || contours.isEmpty()) {
            setStatus("Contours " + coords + ": No contours created.");
            setState(COMPLETED);
            try {
                osm.close();
            } catch (IOException ex) {
                Logger.getLogger(Srtm2Osm.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            return;
        }
        // export contours to file
        setStatus("Contours " + coords + ": Creating OSM file.");
        PrintStream ss = new PrintStream(osm);
        ss.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        ss.println("<osm version=\"0.5\" generator=\"Srtm2Osm.java\">");
        long id, wayId;
        id = Long.parseLong(parameters.getProperty("contour_start_id"));
        wayId = Long.parseLong(parameters.getProperty("contour_start_way_id"));
        for (int i = 0; i < contours.size(); i++) {
            Contour contour = contours.get(i);
            int level = (int) contour.z;
            boolean major = (level % majorInterval) == 0;
            contour.outputOsm(wayId, id, major, ss);
            wayId++;
            id += contour.data.size();
            setStatus("Contours " + coords + ": Creating OSM file - " + (int) (100.0 * i / contours.size()) + " %");
        }
        ss.println("</osm>");
        ss.close();
        setState(COMPLETED);
    }

    /*
     * Add contours from list c to listo contours, merge where possible
     */
    private void addContours(ArrayList<Contour> contours, ArrayList<Contour> add, String logPrefix) {
        for (int i = 0; i < add.size(); i++) {
            Contour c = add.get(i);
            if (c.getData().size() < 2) {
                continue;
            }
            if (contours.isEmpty() || c.isClosed()) {
                contours.add(c);
                continue;
            }
            boolean finished = false;
            for (Contour cc : contours) {
                if (cc.isClosed()) {
                    continue;
                }
                Point start = cc.getData().get(0);
                Point end = cc.getData().get(cc.getData().size() - 1);
                Point newstart = c.getData().get(0);
                Point newend = c.getData().get(c.getData().size() - 1);
                if (end.equals(newstart)) {
                    contours.remove(cc);
                    contours.add(joinContours(cc, c));
                    finished = true;
                    break;
                }
                if (newend.equals(start)) {
                    contours.remove(cc);
                    contours.add(joinContours(c, cc));
                    finished = true;
                    break;
                }
            }
            if (!finished) {
                contours.add(c);
            }
            if (logPrefix != null) {
                setStatus(logPrefix + (int)(100.0*i/add.size()) + " %");
            }
        }
    }

    /*
     * join c2 to the end of c1
     * suppose the last point of c1 equals to the first point of c2 (not
     * checked)
     */
    private Contour joinContours(Contour c1, Contour c2) {
        for (int i = 1; i < c2.getData().size(); i++) {
            c1.getData().add(c2.getData().get(i));
        }
        c1.setClosed(c1.getData().get(0).equals(c1.getData().get(c1.getData().size() - 1)));
        return c1;
    }

    /*
     * Delete segments when a cell contains more than contoursDensity segments;
     * keep major contours if they fit maximum density in a cell
     */
    private void checkContoursDensity(ArrayList<Contour> contours, int nlat, int nlon,
            double startlat, double startlon, double delta, int contoursDensity, int majorInterval) {
        if (contours.isEmpty()) {
            return;
        }
        int[][] density = new int[nlat][nlon];
        int[][] majorDensity = new int[nlat][nlon];
        for (Contour contour : contours) {
            ArrayList<Point> data = contour.getData();
            for (int i = 1; i < data.size(); i++) {
                Point p1 = data.get(i - 1);
                Point p2 = data.get(i);
                double la = (p1.getX() + p2.getX()) / 2;
                double lo = (p1.getY() + p2.getY()) / 2;
                int ii = (int) ((la - startlat) / delta);
                int jj = (int) ((lo - startlon) / delta);
                density[ii][jj]++;
                if (((int) contour.getZ()) % majorInterval == 0) {
                    majorDensity[ii][jj]++;
                }
            }
        }
        for (int k = 0; k < contours.size(); k++) {
            Contour contour = contours.get(k);
            ArrayList<Point> data = contour.getData();
            for (int i = 1; i < data.size(); i++) {
                Point p1 = data.get(i - 1);
                Point p2 = data.get(i);
                double la = (p1.getX() + p2.getX()) / 2;
                double lo = (p1.getY() + p2.getY()) / 2;
                int ii = (int) ((la - startlat) / delta);
                int jj = (int) ((lo - startlon) / delta);
                if ((majorDensity[ii][jj] > contoursDensity)
                        || (density[ii][jj] > contoursDensity
                        && (contour.getZ() % majorInterval != 0))) {
                    // remove segment from contour
                    contour.setClosed(false);
                    if (i == 1) { // first segment, delete first point
                        data.remove(0);
                    } else if (i == (data.size() - 1)) { //last segment, delete last point
                        data.remove(i);
                    } else {
                        // middle segment - break contour
                        Contour newContour = new Contour();
                        newContour.setZ(contour.getZ());
                        while (data.size() > i) {
                            newContour.getData().add(data.remove(i));
                        }
                        contours.add(newContour);
                    }
                }
            }
        }
    }
}
