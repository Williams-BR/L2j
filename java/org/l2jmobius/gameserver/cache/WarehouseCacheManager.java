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
package org.l2jmobius.gameserver.cache;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author -Nemesiss-
 */
public class WarehouseCacheManager
{
	protected static final Map<PlayerInstance, Long> CACHED_WH = new ConcurrentHashMap<>();
	protected static final long CACHE_TIME = Config.WAREHOUSE_CACHE_TIME * 60000;
	
	protected WarehouseCacheManager()
	{
		ThreadPool.scheduleAtFixedRate(new CacheScheduler(), 120000, 60000);
	}
	
	public void addCacheTask(PlayerInstance pc)
	{
		CACHED_WH.put(pc, Chronos.currentTimeMillis());
	}
	
	public void remCacheTask(PlayerInstance pc)
	{
		CACHED_WH.remove(pc);
	}
	
	private class CacheScheduler implements Runnable
	{
		public CacheScheduler()
		{
		}
		
		@Override
		public void run()
		{
			final long cTime = Chronos.currentTimeMillis();
			for (Entry<PlayerInstance, Long> entry : CACHED_WH.entrySet())
			{
				if ((cTime - entry.getValue().longValue()) > CACHE_TIME)
				{
					final PlayerInstance player = entry.getKey();
					player.clearWarehouse();
					CACHED_WH.remove(player);
				}
			}
		}
	}
	
	public static WarehouseCacheManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final WarehouseCacheManager INSTANCE = new WarehouseCacheManager();
	}
}
