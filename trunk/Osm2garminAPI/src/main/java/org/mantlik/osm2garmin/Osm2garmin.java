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
        parameters = params;
        userdir = parameters.getProperty("userdir", userdir);
        // Delete libraries - ensure newest libraries versions are used
        File libdir = new File(userdir + "lib");
        Utilities.deleteFile(libdir);
        // parse regions
        File r = new File(parameters.getProperty("regions"));
        File regDir = r.getParentFile();
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
                    System.out.println("Preparing region " + l[4] + " directory.");
                    Region region = new Region(l[4], parameters.getProperty("maps_dir"),
                            parameters.getProperty("delete_old_maps", "false").equals("true")
                            && (!parameters.getProperty("skip_planet_update", "false").equals("true")),
                            familyID);
                    familyID++;
                    region.lon1 = Float.parseFloat(l[0]);
                    region.lat1 = Float.parseFloat(l[1]);
                    region.lon2 = Float.parseFloat(l[2]);
                    region.lat2 = Float.parseFloat(l[3]);
                    if ((regDir != null)) {
                        File polyFile = new File(regDir, region.name + ".poly");
                        if (polyFile.exists()) {
                            region.polygonFile = polyFile;
                            float[] f = Region.envelope(polyFile);
                            region.lon1 = f[0];
                            region.lat1 = f[1];
                            region.lon2 = f[2];
                            region.lat2 = f[3];
                        } else {
                            region.polygonFile = null;
                        }
                    }
                    regions.add(region);
                    region.changeSupport.addPropertyChangeListener(this);
                    //System.out.println(region.name+": "+region.lon1+" "+region.lat1
                    //        +" to "+region.lon2+" "+region.lat2);
                } else if (l[0].startsWith("x")) {
                    familyID++;
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

        Utilities.loadProperties();

        // start contours updates
        final ThreadProcessor contoursProcessor = new ThreadProcessor(parameters) {
            @Override
            public void run() {
                for (int reg = 0; reg < regions.size(); reg++) {
                    Region region = regions.get(reg);
                    region.processor = new ContoursUpdater(region, parameters);
                    Utilities.getInstance().addProcessToMonitor(region.processor);
                    region.processor.changeSupport.addPropertyChangeListener(Osm2garmin.this);
                    System.out.println(region.name + " contours update started.");
                    region.setState(Region.MAKING_CONTOURS);
                    final ThreadProcessor preg = region.processor;
                    try {
                        synchronized (preg) {
                            preg.wait();
                        }
                    } catch (InterruptedException ex) {
                        region.processor.setStatus("Interrupted.");
                        region.processor.setState(ContoursUpdater.ERROR);
                        synchronized (region) {
                            region.notify();
                        }
                        return;
                    }
                    Utilities.getInstance().removeMonitoredProcess(region.processor);
                    region.setState(Region.CONTOURS_READY);
                    synchronized (region) {
                        region.notify();
                    }
                }
            }
        };

        // download planet file if needed
        planetDownloader = new PlanetDownloader(parameters);
        planetDownloader.changeSupport.addPropertyChangeListener(this);
        Utilities.getInstance().addProcessToMonitor(planetDownloader);
        System.out.println("Planet file download started.");
        final ThreadProcessor pd = planetDownloader;
        synchronized (pd) {
            if (pd.getState() == PlanetDownloader.RUNNING) {
                try {
                    pd.wait();
                } catch (InterruptedException ex) {
                    planetDownloader.setStatus("Interrupted.");
                    planetDownloader.setState(PlanetDownloader.ERROR);
                    return 1;
                }
            }
        }
        Utilities.getInstance().removeMonitoredProcess(planetDownloader);
        if (planetDownloader.getState() != PlanetDownloader.COMPLETED) {
            return 2;
        }

        // start Planet Update Downloader
        planetUpdateDownloader = new PlanetUpdateDownloader(parameters);
        planetUpdateDownloader.changeSupport.addPropertyChangeListener(this);
        Utilities.getInstance().addProcessToMonitor(planetUpdateDownloader);
        System.out.println("Planet updates download started.");
        final ThreadProcessor pud = planetUpdateDownloader;
        synchronized (pud) {
            if (pud.getState() == PlanetUpdateDownloader.RUNNING) {
                try {
                    pud.wait();
                } catch (InterruptedException ex) {
                    planetUpdateDownloader.setStatus("Interrupted.");
                    planetUpdateDownloader.setState(PlanetDownloader.ERROR);
                    return 3;
                }
            }
        }
        Utilities.getInstance().removeMonitoredProcess(planetUpdateDownloader);
        if (planetUpdateDownloader.getState() != PlanetUpdateDownloader.COMPLETED) {
            return 4;
        }

        // start Planet updater
        planetUpdater = new PlanetUpdater(parameters, regions);
        planetUpdater.changeSupport.addPropertyChangeListener(this);
        System.out.println("Planet file update started.");
        Utilities.getInstance().addProcessToMonitor(planetUpdater);
        final ThreadProcessor procpu = planetUpdater;
        synchronized (procpu) {
            if (procpu.getState() == PlanetUpdater.RUNNING) {
                try {
                    procpu.wait();
                } catch (InterruptedException ex) {
                    planetUpdater.setStatus("Interrupted.");
                    planetUpdater.setState(PlanetDownloader.ERROR);
                    return 3;
                }
            }
        }
        Utilities.getInstance().removeMonitoredProcess(planetUpdater);
        if (planetUpdater.getState() != PlanetUpdater.COMPLETED) {
            return 4;
        }

        // process regions
        for (int reg = 0; reg < regions.size(); reg++) {
            Region region = regions.get(reg);
            // wait for finishing contours unpacking if needed
            while (region.getState() == Region.MAKING_CONTOURS) {
                synchronized (region) {
                    try {
                        region.wait();
                    } catch (InterruptedException ex) {
                        region.processor.setStatus("Interrupted.");
                        region.processor.setState(PlanetDownloader.ERROR);
                        return 5;
                    }
                }
            }
            if (region.getState() != Region.CONTOURS_READY) {
                return 6;
            }
            // Making map
            region.processor = new OsmMaker(region, parameters, region.splitterMaxAreas);
            region.processor.changeSupport.addPropertyChangeListener(this);
            System.out.println(region.name + " map creation started.");
            Utilities.getInstance().addProcessToMonitor(region.processor);
            region.setState(Region.MAKING_OSM);
            final ThreadProcessor preg = region.processor;
            synchronized (preg) {
                if (preg.getState() == ThreadProcessor.RUNNING) {
                    try {
                        preg.wait();
                    } catch (InterruptedException ex) {
                        region.processor.setStatus("Interrupted.");
                        region.processor.setState(ContoursUpdater.ERROR);
                        return 7;
                    }
                }
            }
            Utilities.getInstance().removeMonitoredProcess(region.processor);
            region.setState(Region.READY);
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
