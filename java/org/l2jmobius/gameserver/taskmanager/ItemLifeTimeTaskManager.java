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
package org.l2jmobius.gameserver.taskmanager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;

/**
 * @author Mobius
 */
public class ItemLifeTimeTaskManager implements Runnable
{
	private static final Map<ItemInstance, Long> ITEMS = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected ItemLifeTimeTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		_working = true;
		
		final long currentTime = Chronos.currentTimeMillis();
		for (Entry<ItemInstance, Long> entry : ITEMS.entrySet())
		{
			if (currentTime > entry.getValue().longValue())
			{
				final ItemInstance item = entry.getKey();
				ITEMS.remove(item);
				item.endOfLife();
			}
		}
		
		_working = false;
	}
	
	public void add(ItemInstance item, long endTime)
	{
		if (!ITEMS.containsKey(item))
		{
			ITEMS.put(item, endTime);
		}
	}
	
	public void remove(ItemInstance item)
	{
		ITEMS.remove(item);
	}
	
	public static ItemLifeTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemLifeTimeTaskManager INSTANCE = new ItemLifeTimeTaskManager();
	}
}
