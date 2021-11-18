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
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.CrystalType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Heal effect implementation.
 * @author UnAfraid
 */
public class Heal extends AbstractEffect
{
	private final double _power;
	
	public Heal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
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
		if ((target == null) || target.isDead() || target.isDoor() || target.isInvul())
		{
			return;
		}
		
		double amount = _power;
		double staticShotBonus = 0;
		int mAtkMul = 1;
		final boolean sps = info.getSkill().isMagic() && creature.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = info.getSkill().isMagic() && creature.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		if (((sps || bss) && (creature.isPlayer() && creature.getActingPlayer().isMageClass())) || creature.isSummon())
		{
			staticShotBonus = info.getSkill().getMpConsume(); // static bonus for spiritshots
			mAtkMul = bss ? 4 : 2;
			staticShotBonus *= bss ? 2.4 : 1.0;
		}
		else if ((sps || bss) && creature.isNpc())
		{
			staticShotBonus = 2.4 * info.getSkill().getMpConsume(); // always blessed spiritshots
			mAtkMul = 4;
		}
		else
		{
			// no static bonus
			// grade dynamic bonus
			final ItemInstance weaponInst = creature.getActiveWeaponInstance();
			if (weaponInst != null)
			{
				mAtkMul = weaponInst.getItem().getCrystalType() == CrystalType.S84 ? 4 : weaponInst.getItem().getCrystalType() == CrystalType.S80 ? 2 : 1;
			}
			// shot dynamic bonus
			mAtkMul = bss ? mAtkMul * 4 : mAtkMul + 1;
		}
		
		if (!info.getSkill().isStatic())
		{
			amount += staticShotBonus + Math.sqrt(mAtkMul * creature.getMAtk(creature, null));
			amount = target.calcStat(Stat.HEAL_EFFECT, amount, null, null);
			// Heal critic, since CT2.3 Gracia Final
			if (info.getSkill().isMagic() && Formulas.calcMCrit(creature.getMCriticalHit(target, info.getSkill())))
			{
				amount *= 3;
			}
		}
		
		// Prevents overheal and negative amount
		amount = Math.max(Math.min(amount, target.getMaxRecoverableHp() - target.getCurrentHp()), 0);
		if (amount != 0)
		{
			target.setCurrentHp(amount + target.getCurrentHp());
		}
		
		if (target.isPlayer())
		{
			if (info.getSkill().getId() == 4051)
			{
				target.sendPacket(SystemMessageId.REJUVENATING_HP);
			}
			else
			{
				if (creature.isPlayer() && (creature != target))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RESTORED_BY_C1);
					sm.addString(creature.getName());
					sm.addInt((int) amount);
					target.sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HP_HAS_BEEN_RESTORED);
					sm.addInt((int) amount);
					target.sendPacket(sm);
				}
			}
		}
	}
}
