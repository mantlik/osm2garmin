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

/**
 * Chris Miller
 */
package uk.me.parabola.splitter;

/**
 * Maintains a list of int primitives. Internally the ints are held
 * in multiple arrays so we can grow without having to copy one huge
 * array and momentarily require 2x the amount of memory.
 */
public class SplitIntList {
	private static final int DEFAULT_SEGMENT_SIZE = 1000000;

	private final int segmentSize;
	private int[][] segments = new int[0][];
	private int size;

	public SplitIntList() {
		this(DEFAULT_SEGMENT_SIZE);
	}

	public SplitIntList(int segmentSize) {
		this.segmentSize = segmentSize;
	}

	public void add(int value) {
		ensureCapacity();
		segments[segments.length - 1][size++ % segmentSize] = value;
	}

	public int get(int i) {
		return segments[i / segmentSize][i % segmentSize];
	}

	public int size() {
		return size;
	}

	private void ensureCapacity() {
		if (size % segmentSize == 0) {
			int[][] temp = segments;
			segments = new int[temp.length + 1][];
			System.arraycopy(temp, 0, segments, 0, temp.length);
			segments[temp.length] = new int[segmentSize];
		}
	}

	public Iterator getIterator() {
		return new Iterator();
	}

	/**
	 * @return an iterator that deletes segments as it is finished with
	 * them. This is useful when copying the contents into other lists
	 * since it means we can free up some memory as we go rather than
	 * holding it all until the copy has finished.
	 */
	public Iterator getDeletingIterator() {
		return new Iterator(true);
	}

	/**
	 * Iterates over all the segments.
	 */
	public class Iterator {
		private final boolean deleteAfter;
		private int currentIndex;

		private Iterator(boolean deleteAfter) {
			this.deleteAfter = deleteAfter;
		}

		private Iterator() {
			this(false);
		}

		public boolean hasNext() {
			return currentIndex < size;
		}

		public int next() {
			int result = SplitIntList.this.get(currentIndex++);
			if (deleteAfter && currentIndex % segmentSize == 0) {
				// throw away the previous segment
				segments[(currentIndex - 1) / segmentSize] = null;
			}
			return result;
		}
	}
}
