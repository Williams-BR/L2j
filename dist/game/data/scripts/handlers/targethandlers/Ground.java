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
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author St3eT
 */
public class Ground implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final PlayerInstance player = (PlayerInstance) creature;
		final int maxTargets = skill.getAffectLimit();
		final boolean srcInArena = (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE));
		World.getInstance().forEachVisibleObject(creature, Creature.class, character ->
		{
			if ((character != null) && character.isInsideRadius2D(player.getCurrentSkillWorldPosition(), skill.getAffectRange()))
			{
				if (!Skill.checkForAreaOffensiveSkills(creature, character, skill, srcInArena))
				{
					return;
				}
				
				if (character.isDoor())
				{
					return;
				}
				
				if ((maxTargets > 0) && (targetList.size() >= maxTargets))
				{
					return;
				}
				targetList.add(character);
			}
		});
		
		if (targetList.isEmpty() && skill.hasEffectType(EffectType.SUMMON_NPC))
		{
			targetList.add(creature);
		}
		return targetList.isEmpty() ? EMPTY_TARGET_LIST : targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.GROUND;
	}
}