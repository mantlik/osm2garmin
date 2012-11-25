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

public interface MapProcessor {

	/**
	 * @return {@code true} if this processor is only interested in
	 * {@link #startNode(int, double,double)} events, {@code false}
	 * if all events are handled.
	 * <p/>
	 * If this is set to {@code true}, the caller can significantly
	 * cut down on the amount of work it has to do.
	 */
	boolean isStartNodeOnly();

	/**
	 * Called when the bound tag is encountered. Note that it is possible
	 * for this to be called multiple times, eg if there are multiple OSM
	 * files provided as input.
	 * @param bounds the area covered by the map.
	 */
	void boundTag(Area bounds);


	/**
	 * Called when a whole node has been processed. 
	*/
	void processNode(Node n);

	void processWay(Way w);
	
	void processRelation(Relation w);


	/**
	 * Called once the entire map has finished processing.
	 */
	void endMap();
}
