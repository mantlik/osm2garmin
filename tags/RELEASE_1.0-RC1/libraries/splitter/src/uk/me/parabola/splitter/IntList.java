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

import java.util.Arrays;


/**
 * Maintains a list of int primitives.
 */
public class IntList {
	private static final int DEFAULT_BUFFER_SIZE = 10;

	private int[] data;
	private int size;

	public IntList() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public IntList(int initialSize) {
		data = new int[initialSize];
	}

	public void add(int value) {
		ensureCapacity();
		data[size++] = value;
	}

	public int get(int i) {
		return data[i];
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	private void ensureCapacity() {
		if (size == data.length - 1) {
			int[] temp = data;
			data = new int[size * 3 / 2 + 1];
			System.arraycopy(temp, 0, data, 0, size);
		}
	}

	/** Get as a read-only array of integers */
	int []asArray() {
		return Arrays.copyOf(data,size);
	}
}