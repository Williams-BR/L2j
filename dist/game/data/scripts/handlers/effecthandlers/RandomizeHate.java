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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.stats.Formulas;

/**
 * Randomize Hate effect implementation.
 */
public class RandomizeHate extends AbstractEffect
{
	private final int _chance;
	
	public RandomizeHate(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return Formulas.calcProbability(_chance, info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffected() == null) || (info.getEffected() == info.getEffector()) || !info.getEffected().isAttackable())
		{
			return;
		}
		
		final Attackable effectedMob = (Attackable) info.getEffected();
		final List<Creature> aggroList = new ArrayList<>();
		for (Creature creature : effectedMob.getAggroList().keySet())
		{
			if (creature != info.getEffector())
			{
				aggroList.add(creature);
			}
		}
		if (aggroList.isEmpty())
		{
			return;
		}
		
		// Choosing randomly a new target
		final Creature target = aggroList.get(Rnd.get(aggroList.size()));
		final int hate = effectedMob.getHating(info.getEffector());
		effectedMob.stopHating(info.getEffector());
		effectedMob.addDamageHate(target, 0, hate);
	}
}