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
package handlers.effecthandlers;

import org.l2jmobius.gameserver.enums.ShotType;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Physical Attack HP Link effect implementation.
 * @author Adry_85
 */
public class PhysicalAttackHpLink extends AbstractEffect
{
	public PhysicalAttackHpLink(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK_HP_LINK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final Creature target = info.getEffected();
		final Creature creature = info.getEffector();
		final Skill skill = info.getSkill();
		if (creature.isAlikeDead())
		{
			return;
		}
		
		if (creature.isMovementDisabled())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			creature.sendPacket(sm);
			return;
		}
		
		final byte shld = Formulas.calcShldUse(creature, target, skill);
		// Physical damage critical rate is only affected by STR.
		boolean crit = false;
		if (skill.getBaseCritRate() > 0)
		{
			crit = Formulas.calcCrit(creature, target, skill);
		}
		
		int damage = 0;
		final boolean ss = skill.isPhysical() && creature.isChargedShot(ShotType.SOULSHOTS);
		damage = (int) Formulas.calcPhysDam(creature, target, skill, shld, false, ss);
		if (damage > 0)
		{
			creature.sendDamageMessage(target, damage, false, crit, false);
			target.reduceCurrentHp(damage, creature, skill);
			target.notifyDamageReceived(damage, creature, skill, crit, false);
			
			// Check if damage should be reflected.
			Formulas.calcDamageReflected(creature, target, skill, crit);
		}
		else
		{
			creature.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
	}
}