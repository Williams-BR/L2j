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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;

/**
 * Backstab effect implementation.
 * @author Adry_85
 */
public class Backstab extends AbstractEffect
{
	public Backstab(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return !info.getEffector().isInFrontOf(info.getEffected()) && !Formulas.calcPhysicalSkillEvasion(info.getEffector(), info.getEffected(), info.getSkill()) && Formulas.calcBlowSuccess(info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffector().isAlikeDead())
		{
			return;
		}
		
		final Creature target = info.getEffected();
		final Creature creature = info.getEffector();
		final Skill skill = info.getSkill();
		final boolean ss = skill.useSoulShot() && creature.isChargedShot(ShotType.SOULSHOTS);
		final byte shld = Formulas.calcShldUse(creature, target, skill);
		double damage = Formulas.calcBackstabDamage(creature, target, skill, shld, ss);
		
		// Crit rate base crit rate for skill, modified with STR bonus
		if (Formulas.calcCrit(creature, target, skill))
		{
			damage *= 2;
		}
		
		target.reduceCurrentHp(damage, creature, skill);
		target.notifyDamageReceived(damage, creature, skill, true, false);
		
		// Manage attack or cast break of the target (calculating rate, sending message...)
		if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
		{
			target.breakAttack();
			target.breakCast();
		}
		
		if (creature.isPlayer())
		{
			final PlayerInstance activePlayer = creature.getActingPlayer();
			activePlayer.sendDamageMessage(target, (int) damage, false, true, false);
		}
		
		// Check if damage should be reflected
		Formulas.calcDamageReflected(creature, target, skill, true);
	}
}