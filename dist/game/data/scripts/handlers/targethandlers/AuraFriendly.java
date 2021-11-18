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

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * Aura Friendly target handler implementation.
 * @author Sahar
 */
public class AuraFriendly implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final PlayerInstance player = creature.getActingPlayer();
		final int maxTargets = skill.getAffectLimit();
		World.getInstance().forEachVisibleObject(player, Creature.class, obj ->
		{
			if ((obj == creature) || !checkTarget(player, obj))
			{
				return;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				return;
			}
			
			targetList.add(obj);
		});
		
		if (targetList.isEmpty())
		{
			return EMPTY_TARGET_LIST;
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	private boolean checkTarget(PlayerInstance player, Creature target)
	{
		if ((target == null) || !GeoEngine.getInstance().canSeeTarget(player, target))
		{
			return false;
		}
		
		if (target.isAlikeDead() || target.isDoor() || (target instanceof SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			final PlayerInstance targetPlayer = target.getActingPlayer();
			if (player.isInDuelWith(target))
			{
				return false;
			}
			
			if (player.isInPartyWith(target))
			{
				return true;
			}
			
			if (target.isInsideZone(ZoneId.PVP))
			{
				return false;
			}
			
			if (player.isInClanWith(target) || player.isInAllyWith(target) || player.isInCommandChannelWith(target))
			{
				return true;
			}
			
			if ((targetPlayer.getPvpFlag() > 0) || (targetPlayer.getKarma() > 0))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.AURA_FRIENDLY;
	}
}