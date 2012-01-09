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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Splits a density map into multiple areas, none of which
 * exceed the desired threshold.
 *
 * @author Chris Miller
 */
public class SplittableDensityArea implements SplittableArea {
	private DensityMap densities;

	public SplittableDensityArea(DensityMap densities) {
		this.densities = densities;
	}

	@Override
	public Area getBounds() {
		return densities.getBounds();
	}


	public double getAspectRatio() {
		Area bounds = densities.getBounds();
		int width1 = (int) (densities.getWidth() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
		int width2 = (int) (densities.getWidth() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMaxLat()))));
		int width = Math.max(width1, width2);		
		int height = densities.getHeight();
		double ratio = ((double)width)/height;
		return ratio;
	}
	
	
	@Override
	public List<Area> split(int maxNodes) {
		if (densities == null || densities.getNodeCount() == 0)
			return Collections.emptyList();

		Area bounds = densities.getBounds();
		if (densities.getNodeCount() <= maxNodes) {
			System.out.println("Area " + bounds + " contains " + Utils.format(densities.getNodeCount())
					+ " nodes. DONE!");
			densities = null;
			return Collections.singletonList(bounds);
		}

		if (densities.getWidth() < 4 && densities.getHeight() < 4) {
			System.out.println("Area " + bounds + " contains " + Utils.format(densities.getNodeCount())
							+ " nodes but is already at the minimum size so can't be split further");
			return Collections.singletonList(bounds);
		}

		// Decide whether to split vertically or horizontally and go ahead with the split


		SplittableDensityArea[] splitResult = null;

		Integer splitX = getSplitHoriz();
		Integer splitY = getSplitVert();
		
		// Try to split it based on dimension.
		if (getAspectRatio() <= 1.0 && densities.getHeight() >= 4 && splitY != null) {
			splitResult = splitVert(splitY);
		}
		// Either the natural split is horizontal, or no good vertical split. Try horizontal.
		if (splitResult == null && densities.getWidth() >= 4 && splitX != null) {
			splitResult = splitHoriz(splitX);
		} 
		// If the natural horizontal split failed. Try vertical.
		if (getAspectRatio() > 1.0 && splitResult == null && densities.getHeight() >= 4 && splitY != null) {
			splitResult = splitVert(splitY);
		} 
		// No dice. Use this as-is.
		if (splitResult == null) {
			System.out.println("Area " + bounds + " contains " + Utils.format(densities.getNodeCount())
					+ " nodes but can't be split further");
			return Collections.singletonList(bounds);
		}
		densities = null;
		return mixResults(
				splitResult[0].split(maxNodes),
				splitResult[1].split(maxNodes));		
	}

	/** Merge two result lists of regions */
	List<Area> mixResults(List<Area> a1, List<Area> a2) {
		List<Area> results = new ArrayList<Area>();
	
		Iterator<Area> i0 = a1.iterator();
		Iterator<Area> i1 = a2.iterator();

		while (i0.hasNext() && i1.hasNext()) {
		    results.add(i0.next());
		    results.add(i1.next());
		}

		while (i0.hasNext()) {
		    results.add(i0.next());
		}
		while (i1.hasNext()) {
		    results.add(i1.next());
		}
		Collections.reverse(results);
		return results;
	}

	/**
	 * Split into left and right areas. Requires width >= 4 (so that we can have a even midpoint.
	 */
	protected Integer getSplitHoriz() {
		long sum = 0, weightedSum = 0;

		for (int x = 0; x < densities.getWidth(); x++) {
			for (int y = 0; y < densities.getHeight(); y++) {
				int count = densities.getNodeCount(x, y);
				sum += count;
				weightedSum += (count * x);
			}
		}
		return limit(0, densities.getWidth(), (int) (weightedSum / sum));
	}
		
	/** Get the actual split areas */
	protected SplittableDensityArea[] splitHoriz(int splitX) {
		
		Area bounds = densities.getBounds();
		int mid = bounds.getMinLong() + (splitX << densities.getShift());
		Area leftArea = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area rightArea = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());
		DensityMap left = densities.subset(leftArea);
		DensityMap right = densities.subset(rightArea);

		return new SplittableDensityArea[] {new SplittableDensityArea(left), new SplittableDensityArea(right)};
	}

	/**
	 * Split into top and bottom areas. Requires height >= 4 (so that we can have a even midpoint.
	 */
	protected Integer getSplitVert() {
		long sum = 0, weightedSum = 0;
		for (int y = 0; y < densities.getHeight(); y++) {
			for (int x = 0; x < densities.getWidth(); x++) {
				int count = densities.getNodeCount(x, y);
				sum += count;
				weightedSum += (count * y);
			}
		}
		return limit(0, densities.getHeight(), (int) (weightedSum / sum));
	}

	/** Get the actual split areas */
	protected SplittableDensityArea[] splitVert(int splitY) {
		
		Area bounds = densities.getBounds();
		int mid = bounds.getMinLat() + (splitY << densities.getShift());
		Area bottomArea = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area topArea = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());
		DensityMap bottom = densities.subset(bottomArea);
		DensityMap top = densities.subset(topArea);

		return new SplittableDensityArea[]{new SplittableDensityArea(bottom), new SplittableDensityArea(top)};
	}

	/** return calcOffset if it is in the middle three quantiles, use the first or last quantile otherwise. */
	private Integer limit(int first, int second, long calcOffset) {
		int mid = first + (int) calcOffset;
		int limitoff = (second - first) / 5;
		if (mid - first < limitoff)
			mid = first + limitoff;
		else if (second - mid < limitoff)
			mid = second - limitoff;

		if (mid % 2 != 0)
			mid--;
		if (mid == first || mid == second)
			return null;
		
		return mid;
	}
}
