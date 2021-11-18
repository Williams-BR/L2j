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

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Chameleon Rest effect implementation.
 */
public class ChameleonRest extends AbstractEffect
{
	private final double _power;
	
	public ChameleonRest(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public int getEffectFlags()
	{
		return (EffectFlag.SILENT_MOVE.getMask() | EffectFlag.RELAXING.getMask());
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RELAXING;
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffected().isDead())
		{
			return false;
		}
		
		if (info.getEffected().isPlayer() && !info.getEffected().getActingPlayer().isSitting())
		{
			return false;
		}
		
		final double manaDam = _power * getTicksMultiplier();
		if (manaDam > info.getEffected().getCurrentMp())
		{
			info.getEffected().sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
			return false;
		}
		
		info.getEffected().reduceCurrentMp(manaDam);
		return info.getSkill().isToggle();
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffected().isPlayer())
		{
			info.getEffected().getActingPlayer().sitDown(false);
		}
		else
		{
			info.getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
}