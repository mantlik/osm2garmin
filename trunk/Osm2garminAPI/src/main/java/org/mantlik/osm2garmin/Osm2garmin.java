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
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author fm
 */
public class Osm2garmin implements PropertyChangeListener {

    /**
     *
     */
    public static Properties parameters = new Properties();
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
    public static String userdir = ""; // used by GUI to save temporary info
    private static ArrayList<String> runningClasses = new ArrayList<String>();

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        boolean filesOK = true;
        if (!new File("settings.properties").exists()) {
            copyFile(Osm2garmin.class.getResourceAsStream("settings.properties"),
                    new File("settings.properties"));
            filesOK = false;
        }
        if (loadProperties() == null) {
            System.exit(99);
        }
        String regions = parameters.getProperty("regions");
        if (!new File(regions).exists()) {
            copyFile(Osm2garmin.class.getResourceAsStream(regions),
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
        int retcode = new Osm2garmin().start(parameters);
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
        deleteFile(libdir);
        // parse regions
        File r = new File(parameters.getProperty("regions"));
        try {
            if (!r.exists()) {
                copyFile(Osm2garmin.class.getResourceAsStream("regions.txt"),
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
                    familyID ++;
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
                copyFile(Osm2garmin.class.getResourceAsStream("cities15000.zip"),
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
        loadProperties();
        planetDownloader = new PlanetDownloader(parameters);
        planetDownloader.changeSupport.addPropertyChangeListener(this);
        System.out.println("Planet file download started.");
        while (!allDone) {
            loadProperties();
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

    /**
     *
     * @param parms
     * @return
     */
    public static synchronized boolean saveProperties(Properties parms) {
        try {
            parms.store(new BufferedOutputStream(new FileOutputStream(new File("status.properties"))), "");
        } catch (IOException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "Cannot save settings.");
            return false;
        }
        return true;
    }

    /**
     *
     * @return
     */
    public static synchronized Properties loadProperties() {
        try {
            if (new File("status.properties").exists()) {
                parameters.load(new BufferedInputStream(new FileInputStream(new File("status.properties"))));
            }
            if (new File("settings.properties").exists()) {
                parameters.load(new BufferedInputStream(new FileInputStream(new File("settings.properties"))));
            }
        } catch (IOException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
            return null;
        }
        return parameters;
    }

    /**
     *
     * @param extclass external class to run
     * @param method main method in the extclass
     * @param library library folder to load classes from
     * @param loader classloader to use
     * @param args parameters to pass to the method
     * @param processor instance invoking the process (used for wait status
     * display)
     * @throws Exception
     */
    public static void runExternal(String extclass, String method, String library,
            ClassLoader loader, String[] args, ThreadProcessor processor) throws Exception {
        waitExclusive(extclass, processor);
        if (loader == null) {
            loader = libClassLoader(library);
        }
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        Class clazz = loader.loadClass(extclass);
        Method m = clazz.getDeclaredMethod(method, String[].class);
        m.setAccessible(true);
        m.invoke(null, (Object) args);
        Thread.currentThread().setContextClassLoader(currentLoader);
        endExclusive(extclass);
    }

    /**
     *
     * @param library
     * @param loader
     * @return
     * @throws Exception
     */
    public static ClassLoader libClassLoader(String library, ClassLoader loader) throws Exception {
        if (!new File(userdir + "lib/" + library).exists()) {
            copyLibraries(library);
        }
        File dd = new File(userdir + "lib/" + library);
        if (dd == null) {
            return null;
        }
        ArrayList<URL> urls = new ArrayList<URL>();
        String[] files = new File(userdir + "lib/" + library).list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].endsWith(".jar")) {
                File f = new File(userdir + "lib/" + library + "/" + files[i]);
                urls.add(new URL(f.toURI().toURL().toExternalForm()));
            }
        }
        URL[] urlsa = urls.toArray(new URL[0]);
        for (URL url : urlsa) {
            URLConnection conn = url.openConnection();
        }
        ClassLoader ldr;
        if (loader == null) {
            ldr = new URLClassLoader(urlsa);
        } else {
            ldr = new URLClassLoader(urlsa, loader);
        }
        return ldr;
    }

    /**
     *
     * @param library
     * @return
     * @throws Exception
     */
    public static ClassLoader libClassLoader(String library) throws Exception {
        return libClassLoader(library, null);
    }

    /**
     *
     * @param fromFile
     * @param toFile
     * @throws IOException
     */
    public static void copyFile(File fromFile, File toFile) throws IOException {
        String fromFileName = fromFile.getName();
        String toFileName = toFile.getName();
        if (!fromFile.exists()) {
            throw new IOException("FileCopy: " + "no such source file: "
                    + fromFile.getPath());
        }
        if (!fromFile.isFile()) {
            throw new IOException("FileCopy: " + "can't copy directory: "
                    + fromFileName);
        }
        if (!fromFile.canRead()) {
            throw new IOException("FileCopy: " + "source file is unreadable: "
                    + fromFileName);
        }

        if (toFile.isDirectory()) {
            toFile = new File(toFile, fromFile.getName());
        }

        String parent = toFile.getParent();
        if (parent == null) {
            parent = System.getProperty("user.dir");
        }
        File dir = new File(parent);
        if (!dir.exists()) {
            throw new IOException("FileCopy: "
                    + "destination directory doesn't exist: " + parent);
        }
        if (dir.isFile()) {
            throw new IOException("FileCopy: "
                    + "destination is not a directory: " + parent);
        }
        if (!dir.canWrite()) {
            throw new IOException("FileCopy: "
                    + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from;
        from = new FileInputStream(fromFile);
        copyFile(from, toFile);
    }

    /**
     *
     * @param from
     * @param toFile
     * @throws IOException
     */
    public static void copyFile(InputStream from, File toFile)
            throws IOException {

        FileOutputStream to = null;
        try {
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead); // write
            }
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /*
     * Copy libraries from resources to working dir
     */
    private static void copyLibraries(String library) throws URISyntaxException, IOException {
        String libpath = userdir + "lib/" + library + "/";
        File libfile = new File(libpath);
        libfile.mkdirs();
        String[] listfiles = getResourceListing(Osm2garmin.class, "org/mantlik/osm2garmin/" + library + "/");
        for (String name : listfiles) {
            InputStream stream = Osm2garmin.class.getResourceAsStream(library + "/" + name);
            if (!name.equals("")) {
                if (name.endsWith("pack.gz")) {
                    String jarname = name.replace("pack.gz", "jar");
                    InputStream input = new GZIPInputStream(stream);
                    Unpacker unpacker = Pack200.newUnpacker();
                    JarOutputStream jo = new JarOutputStream(new FileOutputStream(libpath + jarname));
                    unpacker.unpack(input, jo);
                    jo.close();
                } else {
                    copyFile(stream, new File(libpath + name));
                }
            }
        }
    }

    /**
     * http://www.uofr.net/~greg/java/get-resource-listing.html
     *
     * List directory contents for a resource folder. Not recursive. This is
     * basically a brute-force implementation. Works for regular files and also
     * JARs.
     *
     * @author Greg Briggs
     * @param clazz Any java class that lives in the same place as the resources
     * you want.
     * @param path Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     */
    private static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /*
             * A file path: easy enough
             */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory. Have
             * to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /*
             * A JAR path
             */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    /**
     * This function will recursively delete directories and files.
     *
     * @param path File or Directory to be deleted
     * @return true indicates success.
     */
    public static boolean deleteFile(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteFile(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (path.delete());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        changeSupport.firePropertyChange(evt);
    }

    /*
     * Waits until no running instance of the same class runs
     */
    private static void waitExclusive(String extclass, ThreadProcessor processor) throws InterruptedException {
        long interval = 10000;
        processor.parameters.setProperty("wait_status", processor.getStatus());
        while (isExclusive(extclass)) {
            Thread.sleep(interval);
            if (!isExclusive(extclass)) {
                if (setExclusive(extclass)) {
                    processor.setStatus(processor.parameters.getProperty("wait_status"));
                    processor.parameters.remove("wait_status");
                    processor.parameters.remove("wait_sleep");
                    return;
                }
            }
            long sleep = Long.parseLong(processor.parameters.getProperty("wait_sleep", "0"));
            sleep += interval;
            processor.parameters.setProperty("wait_sleep", "" + sleep);
            int sec = (int) ((sleep / 1000) % 60);
            int min = (int) ((sleep / 1000 / 60) % 60);
            int hour = (int) ((sleep / 1000 / 60 / 60) % 24);
            int day = (int) (sleep / 1000 / 60 / 60 / 24);
            String wait = "";
            if (day == 1) {
                wait = "1 day ";
            } else if (day > 1) {
                wait = day + " days ";
            }
            wait += hour + ":" + min + ":" + sec;
            processor.setStatus(processor.parameters.getProperty("wait_status", "") + " (waiting " + wait + ")");
        }
        processor.setStatus(processor.parameters.getProperty("wait_status"));
        processor.parameters.remove("wait_status");
        if (!setExclusive(extclass)) {
            waitExclusive(extclass, processor);
        }
    }

    private static synchronized boolean setExclusive(String extclass) {
        if (runningClasses.contains(extclass)) {
            return false;
        }
        runningClasses.add(extclass);
        return true;
    }

    private static synchronized boolean isExclusive(String extclass) {
        if (parameters.getProperty("exclusive_utils", "true").equals("true")) {
            return !runningClasses.isEmpty();
        }
        return runningClasses.contains(extclass);
    }

    private static synchronized void endExclusive(String extclass) {
        while (runningClasses.contains(extclass)) {
            runningClasses.remove(extclass);
        }
    }
}
