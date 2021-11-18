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
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author UnAfraid
 */
public class CorpseClan implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<WorldObject> targetList = new ArrayList<>();
		if (creature.isPlayable())
		{
			final PlayerInstance player = creature.getActingPlayer();
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new WorldObject[]
				{
					player
				};
			}
			
			final Clan clan = player.getClan();
			if (clan != null)
			{
				final int radius = skill.getAffectRange();
				final int maxTargets = skill.getAffectLimit();
				for (ClanMember member : clan.getMembers())
				{
					final PlayerInstance obj = member.getPlayerInstance();
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
					
					if (player.isOnEvent() && !player.isOnSoloEvent() && obj.isOnEvent() && (player.getTeam() != obj.getTeam()))
					{
						continue;
					}
					
					if (!Skill.addCharacter(creature, obj, radius, true))
					{
						continue;
					}
					
					// check target is not in a active siege zone
					if (obj.isInsideZone(ZoneId.SIEGE) && !obj.isInSiege())
					{
						continue;
					}
					
					if (onlyFirst)
					{
						return new WorldObject[]
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
			}
		}
		else if (creature.isNpc())
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			final Npc npc = (Npc) creature;
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return new WorldObject[]
				{
					creature
				};
			}
			
			targetList.add(creature);
			
			for (Npc newTarget : World.getInstance().getVisibleObjectsInRange(creature, Npc.class, skill.getCastRange()))
			{
				if (npc.isInMyClan(newTarget))
				{
					if (targetList.size() >= skill.getAffectLimit())
					{
						break;
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList.toArray(new WorldObject[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CORPSE_CLAN;
	}
}
