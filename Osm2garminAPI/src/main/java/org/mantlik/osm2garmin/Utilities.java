/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 - 2012 Frantisek Mantlik <frantisek at mantlik.cz>
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
import java.lang.reflect.Method;
import java.net.*;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class Utilities {

    public static final long REFRESH_INTERVAL = 1000;
    private static final long START_DATE_ODBL = new SimpleDateFormat("yyyy-MM-dd'T'HH\\:mm\\:ss'Z'").
            parse("timestamp=2012-09-12T08\\:00\\:00Z", new ParsePosition(10)).getTime();
    public static final String[] ARGS_FILES = new String[]{"mkgmap.help", "contours.args", "gmapsupp.args",
        "gmapsupp_contours.args", "osm2img.args"};
    private static ArrayList<String> runningClasses = new ArrayList<String>();
    private static Utilities instance = null;
    private static ArrayList<ThreadProcessor> processesToMonitor = new ArrayList<ThreadProcessor>();
    private static ThreadProcessor monitor;

    public Utilities() {
        Properties monitorParams = new Properties();
        monitor = new ThreadProcessor(monitorParams) {
            @Override
            public void run() {
                while (true) {
                    if (!processesToMonitor.isEmpty()) {
                        for (int i = 0; i < processesToMonitor.size(); i++) {
                            ThreadProcessor process = processesToMonitor.get(i);
                            if (process != null) {
                                process.changeSupport.firePropertyChange("status", null, process.getStatus());
                                process.changeSupport.firePropertyChange("progress", -1, process.getProgress());
                                //process.changeSupport.firePropertyChange("state", -1, process.getState());
                                process.lifeCycleCheck(process.getClass().getSimpleName());
                            }
                        }
                    }
                    try {
                        Thread.sleep(REFRESH_INTERVAL);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        };
    }

    public static Utilities getInstance() {
        if (instance == null) {
            instance = new Utilities();
        }
        return instance;
    }

    /**
     *
     * Add process to monitor and fire status, state and progress change in
     * regular interval REFRESH_INTERVAL
     *
     * Each process will be added only once.
     *
     * @param process process to add
     */
    public void addProcessToMonitor(ThreadProcessor process) {
        if (!processesToMonitor.contains(process)) {
            processesToMonitor.add(process);
        }
    }

    /**
     * Removes process from monitoring.
     *
     * @param process process to remove
     * @return process if process was monitored. otherwise returns null.
     */
    public ThreadProcessor removeMonitoredProcess(ThreadProcessor process) {
        ThreadProcessor p = null;
        if (processesToMonitor.remove(process)) {
            p = process;
        }
        while (processesToMonitor.remove(process)) {
        }
        return p;
    }

    /**
     *
     * @param extclass external class to run
     * @param method main method in the extclass
     * @param library library folder to load classes from
     * @param args parameters to pass to the method
     * @param processor instance invoking the process (used for wait status
     * display)
     * @throws Exception
     */
    public void runExternal(String extclass, String method, String library,
            String[] args, ThreadProcessor processor) throws Exception {
        try {
            waitExclusive(extclass, processor);
        } catch (InterruptedException ex) {
            processor.setState(ThreadProcessor.ERROR);
            processor.setStatus("Interrupted.");
            return;
        }
        processor.classLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = libClassLoader(library, processor);
        if (processor.getClass().isAssignableFrom(OsmMaker.class)) {
            ((OsmMaker) processor).setSplitterLoader(loader);
        }
        //System.err.println("running " + library);
        Thread.currentThread().setContextClassLoader(loader);
        Class clazz = loader.loadClass(extclass);
        Method m = clazz.getDeclaredMethod(method, String[].class);
        m.setAccessible(true);
        m.invoke(null, (Object) args);
        if (processor.getClass().isAssignableFrom(OsmMaker.class)) {
            ((OsmMaker) processor).setSplitterLoader(null);
        }
        Thread.currentThread().setContextClassLoader(processor.classLoader);
        endExclusive(extclass);
    }

    /**
     *
     * @param library
     * @param processor
     * @return
     * @throws Exception
     */
    public ClassLoader libClassLoader(String library, ThreadProcessor processor) throws Exception {
        String userdir = getUserdir(processor);
        if (!new File(userdir + "lib/" + library).exists()) {
            copyLibraries(library, processor);
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
        ClassLoader ldr = URLClassLoader.newInstance(urlsa);
        return ldr;
    }

    /*
     * Waits until no running instance of the same class runs
     */
    public synchronized void waitExclusive(String extclass, ThreadProcessor processor) throws InterruptedException {
        if (isNotExclusive(extclass, processor)) {
            processor.waitFrom = System.currentTimeMillis();
            processesToMonitor.add(processor);
            wait();
            processesToMonitor.remove(processor);
        }
        if (!setExclusive(extclass, processor)) {
            System.err.println("Cannot run " + extclass + " exclusively. Retrying.");
            waitExclusive(extclass, processor);
        }
        processor.waitFrom = -1;
    }

    private synchronized boolean setExclusive(String extclass, ThreadProcessor processor) {
        if (isNotExclusive(extclass, processor)) {
            return false;
        }
        runningClasses.add(extclass);
        return true;
    }

    private synchronized boolean isNotExclusive(String extclass, ThreadProcessor processor) {
        if (processor.parameters.getProperty("exclusive_utils", "true").equals("true")) {
            return !runningClasses.isEmpty();
        }
        return runningClasses.contains(extclass);
    }

    public synchronized void endExclusive(String extclass) {
        while (runningClasses.contains(extclass)) {
            runningClasses.remove(extclass);
        }
        notify();
    }

    /*
     * Copy libraries from resources to working dir
     */
    private void copyLibraries(String library, ThreadProcessor processor) throws URISyntaxException, IOException {
        String userdir = getUserdir(processor);
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
                    Pack200.Unpacker unpacker = Pack200.newUnpacker();
                    JarOutputStream jo = new JarOutputStream(new FileOutputStream(libpath + jarname));
                    unpacker.unpack(input, jo);
                    jo.close();
                } else {
                    copyFile(stream, new File(libpath + name));
                }
            }
        }
    }

    public static String getUserdir(ThreadProcessor processor) {
        return processor.parameters.getProperty("userdir", "./");
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
                        if (!deleteFile(files[i])) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ex) {
                            }
                            deleteFile(files[i]);
                        }
                    } else {
                        if (!files[i].delete()) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ex) {
                            }
                            files[i].delete();
                        }
                    }
                }
            }
            if (!path.delete()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                }
                return path.delete();
            } else {
                return true;
            }
        } else {
            return true;
        }
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
        Properties parameters = new Properties();
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
     * Unpack files from zip archive zipFile to directory destDir
     *
     * @param zipFile
     * @param destDir
     */
    public static void unpackZipFiles(File zipFile, String destDir) {
        byte[] buf = new byte[1024];
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

    /**
     * Count total uncompressed length of all files in zipFile
     *
     * @param zipFile
     * @return
     */
    public static long zipGetTotalLength(File zipFile) {
        long length = 0;
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                long l = entry.getSize();
                if (l > 0) {
                    length += l;
                }
            }
            zip.close();
        } catch (IOException ex) {
            Logger.getLogger(ContoursUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException ex) {
            }
        }
        return length;
    }

    /**
     * Download state.txt for given date (approximately, no later)
     *
     * @param date date in the form YYYY-MM-DD
     * @param mirror mirror to download state.txt from
     * @param targetPath path to osmosis state dir
     * @return true in case of success
     */
    public static boolean downloadOsmosisStateFile(Long date, String mirror,
            String targetPath) {
        int max_retries = 60;
        if (!mirror.endsWith("/")) {
            mirror = mirror + "/";
        }
        long planetTime = date;
        long startReplTime = new SimpleDateFormat("yyyy-MM-dd").parse(
                "2012-09-12", new ParsePosition(0)).getTime();
        int sequenceNo = (int) ((planetTime - startReplTime) / 1000 / 60 / 60); // hours
        String stateUrl = mirror + updateName(sequenceNo).replace("osc.gz", "state.txt");
        while (!downloadFile(stateUrl, targetPath)) {
            // try older file
            sequenceNo--;
            max_retries--;
            if (max_retries <= 0) {
                return false;
            }
            stateUrl = mirror + updateName(sequenceNo).replace("osc.gz", "state.txt");
        }
        return true;
    }

    /**
     * Download file from url and save to destination file
     *
     * @param url
     * @param destination
     * @return false means not downloaded
     */
    public static boolean downloadFile(String url, String destination) {
        URL surl;
        try {
            surl = new URL(url);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }
        Downloader downloader = new Downloader(surl, destination);
        while (downloader.getStatus() != Downloader.COMPLETE) {
            if (downloader.getStatus() == Downloader.ERROR) {
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                //Logger.getLogger(PlanetUpdateDownloader.class.getName()).log(Level.SEVERE, null, ex);
                //setState(ERROR);
                return false;
            }
        }
        return true;
    }

    /**
     * Get timestamp from state.txt file
     *
     * @param timestampFile
     * @return
     */
    public static long getPlanetTimestamp(File timestampFile) {
        Long time = -1l;
        try {
            Scanner scanner = new Scanner(new FileInputStream(timestampFile));
            while (scanner.hasNext()) {
                String tstamp = scanner.nextLine();
                if (tstamp.startsWith("timestamp=")) {
                    Date tdate = new SimpleDateFormat("yyyy-MM-dd'T'HH\\:mm\\:ss'Z'").parse(tstamp, new ParsePosition(10));
                    time = tdate.getTime();
                }
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return time;
    }

    /**
     * name of the sequence file relative to planet directory
     *
     * @param fileno sequence number of the hour-replicate update file
     * @return
     */
    public static String updateName(int fileno) {
        final DecimalFormat D9 = new DecimalFormat("000000000");
        String prefix = "replication/";
        String d = D9.format(fileno);
        return prefix + "hour/" + d.substring(0, 3) + "/" + d.substring(3, 6) + "/" + d.substring(6) + ".osc.gz";
    }

    /**
     * Get sequence number from state.txt
     *
     * @param osmosisstate
     * @return
     * @throws IOException
     */
    public static int getSequenceNo(File osmosisstate) throws IOException {
        if (getPlanetTimestamp(osmosisstate) < START_DATE_ODBL) {
            return 0;
        }
        Scanner scanner = new Scanner(osmosisstate);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("sequenceNumber=")) {
                int sequenceNo = Integer.parseInt(line.replace("sequenceNumber=", ""));
                scanner.close();
                return sequenceNo;
            }
        }
        return -1;
    }

    public static void checkArgFiles(String userdir) {
        for (String name : ARGS_FILES) {
            File argsFile = new File(userdir, name);
            if (!argsFile.exists() || name.endsWith("help")) {
                try {
                    Utilities.copyFile(Utilities.class.getResourceAsStream(name),
                            argsFile);
                } catch (IOException ex) {
                    Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static long coordToMap(double coord) {
        long factor = 46603;
        long base = 2048;
        long raw = (long) coord * factor;
        long remainder = raw % base;
        if (remainder <= base / 2) {
            return raw - remainder;
        }
        return raw + base - remainder;
    }
}
