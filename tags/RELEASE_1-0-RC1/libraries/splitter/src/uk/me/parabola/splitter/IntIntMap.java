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

import java.util.Iterator;

/**
 * A map from int to int, designed to minimise memory use while still maintaining
 * good performance.
 *
 * It doesn't behave exactly the same way as a map would. Note also that zero is
 * reserved and should not be used as a key. Doing so will result in undefined
 * behaviour.
 *
 * @author Steve Ratcliffe
 */
public class IntIntMap {
	private static final int INIT_SIZE = 1 << 16;

	private int size;
	private int[] keys;
	private int[] values;

	private int capacity;

	private int targetSize;
	private final float loadFactor;

	private static final int OFF = 1472057057;

	public IntIntMap() {
		this(INIT_SIZE, 0.9f);
	}

	public IntIntMap(int initCap, float load) {
		if (!Utils.isPowerOfTwo(initCap))
			throw new IllegalArgumentException("The initial capacity " + initCap + " must be a power of two");
		keys = new int[initCap];
		values = new int[initCap];
		capacity = initCap;

		loadFactor = load;
		targetSize = (int) (initCap * load);
		assert targetSize > 0;
	}


	public int size() {
		return size;
	}

	public int get(int key) {
		int ind = keyPos(key);
		if (keys[ind] == 0)
			return 0;

		return values[ind];
	}

	public int put(int key, int value) {
		ensureSpace();

		int ind = keyPos(key);
		keys[ind] = key;

		int old = values[ind];
		if (old == 0)
			size++;
		values[ind] = value;

		return old;
	}

	public Iterator<Entry> entryIterator() {
		return new Iterator<Entry>() {
			private final Entry entry = new Entry();
			private int itercount;

			public boolean hasNext() {
				while (itercount < capacity)
					if (values[itercount++] != 0)
						return true;
				return false;
			}

			public Entry next() {
				entry.setKey(keys[itercount-1]);
				entry.setValue(values[itercount-1]);
				return entry;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private void ensureSpace() {
		while (size + 1 >= targetSize) {
			int ncap = capacity << 1;
			targetSize = (int) (ncap * loadFactor);

			int[] okey = keys;
			int[] oval = values;

			size = 0;
			keys = new int[ncap];
			values = new int[ncap];
			capacity = ncap;
			//hit= miss = 0;
			for (int i = 0; i < okey.length; i++) {
				int k = okey[i];
				if (k != 0)
					put(k, oval[i]);
			}
		}
		assert size < capacity;
	}

	private int keyPos(int key) {
		int mask = capacity - 1;
		int k = key & mask;
		int h1 = keys[k];
		while (h1 != 0 && h1 != key) {
			k = (k + OFF) & mask;
			h1 = keys[k];
		}
		return k;
	}

	/**
     * An primative integer version of the Map.Entry class.
	 *
	 * @author Steve Ratcliffe
	 */
	public static class Entry {
		private int key;
		private int value;

		public int getKey() {
			return key;
		}

		void setKey(int key) {
			this.key = key;
		}

		public int getValue() {
			return value;
		}

		void setValue(int value) {
			this.value = value;
		}
	}
}
