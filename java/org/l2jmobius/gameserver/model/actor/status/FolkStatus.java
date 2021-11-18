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
package org.l2jmobius.gameserver.model.actor.status;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.ClanHallManagerInstance;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;

public class FolkStatus extends NpcStatus
{
	public FolkStatus(Npc activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
	}
	
	@Override
	public void reduceMp(double value)
	{
		// If Clan Hall buff are free and it's a Clan Hall Manager MP won't get reduced.
		if (Config.CH_BUFF_FREE && (getActiveChar() instanceof ClanHallManagerInstance))
		{
			return;
		}
		super.reduceMp(value);
	}
	
	@Override
	public NpcInstance getActiveChar()
	{
		return (NpcInstance) super.getActiveChar();
	}
}
