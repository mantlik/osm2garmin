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
package org.mantlik.osm2garmin.srtm2osm;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author fm
 */
public class Srtm {

    public int[][] data = new int[1201][1201];
    private double[][] easting = new double[1201][1201];
    private double[][] northing = new double[1201][1201];
    private String path = "c:\\SRTM";
    private String url = "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/";
    private int lon, lat;
    static final String[] REGIONS = {"Eurasia", "Africa", "Australia", "Islands",
        "North_America", "South_America"};
    int lastIndex = 1;
    static String[] lists = new String[REGIONS.length];
    private static final Map<String, Integer> regionMap = new HashMap<String, Integer>();

    Srtm(Properties parameters) {
        this.setPath(parameters.getProperty("srtm_dir"));
        this.setUrl(parameters.getProperty("srtm_url"));
    }

    public static String getName(int lon, int lat) {
        String dirlat = "N";
        if (lat < 0) {
            dirlat = "S";
        }
        String dirlon = "E";
        if (lon < 0) {
            dirlon = "W";
        }
        String st = String.valueOf(Math.abs(lat));
        while (st.length() < 2) {
            st = "0" + st;
        }
        String fname = dirlat + st;
        st = String.valueOf(Math.abs(lon));
        while (st.length() < 3) {
            st = "0" + st;
        }
        fname = fname + dirlon + st + ".hgt";
        return fname;
    }

    public boolean load(int lon, int lat) {
// loads SRTM data for the lon,lat
        String fname = getName(lon, lat);
        String region = findRegion(fname, path, url);
        if (region == null) {
            return false;
        }
        File srtmFile;
        File srtmZipFile;
        if (!path.equals("")) {
            srtmFile = new File(path + "/" + region + "/" + fname);
            srtmZipFile = new File(path + "/" + region + "/" + fname + ".zip");
        } else {
            srtmFile = new File(fname);
            srtmZipFile = new File(fname + ".zip");
        }
        InputStream s;
        if (srtmZipFile.exists()) {
            try {
                // try zip file
                ZipFile zf = new ZipFile(srtmZipFile);
                ZipEntry entry = zf.getEntry(fname);
                if (entry == null) {
                    throw(new IOException("Can't read zip file " + srtmZipFile));
                }
                s = zf.getInputStream(entry);
                s.close();
            } catch (IOException ex) {
                // broken download, try again
                Logger.getLogger(Srtm.class.getName()).log(Level.WARNING, ex.getMessage());
                srtmZipFile.delete();
            }
        }
        if (!(srtmFile.exists() || srtmZipFile.exists() || download(fname))) {
            //SRTMS.SRTMS.put(100 * lon + lat, null);
            return false;
        }
        if (srtmFile.exists()) {
            try {
                s = new FileInputStream(srtmFile);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
                return false;
            }
        } else { // try zip file
            try {
                // try zip file
                ZipFile zf = new ZipFile(srtmZipFile);
                ZipEntry entry = zf.getEntry(fname);
                if (entry == null) {
                    Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, 
                            "Can''t extract {0} from the archive {1}", 
                            new Object[]{fname, srtmZipFile.getAbsolutePath()});
                    return false;
                }
                s = zf.getInputStream(entry);

            } catch (IOException ex) {
                Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
                return false;
            }
        }
        int i = 0;
        while (i <= 1200) {
            int j = 0;
            while (j <= 1200) {
                try {
                    data[1200 - i][j] = 256 * s.read() + s.read();
                } catch (IOException ex) {
                    Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
                    return false;
                }
                j++;
            }
            i++;
        }
        try {
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }
        this.lon = lon;
        this.lat = lat;
        return true;
    }

    private boolean download(String fname) {
        File output;
        String region = findRegion(fname, path, url);
        if (region == null) {
            return false;
        }
        if (path.equals("")) {
            output = new File(region + "/" + fname + ".zip");
        } else {
            output = new File(path + "/" + region + "/" + fname + ".zip");
        }
        boolean result = downloadFile(getUrl() + region
                + "/" + fname + ".zip", output);
        // fix SRTM 2.1 naming problem in North America
        if ((!result) && fname.startsWith("N5") && region.equalsIgnoreCase("North_America")) {
            if (downloadFile(getUrl() + region
                    + "/" + fname.replace(".hgt", "hgt") + ".zip", output)) {
                return true;
            }
        }
        return result;
    }

    /*
     * Returns region name for a file
     */
    private static String findRegion(String fname, String srtmPath, String url) {
        if (regionMap.isEmpty()) {
            System.err.println("Downloading SRTM map data.");
            String region;
            for (int i = 0; i < REGIONS.length; i++) {
                region = REGIONS[i];
                String indexPath = region;
                if (!srtmPath.equals("")) {
                    indexPath = srtmPath + "/" + indexPath;
                }
                File indexDir = new File(indexPath);
                if (!indexDir.exists()) {
                    indexDir.mkdirs();
                }
                indexPath += ".index.html";
                File indexFile = new File(indexPath);
                if (!indexFile.exists()) {
                    if (!downloadRegionIndex(i, srtmPath, url)) {
                        // download error, try again with the next attempt
                        regionMap.clear();
                        return null;
                    }
                }
                try {
                    Scanner scanner = new Scanner(indexFile);
                    while (scanner.hasNext()) {
                        String line = scanner.next();
                        if (line.contains("href=\"")) {
                            int index = line.indexOf(".hgt.zip") - 7;
                            if (index >= 0) {
                                String srtm = line.substring(index, index + 7);
                                regionMap.put(srtm, i);
                            } else {
                                index = line.indexOf("hgt.zip") - 7;
                                if (index >= 0) {
                                    String srtm = line.substring(index, index + 7);
                                    regionMap.put(srtm, i);
                                }
                            }
                        }
                    }
                    scanner.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("SRTM map filled in with " + regionMap.size() + " entries.");
        }
        String name = fname.replace(".hgt", "");
        if (regionMap.containsKey(name)) {
            return REGIONS[regionMap.get(name)];
        }
        return null;
    }

    private static boolean downloadRegionIndex(int region, String srtmPath, String url) {
        String regionIndex = REGIONS[region] + ".index.html";
        if (!srtmPath.equals("")) {
            regionIndex = srtmPath + "/" + regionIndex;
        }
        File regionIndexFile = new File(regionIndex);
        return downloadFile(url + REGIONS[region] + "/", regionIndexFile);
    }

    private static boolean downloadFile(String urlAddress, File output) {
        URL url1;
        InputStream inputs;
        BufferedOutputStream outputs;
        try {
            url1 = new URL(urlAddress);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }
        try {
            inputs = url1.openStream();
            outputs = new BufferedOutputStream(new FileOutputStream(output));
            int i = 0;
            int ch = 0;
            while (ch >= 0) {
                ch = inputs.read();
                if (ch >= 0) {
                    outputs.write(ch);
                }
                i++;
                if (i % 1000 == 0) {
                    // progress.progress(i);
                }
            }
            inputs.close();
            outputs.close();

        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            Logger.getLogger(Srtm.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }

        return true;
    }

    public final void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public final void setUrl(String url) {
        this.url = url;
    }

    public static boolean exists(int lon, int lat, Properties props) {
        String fname = getName(lon, lat);
        String region = findRegion(fname, props.getProperty("srtm_dir"), props.getProperty("srtm_url"));
        return (region != null);
    }

    public static Srtm get(int lon, int lat, Properties props) {
        Srtm srtm = new Srtm(props);
        if (srtm.load(lon, lat)) {
            return srtm;
        }
        return null;
    }

    /*
     * public double[] getCoords(int i, int j, CrsTransformation srtm2projected)
     * { if (easting[i][j] != 0d && northing[i][j] != 0d) { return new
     * double[]{easting[i][j], northing[i][j], data[i][j]}; } double di = i;
     * double dj = j; double ptLat = 1.0d * lat + project.getSrtmDlat() + di /
     * 1200d; double ptLon = 1.0d * lon + project.getSrtmDlon() + dj / 1200d;
     * double[] from = new double[3]; double[] to = new double[3]; from[0] =
     * ptLat; from[1] = ptLon; from[2] = data[i][j]; try { to =
     * srtm2projected.transform(from); } catch (Exception ex) {
     * Exceptions.printStackTrace(ex); } easting[i][j] = to[0]; northing[i][j] =
     * to[1]; return to; }
     */

    /*
     * Get SRTM elevation in meters for lon and lat WGS-84 coordinates
     */
    public static int getData(double lon, double lat, Properties props) {
        Srtm srtm = get((int) Math.floor(lon), (int) Math.floor(lat), props);
        int i = (int) Math.round(1200d * (lat - Math.floor(lat)));
        int j = (int) Math.round(1200d * (lon - Math.floor(lon)));
        if (srtm == null) {
            return -32768; // SRTM NaN
        }
        return srtm.data[i][j];
    }
}
