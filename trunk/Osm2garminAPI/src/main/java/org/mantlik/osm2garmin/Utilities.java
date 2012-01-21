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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class Utilities {

    private static ArrayList<String> runningClasses = new ArrayList<String>();
    private static Utilities instance = null;

    public static Utilities getInstance() {
        if (instance == null) {
            instance = new Utilities();
        }
        return instance;
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
        waitExclusive(extclass, processor);
        processor.classLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = libClassLoader(library, processor);
        if (processor.getClass().isAssignableFrom(OsmMaker.class)) {
            ((OsmMaker) processor).setSplitterLoader(loader);
        }
        System.err.println("running " + library);
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
    public void waitExclusive(String extclass, ThreadProcessor processor) throws InterruptedException {
        long interval = 10000;
        String wait_status = processor.getStatus();
        while (isNotExclusive(extclass, processor)) {
            synchronized (processor) {
                processor.wait(interval);
            }
            if (setExclusive(extclass, processor)) {
                processor.setStatus(wait_status);
                processor.parameters.remove("wait_sleep");
                return;
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
            processor.setStatus(wait_status + " (waiting " + wait + ")");
        }
        processor.setStatus(wait_status);
        if (!setExclusive(extclass, processor)) {
            waitExclusive(extclass, processor);
        } else {
            processor.parameters.remove("wait_sleep");
        }
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
        return processor.parameters.getProperty("userdir", "");
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
                        deleteFile(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (path.delete());
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
}
