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
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Target Me effect implementation.
 * @author -Nemesiss-
 */
public class TargetMe extends AbstractEffect
{
	public TargetMe(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (info.getEffected().isPlayable())
		{
			((Playable) info.getEffected()).setLockedTarget(null);
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffected().isPlayable())
		{
			if (info.getEffected().getTarget() != info.getEffector())
			{
				final PlayerInstance effector = info.getEffector().getActingPlayer();
				// If effector is null, then its not a player, but NPC. If its not null, then it should check if the skill is pvp skill.
				if ((effector == null) || effector.checkPvpSkill(info.getEffected(), info.getSkill()))
				{
					// Target is different
					info.getEffected().setTarget(info.getEffector());
				}
			}
			
			((Playable) info.getEffected()).setLockedTarget(info.getEffector());
		}
	}
}
