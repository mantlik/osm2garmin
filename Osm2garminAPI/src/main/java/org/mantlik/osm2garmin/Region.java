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

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fm
 */
public class Region {

    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    public static final int NEW = 0, MAKING_CONTOURS = 1, CONTOURS_READY = 2,
            MAKING_OSM = 3, READY = 4, ERROR = 5;
    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    /**
     * 
     */
    public float lon1, lat1, lon2, lat2;
    private int state = NEW;
    /**
     * 
     */
    public File dir;
    /**
     * 
     */
    public String name = "";
    /**
     * 
     */
    public ThreadProcessor processor = null;
    /**
     * 
     */
    public PropertyChangeSupport changeSupport;
    /**
     * 
     */
    public int splitterMaxAreas = 32;
    /*
     * 
     */
    public int familyID = 5000;

    /**
     * 
     * @param name region name used as map dir name as well
     * @param mapsdir directory to save map dir
     * @param deleteOldMap  delete contents of map dir if exists
     * @param familyID family ID for registration in GMAPSUPP - must be unique for the whole system 
     */
    public Region(String name, String mapsdir, boolean deleteOldMap, int familyID) {
        this.name = name.trim();
        if (!mapsdir.endsWith("/")) {
            mapsdir = mapsdir + "/";
        }
        dir = new File(mapsdir + this.name);
        if (dir.exists() && deleteOldMap) {
            Osm2garmin.deleteFile(dir);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        changeSupport = new PropertyChangeSupport(this);
    }

    /**
     * 
     * @return
     */
    public int getState() {
        return state;
    }

    /**
     * 
     * @param state
     */
    public void setState(int state) {
        if (this.state == state) {
            return;
        }
        changeSupport.firePropertyChange("state", this.state, state);
        this.state = state;
    }

    /*
     * Create MapSource installer and uninstaller for ID no 5001+reg
     */
    void makeInstallers(int reg) {
        // contours map ID
        int familyID_cont = familyID + 1000;
        String hexid = Integer.toString(familyID, 16).toUpperCase();
        // swap bytes
        hexid = hexid.substring(2) + hexid.substring(0, 2);

        String hexid_cont = Integer.toString(familyID_cont, 16).toUpperCase();
        hexid_cont = hexid_cont.substring(2) + hexid_cont.substring(0, 2);
        try {
            PrintStream installer = new PrintStream(new File(dir.getPath() + "/installer.bat"));
            PrintStream uninstaller = new PrintStream(new File(dir.getPath() + "/uninstaller.bat"));

            installer.print("set KEY=HKLM\\SOFTWARE\\Wow6432Node\\Garmin\\MapSource\r\n");
            installer.print("if %PROCESSOR_ARCHITECTURE% == AMD64 goto key_ok\r\n");
            installer.print("set KEY=HKLM\\SOFTWARE\\Garmin\\MapSource\r\n");
            installer.print(":key_ok\r\n\r\n");

            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID + " /v ID /t REG_BINARY /d " + hexid + " /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID + "\\1 /v Loc /t REG_SZ /d \"%~dp0\\\" /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID + "\\1 /v Bmap /t REG_SZ /d \"%~dp0osmmap.img\" /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID + "\\1 /v Tdb /t REG_SZ /d \"%~dp0osmmap.tdb\" /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID_cont + " /v ID /t REG_BINARY /d " + hexid_cont + " /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID_cont + "\\1 /v Loc /t REG_SZ /d \"%~dp0\\\" /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID_cont + "\\1 /v Bmap /t REG_SZ /d \"%~dp0osmmap.img\" /f\r\n");
            installer.print("reg ADD %KEY%\\Families\\FAMILY_" + familyID_cont + "\\1 /v Tdb /t REG_SZ /d \"%~dp0osmmap.tdb\" /f\r\n");
            installer.close();

            uninstaller.print("set KEY=HKLM\\SOFTWARE\\Wow6432Node\\Garmin\\MapSource\r\n");
            uninstaller.print("if %PROCESSOR_ARCHITECTURE% == AMD64 goto key_ok\r\n");
            uninstaller.print("set KEY=HKLM\\SOFTWARE\\Garmin\\MapSource\r\n");
            uninstaller.print(":key_ok\r\n\r\n");

            uninstaller.print("reg DELETE %KEY%\\Families\\FAMILY_" + familyID + " /f\r\n");
            uninstaller.print("reg DELETE %KEY%\\Families\\FAMILY_" + familyID_cont + " /f\r\n");
            uninstaller.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
        }

    }
}
