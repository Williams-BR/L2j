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
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class AreaCorpseMob implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		if ((target == null) || !target.isAttackable() || !target.isDead())
		{
			creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return EMPTY_TARGET_LIST;
		}
		
		if (onlyFirst)
		{
			return new Creature[]
			{
				target
			};
		}
		
		final List<Creature> targetList = new ArrayList<>();
		targetList.add(target);
		
		final boolean srcInArena = creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE);
		World.getInstance().forEachVisibleObject(creature, Creature.class, obj ->
		{
			if (!(obj.isAttackable() || obj.isPlayable()) || !Util.checkIfInRange(skill.getAffectRange(), target, obj, true))
			{
				return;
			}
			
			if (!Skill.checkForAreaOffensiveSkills(creature, obj, skill, srcInArena))
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
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.AREA_CORPSE_MOB;
	}
}
