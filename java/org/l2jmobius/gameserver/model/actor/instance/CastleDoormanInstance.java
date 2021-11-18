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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;

public class CastleDoormanInstance extends DoormanInstance
{
	public CastleDoormanInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.CastleDoormanInstance);
	}
	
	@Override
	protected final void openDoors(PlayerInstance player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			if (getConquerableHall() != null)
			{
				getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), true);
			}
			else
			{
				getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
			}
		}
	}
	
	@Override
	protected final void closeDoors(PlayerInstance player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			if (getConquerableHall() != null)
			{
				getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), false);
			}
			else
			{
				getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
			}
		}
	}
	
	@Override
	protected final boolean isOwnerClan(PlayerInstance player)
	{
		if ((player.getClan() != null) && player.hasClanPrivilege(ClanPrivilege.CS_OPEN_DOOR))
		{
			final SiegableHall hall = getConquerableHall();
			// save in variable because it's a costly call
			if (hall != null)
			{
				if (player.getClanId() == hall.getOwnerId())
				{
					return true;
				}
			}
			else if (getCastle() != null)
			{
				if (player.getClanId() == getCastle().getOwnerId())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected final boolean isUnderSiege()
	{
		final SiegableHall hall = getConquerableHall();
		if (hall != null)
		{
			return hall.isInSiege();
		}
		return getCastle().getZone().isActive();
	}
}
