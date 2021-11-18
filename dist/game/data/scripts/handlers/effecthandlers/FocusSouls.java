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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Focus Souls effect implementation.
 * @author nBd, Adry_85
 */
public class FocusSouls extends AbstractEffect
{
	private final int _charge;
	
	public FocusSouls(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_charge = params.getInt("charge", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (!info.getEffected().isPlayer() || info.getEffected().isAlikeDead())
		{
			return;
		}
		
		final PlayerInstance target = info.getEffected().getActingPlayer();
		final int maxSouls = (int) target.calcStat(Stat.MAX_SOULS, 0, null, null);
		if (maxSouls > 0)
		{
			final int amount = _charge;
			if ((target.getChargedSouls() < maxSouls))
			{
				final int count = ((target.getChargedSouls() + amount) <= maxSouls) ? amount : (maxSouls - target.getChargedSouls());
				target.increaseSouls(count);
			}
			else
			{
				target.sendPacket(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
			}
		}
	}
}