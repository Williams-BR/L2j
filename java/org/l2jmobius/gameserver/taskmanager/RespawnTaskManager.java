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
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;

/**
 * @author Mobius
 */
public class RespawnTaskManager implements Runnable
{
	private static final Map<Npc, Long> PENDING_RESPAWNS = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected RespawnTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 0, 1000);
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
		for (Entry<Npc, Long> entry : PENDING_RESPAWNS.entrySet())
		{
			if (time > entry.getValue().longValue())
			{
				final Npc npc = entry.getKey();
				PENDING_RESPAWNS.remove(npc);
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					spawn.respawnNpc(npc);
					spawn._scheduledCount--;
				}
			}
		}
		
		_working = false;
	}
	
	public void add(Npc npc, long time)
	{
		PENDING_RESPAWNS.put(npc, time);
	}
	
	public static RespawnTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RespawnTaskManager INSTANCE = new RespawnTaskManager();
	}
}
