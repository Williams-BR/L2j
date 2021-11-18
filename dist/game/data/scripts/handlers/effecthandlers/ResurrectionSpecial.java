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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Resurrection Special effect implementation.
 * @author Zealar
 */
public class ResurrectionSpecial extends AbstractEffect
{
	private final int _power;
	
	public ResurrectionSpecial(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getInt("power", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RESURRECTION_SPECIAL;
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.RESURRECTION_SPECIAL.getMask();
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (!info.getEffected().isPlayer() && !info.getEffected().isPet())
		{
			return;
		}
		final PlayerInstance caster = info.getEffector().getActingPlayer();
		if (info.getEffected().isPlayer())
		{
			info.getEffected().getActingPlayer().reviveRequest(caster, false, _power);
			return;
		}
		if (info.getEffected().isPet())
		{
			final PetInstance pet = (PetInstance) info.getEffected();
			info.getEffected().getActingPlayer().reviveRequest(pet.getActingPlayer(), true, _power);
		}
	}
}