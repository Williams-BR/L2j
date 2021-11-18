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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * Format: (ch) Sd
 * @author -Wooden-
 */
public class RequestPledgeSetMemberPowerGrade implements IClientIncomingPacket
{
	private String _member;
	private int _powerGrade;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_member = packet.readS();
		_powerGrade = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (!player.hasClanPrivilege(ClanPrivilege.CL_MANAGE_RANKS))
		{
			return;
		}
		
		final ClanMember member = clan.getClanMember(_member);
		if (member == null)
		{
			return;
		}
		
		if (member.getObjectId() == clan.getLeaderId())
		{
			return;
		}
		
		if (member.getPledgeType() == Clan.SUBUNIT_ACADEMY)
		{
			// also checked from client side
			player.sendMessage("You cannot change academy member grade");
			return;
		}
		
		member.setPowerGrade(_powerGrade);
		clan.broadcastClanStatus();
	}
}