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
package org.l2jmobius.gameserver.model.conditions;

import java.util.List;

import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.skills.Skill;

/**
 * The Class ConditionPlayerInstanceId.
 */
public class ConditionPlayerInstanceId extends Condition
{
	private final List<Integer> _instanceIds;
	
	/**
	 * Instantiates a new condition player instance id.
	 * @param instanceIds the instance ids
	 */
	public ConditionPlayerInstanceId(List<Integer> instanceIds)
	{
		_instanceIds = instanceIds;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.getActingPlayer() == null)
		{
			return false;
		}
		
		final int instanceId = effector.getInstanceId();
		if (instanceId <= 0)
		{
			return false; // player not in instance
		}
		
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(effector.getActingPlayer());
		return (world != null) && (world.getInstanceId() == instanceId) && _instanceIds.contains(world.getTemplateId());
	}
}
