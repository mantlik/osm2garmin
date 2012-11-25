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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Some miscellaneous functions that are used within the .img code.
 *
 * @author Steve Ratcliffe
 */
public class Utils {

	private static final NumberFormat FORMATTER = NumberFormat.getIntegerInstance();

	public static String format(int number) {
		return FORMATTER.format(number);
	}

	public static String format(long number) {
		return FORMATTER.format(number);
	}

	public static double toDegrees(int val) {
		return (double) val / ((1 << 24) / 360.0);
	}

	/**
	 * A map unit is an integer value that is 1/(2^24) degrees of latitude or
	 * longitude.
	 *
	 * @param l The lat or long as decimal degrees.
	 * @return An integer value in map units.
	 */
	public static int toMapUnit(double l) {
		double DELTA = 0.000001; // TODO check if we really mean this
		if (l > 0)
			return (int) ((l + DELTA) * (1 << 24)/360);
		else
			return (int) ((l - DELTA) * (1 << 24)/360);
	}
	
	public static double toRadians(int latitude) {
		return toDegrees(latitude) * Math.PI / 180;
	}

	/**
	 * @param n the integer to test
	 * @return {@code true} if the integer is a power of two, {@code false} otherwise.
	 */
	public static boolean isPowerOfTwo(int n) {
		return ((n & (n - 1)) == 0) && n > 0;
	}

	/**
	 * Open a file and apply filters necessary to reading it such as decompression.
	 *
	 * @param name The file to open. gz, zip, bz2 are supported.
	 * @return A stream that will read the file, positioned at the beginning.
	 * @throws IOException If the file cannot be opened for any reason.
	 */
	public static Reader openFile(String name, boolean backgroundReader) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(name), 8192);
		if (name.endsWith(".gz")) {
			try {
				is = new GZIPInputStream(is);
			} catch (IOException e) {
				throw new IOException( "Could not read " + name + " as a gz compressed file", e);
			}
		} else if (name.endsWith(".bz2")) {
			try {
				is.read(); is.read();
				is = new CBZip2InputStream(is);
			} catch (IOException e) {
				throw new IOException( "Could not read " + name + " as a bz2 compressed file", e);
			}
		} else if (name.endsWith(".zip")) {
			ZipInputStream zis = new ZipInputStream(is);
			name = new File(name).getName();  // Strip off any path
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().startsWith(name.substring(0, name.length() - 4))) {
					is = zis;
					break;
				}
			}
			if (is != zis) {
				zis.close();
				throw new IOException("Unable to find a file inside " + name + " that starts with " + name.substring(0, name.length() - 4));
			}
		}
		if (backgroundReader) {
			is = new BackgroundInputStream(is);
		}
		return new InputStreamReader(is, Charset.forName("UTF-8"));
	}
}