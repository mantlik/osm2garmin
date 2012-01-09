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
 * Conversion utility methods
 *
 * @author Chris Miller
 */
public class Convert {

	/**
	 * Converts an int into a char[]. The supplied char[] must be at least 11 characters long.
	 * @param i the int to convert to a char[].
	 * @param buf the char[] to write the integer into.
	 * @return the number of characters that were written into the character array.
	 */
	public static int intToString(int i, char[] buf) {
		return intToString(i, buf, 0);
	}

	public static int intToString(int i, char[] buf, int startIndex) {
		if (i == Integer.MIN_VALUE) {
			System.arraycopy(MIN_VALUE, 0, buf, startIndex, MIN_VALUE.length);
			return MIN_VALUE.length;
		}
		int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
		getChars(i, startIndex + size, buf);
		return size;
	}

	private final static char[] MIN_VALUE = new char[] {'-','2','1','4','7','4','8','3','6','4','8'};
	final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};

	final static char[] DigitTens = {
					'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
					'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
					'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
					'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
					'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
					'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
					'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
					'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
					'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
					'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
	};

	final static char[] DigitOnes = {
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	};

	// Requires positive x
	static int stringSize(int x) {
		for (int i = 0; ; i++)
			if (x <= sizeTable[i])
				return i + 1;
	}

	/**
	 * Places characters representing the integer i into the
	 * character array buf. The characters are placed into
	 * the buffer backwards starting with the least significant
	 * digit at the specified index (exclusive), and working
	 * backwards from there.
	 *
	 * Will fail if i == Integer.MIN_VALUE
	 */
	static void getChars(int i, int charPos, char[] buf) {
		int q, r;
		char sign = 0;

		if (i < 0) {
			sign = '-';
			i = -i;
		}

		// Generate two digits per iteration
		while (i >= 65536) {
			q = i / 100;
			// really: r = i - (q * 100);
			r = i - ((q << 6) + (q << 5) + (q << 2));
			i = q;
			buf[--charPos] = DigitOnes[r];
			buf[--charPos] = DigitTens[r];
		}

		// Fall through to fast mode for smaller numbers
		for (; ;) {
			q = (i * 52429) >>> (16 + 3);
			r = i - ((q << 3) + (q << 1));
			// r = i-(q*10) ...
			buf[--charPos] = (char) ('0' + r);
			i = q;
			if (i == 0) break;
		}
		if (sign != 0) {
			buf[--charPos] = sign;
		}
	}

	private static final double[] PowersOfTen = new double[] {
					10d,
					100d,
					1000d,
					10000d,
					100000d,
					1000000d,
					10000000d,
					100000000d,
					1000000000d,
					10000000000d,
					100000000000d,
					1000000000000d,
					10000000000000d,
					100000000000000d,
					1000000000000000d,
					10000000000000000d,
					100000000000000000d,
					1000000000000000000d,
					10000000000000000000d,
	};

	/**
	 * Parses a string into a double. This code is optimised for performance
	 * when parsing typical doubles encountered in .osm files.
	 *
	 * @param cs the characters to parse into a double
	 * @return the double value represented by the string.
	 * @throws NumberFormatException if the value failed to parse.
	 */
	public static double parseDouble(String cs) throws NumberFormatException
	{
		int end = Math.min(cs.length(), 19);  // No point trying to handle more digits than a double precision number can deal with
		int i = 0;
		char c = cs.charAt(i);

		boolean isNegative = (c == '-');
		if ((isNegative || (c == '+')) && (++i < end))
			c = cs.charAt(i);

		long decimal = 0;
		int decimalPoint = -1;
		while (true) {
			int digit = c - '0';
			if ((digit >= 0) && (digit < 10)) {
				long tmp = decimal * 10 + digit;
				if (tmp < decimal)
					throw new NumberFormatException("Overflow! Too many digits in " + cs);
				decimal = tmp;
			} else if ((c == '.') && (decimalPoint < 0))
				decimalPoint = i;
			else {
				// We're out of our depth, let the JDK have a go. This is *much* slower
				return Double.parseDouble(cs);
			}
			if (++i >= end)
				break;
			c = cs.charAt(i);
		}
		if (isNegative)
			decimal = -decimal;

               if (decimalPoint >= 0 && decimalPoint < i - 1)
			return decimal / PowersOfTen[i - decimalPoint - 2];
		else
			return decimal;
	}
}
