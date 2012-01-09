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
 * Builds up a map of node densities across the total area being split.
 * Density information is held at the maximum desired map resolution.
 * Every step up in resolution increases the size of the density map by
 * a factor of 4.
 *
 * @author Chris Miller
 */
public class DensityMap {
	private final int width, height, shift;
	private final int[][] nodeMap;
	private Area bounds;
	private int totalNodeCount;
	private boolean trim;

	/**
	 * Creates a density map.
	 * @param area the area that the density map covers.
	 * @param resolution the resolution of the density map. This must be a value between 1 and 24.
	 */
	public DensityMap(Area area, boolean trim, int resolution) {
		this.trim = trim;
		assert resolution >=1 && resolution <= 24;
		shift = 24 - resolution;

		bounds = RoundingUtils.round(area, resolution);
		height = bounds.getHeight() >> shift;
		width = bounds.getWidth() >> shift;
		nodeMap = new int[width][];
	}

	public int getShift() {
		return shift;
	}

	public Area getBounds() {
		return bounds;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int addNode(int lat, int lon) {
		if (!bounds.contains(lat, lon))
			return 0;

		totalNodeCount++;
		int x = lonToX(lon);
		if (x == width)
			x--;
		int y = latToY(lat);
		if (y == height)
			y--;

		if (nodeMap[x] == null)
			nodeMap[x] = new int[height];
		return ++nodeMap[x][y];
	}

	public int getNodeCount() {
		return totalNodeCount;
	}

	public int getNodeCount(int x, int y) {
		return nodeMap[x] != null ? nodeMap[x][y] : 0;
	}

	public DensityMap subset(Area subset) {
		int minLat = Math.max(bounds.getMinLat(), subset.getMinLat());
		int minLon = Math.max(bounds.getMinLong(), subset.getMinLong());
		int maxLat = Math.min(bounds.getMaxLat(), subset.getMaxLat());
		int maxLon = Math.min(bounds.getMaxLong(), subset.getMaxLong());

		// If the area doesn't intersect with the density map, return an empty map
		if (minLat > maxLat || minLon > maxLon) {
			return new DensityMap(Area.EMPTY, trim, 24 - shift);
		}

		subset = new Area(minLat, minLon, maxLat, maxLon);
		if (trim) {
			subset = trim(subset);
		}

		// If there's nothing in the area return an empty map
		if (subset.getWidth() == 0 || subset.getHeight() == 0) {
			return new DensityMap(Area.EMPTY, trim, 24 - shift);
		}

		DensityMap result = new DensityMap(subset, trim, 24 - shift);

		int startX = lonToX(subset.getMinLong());
		int startY = latToY(subset.getMinLat());
		int maxX = subset.getWidth() >> shift;
		int maxY = subset.getHeight() >> shift;
		for (int x = 0; x < maxX; x++) {
			if (startY == 0 && maxY == height) {
				result.nodeMap[x] = nodeMap[startX + x];
			} else if (nodeMap[startX + x] != null) {
				result.nodeMap[x] = new int[maxY];
				try {
					System.arraycopy(nodeMap[startX + x], startY, result.nodeMap[x], 0, maxY);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("subSet() died at " + startX + ',' + startY + "  " + maxX + ',' + maxY + "  " + x);
				}
			}
			for (int y = 0; y < maxY; y++) {
				if (result.nodeMap[x] != null)
					result.totalNodeCount += result.nodeMap[x][y];
			}
		}
		return result;
	}

	/**
	 * Sets the trimmed bounds based on any empty edges around the density map
	 */
	private Area trim(Area area) {

		int minX = lonToX(area.getMinLong());
		int maxX = lonToX(area.getMaxLong());
		int minY = latToY(area.getMinLat());
		int maxY = latToY(area.getMaxLat());

		while (minX < maxX && (nodeMap[minX] == null || isEmptyX(minX, minY, maxY))) {
			minX++;
		}
		if (minX == maxX) {
			return Area.EMPTY;
		}

		while (nodeMap[maxX - 1] == null || isEmptyX(maxX - 1, minY, maxY)) {
			maxX--;
		}

		while (minY < maxY && isEmptyY(minY, minX, maxX)) {
			minY++;
		}
		if (minY == maxY) {
			return Area.EMPTY;
		}

		while (isEmptyY(maxY - 1, minX, maxX)) {
			maxY--;
		}

		Area trimmedArea = new Area(yToLat(minY), xToLon(minX), yToLat(maxY), xToLon(maxX));
		Area rounded = RoundingUtils.round(trimmedArea, 24 - shift);

		// Make sure the rounding hasn't pushed the area outside its original boundaries
		int latAdjust = Math.max(0, rounded.getMaxLat() - area.getMaxLat());
		int lonAdjust = Math.max(0, rounded.getMaxLong() - area.getMaxLong());
		if (latAdjust > 0 || lonAdjust > 0) {
			rounded = new Area(rounded.getMinLat() - latAdjust,
							rounded.getMinLong() - lonAdjust,
							rounded.getMaxLat() - latAdjust,
							rounded.getMaxLong() - lonAdjust);
		}
		return rounded;
	}

	private boolean isEmptyX(int x, int start, int end) {
		int[] array = nodeMap[x];
		if (array != null) {
			for (int y = start; y < end; y++) {
				if (array[y] != 0)
					return false;
			}
		}
		return true;
	}

	private boolean isEmptyY(int y, int start, int end) {
		for (int x = start; x < end; x++) {
			if (nodeMap[x] != null && nodeMap[x][y] != 0)
				return false;
		}
		return true;
	}

	private int yToLat(int y) {
		return (y << shift) + bounds.getMinLat();
	}

	private int xToLon(int x) {
		return (x << shift) + bounds.getMinLong();
	}

	private int latToY(int lat) {
		return lat - bounds.getMinLat() >>> shift;
	}

	private int lonToX(int lon) {
		return lon - bounds.getMinLong() >>> shift;
	}
}
