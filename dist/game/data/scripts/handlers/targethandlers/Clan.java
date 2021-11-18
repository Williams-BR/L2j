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
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;

/**
 * @author UnAfraid
 */
public class Clan implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if (creature.isPlayable())
		{
			final PlayerInstance player = creature.getActingPlayer();
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new Creature[]
				{
					player
				};
			}
			
			if (onlyFirst)
			{
				return new Creature[]
				{
					player
				};
			}
			
			targetList.add(player);
			
			final int radius = skill.getAffectRange();
			final org.l2jmobius.gameserver.model.clan.Clan clan = player.getClan();
			if (Skill.addSummon(creature, player, radius, false))
			{
				targetList.add(player.getSummon());
			}
			
			if (clan != null)
			{
				PlayerInstance obj;
				for (ClanMember member : clan.getMembers())
				{
					obj = member.getPlayerInstance();
					if ((obj == null) || (obj == player))
					{
						continue;
					}
					
					if (player.isInDuel())
					{
						if (player.getDuelId() != obj.getDuelId())
						{
							continue;
						}
						if (player.isInParty() && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
						{
							continue;
						}
					}
					
					// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
					if (!player.checkPvpSkill(obj, skill))
					{
						continue;
					}
					
					if (player.isOnEvent() && obj.isOnEvent() && (player.getTeam() != obj.getTeam()))
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
					
					targetList.add(obj);
				}
			}
		}
		else if (creature.isNpc())
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			final Npc npc = (Npc) creature;
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return new Creature[]
				{
					creature
				};
			}
			
			targetList.add(creature);
			
			for (Npc newTarget : World.getInstance().getVisibleObjectsInRange(creature, Npc.class, skill.getCastRange()))
			{
				if (newTarget.isNpc() && npc.isInMyClan(newTarget))
				{
					final int maxTargets = skill.getAffectLimit();
					if ((maxTargets > 0) && (targetList.size() >= maxTargets))
					{
						break;
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CLAN;
	}
}
