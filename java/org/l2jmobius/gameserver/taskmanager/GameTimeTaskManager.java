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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.instancemanager.DayNightSpawnManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.util.UnboundArrayList;

/**
 * Game Time task manager class.
 * @author Forsaiken
 */
public class GameTimeTaskManager extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(GameTimeTaskManager.class.getName());
	
	public static final int TICKS_PER_SECOND = 10; // not able to change this without checking through code
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	
	private static final UnboundArrayList<Creature> _movingObjects = new UnboundArrayList<>();
	private final long _referenceTime;
	
	protected GameTimeTaskManager()
	{
		super("GameTimeTaskManager");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	public int getGameTime()
	{
		return (getGameTicks() % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
	}
	
	public int getGameHour()
	{
		return getGameTime() / 60;
	}
	
	public int getGameMinute()
	{
		return getGameTime() % 60;
	}
	
	public boolean isNight()
	{
		return getGameHour() < 6;
	}
	
	/**
	 * The true GameTime tick. Directly taken from current time. This represents the tick of the time.
	 * @return
	 */
	public int getGameTicks()
	{
		return (int) ((Chronos.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
	}
	
	/**
	 * Add a Creature to movingObjects of GameTimeTaskManager.
	 * @param creature The Creature to add to movingObjects of GameTimeTaskManager
	 */
	public void registerMovingObject(Creature creature)
	{
		if (creature == null)
		{
			return;
		}
		
		_movingObjects.addIfAbsent(creature);
	}
	
	/**
	 * Move all Creatures contained in movingObjects of GameTimeTaskManager.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * All Creature in movement are identified in <b>movingObjects</b> of GameTimeTaskManager.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <ul>
	 * <li>Update the position of each Creature</li>
	 * <li>If movement is finished, the Creature is removed from movingObjects</li>
	 * <li>Create a task to update the _knownObject and _knowPlayers of each Creature that finished its movement and of their already known WorldObject then notify AI with EVT_ARRIVED</li>
	 * </ul>
	 */
	private void moveObjects()
	{
		List<Creature> finished = null;
		for (int i = 0; i < _movingObjects.size(); i++)
		{
			final Creature creature = _movingObjects.get(i);
			if (creature == null)
			{
				continue;
			}
			
			if (creature.updatePosition())
			{
				if (finished == null)
				{
					finished = new ArrayList<>();
				}
				finished.add(creature);
			}
		}
		
		if (finished != null)
		{
			for (int i = 0; i < finished.size(); i++)
			{
				_movingObjects.remove(finished.get(i));
			}
		}
	}
	
	public void stopTimer()
	{
		super.interrupt();
		LOGGER.info(getClass().getSimpleName() + ": Stopped.");
	}
	
	@Override
	public void run()
	{
		LOGGER.info(getClass().getSimpleName() + ": Started.");
		
		long nextTickTime;
		long sleepTime;
		boolean isNight = isNight();
		
		if (isNight)
		{
			ThreadPool.execute(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
		}
		
		while (true)
		{
			nextTickTime = ((Chronos.currentTimeMillis() / MILLIS_IN_TICK) * MILLIS_IN_TICK) + 100;
			
			try
			{
				moveObjects();
			}
			catch (Throwable e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName(), e);
			}
			
			sleepTime = nextTickTime - Chronos.currentTimeMillis();
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (Exception e)
				{
					// Ignore.
				}
			}
			
			if (isNight() != isNight)
			{
				isNight = !isNight;
				ThreadPool.execute(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
			}
		}
	}
	
	public static final GameTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeTaskManager INSTANCE = new GameTimeTaskManager();
	}
}