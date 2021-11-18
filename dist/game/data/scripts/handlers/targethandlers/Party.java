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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;

/**
 * @author UnAfraid
 */
public class Party implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if (onlyFirst)
		{
			return new Creature[]
			{
				creature
			};
		}
		
		targetList.add(creature);
		
		final int radius = skill.getAffectRange();
		final PlayerInstance player = creature.getActingPlayer();
		if (creature.isSummon())
		{
			if (Skill.addCharacter(creature, player, radius, false))
			{
				targetList.add(player);
			}
		}
		else if (creature.isPlayer())
		{
			if (Skill.addSummon(creature, player, radius, false))
			{
				targetList.add(player.getSummon());
			}
		}
		
		if (creature.isInParty())
		{
			// Get a list of Party Members
			for (PlayerInstance partyMember : creature.getParty().getMembers())
			{
				if ((partyMember == null) || (partyMember == player))
				{
					continue;
				}
				
				if (Skill.addCharacter(creature, partyMember, radius, false))
				{
					targetList.add(partyMember);
				}
				
				if (Skill.addSummon(creature, partyMember, radius, false))
				{
					targetList.add(partyMember.getSummon());
				}
			}
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PARTY;
	}
}
