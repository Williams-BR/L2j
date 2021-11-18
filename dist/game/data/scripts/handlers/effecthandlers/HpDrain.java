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
import org.l2jmobius.gameserver.model.stats.Formulas;

/**
 * HP Drain effect implementation.
 * @author Adry_85
 */
public class HpDrain extends AbstractEffect
{
	private final double _power;
	
	public HpDrain(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HP_DRAIN;
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
		
		// TODO: Unhardcode Cubic Skill to avoid double damage
		if (creature.isAlikeDead() || (info.getSkill().getId() == 4050))
		{
			return;
		}
		
		final boolean sps = info.getSkill().useSpiritShot() && creature.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = info.getSkill().useSpiritShot() && creature.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcMCrit(creature.getMCriticalHit(target, info.getSkill()));
		final byte shld = Formulas.calcShldUse(creature, target, info.getSkill());
		final int damage = (int) Formulas.calcMagicDam(creature, target, info.getSkill(), shld, sps, bss, mcrit);
		int drain = 0;
		final int cp = (int) target.getCurrentCp();
		final int hp = (int) target.getCurrentHp();
		if (cp > 0)
		{
			drain = (damage < cp) ? 0 : (damage - cp);
		}
		else if (damage > hp)
		{
			drain = hp;
		}
		else
		{
			drain = damage;
		}
		
		final double hpAdd = (_power * drain);
		final double hpFinal = ((creature.getCurrentHp() + hpAdd) > creature.getMaxHp() ? creature.getMaxHp() : (creature.getCurrentHp() + hpAdd));
		creature.setCurrentHp(hpFinal);
		
		if (damage > 0)
		{
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			creature.sendDamageMessage(target, damage, mcrit, false, false);
			target.reduceCurrentHp(damage, creature, info.getSkill());
			target.notifyDamageReceived(damage, creature, info.getSkill(), mcrit, false);
		}
	}
}