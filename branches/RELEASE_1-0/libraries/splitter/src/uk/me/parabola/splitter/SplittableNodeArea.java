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
import java.util.List;

/**
 * Used to split an area down into roughly equal sized pieces.
 * They will all have less than a specified number of nodes.
 * 
 * @author Steve Ratcliffe
 */
public class SplittableNodeArea implements SplittableArea {
	private final int shift;
	private final int resolution;
	private final Area bounds;
	private SplitIntList coords;
	private int size;

	public SplittableNodeArea(Area bounds, int resolution) {
		this(bounds, new SplitIntList(), resolution);
	}

	public SplittableNodeArea(Area bounds, SplitIntList coords, int resolution) {
		this.bounds = bounds;
		this.coords = coords;
		this.resolution = resolution;
		shift = 24 - resolution;
	}

	@Override
	public Area getBounds() {
		return bounds;
	}

	public void clear() {
		if (coords != null)
			size = coords.size();
		coords = null;
	}

	private void add(int co) {
		coords.add(co);
	}

	public int getSize() {
		if (coords != null)
			return coords.size();
		else
			return size;
	}
	/**
	 * Split a single area which would normally be the complete area of the map.
	 * We just split areas that are too big into two.  We make a rough determination
	 * of the largest dimension and split that way.
	 * 
	 * @param area The original area.
	 * @param maxNodes The maximum number of nodes that any area can contain.
	 * @return An array of areas.  Each will have less than the specified number of nodes.
	 */
	@Override
	public List<Area> split(int maxNodes) {
		if (coords == null || coords.size() == 0)
			return Collections.emptyList();

		if (coords.size() < maxNodes) {
			clear();
			return Collections.singletonList(bounds);
		}

		int height = bounds.getHeight();
		int width = bounds.getWidth();

		// If we've already split the area down to the minimum allowable size, we don't split it further
		boolean minHeight = height <= 2 << shift;
		boolean minWidth = width <= 2 << shift;
		if (minHeight && minWidth) {
			System.out.println("Area " + bounds + " contains " + Utils.format(getSize())
							+ " nodes but is already at the minimum size so can't be split further");
			return Collections.singletonList(bounds);
		}

		List<Area> results = new ArrayList<Area>();

		// Decide whether to split vertically or horizontally and go ahead with the split
		int width1 = (int) (width * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
		int width2 = (int) (width * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMaxLat()))));
		width = Math.max(width1, width2);
		SplittableNodeArea[] splitResult;
		if (height > width && !minHeight) {
			splitResult = splitVert();
		} else {
			splitResult = splitHoriz();
		}
		clear();
		results.addAll(splitResult[0].split(maxNodes));
		results.addAll(splitResult[1].split(maxNodes));
		return results;
	}

	protected SplittableNodeArea[] splitHoriz() {
		int left = bounds.getMinLong();
		int right = bounds.getMaxLong();

	  SplitIntList.Iterator it = coords.getIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			int val = it.next();
			int lon = extractLongitude(val);
			assert lon >= left && lon <= right : lon;
			count++;
			total += lon - left + 1;
		}
		int mid = limit(left, right, total / count);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area b2 = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());

		SplittableNodeArea a1 = new SplittableNodeArea(b1, resolution);
		SplittableNodeArea a2 = new SplittableNodeArea(b2, resolution);

		it = coords.getDeletingIterator();
		while (it.hasNext()) {
			int co = it.next();
			if (extractLongitude(co) < mid) {
				a1.add(co);
			} else {
				a2.add(co);
			}
		}
		return new SplittableNodeArea[]{a1, a2};
	}

	protected SplittableNodeArea[] splitVert() {
		int top = bounds.getMaxLat();
		int bot = bounds.getMinLat();

		SplitIntList.Iterator it = coords.getIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			int val = it.next();
			int lat = extractLatitude(val);
			assert lat >= bot && extractLongitude(val) <= top : lat;
			count++;
			total += lat - bot;
		}
		int mid = limit(bot, top, total / count);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area b2 = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());

		SplittableNodeArea a1 = new SplittableNodeArea(b1, resolution);
		SplittableNodeArea a2 = new SplittableNodeArea(b2, resolution);

		it = coords.getDeletingIterator();
		while (it.hasNext()) {
			int co = it.next();
			if (extractLatitude(co) <= mid) {
				a1.add(co);
			} else {
				a2.add(co);
			}
		}
		return new SplittableNodeArea[]{a1, a2};
	}

	private int extractLatitude(int value) {
		return ((value & 0xffff0000) >> 8);
	}

	private int extractLongitude(int value) {
		int lon = value & 0xffff;
		if ((lon & 0x8000) != 0)
			lon |= 0xffff0000;
		return lon << 8;
	}

	private int limit(int first, int second, long calcOffset) {
		int mid = first + (int) calcOffset;
		int limitoff = (second - first) / 5;
		if (mid - first < limitoff)
			mid = first + limitoff;
		else if (second - mid < limitoff)
			mid = second - limitoff;

		// Round to a garmin map unit at the desired zoom level.
		int nmid = RoundingUtils.round(mid, shift);

		// Check that the midpoint is on the appropriate alignment boundary. If not, adjust
		int alignment = 1 << shift;
		if ((nmid & alignment) != (first & alignment)) {
			if (nmid < mid) {
				nmid += alignment;
  		} else {
				nmid -= alignment;
			}
		}

		// Check if we're going to end up on the edge of a tile. If so, move away. We always
		// have room to move away because a split is only attempted in the first place if
		// the tile to split is bigger than the minimum tile width.
		if (nmid == first) {
			nmid += alignment << 1;
		} else if (nmid == second) {
			nmid -= alignment << 1;
		}

		assert nmid > first && nmid < second;
		return nmid;
	}
}
