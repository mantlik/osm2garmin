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

import org.xml.sax.SAXException;

/**
 * Thrown at the end on pass 1, where we are only interested in the nodes.
 * As soon as a way is seen this is thrown to indicate that we are done.
 * 
 * @author Steve Ratcliffe
 */
public class EndOfNodesException extends SAXException {
	public EndOfNodesException() {
		super("End of nodes");
	}
}
