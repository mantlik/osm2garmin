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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fm
 */
public class Osm2garmin implements PropertyChangeListener {

    /**
     *
     */
    private Properties parameters = new Properties();
    File planetFile, oldPlanetFile, contoursDir;
    private boolean planetDownloadReady = false;
    /**
     *
     */
    public PlanetDownloader planetDownloader;
    /**
     *
     */
    public PlanetUpdateDownloader planetUpdateDownloader = null;
    private boolean planetUpdateDownloaded = false;
    /**
     *
     */
    public PlanetUpdater planetUpdater = null;
    private boolean planetUpdated = false;
    /**
     *
     */
    public ArrayList<Region> regions = new ArrayList<Region>();
    private boolean regionsReady = false;
    /**
     *
     */
    public boolean allDone = false;
    /**
     *
     */
    public PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    static boolean stop = false;
    /**
     *
     */
    private static String userdir = "./"; // used by GUI to save temporary info

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        boolean filesOK = true;
        if (!new File("settings.properties").exists()) {
            Utilities.copyFile(Osm2garmin.class.getResourceAsStream("settings.properties"),
                    new File("settings.properties"));
            filesOK = false;
        }
        Properties props = Utilities.loadProperties();
        if (props == null) {
            System.exit(99);
        }
        String regions = props.getProperty("regions");
        if (!new File(regions).exists()) {
            Utilities.copyFile(Osm2garmin.class.getResourceAsStream(regions),
                    new File(regions));
            filesOK = false;
        }
        if (!filesOK) {
            System.out.println("Control files created:");
            System.out.println(new File("settings.properties").getAbsolutePath());
            System.out.println(new File(regions).getAbsolutePath());
            System.out.println("Edit these files according to your needs and run again.");
            System.exit(98);
        }
        if (Runtime.getRuntime().maxMemory() < 1300000000) {
            System.out.println("Total available memory: " + Runtime.getRuntime().maxMemory());
            System.out.print("NOT ENOUGH MEMORY to proceed. Run with -Xmx1400m command-line argument.");
            System.exit(97);
        }
        try {
            System.setErr(new PrintStream(new FileOutputStream(new File("messages.log"))));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PlanetUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        int retcode = new Osm2garmin().start(props);
        System.exit(retcode);
    }

    /**
     * Program execution
     *
     * @param params
     * @return 0 means OK
     */
    public int start(Properties params) {
        stop = false;
        boolean splitterBusy = false;
        boolean contoursSplitterBusy = false;
        parameters = params;
        userdir = parameters.getProperty("userdir", userdir);
        // Delete libraries - ensure newest libraries versions are used
        File libdir = new File(userdir + "lib");
        Utilities.deleteFile(libdir);
        // parse regions
        File r = new File(parameters.getProperty("regions"));
        try {
            if (!r.exists()) {
                Utilities.copyFile(Osm2garmin.class.getResourceAsStream("regions.txt"),
                        r);
            }
            Scanner s = new Scanner(new FileInputStream(r));
            int familyID = 5001;
            while (s.hasNext()) {
                String[] l = s.nextLine().split(" +");
                // Comment starts with #
                // GUI temporarily excluded region starts with x
                if (l.length >= 5 && !(l[0].startsWith("#") || l[0].startsWith("x"))) {
                    Region region = new Region(l[4], parameters.getProperty("maps_dir"),
                            parameters.getProperty("delete_old_maps", "false").equals("true"), familyID);
                    familyID++;
                    region.lon1 = Float.parseFloat(l[0]);
                    region.lat1 = Float.parseFloat(l[1]);
                    region.lon2 = Float.parseFloat(l[2]);
                    region.lat2 = Float.parseFloat(l[3]);
                    regions.add(region);
                    region.changeSupport.addPropertyChangeListener(this);
                    //System.out.println(region.name+": "+region.lon1+" "+region.lat1
                    //        +" to "+region.lon2+" "+region.lat2);
                }
            }
            s.close();
            // copy cities15000.zip
            if (!new File(userdir + "cities15000.zip").exists()) {
                Utilities.copyFile(Osm2garmin.class.getResourceAsStream("cities15000.zip"),
                        new File(userdir + "cities15000.zip"));
            }
        } catch (Exception ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Problem parsing regions.", ex);
            return 98;
        }
        if (regions.isEmpty()) {
            System.out.println("No regions read from the file " + parameters.getProperty("regions") + " exiting.");
            return 97;
        } else {
            System.out.println("Found " + regions.size() + " regions to process.");
        }
        contoursDir = new File(parameters.getProperty("contours_dir"));
        if (!contoursDir.exists()) {
            contoursDir.mkdirs();
        }

        // download planet file if needed
        Utilities.loadProperties();
        planetDownloader = new PlanetDownloader(parameters);
        planetDownloader.changeSupport.addPropertyChangeListener(this);
        System.out.println("Planet file download started.");
        while (!allDone) {
            Utilities.loadProperties();
            if (planetDownloadReady && planetUpdateDownloader == null) {
                // start Planet Update Downloader
                planetUpdateDownloader = new PlanetUpdateDownloader(parameters);
                planetUpdateDownloader.changeSupport.addPropertyChangeListener(this);
                System.out.println("Planet updates download started.");
            }
            // check planet download status
            if (!planetDownloadReady) {
                planetDownloadReady = planetDownloader.lifeCycleCheck("Planet file download");
            }
            if ((!planetUpdateDownloaded) && (planetUpdateDownloader != null)) {
                planetUpdateDownloaded = planetUpdateDownloader.lifeCycleCheck("Planet updates download");
            }

            if (planetDownloadReady && planetUpdateDownloaded && (!planetUpdated)) {
                // update planet file from downloaded updates
                if (planetUpdater == null) {
                    planetUpdater = new PlanetUpdater(parameters, regions);
                    planetUpdater.changeSupport.addPropertyChangeListener(this);
                    System.out.println("Planet file update started.");
                }
                planetUpdated = planetUpdater.lifeCycleCheck("Planet file update");
            }

            // Regions processing
            regionsReady = true;
            boolean nextSplitterBusy = false;
            boolean nextContoursSplitterBusy = false;
            for (int reg = 0; reg < regions.size(); reg++) {
                Region region = regions.get(reg);
                // Check and create contours
                if (region.getState() == Region.NEW) {
                    if (!contoursSplitterBusy) {
                        contoursSplitterBusy = true;
                        nextContoursSplitterBusy = true;
                        region.setState(Region.MAKING_CONTOURS);
                        region.processor = new ContoursUpdater(region, parameters);
                        region.processor.changeSupport.addPropertyChangeListener(this);
                        System.out.println(region.name + " contours update started.");
                    }
                } else if (region.getState() == Region.MAKING_CONTOURS) {
                    nextContoursSplitterBusy = true;
                    if (region.processor.lifeCycleCheck(region.name + " contours update")) {
                        if (region.processor.getState() == ThreadProcessor.ERROR) {
                            region.setState(Region.ERROR);
                        } else {
                            region.setState(Region.CONTOURS_READY);
                        }
                    }
                } else if (region.getState() == Region.CONTOURS_READY) {
                    if ((!splitterBusy) && planetUpdated) {
                        splitterBusy = true;
                        nextSplitterBusy = true;
                        region.setState(Region.MAKING_OSM);
                        region.processor = new OsmMaker(region, parameters, region.splitterMaxAreas);
                        region.processor.changeSupport.addPropertyChangeListener(this);
                        System.out.println(region.name + " map creation started.");
                    }
                } else if (region.getState() == Region.MAKING_OSM) {
                    nextSplitterBusy = true;
                    if (region.processor.lifeCycleCheck(region.name)) {
                        if (region.processor.getState() == ThreadProcessor.ERROR) {
                            region.setState(Region.ERROR);
                        } else {
                            region.makeInstallers(reg);
                            region.setState(Region.READY);
                        }
                    }
                }

                if (region.getState() < Region.READY) {
                    regionsReady = false;
                }
            }
            // start new contour creation or splitting only if none running
            splitterBusy = nextSplitterBusy;
            contoursSplitterBusy = nextContoursSplitterBusy;

            if (planetUpdated && regionsReady) {
                allDone = true;
            }
        }
        return 0;
    }

    /**
     *
     */
    public void stop() {
        stop = true;
    }
    
    public Properties getParameters() {
        return parameters;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        changeSupport.firePropertyChange(evt);
    }

}
