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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.concurrent.Future;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Decoy;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.taskmanager.DecayTaskManager;

public class DecoyInstance extends Decoy
{
	private int _totalLifeTime;
	private int _timeRemaining;
	private Future<?> _decoyLifeTask;
	private Future<?> _hateSpam;
	
	/**
	 * Creates a decoy.
	 * @param template the decoy NPC template
	 * @param owner the owner
	 * @param totalLifeTime the total life time
	 */
	public DecoyInstance(NpcTemplate template, PlayerInstance owner, int totalLifeTime)
	{
		super(template, owner);
		setInstanceType(InstanceType.DecoyInstance);
		_totalLifeTime = totalLifeTime;
		_timeRemaining = _totalLifeTime;
		final int skilllevel = getTemplate().getDisplayId() - 13070;
		_decoyLifeTask = ThreadPool.scheduleAtFixedRate(new DecoyLifetime(getOwner(), this), 1000, 1000);
		_hateSpam = ThreadPool.scheduleAtFixedRate(new HateSpam(this, SkillData.getInstance().getSkill(5272, skilllevel)), 2000, 5000);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		if (_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		_totalLifeTime = 0;
		DecayTaskManager.getInstance().add(this);
		return true;
	}
	
	static class DecoyLifetime implements Runnable
	{
		private final PlayerInstance _player;
		
		private final DecoyInstance _decoy;
		
		DecoyLifetime(PlayerInstance player, DecoyInstance decoy)
		{
			_player = player;
			_decoy = decoy;
		}
		
		@Override
		public void run()
		{
			try
			{
				_decoy.decTimeRemaining(1000);
				final double newTimeRemaining = _decoy.getTimeRemaining();
				if (newTimeRemaining < 0)
				{
					_decoy.unSummon(_player);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Decoy Error: ", e);
			}
		}
	}
	
	private static class HateSpam implements Runnable
	{
		private final DecoyInstance _player;
		private final Skill _skill;
		
		HateSpam(DecoyInstance player, Skill hate)
		{
			_player = player;
			_skill = hate;
		}
		
		@Override
		public void run()
		{
			try
			{
				_player.setTarget(_player);
				_player.doCast(_skill);
			}
			catch (Throwable e)
			{
				LOGGER.log(Level.SEVERE, "Decoy Error: ", e);
			}
		}
	}
	
	@Override
	public synchronized void unSummon(PlayerInstance owner)
	{
		if (_decoyLifeTask != null)
		{
			_decoyLifeTask.cancel(true);
			_decoyLifeTask = null;
		}
		if (_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		super.unSummon(owner);
	}
	
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
}
