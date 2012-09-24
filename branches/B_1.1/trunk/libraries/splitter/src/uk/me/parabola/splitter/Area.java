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
 * A map area in map units.  There is a constructor available for creating
 * in lat/long form.
 *
 * @author Steve Ratcliffe
 */
public class Area {

	public static final Area EMPTY = new Area();

	private int mapId;
	private String name;
	private final int minLat;
	private final int minLong;
	private final int maxLat;
	private final int maxLong;

	/**
	 * Create an area from the given Garmin coordinates. We ensure that no dimension is zero.
	 *
	 * @param minLat The western latitude.
	 * @param minLong The southern longitude.
	 * @param maxLat The eastern lat.
	 * @param maxLong The northern long.
	 */
	public Area(int minLat, int minLong, int maxLat, int maxLong) {
		this.minLat = minLat;
		if (maxLat == minLat)
			this.maxLat = minLat + 1;
		else
			this.maxLat = maxLat;

		this.minLong = minLong;
		if (minLong == maxLong)
			this.maxLong = maxLong + 1;
		else
			this.maxLong = maxLong;
	}

	/**
	 * Creates an empty area.
	 */
	private Area() {
		minLat = 0;
		maxLat = 0;
		minLong = 0;
		maxLong = 0;
	}

	public void setMapId(int mapId) {
		this.mapId = mapId;
	}

	public int getMapId() {
		return mapId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMinLat() {
		return minLat;
	}

	public int getMinLong() {
		return minLong;
	}

	public int getMaxLat() {
		return maxLat;
	}

	public int getMaxLong() {
		return maxLong;
	}

	public int getWidth() {
		return maxLong - minLong;
	}

	public int getHeight() {
		return maxLat - minLat;
	}

	public String toString() {
		return "("
				+ Utils.toDegrees(minLat) + ','
				+ Utils.toDegrees(minLong) + ") to ("
				+ Utils.toDegrees(maxLat) + ','
				+ Utils.toDegrees(maxLong) + ')'
				;
	}

	public String toHexString() {
		return "(0x"
				+ Integer.toHexString(minLat) + ",0x"
				+ Integer.toHexString(minLong) + ") to (0x"
				+ Integer.toHexString(maxLat) + ",0x"
				+ Integer.toHexString(maxLong) + ')';
	}

	public boolean contains(int lat, int lon) {
		return lat >= minLat
				&& lat <= maxLat
				&& lon >= minLong
				&& lon <= maxLong;
	}

	public Area add(Area area) {
		return new Area(
						Math.min(minLat, area.minLat),
						Math.min(minLong, area.minLong),
						Math.max(maxLat, area.maxLat),
						Math.max(maxLong, area.maxLong)
		);
	}
}