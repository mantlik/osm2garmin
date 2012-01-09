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

import java.io.File;
import java.io.IOException;

public abstract class OSMWriter {
	
	protected final Area bounds;
	protected Area extendedBounds;
	protected File outputDir;
	

	public OSMWriter(Area bounds, File outputDir) {
		this.bounds = bounds;
		this.outputDir = outputDir;
	}

	public Area getExtendedBounds() {
		return extendedBounds;
	}
	
	public abstract void initForWrite(int mapId, int extra);

	public abstract void finishWrite();

	public boolean nodeBelongsToThisArea(Node node) {
		return (extendedBounds.contains(node.getMapLat(), node.getMapLon()));
	}
	
	public abstract void write(Node node) throws IOException;

	public abstract void write(Way way) throws IOException;

	public abstract void write(Relation rel) throws IOException;
}
