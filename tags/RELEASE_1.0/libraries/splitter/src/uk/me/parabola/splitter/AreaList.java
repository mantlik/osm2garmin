/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.splitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;

/**
 * A list of areas.  It can be read and written to a file.
 */
public class AreaList {
	private List<Area> areas;

	public AreaList(List<Area> areas) {
		this.areas = areas;
	}

	/**
	 * This constructor is called when you are going to be reading in the list from
	 * a file, rather than making it from an already constructed list.
	 */
	public AreaList() {
	}

	/**
	 * Write out a file containing the list of areas that we calculated.  This allows us to reuse the
	 * same areas on a subsequent run without having to re-calculate them.
	 *
	 * @param filename The filename to write to.
	 */
	public void write(String filename) throws IOException {

		Writer w = null;
		try {
			w = new FileWriter(filename);
			PrintWriter pw = new PrintWriter(w);

			pw.println("# List of areas");
			pw.format("# Generated %s\n", new Date());
			//pw.format("# Options: max-nodes=%d\n", main.getMaxNodes());
			pw.println("#");

			for (Area area : areas) {
				pw.format(Locale.ROOT, "%08d: %d,%d to %d,%d\n",
						area.getMapId(),
						area.getMinLat(), area.getMinLong(),
						area.getMaxLat(), area.getMaxLong());
				pw.format(Locale.ROOT, "#       : %f,%f to %f,%f\n",
						Utils.toDegrees(area.getMinLat()), Utils.toDegrees(area.getMinLong()),
						Utils.toDegrees(area.getMaxLat()), Utils.toDegrees(area.getMaxLong()));
				pw.println();
			}

		} catch (IOException e) {
			System.err.println("Could not write areas.list file");
		} finally {
			if (w != null)
				w.close();
		}
	}

	/**
	 * Write out a KML file containing the areas that we calculated. This KML file
	 * can be opened in Google Earth etc to see the areas that were split.
	 *
	 * @param filename The KML filename to write to.
	 */
	public void writeKml(String filename) throws IOException {

		Writer w = null;
		try {
			w = new FileWriter(filename);
			PrintWriter pw = new PrintWriter(w);

			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								 "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
								 "<Document>\n" +
								 "  <Style id=\"transWhitePoly\">\n" +
								 "    <LineStyle>\n" +
								 "      <width>1.5</width>\n" +
								 "    </LineStyle>\n" +
								 "    <PolyStyle>\n" +
								 "      <color>00ffffff</color>\n" +
								 "      <colorMode>normal</colorMode>\n" +
								 "    </PolyStyle>\n" +
								 "  </Style>\n");

			for (Area area : areas) {
				double south = Utils.toDegrees(area.getMinLat());
				double west = Utils.toDegrees(area.getMinLong());
				double north = Utils.toDegrees(area.getMaxLat());
				double east = Utils.toDegrees(area.getMaxLong());

				String name = area.getName() == null ? String.valueOf(area.getMapId()) : area.getName();
				pw.format(Locale.ROOT,
								  "  <Placemark>\n" +
									"    <name>%1$d</name>\n" +
									"    <styleUrl>#transWhitePoly</styleUrl>\n" +
									"      <description>\n" +
									"        <![CDATA[%2$s]]>\n" +
									"      </description>\n" +
									"    <Polygon>\n" +
									"      <outerBoundaryIs>\n" +
									"        <LinearRing>\n" +
									"          <coordinates>\n" +
									"            %4$f,%3$f\n" +
									"            %4$f,%5$f\n" +
									"            %6$f,%5$f\n" +
									"            %6$f,%3$f\n" +
									"            %4$f,%3$f\n" +
									"          </coordinates>\n" +
									"        </LinearRing>\n" +
									"      </outerBoundaryIs>\n" +
									"    </Polygon>\n" +
									"  </Placemark>\n", area.getMapId(), name, south, west, north, east);
			}
			pw.print("</Document>\n</kml>");
		} catch (IOException e) {
			System.err.println("Could not write KML file " + filename);
		} finally {
			if (w != null)
				w.close();
		}
	}

	public void read(String filename) throws IOException {
		String lower = filename.toLowerCase();
		if (lower.endsWith(".kml") || lower.endsWith(".kml.gz") || lower.endsWith(".kml.bz2")) {
			readKml(filename);
		} else {
			readList(filename);
		}
	}

	/**
	 * Read in an area definition file that we previously wrote.
	 * Obviously other tools could create the file too.
	 */
	private void readList(String filename) throws IOException {
		Reader r = null;
		areas = new ArrayList<Area>();

		Pattern pattern = Pattern.compile("([0-9]{8}):" +
		" ([\\p{XDigit}x-]+),([\\p{XDigit}x-]+)" +
		" to ([\\p{XDigit}x-]+),([\\p{XDigit}x-]+)");

		try {
			r = new FileReader(filename);
			BufferedReader br = new BufferedReader(r);

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#')
					continue;

				Matcher matcher = pattern.matcher(line);
				matcher.find();
				String mapid = matcher.group(1);

				Area area = new Area(
						Integer.decode(matcher.group(2)),
						Integer.decode(matcher.group(3)),
						Integer.decode(matcher.group(4)),
						Integer.decode(matcher.group(5)));
				area.setMapId(Integer.parseInt(mapid));
				areas.add(area);
			}
		} catch (NumberFormatException e) {
			areas = Collections.emptyList();
			System.err.println("Bad number in areas list file");
		} finally {
			if (r != null)
				r.close();
		}
	}

	private void readKml(String filename) throws IOException {
		try {
			KmlParser parser = new KmlParser();
			parser.setReader(Utils.openFile(filename, false));
			parser.parse();
			areas = parser.getAreas();
		} catch (XmlPullParserException e) {
			throw new IOException("Unable to parse KML file " + filename, e);
		}
	}

	public List<Area> getAreas() {
		return areas;
	}

	public void dump() {
		System.out.println("Areas read from file");
		for (Area area : areas) {
			System.out.println(area.getMapId() + " " + area.toString());
		}
	}
}
