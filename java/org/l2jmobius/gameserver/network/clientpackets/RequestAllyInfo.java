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
import org.l2jmobius.gameserver.model.clan.ClanInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AllianceInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @version $Revision: 1479 $ $Date: 2005-11-09 00:47:42 +0100 (mer., 09 nov. 2005) $
 */
public class RequestAllyInfo implements IClientIncomingPacket
{
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
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
		
		SystemMessage sm;
		final int allianceId = player.getAllyId();
		if (allianceId > 0)
		{
			final AllianceInfo ai = new AllianceInfo(allianceId);
			player.sendPacket(ai);
			
			// send for player
			sm = new SystemMessage(SystemMessageId.ALLIANCE_INFORMATION);
			player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.ALLIANCE_NAME_S1);
			sm.addString(ai.getName());
			player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.ALLIANCE_LEADER_S2_OF_S1);
			sm.addString(ai.getLeaderC());
			sm.addString(ai.getLeaderP());
			player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.CONNECTION_S1_TOTAL_S2);
			sm.addInt(ai.getOnline());
			sm.addInt(ai.getTotal());
			player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.AFFILIATED_CLANS_TOTAL_S1_CLAN_S);
			sm.addInt(ai.getAllies().length);
			player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.CLAN_INFORMATION);
			for (ClanInfo aci : ai.getAllies())
			{
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.CLAN_NAME_S1);
				sm.addString(aci.getClan().getName());
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.CLAN_LEADER_S1);
				sm.addString(aci.getClan().getLeaderName());
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.CLAN_LEVEL_S1);
				sm.addInt(aci.getClan().getLevel());
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.CONNECTION_S1_TOTAL_S2);
				sm.addInt(aci.getOnline());
				sm.addInt(aci.getTotal());
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.EMPTY_4);
			}
			
			sm = new SystemMessage(SystemMessageId.EMPTY_5);
			player.sendPacket(sm);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS);
		}
	}
}
