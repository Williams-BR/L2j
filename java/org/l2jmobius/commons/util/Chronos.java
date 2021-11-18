/*
 * Copyright (c) 2021 Pantelis Andrianakis
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.l2jmobius.commons.util;

/**
 * Chronos updates an internal long value with System.currentTimeMillis() approximately every 1 millisecond.<br>
 * To get the current time in milliseconds use Chronos.currentTimeMillis()
 * @author Pantelis Andrianakis
 * @version February 3rd 2021
 */
public class Chronos extends Thread
{
	private long _currentTimeMillis = System.currentTimeMillis();
	
	public Chronos()
	{
		super.setName("Chronos");
		super.setPriority(MAX_PRIORITY);
		super.setDaemon(true);
		super.start();
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			_currentTimeMillis = System.currentTimeMillis();
			
			// Sleep for approximately 1 millisecond.
			try
			{
				Thread.sleep(1);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Returns the current time in milliseconds. Note that while the unit of time of the return value is a millisecond,the granularity of the value depends on the underlying operating system and may be larger. For example, many operating systems measure time in units of tens of milliseconds.<br>
	 * <br>
	 * See the description of the class Date for a discussion of slight discrepancies that may arise between "computer time" and coordinated universal time (UTC).
	 * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
	 */
	public static long currentTimeMillis()
	{
		return getInstance()._currentTimeMillis;
	}
	
	public static Chronos getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Chronos INSTANCE = new Chronos();
	}
}
