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

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Servitor Share effect implementation.<br>
 * Synchronizing effects on player and servitor if one of them gets removed for some reason the same will happen to another. Partner's effect exit is executed in own thread, since there is no more queue to schedule the effects,<br>
 * partner's effect is called while this effect is still exiting issuing an exit call for the effect, causing a stack over flow.
 * @author UnAfraid, Zoey76
 */
public class ServitorShare extends AbstractEffect
{
	private final Map<Stat, Double> _stats = new HashMap<>(9);
	
	public ServitorShare(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		for (String key : params.getSet().keySet())
		{
			_stats.put(Stat.valueOfXml(key), params.getDouble(key, 1.));
		}
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.SERVITOR_SHARE.getMask();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		super.onStart(info);
		
		info.getEffected().getActingPlayer().setServitorShare(_stats);
		if (info.getEffected().getActingPlayer().getSummon() != null)
		{
			info.getEffected().getActingPlayer().getSummon().broadcastInfo();
			info.getEffected().getActingPlayer().getSummon().getStatus().startHpMpRegeneration();
		}
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().getActingPlayer().setServitorShare(null);
		if (info.getEffected().getSummon() != null)
		{
			if (info.getEffected().getSummon().getCurrentHp() > info.getEffected().getSummon().getMaxHp())
			{
				info.getEffected().getSummon().setCurrentHp(info.getEffected().getSummon().getMaxHp());
			}
			if (info.getEffected().getSummon().getCurrentMp() > info.getEffected().getSummon().getMaxMp())
			{
				info.getEffected().getSummon().setCurrentMp(info.getEffected().getSummon().getMaxMp());
			}
			info.getEffected().getSummon().broadcastInfo();
		}
	}
}
