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

import crosby.binary.file.BlockInputStream;
import org.xmlpull.v1.XmlPullParserException;
import uk.me.parabola.splitter.args.ParamParser;
import uk.me.parabola.splitter.args.SplitterParams;
import uk.me.parabola.splitter.geo.City;
import uk.me.parabola.splitter.geo.CityFinder;
import uk.me.parabola.splitter.geo.CityLoader;
import uk.me.parabola.splitter.geo.DefaultCityFinder;
import uk.me.parabola.splitter.geo.DummyCityFinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Splitter for OSM files with the purpose of providing input files for mkgmap.
 * <p/>
 * The input file is split so that no piece has more than a given number of nodes in it.
 *
 * @author Steve Ratcliffe
 */
public class Main {
	private static final String DEFAULT_DIR = ".";

	// We can only process a maximum of 255 areas at a time because we
	// compress an area ID into 8 bits to save memory (and 0 is reserved)
	private int maxAreasPerPass;

	// A list of the OSM files to parse.
	private List<String> filenames;

	// The description to write into the template.args file.
	private String description;

	// The starting map ID.
	private int mapId;

	// The amount in map units that tiles overlap (note that the final img's will not overlap
	// but the input files do).
	private int overlapAmount;

	// The max number of nodes that will appear in a single file.
	private int maxNodes;

	// The maximum resolution of the map to be produced by mkgmap. This is a value in the range
	// 0-24. Higher numbers mean higher detail. The resolution determines how the tiles must
	// be aligned. Eg a resolution of 13 means the tiles need to have their edges aligned to
	// multiples of 2 ^ (24 - 13) = 2048 map units, and their widths and heights must be a multiple
	// of 2 * 2 ^ (24 - 13) = 4096 units. The tile widths and height multiples are double the tile
	// alignment because the center point of the tile is stored, and that must be aligned the
	// same as the tile edges are.
	private int resolution;

	// Whether or not to trim tiles of any empty space around their edges.
	private boolean trim;
	// This gets set if no osm file is supplied as a parameter and the cache is empty.
	private boolean useStdIn;
	// Set if there is a previous area file given on the command line.
	private AreaList areaList;
	// Whether or not the source OSM file(s) contain strictly nodes first, then ways, then rels,
	// or they're all mixed up. Running with mixed enabled takes longer.
	private boolean mixed;
	// The path where the results are written out to.
	private File fileOutputDir;
	// A GeoNames file to use for naming the tiles.
	private String geoNamesFile;
	// How often (in seconds) to provide JVM status information. Zero = no information.
	private int statusFreq;
	// Whether to use the density map. Disabling this (not recommended) causes the splitter to
	// revert to using legacy mode which takes MUCH more memory during phase one.
	private boolean densityMap;

	private String kmlOutputFile;
	// The maximum number of threads the splitter should use.
	private int maxThreads;

	public static void main(String[] args) {

		Main m = new Main();
		m.start(args);
	}

	private void start(String[] args) {
		readArgs(args);
		if (statusFreq > 0) {
			JVMHealthMonitor.start(statusFreq);
		}
		long start = System.currentTimeMillis();
		System.out.println("Time started: " + new Date());
		try {
			split();
		} catch (IOException e) {
			System.err.println("Error opening or reading file " + e);
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			System.err.println("Error parsing xml from file " + e);
			e.printStackTrace();
		}
		System.out.println("Time finished: " + new Date());
		System.out.println("Total time taken: " + (System.currentTimeMillis() - start) / 1000 + 's');
	}

	private void split() throws IOException, XmlPullParserException {

		File outputDir = fileOutputDir;
		if (!outputDir.exists()) {
			System.out.println("Output directory not found. Creating directory '" + fileOutputDir + "'");
			if (!outputDir.mkdirs()) {
				System.err.println("Unable to create output directory! Using default directory instead");
				fileOutputDir = new File(DEFAULT_DIR);
			}
		} else if (!outputDir.isDirectory()) {
			System.err.println("The --output-dir parameter must specify a directory. The --output-dir parameter is being ignored, writing to default directory instead.");
			fileOutputDir = new File(DEFAULT_DIR);
		}

		if (filenames.isEmpty()) {
			if (areaList == null) {
				throw new IllegalArgumentException("No .osm files were supplied so at least one of --cache or --split-file must be specified");
			} else {
				int areaCount = areaList.getAreas().size();
				int passes = getAreasPerPass(areaCount);
				if (passes > 1) {
					throw new IllegalArgumentException("No .osm files or --cache parameter were supplied, but stdin cannot be used because " + passes
							+ " passes are required to write out the areas. Either provide --cache or increase --max-areas to match the number of areas (" + areaCount + ')');
				}
				useStdIn = true;
			}
		}

		if (areaList == null) {
			int alignment = 1 << (24 - resolution);
			System.out.println("Map is being split for resolution " + resolution + ':');
			System.out.println(" - area boundaries are aligned to 0x" + Integer.toHexString(alignment) + " map units");
			System.out.println(" - areas are multiples of 0x" + Integer.toHexString(alignment * 2) + " map units wide and high");
			areaList = calculateAreas();
			for (Area area : areaList.getAreas()) {
				area.setMapId(mapId++);
			}
			nameAreas();
			areaList.write(new File(fileOutputDir, "areas.list").getPath());
		} else {
			nameAreas();
		}

		List<Area> areas = areaList.getAreas();
		System.out.println(areas.size() + " areas:");
		for (Area area : areas) {
			System.out.print("Area " + area.getMapId() + " covers " + area.toHexString());
			if (area.getName() != null)
				System.out.print(' ' + area.getName());
			System.out.println();
		}

		if (kmlOutputFile != null) {
			File out = new File(kmlOutputFile);
			if (!out.isAbsolute())
				kmlOutputFile = new File(fileOutputDir, kmlOutputFile).getPath();
			System.out.println("Writing KML file to " + kmlOutputFile);
			areaList.writeKml(kmlOutputFile);
		}

		writeAreas(areas);
		writeArgsFile(areas);
	}

	private int getAreasPerPass(int areaCount) {
		return (int) Math.ceil((double) areaCount / (double) maxAreasPerPass);
	}

	/**
	 * Deal with the command line arguments.
	 */
	private void readArgs(String[] args) {
		ParamParser parser = new ParamParser();
		SplitterParams params = parser.parse(SplitterParams.class, args);

		if (!parser.getErrors().isEmpty()) {
			System.out.println();
			System.out.println("Invalid parameter(s):");
			for (String error : parser.getErrors()) {
				System.out.println("  " + error);
			}
			System.out.println();
			parser.displayUsage();
			System.exit(-1);
		}

		for (Map.Entry<String, Object> entry : parser.getConvertedParams().entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			System.out.println(name + '=' + (value == null ? "" : value));
		}

		mapId = params.getMapid();
		overlapAmount = params.getOverlap();
		maxNodes = params.getMaxNodes();
		description = params.getDescription();
		geoNamesFile = params.getGeonamesFile();
		resolution = params.getResolution();
		trim = !params.isNoTrim();
		if (resolution < 1 || resolution > 24) {
			System.err.println("The --resolution parameter must be a value between 1 and 24. Resetting to 13.");
			resolution = 13;
		}
		mixed = params.isMixed();
		statusFreq = params.getStatusFreq();
		
		String outputDir = params.getOutputDir();
		fileOutputDir = new File(outputDir == null? DEFAULT_DIR: outputDir);

		maxAreasPerPass = params.getMaxAreas();
		if (maxAreasPerPass < 1 || maxAreasPerPass > 255) {
			System.err.println("The --max-areas parameter must be a value between 1 and 255. Resetting to 255.");
			maxAreasPerPass = 255;
		}
		kmlOutputFile = params.getWriteKml();
		densityMap = !params.isLegacyMode();
		if (!densityMap) {
			System.out.println("WARNING: Specifying --legacy-split will cause the first stage of the split to take much more memory! This option is considered deprecated and will be removed in a future build.");
		}

		maxThreads = params.getMaxThreads().getCount();
		filenames = parser.getAdditionalParams();

		String splitFile = params.getSplitFile();
		if (splitFile != null) {
			try {
				areaList = new AreaList();
				areaList.read(splitFile);
				areaList.dump();
			} catch (IOException e) {
				areaList = null;
				System.err.println("Could not read area list file");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Calculate the areas that we are going to split into by getting the total area and
	 * then subdividing down until each area has at most max-nodes nodes in it.
	 */
	private AreaList calculateAreas() throws IOException, XmlPullParserException {

		MapCollector nodes = densityMap ? new DensityMapCollector(trim, resolution) : new NodeCollector();
		MapProcessor processor = nodes;

		processMap(processor);
		//MapReader mapReader = processMap(processor);

		//System.out.print("A total of " + Utils.format(mapReader.getNodeCount()) + " nodes, " +
		//				Utils.format(mapReader.getWayCount()) + " ways and " +
		//				Utils.format(mapReader.getRelationCount()) + " relations were processed ");

		System.out.println("in " + filenames.size() + (filenames.size() == 1 ? " file" : " files"));

		//System.out.println("Min node ID = " + mapReader.getMinNodeId());
		//System.out.println("Max node ID = " + mapReader.getMaxNodeId());

		System.out.println("Time: " + new Date());

		Area exactArea = nodes.getExactArea();
		SplittableArea splittableArea = nodes.getRoundedArea(resolution);
		System.out.println("Exact map coverage is " + exactArea);
		System.out.println("Trimmed and rounded map coverage is " + splittableArea.getBounds());
		System.out.println("Splitting nodes into areas containing a maximum of " + Utils.format(maxNodes) + " nodes each...");

		List<Area> areas = splittableArea.split(maxNodes);
		return new AreaList(areas);
	}

	private void nameAreas() throws IOException {
		CityFinder cityFinder;
		if (geoNamesFile != null) {
			CityLoader cityLoader = new CityLoader(true);
			List<City> cities = cityLoader.load(geoNamesFile);
			cityFinder = new DefaultCityFinder(cities);
		} else {
			cityFinder = new DummyCityFinder();
		}

		for (Area area : areaList.getAreas()) {
			// Decide what to call the area
			Set<City> found = cityFinder.findCities(area);
			City bestMatch = null;
			for (City city : found) {
				if (bestMatch == null || city.getPopulation() > bestMatch.getPopulation()) {
					bestMatch = city;
				}
			}
			if (bestMatch != null)
				area.setName(bestMatch.getCountryCode() + '-' + bestMatch.getName());
			else
				area.setName(description);
		}
	}

	/**
	 * Second pass, we have the areas so parse the file(s) again and write out each element
	 * to the file(s) that should contain it.
	 *
	 * @param areaList Area list determined on the first pass.
	 */
	private void writeAreas(List<Area> areas) throws IOException, XmlPullParserException {
		System.out.println("Writing out split osm files " + new Date());

		int numPasses = getAreasPerPass(areas.size());
		int areasPerPass = (int) Math.ceil((double) areas.size() / (double) numPasses);

		if (numPasses > 1) {
			System.out.println("Processing " + areas.size() + " areas in " + numPasses + " passes, " + areasPerPass + " areas at a time");
		} else {
			System.out.println("Processing " + areas.size() + " areas in a single pass");
		}

		for (int i = 0; i < numPasses; i++) {
			OSMWriter[] currentWriters = new OSMWriter[Math.min(areasPerPass, areas.size() - i * areasPerPass)];
			for (int j = 0; j < currentWriters.length; j++) {
				Area area = areas.get(i * areasPerPass + j);
				currentWriters[j] = new OSMWriter(area, fileOutputDir);
				currentWriters[j].initForWrite(area.getMapId(), overlapAmount);
			}

			System.out.println("Starting pass " + (i + 1) + " of " + numPasses + ", processing " + currentWriters.length +
							" areas (" + areas.get(i * areasPerPass).getMapId() + " to " +
							areas.get(i * areasPerPass + currentWriters.length - 1).getMapId() + ')');

			MapProcessor processor = new SplitProcessor(currentWriters, maxThreads);
			processMap(processor);
			//System.out.println("Wrote " + Utils.format(mapReader.getNodeCount()) + " nodes, " +
			//				Utils.format(mapReader.getWayCount()) + " ways, " +
			//				Utils.format(mapReader.getRelationCount()) + " relations");
		}
	}
	
	private void processMap(MapProcessor processor) throws XmlPullParserException {
		// Create both an XML reader and a binary reader, Dispatch each input to the
		// Appropriate parser.
		OSMParser parser = new OSMParser(processor, mixed);
		if (useStdIn) {
			System.out.println("Reading osm data from stdin...");
			Reader reader = new InputStreamReader(System.in, Charset.forName("UTF-8"));
			parser.setReader(reader);
			try {
				try {
					parser.parse();
				} finally {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (String filename : filenames) {
			System.out.println("Processing " + filename);
			try {
				if (filename.endsWith(".osm.pbf")) {
					// Is it a binary file?
					File file = new File(filename);
					BlockInputStream blockinput = (new BlockInputStream(
							new FileInputStream(file), new BinaryMapParser(processor)));
					try {
						blockinput.process();
					} finally {
						blockinput.close();
					}
				} else {
					// No, try XML.
					Reader reader = Utils.openFile(filename, maxThreads > 1);
					parser.setReader(reader);
					try {
						parser.parse();
					} finally {
						reader.close();
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		processor.endMap();
	}
	
	/**
	 * Write a file that can be given to mkgmap that contains the correct arguments
	 * for the split file pieces.  You are encouraged to edit the file and so it
	 * contains a template of all the arguments that you might want to use.
	 */
	protected void writeArgsFile(List<Area> areas) {
		PrintWriter w;
		try {
			w = new PrintWriter(new FileWriter(new File(fileOutputDir, "template.args")));
		} catch (IOException e) {
			System.err.println("Could not write template.args file");
			return;
		}

		w.println("#");
		w.println("# This file can be given to mkgmap using the -c option");
		w.println("# Please edit it first to add a description of each map.");
		w.println("#");
		w.println();

		w.println("# You can set the family id for the map");
		w.println("# family-id: 980");
		w.println("# product-id: 1");

		w.println();
		w.println("# Following is a list of map tiles.  Add a suitable description");
		w.println("# for each one.");
		for (Area a : areas) {
			w.println();
			w.format("mapname: %d\n", a.getMapId());
			if (a.getName() == null)
				w.println("# description: OSM Map");
			else
				w.println("description: " + a.getName());
			w.format("input-file: %d.osm.gz\n", a.getMapId());
		}

		w.println();
		w.close();
	}
}
