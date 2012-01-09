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
 * Collects node coordinates in a map.
 */
class NodeCollector implements MapCollector {

	private SplitIntList coords = new SplitIntList();
	private final MapDetails details = new MapDetails();
	private Area bounds;

	@Override
	public boolean isStartNodeOnly() {
		return true;
	}

	@Override
	public void boundTag(Area bounds) {
		if (this.bounds == null)
			this.bounds = bounds;
		else
			this.bounds = this.bounds.add(bounds);
	}

	@Override
	public void processNode(Node n) {
		// Since we are rounding areas to fit on a low zoom boundary we
		// can drop the bottom 8 bits of the lat and lon and then fit
		// the whole lot into a single int.

		int glat = n.getMapLat();
		int glon = n.getMapLon();
		int coord = ((glat << 8) & 0xffff0000) + ((glon >> 8) & 0xffff);

		coords.add(coord);
		details.addToBounds(glat, glon);
	}

	@Override
	public void processWay(Way w) {}

	@Override
	public void processRelation(Relation r) {}

	@Override
	public void endMap() {}

	@Override
	public Area getExactArea() {
		if (bounds != null) {
			return bounds;
		} else {
			return details.getBounds();
		}
	}

	@Override
	public SplittableArea getRoundedArea(int resolution) {
		Area bounds = RoundingUtils.round(getExactArea(), resolution);
		SplittableArea result = new SplittableNodeArea(bounds, coords, resolution);
		coords = null;
		return result;
	}
}