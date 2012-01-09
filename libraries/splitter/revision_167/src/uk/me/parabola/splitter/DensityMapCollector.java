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
 * Builds up a density map.
 */
class DensityMapCollector implements MapCollector {

	private final DensityMap densityMap;
	private final MapDetails details = new MapDetails();
	private Area bounds;

	DensityMapCollector(boolean trim, int resolution) {
		this(null, trim, resolution);
	}

	DensityMapCollector(Area bounds, boolean trim, int resolution) {
		if (bounds == null) {
			// If we don't receive any bounds we have to assume the whole planet
			bounds = new Area(-0x400000, -0x800000, 0x400000, 0x800000);
		}
		densityMap = new DensityMap(bounds, trim, resolution);
	}

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
		int glat = Utils.toMapUnit(n.getLat());
		int glon = Utils.toMapUnit(n.getLon());
		densityMap.addNode(glat, glon);
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
		return new SplittableDensityArea(densityMap.subset(bounds));
	}}