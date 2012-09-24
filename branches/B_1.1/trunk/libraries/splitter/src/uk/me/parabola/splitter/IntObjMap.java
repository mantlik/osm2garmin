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
 * A map from int to Object, designed to minimise memory use while still maintaining
 * good performance.
 * <p/>
 * It doesn't behave exactly the same way as a map would. Note also that zero is
 * reserved and should not be used as a key. Doing so will result in undefined
 * behaviour.
 */
public class IntObjMap<V> {
	private static final int INIT_SIZE = 1 << 8;

	private int size;
	private int[] keys;
	private V[] values;

	private int capacity;

	private int targetSize;
	private final float loadFactor;

	private static final int OFF = 7;

	public IntObjMap() {
		this(INIT_SIZE, 0.9f);
	}

	public IntObjMap(int initCap, float load) {
		if (!Utils.isPowerOfTwo(initCap))
			throw new IllegalArgumentException("The initial capacity " + initCap + " must be a power of two");
		keys = new int[initCap];
		values = (V[]) new Object[initCap];
		capacity = initCap;

		loadFactor = load;
		targetSize = (int) (initCap * load);
		assert targetSize > 0;
	}


	public int size() {
		return size;
	}

	public V get(int key) {
		int ind = keyPos(key);
		if (keys[ind] == 0)
			return null;

		return values[ind];
	}

	public V put(int key, V value) {
		ensureSpace();

		int ind = keyPos(key);
		keys[ind] = key;

		V old = values[ind];
		if (old == null)
			size++;
		values[ind] = value;

		return old;
	}

	private void ensureSpace() {
		while (size + 1 >= targetSize) {
			int ncap = capacity << 1;
			targetSize = (int) (ncap * loadFactor);

			int[] okey = keys;
			V[] oval = values;

			size = 0;
			keys = new int[ncap];
			values = (V[]) new Object[ncap];
			capacity = ncap;
			for (int i = 0; i < okey.length; i++) {
				int k = okey[i];
				if (k != 0)
					put(k, oval[i]);
			}
		}
		assert size < capacity;
	}

	private int keyPos(int key) {
		int k = key & (capacity - 1);

		int h1 = keys[k];
		if (h1 != 0 && h1 != key) {
			for (int k2 = k+OFF; ; k2+= OFF) {
				if (k2 >= capacity)
					k2 -= capacity;

				int fk = keys[k2];
				if (fk == 0 || fk == key) {
					return k2;
				}
			}
		}
		return k;
	}
}