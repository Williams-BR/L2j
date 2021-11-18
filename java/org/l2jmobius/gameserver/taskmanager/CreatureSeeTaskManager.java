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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;

/**
 * @author Mobius
 */
public class CreatureSeeTaskManager implements Runnable
{
	private static final Set<Creature> CREATURES = ConcurrentHashMap.newKeySet();
	private static boolean _working = false;
	
	protected CreatureSeeTaskManager()
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
		
		for (Creature creature : CREATURES)
		{
			creature.updateSeenCreatures();
		}
		
		_working = false;
	}
	
	public void add(Creature creature)
	{
		CREATURES.add(creature);
	}
	
	public void remove(Creature creature)
	{
		CREATURES.remove(creature);
	}
	
	public static CreatureSeeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureSeeTaskManager INSTANCE = new CreatureSeeTaskManager();
	}
}
