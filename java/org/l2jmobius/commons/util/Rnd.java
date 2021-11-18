/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.commons.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mobius
 */
public class Rnd
{
	private static final int MINIMUM_POSITIVE_INT = 1;
	private static final long MINIMUM_POSITIVE_LONG = 1L;
	private static final double MINIMUM_POSITIVE_DOUBLE = Double.longBitsToDouble(0x1L);
	
	/**
	 * @return a random boolean value.
	 */
	public static boolean nextBoolean()
	{
		return ThreadLocalRandom.current().nextBoolean();
	}
	
	/**
	 * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the length of the byte array.
	 * @param bytes the byte array to fill with random bytes.
	 */
	public static void nextBytes(byte[] bytes)
	{
		ThreadLocalRandom.current().nextBytes(bytes);
	}
	
	/**
	 * @param bound (int)
	 * @return a random int value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static int get(int bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextInt(bound);
	}
	
	/**
	 * @param origin (int)
	 * @param bound (int)
	 * @return a random int value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static int get(int origin, int bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextInt(origin, bound == Integer.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_INT);
	}
	
	/**
	 * @return a random int value.
	 */
	public static int nextInt()
	{
		return ThreadLocalRandom.current().nextInt();
	}
	
	/**
	 * @param bound (long)
	 * @return a random long value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static long get(long bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextLong(bound);
	}
	
	/**
	 * @param origin (long)
	 * @param bound (long)
	 * @return a random long value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static long get(long origin, long bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextLong(origin, bound == Long.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_LONG);
	}
	
	/**
	 * @return a random long value.
	 */
	public static long nextLong()
	{
		return ThreadLocalRandom.current().nextLong();
	}
	
	/**
	 * @param bound (double)
	 * @return a random double value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static double get(double bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextDouble(bound);
	}
	
	/**
	 * @param origin (double)
	 * @param bound (double)
	 * @return a random double value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static double get(double origin, double bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextDouble(origin, bound == Double.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_DOUBLE);
	}
	
	/**
	 * @return a random double value between zero (inclusive) and one (exclusive).
	 */
	public static double nextDouble()
	{
		return ThreadLocalRandom.current().nextDouble();
	}
	
	/**
	 * @return the next random, Gaussian ("normally") distributed double value with mean 0.0 and standard deviation 1.0 from this random number generator's sequence.
	 */
	public static double nextGaussian()
	{
		return ThreadLocalRandom.current().nextGaussian();
	}
}
