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
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author UnAfraid
 */
public class PcBody implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if ((target != null) && target.isDead())
		{
			final PlayerInstance player;
			if (creature.isPlayer())
			{
				player = creature.getActingPlayer();
			}
			else
			{
				player = null;
			}
			
			final PlayerInstance targetPlayer;
			if (target.isPlayer())
			{
				targetPlayer = target.getActingPlayer();
			}
			else
			{
				targetPlayer = null;
			}
			
			final PetInstance targetPet;
			if (target.isPet())
			{
				targetPet = (PetInstance) target;
			}
			else
			{
				targetPet = null;
			}
			
			if ((player != null) && ((targetPlayer != null) || (targetPet != null)))
			{
				boolean condGood = true;
				if (skill.hasEffectType(EffectType.RESURRECTION) && (targetPlayer != null))
				{
					// check target is not in a active siege zone
					if (targetPlayer.isInsideZone(ZoneId.SIEGE) && !targetPlayer.isInSiege())
					{
						condGood = false;
						creature.sendPacket(SystemMessageId.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEFIELDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE);
					}
					
					if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
					{
						condGood = false;
						creature.sendMessage("You may not resurrect participants in a festival.");
					}
				}
				
				if (condGood)
				{
					if (!onlyFirst)
					{
						targetList.add(target);
						return targetList.toArray(new WorldObject[targetList.size()]);
					}
					return new Creature[]
					{
						target
					};
				}
			}
		}
		creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		return EMPTY_TARGET_LIST;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PC_BODY;
	}
}
