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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import org.l2jmobius.Config;

public class CommonUtil
{
	private static final char[] ILLEGAL_CHARACTERS =
	{
		'/',
		'\n',
		'\r',
		'\t',
		'\0',
		'\f',
		'`',
		'?',
		'*',
		'\\',
		'<',
		'>',
		'|',
		'\"',
		':'
	};
	
	/**
	 * Method to generate the hexadecimal representation of a byte array.<br>
	 * 16 bytes per row, while ascii chars or "." is shown at the end of the line.
	 * @param data the byte array to be represented in hexadecimal representation
	 * @param len the number of bytes to represent in hexadecimal representation
	 * @return byte array represented in hexadecimal format
	 */
	public static String printData(byte[] data, int len)
	{
		return new String(HexUtils.bArr2HexEdChars(data, len));
	}
	
	/**
	 * This call is equivalent to Util.printData(data, data.length)
	 * @see CommonUtil#printData(byte[],int)
	 * @param data data to represent in hexadecimal
	 * @return byte array represented in hexadecimal format
	 */
	public static String printData(byte[] data)
	{
		return printData(data, data.length);
	}
	
	/**
	 * Method to represent the remaining bytes of a ByteBuffer as hexadecimal
	 * @param buf ByteBuffer to represent the remaining bytes of as hexadecimal
	 * @return hexadecimal representation of remaining bytes of the ByteBuffer
	 */
	public static String printData(ByteBuffer buf)
	{
		final byte[] data = new byte[buf.remaining()];
		buf.get(data);
		final String hex = printData(data, data.length);
		buf.position(buf.position() - data.length);
		return hex;
	}
	
	/**
	 * Method to generate a random sequence of bytes returned as byte array
	 * @param size number of random bytes to generate
	 * @return byte array with sequence of random bytes
	 */
	public static byte[] generateHex(int size)
	{
		final byte[] array = new byte[size];
		Rnd.nextBytes(array);
		
		// Don't allow 0s inside the array!
		for (int i = 0; i < array.length; i++)
		{
			while (array[i] == 0)
			{
				array[i] = (byte) Rnd.get(Byte.MAX_VALUE);
			}
		}
		return array;
	}
	
	/**
	 * Replaces most invalid characters for the given string with an underscore.
	 * @param str the string that may contain invalid characters
	 * @return the string with invalid character replaced by underscores
	 */
	public static String replaceIllegalCharacters(String str)
	{
		String valid = str;
		for (char c : ILLEGAL_CHARACTERS)
		{
			valid = valid.replace(c, '_');
		}
		return valid;
	}
	
	/**
	 * Verify if a file name is valid.
	 * @param name the name of the file
	 * @return {@code true} if the file name is valid, {@code false} otherwise
	 */
	public static boolean isValidFileName(String name)
	{
		final File f = new File(name);
		try
		{
			f.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	/**
	 * Split words with a space.
	 * @param input the string to split
	 * @return the split string
	 */
	public static String splitWords(String input)
	{
		return input.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
	}
	
	/**
	 * Gets the next or same closest date from the specified days in {@code daysOfWeek Array} at specified {@code hour} and {@code min}.
	 * @param daysOfWeek the days of week
	 * @param hour the hour
	 * @param min the min
	 * @return the next or same date from the days of week at specified time
	 */
	public static LocalDateTime getNextClosestDateTime(DayOfWeek[] daysOfWeek, int hour, int min)
	{
		return getNextClosestDateTime(Arrays.asList(daysOfWeek), hour, min);
	}
	
	/**
	 * Gets the next or same closest date from the specified days in {@code daysOfWeek List} at specified {@code hour} and {@code min}.
	 * @param daysOfWeek the days of week
	 * @param hour the hour
	 * @param min the min
	 * @return the next or same date from the days of week at specified time
	 */
	public static LocalDateTime getNextClosestDateTime(List<DayOfWeek> daysOfWeek, int hour, int min)
	{
		if (daysOfWeek.isEmpty())
		{
			throw new IllegalArgumentException("daysOfWeek should not be empty.");
		}
		
		final LocalDateTime dateNow = LocalDateTime.now();
		final LocalDateTime dateNowWithDifferentTime = dateNow.withHour(hour).withMinute(min).withSecond(0);
		
		// @formatter:off
		return daysOfWeek.stream()
			.map(d -> dateNowWithDifferentTime.with(TemporalAdjusters.nextOrSame(d)))
			.filter(d -> d.isAfter(dateNow))
			.min(Comparator.naturalOrder())
			.orElse(dateNowWithDifferentTime.with(TemporalAdjusters.next(daysOfWeek.get(0))));
		// @formatter:on
	}
	
	/**
	 * Method to get the stack trace of a Throwable into a String
	 * @param t Throwable to get the stacktrace from
	 * @return stack trace from Throwable as String
	 */
	public static String getStackTrace(Throwable t)
	{
		final StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static String getTraceString(StackTraceElement[] stackTraceElements)
	{
		final StringJoiner sj = new StringJoiner(Config.EOL);
		for (StackTraceElement stackTraceElement : stackTraceElements)
		{
			sj.add(stackTraceElement.toString());
		}
		return sj.toString();
	}
	
	/**
	 * @param <T>
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
		{
			if (element.equals(obj))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the integer to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int obj)
	{
		for (int element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @param ignoreCase
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static boolean contains(String[] array, String obj, boolean ignoreCase)
	{
		for (String element : array)
		{
			if (element.equals(obj) || (ignoreCase && element.equalsIgnoreCase(obj)))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param numToTest : The number to test.
	 * @param min : The minimum limit.
	 * @param max : The maximum limit.
	 * @return the number or one of the limit (mininum / maximum).
	 */
	public static int limit(int numToTest, int min, int max)
	{
		return (numToTest > max) ? max : ((numToTest < min) ? min : numToTest);
	}
}
