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

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author Mobius
 */
public class PlayerAutoSaveTaskManager implements Runnable
{
	private static final Map<PlayerInstance, Long> PLAYER_TIMES = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected PlayerAutoSaveTaskManager()
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
		
		final long time = Chronos.currentTimeMillis();
		SEARCH: for (Entry<PlayerInstance, Long> entry : PLAYER_TIMES.entrySet())
		{
			if (time > entry.getValue().longValue())
			{
				final PlayerInstance player = entry.getKey();
				if ((player != null) && player.isOnline())
				{
					player.autoSave();
					PLAYER_TIMES.put(entry.getKey(), time + Config.CHAR_DATA_STORE_INTERVAL);
					break SEARCH; // Prevent SQL flood.
				}
			}
		}
		
		_working = false;
	}
	
	public void add(PlayerInstance player)
	{
		PLAYER_TIMES.put(player, Chronos.currentTimeMillis() + Config.CHAR_DATA_STORE_INTERVAL);
	}
	
	public void remove(PlayerInstance player)
	{
		PLAYER_TIMES.remove(player);
	}
	
	public static PlayerAutoSaveTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerAutoSaveTaskManager INSTANCE = new PlayerAutoSaveTaskManager();
	}
}
