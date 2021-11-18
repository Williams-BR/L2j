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

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import org.l2jmobius.gameserver.model.FortSiegeSpawn;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

public class FortCommanderInstance extends DefenderInstance
{
	private boolean _canTalk;
	
	/**
	 * Creates a fort commander.
	 * @param template the fort commander NPC template
	 */
	public FortCommanderInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FortCommanderInstance);
		_canTalk = true;
	}
	
	/**
	 * Return True if a siege is in progress and the Creature attacker isn't a Defender.
	 * @param attacker The Creature that the CommanderInstance try to attack
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if ((attacker == null) || !attacker.isPlayer())
		{
			return false;
		}
		
		final boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getSiege().isInProgress() && !getFort().getSiege().checkIsDefender(((PlayerInstance) attacker).getClan()));
		
		// Attackable during siege by all except defenders
		return (isFort);
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof FortCommanderInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (getFort().getSiege().isInProgress())
		{
			getFort().getSiege().killedCommander(this);
		}
		
		return true;
	}
	
	/**
	 * This method forces guard to return to home location previously set
	 */
	@Override
	public void returnHome()
	{
		if (!isInsideRadius2D(getSpawn(), 200))
		{
			setReturningToSpawnPoint(true);
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getSpawn().getLocation());
			}
		}
	}
	
	@Override
	public void addDamage(Creature creature, int damage, Skill skill)
	{
		Creature attacker = creature;
		final Spawn spawn = getSpawn();
		if ((spawn != null) && canTalk())
		{
			final List<FortSiegeSpawn> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getResidenceId());
			for (FortSiegeSpawn spawn2 : commanders)
			{
				if (spawn2.getId() == spawn.getId())
				{
					NpcStringId npcString = null;
					switch (spawn2.getMessageId())
					{
						case 1:
						{
							npcString = NpcStringId.ATTACKING_THE_ENEMY_S_REINFORCEMENTS_IS_NECESSARY_TIME_TO_DIE;
							break;
						}
						case 2:
						{
							if (attacker.isSummon())
							{
								attacker = ((Summon) attacker).getOwner();
							}
							npcString = NpcStringId.EVERYONE_CONCENTRATE_YOUR_ATTACKS_ON_S1_SHOW_THE_ENEMY_YOUR_RESOLVE;
							break;
						}
						case 3:
						{
							npcString = NpcStringId.SPIRIT_OF_FIRE_UNLEASH_YOUR_POWER_BURN_THE_ENEMY;
							break;
						}
					}
					if (npcString != null)
					{
						final NpcSay ns = new NpcSay(getObjectId(), ChatType.NPC_SHOUT, getId(), npcString);
						if (npcString.getParamCount() == 1)
						{
							ns.addStringParameter(attacker.getName());
						}
						
						broadcastPacket(ns);
						setCanTalk(false);
						ThreadPool.schedule(new ScheduleTalkTask(), 10000);
					}
				}
			}
		}
		super.addDamage(attacker, damage, skill);
	}
	
	private class ScheduleTalkTask implements Runnable
	{
		public ScheduleTalkTask()
		{
		}
		
		@Override
		public void run()
		{
			setCanTalk(true);
		}
	}
	
	void setCanTalk(boolean value)
	{
		_canTalk = value;
	}
	
	private boolean canTalk()
	{
		return _canTalk;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
