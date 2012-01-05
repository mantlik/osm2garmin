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

import java.io.PrintStream;
import java.util.ArrayList;


/**
 *
 * @author fm
 */
public class Contour {

    public ArrayList <Point> data = new ArrayList <Point>();
    public double z;
    public boolean finished = false;
    public boolean closed = false;

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public ArrayList<Point> getData() {
        return data;
    }

    public void setData(ArrayList<Point> data) {
        this.data = data;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
    
    public void outputOsm(long wayid, long startid, boolean major, PrintStream s) {
        int size = data.size();
        if (isClosed()) {
            size --;  // repeat first node at the way end
        }
        if (data.isEmpty() || size < 2) {
            return;
        }
        long id = startid;
        for (int i=0; i<size;i++) {
            s.println("<node id=\""+id+"\" lat=\""+data.get(i).x +"\" lon=\""+data.get(i).y +"\" />");
            id++;
        }
        id = startid;
        s.println("<way id=\""+wayid+"\">");
        for (int i=0; i<size;i++) {
            s.println("<nd ref=\""+id+"\" />");
            id++;
        }
        if (isClosed()) {  // last node equals to the first one
            s.println("<nd ref=\""+startid+"\" />");
        }
        s.println("<tag k=\"ele\" v=\""+z+"\" />");
        s.println("<tag k=\"contour\" v=\"elevation\" />");
        if (major) {
            s.println("<tag k=\"contour_ext\" v=\"elevation_major\" />");
        } else {
            s.println("<tag k=\"contour_ext\" v=\"elevation_minor\" />");
        }
        s.println("</way>");
    }
}
