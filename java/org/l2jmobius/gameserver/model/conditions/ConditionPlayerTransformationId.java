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

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.skills.Skill;

/**
 * This condition becomes true whether the player is transformed and the transformation Id match the parameter or<br>
 * the parameter is -1 which returns true if player is transformed regardless the transformation Id.
 * @author Zoey76
 */
public class ConditionPlayerTransformationId extends Condition
{
	private final int _id;
	
	/**
	 * Instantiates a new condition player is transformed.
	 * @param id the transformation Id.
	 */
	public ConditionPlayerTransformationId(int id)
	{
		_id = id;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, Item item)
	{
		final PlayerInstance player = effector.getActingPlayer();
		return (player != null) && (_id == -1 ? player.isTransformed() : player.getTransformationId() == _id);
	}
}
