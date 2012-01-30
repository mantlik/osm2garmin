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

/**
 * Utility methods for rounding numbers and areas
 *
 * @author Chris Miller
 */
public class RoundingUtils {
	/**
	 * Rounds an integer down to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round down.
	 * @param shift the power of two to round down to.
	 * @return the rounded integer.
	 */
	public static int roundDown(int val, int shift) {
		return val >>> shift << shift;
	}

	/**
	 * Rounds an integer up to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round up.
	 * @param shift the power of two to round up to.
	 * @return the rounded integer.
	 */
	public static int roundUp(int val, int shift) {
		return (val + (1 << shift) - 1) >>> shift << shift;
	}

	/**
	 * Rounds an integer up or down to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round.
	 * @param shift the power of two to round to.
	 * @return the rounded integer.
	 */
	public static int round(int val, int shift) {
		return (val + (1 << (shift - 1))) >>> shift << shift;
	}

	/**
	 * Rounds an area's borders off to suit the supplied resolution. This
	 * means edges are aligned at 2 ^ (24 - resolution) boundaries, and area
	 * widths and heights are multiples of twice the alignment.
	 *
	 * @param b the area to round
	 * @param resolution the map resolution to align the borders at
	 * @return the rounded area
	 */
	static Area round(Area b, int resolution) {
		int shift = 24 - resolution;
		int alignment = 1 << shift;
		int doubleAlignment = alignment << 1;

		// Avoid pathological behaviour near the poles by discarding anything
		// greater than +/-85 degrees latitude.
		int minLat = Math.max(b.getMinLat(), Utils.toMapUnit(-85.0d));
		int maxLat = Math.min(b.getMaxLat(), Utils.toMapUnit(85.0d));

		int roundedMinLat = roundUp(minLat, shift);
		int roundedMaxLat = roundUp(maxLat, shift);
		if ((roundedMinLat & alignment) != (roundedMaxLat & alignment)) {
			// The new height isn't a multiple of twice the alignment. Fix it by pushing
			// the tile edge that moved the least out by another 'alignment' units.
			if (minLat - roundedMinLat < maxLat - roundedMaxLat) {
				roundedMinLat -= alignment;
			} else {
				roundedMaxLat += alignment;
			}
		}
		assert roundedMinLat % alignment == 0 : "The area's min latitude is not aligned to a multiple of " + alignment;
		assert roundedMaxLat % alignment == 0 : "The area's max latitude is not aligned to a multiple of " + alignment;
		assert (roundedMaxLat - roundedMinLat) % doubleAlignment == 0 : "The area's height is not a multiple of " + doubleAlignment;

		int roundedMinLon = roundDown(b.getMinLong(), shift);
		int roundedMaxLon = roundDown(b.getMaxLong(), shift);
		if ((roundedMinLon & alignment) != (roundedMaxLon & alignment)) {
			// The new width isn't a multiple of twice the alignment. Fix it by pushing
			// the tile edge that moved the least out by another 'alignment' units.
			if (b.getMinLong() - roundedMinLon < roundedMaxLon - b.getMaxLong()) {
				roundedMinLon -= alignment;
			} else {
				roundedMaxLon += alignment;
			}
		}
		assert roundedMinLon % alignment == 0 : "The area's min longitude is not aligned to a multiple of " + alignment;
		assert roundedMaxLon % alignment == 0 : "The area's max longitude is not aligned to a multiple of " + alignment;
		assert (roundedMaxLon - roundedMinLon) % doubleAlignment == 0 : "The area's width is not a multiple of " + doubleAlignment;

		return new Area(roundedMinLat, roundedMinLon, roundedMaxLat, roundedMaxLon);
	}
}
