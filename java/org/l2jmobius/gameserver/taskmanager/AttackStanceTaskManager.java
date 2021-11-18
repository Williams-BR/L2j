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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.CubicInstance;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;

/**
 * Attack stance task manager.
 * @author Luca Baldi
 */
public class AttackStanceTaskManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(AttackStanceTaskManager.class.getName());
	
	public static final long COMBAT_TIME = 15000;
	
	private static final Map<Creature, Long> _attackStanceTasks = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected AttackStanceTaskManager()
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
		
		final long current = Chronos.currentTimeMillis();
		try
		{
			final Iterator<Entry<Creature, Long>> iterator = _attackStanceTasks.entrySet().iterator();
			Entry<Creature, Long> entry;
			Creature creature;
			while (iterator.hasNext())
			{
				entry = iterator.next();
				if ((current - entry.getValue().longValue()) > COMBAT_TIME)
				{
					creature = entry.getKey();
					if (creature != null)
					{
						creature.broadcastPacket(new AutoAttackStop(creature.getObjectId()));
						creature.getAI().setAutoAttacking(false);
						if (creature.isPlayer() && creature.hasSummon())
						{
							creature.getSummon().broadcastPacket(new AutoAttackStop(creature.getSummon().getObjectId()));
						}
					}
					iterator.remove();
				}
			}
		}
		catch (Exception e)
		{
			// Unless caught here, players remain in attack positions.
			LOGGER.log(Level.WARNING, "Error in AttackStanceTaskManager: " + e.getMessage(), e);
		}
		
		_working = false;
	}
	
	/**
	 * Adds the attack stance task.
	 * @param creature the actor
	 */
	public void addAttackStanceTask(Creature creature)
	{
		if (creature == null)
		{
			return;
		}
		
		if (creature.isPlayable())
		{
			for (CubicInstance cubic : creature.getActingPlayer().getCubics().values())
			{
				if (cubic.getId() != CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction();
				}
			}
		}
		_attackStanceTasks.put(creature, Chronos.currentTimeMillis());
	}
	
	/**
	 * Removes the attack stance task.
	 * @param creature the actor
	 */
	public void removeAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.getActingPlayer();
			}
			_attackStanceTasks.remove(actor);
		}
	}
	
	/**
	 * Checks for attack stance task.
	 * @param creature the actor
	 * @return {@code true} if the character has an attack stance task, {@code false} otherwise
	 */
	public boolean hasAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.getActingPlayer();
			}
			return _attackStanceTasks.containsKey(actor);
		}
		return false;
	}
	
	/**
	 * Gets the single instance of AttackStanceTaskManager.
	 * @return single instance of AttackStanceTaskManager
	 */
	public static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager INSTANCE = new AttackStanceTaskManager();
	}
}
