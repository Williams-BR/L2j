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

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Corpse Mob target handler.
 * @author UnAfraid, Zoey76
 */
public class CorpseMob implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		if ((target == null) || !target.isAttackable() || !target.isDead())
		{
			creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return EMPTY_TARGET_LIST;
		}
		
		if (skill.hasEffectType(EffectType.SUMMON) && target.isServitor() && (target.getActingPlayer() != null) && (target.getActingPlayer().getObjectId() == creature.getObjectId()))
		{
			return EMPTY_TARGET_LIST;
		}
		
		if (skill.hasEffectType(EffectType.HP_DRAIN) && ((Attackable) target).isOldCorpse(creature.getActingPlayer(), Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true))
		{
			return EMPTY_TARGET_LIST;
		}
		
		return new Creature[]
		{
			target
		};
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CORPSE_MOB;
	}
}
