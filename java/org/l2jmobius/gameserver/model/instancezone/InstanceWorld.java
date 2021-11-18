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
package org.l2jmobius.gameserver.model.instancezone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * Basic instance zone data transfer object.
 * @author Zoey76
 */
public class InstanceWorld
{
	private Instance _instance;
	private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet();
	private final StatSet _parameters = new StatSet();
	
	/**
	 * Sets the instance.
	 * @param instance the instance
	 */
	public void setInstance(Instance instance)
	{
		_instance = instance;
	}
	
	/**
	 * Gets the dynamically generated instance ID.
	 * @return the instance ID
	 */
	public int getInstanceId()
	{
		return _instance.getId();
	}
	
	/**
	 * Get template ID of instance world.
	 * @return instance template ID
	 */
	public int getTemplateId()
	{
		return _instance.getTemplateId();
	}
	
	public List<PlayerInstance> getAllowed()
	{
		final List<PlayerInstance> allowed = new ArrayList<>(_allowed.size());
		for (int playerId : _allowed)
		{
			final PlayerInstance player = World.getInstance().getPlayer(playerId);
			if (player != null)
			{
				allowed.add(player);
			}
		}
		return allowed;
	}
	
	public void removeAllowed(PlayerInstance player)
	{
		final Integer playerId = player.getId();
		_allowed.remove(playerId);
	}
	
	public void addAllowed(PlayerInstance player)
	{
		final Integer playerId = player.getId();
		if (!_allowed.contains(playerId))
		{
			_allowed.add(playerId);
		}
	}
	
	public boolean isAllowed(PlayerInstance player)
	{
		return _allowed.contains(player.getId());
	}
	
	/**
	 * Set instance world parameter.
	 * @param key parameter name
	 * @param value parameter value
	 */
	public void setParameter(String key, Object value)
	{
		if (value == null)
		{
			_parameters.remove(key);
		}
		else
		{
			_parameters.set(key, value);
		}
	}
	
	/**
	 * Get instance world parameters.
	 * @return instance parameters
	 */
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Get status of instance world.
	 * @return instance status, otherwise 0
	 */
	public int getStatus()
	{
		return _parameters.getInt("INSTANCE_STATUS", 0);
	}
	
	/**
	 * Check if instance status is equal to {@code status}.
	 * @param status number used for status comparison
	 * @return {@code true} when instance status and {@code status} are equal, otherwise {@code false}
	 */
	public boolean isStatus(int status)
	{
		return getStatus() == status;
	}
	
	/**
	 * Set status of instance world.
	 * @param value new world status
	 */
	public void setStatus(int value)
	{
		_parameters.set("INSTANCE_STATUS", value);
	}
	
	/**
	 * Increment instance world status
	 * @return new world status
	 */
	public int incStatus()
	{
		final int status = getStatus() + 1;
		setStatus(status);
		return status;
	}
	
	/**
	 * Get spawned NPCs from instance.
	 * @return set of NPCs from instance
	 */
	public Collection<Npc> getNpcs()
	{
		return _instance.getNpcs();
	}
	
	/**
	 * Get alive NPCs from instance.
	 * @return set of NPCs from instance
	 */
	public List<Npc> getAliveNpcs()
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (npc.getCurrentHp() > 0)
			{
				result.add(npc);
			}
		}
		return result;
	}
	
	/**
	 * Get spawned NPCs from instance with specific IDs.
	 * @param id IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	public List<Npc> getNpcs(int... id)
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (CommonUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		return result;
	}
	
	/**
	 * Get spawned NPCs from instance with specific IDs and class type.
	 * @param <T>
	 * @param clazz
	 * @param ids IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new ArrayList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (((ids.length == 0) || CommonUtil.contains(ids, npc.getId())) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		return result;
	}
	
	/**
	 * Get spawned and alive NPCs from instance with specific IDs and class type.
	 * @param <T>
	 * @param clazz
	 * @param ids IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getAliveNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new ArrayList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if ((((ids.length == 0) || CommonUtil.contains(ids, npc.getId())) && (npc.getCurrentHp() > 0)) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		return result;
	}
	
	/**
	 * Get alive NPCs from instance with specific IDs.
	 * @param id IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	public List<Npc> getAliveNpcs(int... id)
	{
		final List<Npc> result = new ArrayList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if ((npc.getCurrentHp() > 0) && CommonUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		return result;
	}
	
	/**
	 * Get first found spawned NPC with specific ID.
	 * @param id ID of NPC to be found
	 * @return first found NPC with specified ID, otherwise {@code null}
	 */
	public Npc getNpc(int id)
	{
		for (Npc npc : _instance.getNpcs())
		{
			if (npc.getId() == id)
			{
				return npc;
			}
		}
		return null;
	}
	
	/**
	 * Spawns group of instance NPCs
	 * @param groupName the name of group from XML definition to spawn
	 * @return list of spawned NPCs
	 */
	public List<Npc> spawnGroup(String groupName)
	{
		return _instance.spawnGroup(groupName);
	}
	
	/**
	 * Open a door if it is present in the instance and its not open.
	 * @param doorId the ID of the door to open
	 */
	public void openDoor(int doorId)
	{
		final DoorInstance door = _instance.getDoor(doorId);
		if ((door != null) && !door.isOpen())
		{
			door.openMe();
		}
	}
	
	/**
	 * Close a door if it is present in the instance and its open.
	 * @param doorId the ID of the door to close
	 */
	public void closeDoor(int doorId)
	{
		final DoorInstance door = _instance.getDoor(doorId);
		if ((door != null) && door.isOpen())
		{
			door.closeMe();
		}
	}
}
