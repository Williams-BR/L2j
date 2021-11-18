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
package org.l2jmobius.gameserver.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.ControllableMobAI;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.ControllableMobInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author littlecrow
 */
public class MobGroup
{
	private final NpcTemplate _npcTemplate;
	private final int _groupId;
	private final int _maxMobCount;
	
	private List<ControllableMobInstance> _mobs;
	
	public MobGroup(int groupId, NpcTemplate npcTemplate, int maxMobCount)
	{
		_groupId = groupId;
		_npcTemplate = npcTemplate;
		_maxMobCount = maxMobCount;
	}
	
	public int getActiveMobCount()
	{
		return getMobs().size();
	}
	
	public int getGroupId()
	{
		return _groupId;
	}
	
	public int getMaxMobCount()
	{
		return _maxMobCount;
	}
	
	public List<ControllableMobInstance> getMobs()
	{
		if (_mobs == null)
		{
			_mobs = new CopyOnWriteArrayList<>();
		}
		return _mobs;
	}
	
	public String getStatus()
	{
		try
		{
			switch (((ControllableMobAI) getMobs().get(0).getAI()).getAlternateAI())
			{
				case ControllableMobAI.AI_NORMAL:
				{
					return "Idle";
				}
				case ControllableMobAI.AI_FORCEATTACK:
				{
					return "Force Attacking";
				}
				case ControllableMobAI.AI_FOLLOW:
				{
					return "Following";
				}
				case ControllableMobAI.AI_CAST:
				{
					return "Casting";
				}
				case ControllableMobAI.AI_ATTACK_GROUP:
				{
					return "Attacking Group";
				}
				default:
				{
					return "Idle";
				}
			}
		}
		catch (Exception e)
		{
			return "Unspawned";
		}
	}
	
	public NpcTemplate getTemplate()
	{
		return _npcTemplate;
	}
	
	public boolean isGroupMember(ControllableMobInstance mobInst)
	{
		for (ControllableMobInstance groupMember : getMobs())
		{
			if (groupMember == null)
			{
				continue;
			}
			
			if (groupMember.getObjectId() == mobInst.getObjectId())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void spawnGroup(int x, int y, int z)
	{
		if (!getMobs().isEmpty())
		{
			return;
		}
		
		try
		{
			for (int i = 0; i < _maxMobCount; i++)
			{
				final GroupSpawn spawn = new GroupSpawn(_npcTemplate);
				final int signX = Rnd.nextBoolean() ? -1 : 1;
				final int signY = Rnd.nextBoolean() ? -1 : 1;
				final int randX = Rnd.get(MobGroupTable.RANDOM_RANGE);
				final int randY = Rnd.get(MobGroupTable.RANDOM_RANGE);
				spawn.setXYZ(x + (signX * randX), y + (signY * randY), z);
				spawn.stopRespawn();
				
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				getMobs().add((ControllableMobInstance) spawn.doGroupSpawn());
			}
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
	
	public void spawnGroup(PlayerInstance player)
	{
		spawnGroup(player.getX(), player.getY(), player.getZ());
	}
	
	public void teleportGroup(PlayerInstance player)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				final int x = player.getX() + Rnd.get(50);
				final int y = player.getY() + Rnd.get(50);
				mobInst.teleToLocation(new Location(x, y, player.getZ()), true);
				((ControllableMobAI) mobInst.getAI()).follow(player);
			}
		}
	}
	
	public ControllableMobInstance getRandomMob()
	{
		removeDead();
		return getMobs().isEmpty() ? null : getMobs().get(Rnd.get(getMobs().size()));
	}
	
	public void unspawnGroup()
	{
		removeDead();
		
		if (getMobs().isEmpty())
		{
			return;
		}
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.deleteMe();
			}
			
			SpawnTable.getInstance().deleteSpawn(mobInst.getSpawn(), false);
		}
		
		getMobs().clear();
	}
	
	public void killGroup(PlayerInstance player)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.reduceCurrentHp(mobInst.getMaxHp() + 1, player, null);
			}
			
			SpawnTable.getInstance().deleteSpawn(mobInst.getSpawn(), false);
		}
		
		getMobs().clear();
	}
	
	public void setAttackRandom()
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			final ControllableMobAI ai = (ControllableMobAI) mobInst.getAI();
			ai.setAlternateAI(ControllableMobAI.AI_NORMAL);
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
	
	public void setAttackTarget(Creature target)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).forceAttack(target);
		}
	}
	
	public void setIdleMode()
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).stop();
		}
	}
	
	public void returnGroup(Creature creature)
	{
		setIdleMode();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).move(creature.getX() + ((Rnd.nextBoolean() ? -1 : 1) * Rnd.get(MobGroupTable.RANDOM_RANGE)), creature.getY() + ((Rnd.nextBoolean() ? -1 : 1) * Rnd.get(MobGroupTable.RANDOM_RANGE)), creature.getZ());
		}
	}
	
	public void setFollowMode(Creature creature)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).follow(creature);
		}
	}
	
	public void setCastMode()
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).setAlternateAI(ControllableMobAI.AI_CAST);
		}
	}
	
	public void setNoMoveMode(boolean enabled)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).setNotMoving(enabled);
		}
	}
	
	protected void removeDead()
	{
		final List<ControllableMobInstance> deadMobs = new LinkedList<>();
		for (ControllableMobInstance mobInst : getMobs())
		{
			if ((mobInst != null) && mobInst.isDead())
			{
				deadMobs.add(mobInst);
			}
		}
		
		getMobs().removeAll(deadMobs);
	}
	
	public void setInvul(boolean invulState)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst != null)
			{
				mobInst.setInvul(invulState);
			}
		}
	}
	
	public void setAttackGroup(MobGroup otherGrp)
	{
		removeDead();
		
		for (ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			final ControllableMobAI ai = (ControllableMobAI) mobInst.getAI();
			ai.forceAttackGroup(otherGrp);
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
}