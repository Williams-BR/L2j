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
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;

/**
 * @author UnAfraid
 */
public class PartyClan implements ITargetTypeHandler
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
		
		final PlayerInstance player = creature.getActingPlayer();
		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}
		
		targetList.add(player);
		
		final int radius = skill.getAffectRange();
		final boolean hasClan = player.getClan() != null;
		final boolean hasParty = player.isInParty();
		if (Skill.addSummon(creature, player, radius, false))
		{
			targetList.add(player.getSummon());
		}
		
		// if player in clan and not in party
		if (!(hasClan || hasParty))
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}
		
		// Get all visible objects in a spherical area near the Creature
		final int maxTargets = skill.getAffectLimit();
		for (PlayerInstance obj : World.getInstance().getVisibleObjectsInRange(creature, PlayerInstance.class, radius))
		{
			if (obj == null)
			{
				continue;
			}
			
			// olympiad mode - adding only own side
			if (player.isInOlympiadMode())
			{
				if (!obj.isInOlympiadMode())
				{
					continue;
				}
				if (player.getOlympiadGameId() != obj.getOlympiadGameId())
				{
					continue;
				}
				if (player.getOlympiadSide() != obj.getOlympiadSide())
				{
					continue;
				}
			}
			
			if (player.isInDuel())
			{
				if (player.getDuelId() != obj.getDuelId())
				{
					continue;
				}
				
				if (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
				{
					continue;
				}
			}
			
			if (!((hasClan && (obj.getClanId() == player.getClanId())) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() == obj.getParty().getLeaderObjectId()))))
			{
				continue;
			}
			
			// Don't add this target if this is a Pc->Pc pvp
			// casting and pvp condition not met
			if (!player.checkPvpSkill(obj, skill))
			{
				continue;
			}
			
			if (player.isOnEvent() && !player.isOnSoloEvent() && obj.isOnEvent() && (player.getTeam() != obj.getTeam()))
			{
				continue;
			}
			
			if (!onlyFirst && Skill.addSummon(creature, obj, radius, false))
			{
				targetList.add(obj.getSummon());
			}
			
			if (!Skill.addCharacter(creature, obj, radius, false))
			{
				continue;
			}
			
			if (onlyFirst)
			{
				return new Creature[]
				{
					obj
				};
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
			
			targetList.add(obj);
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PARTY_CLAN;
	}
}
