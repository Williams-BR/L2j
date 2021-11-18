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
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinPledge;

/**
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestJoinPledge implements IClientIncomingPacket
{
	private int _target;
	private int _pledgeType;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_target = packet.readD();
		_pledgeType = packet.readD();
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
		
		final PlayerInstance target = World.getInstance().getPlayer(_target);
		if (target == null)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}
		
		if (!clan.checkClanJoinCondition(player, target, _pledgeType))
		{
			return;
		}
		
		if (!player.getRequest().setRequest(target, this))
		{
			return;
		}
		
		final String pledgeName = player.getClan().getName();
		final String subPledgeName = (player.getClan().getSubPledge(_pledgeType) != null ? player.getClan().getSubPledge(_pledgeType).getName() : null);
		target.sendPacket(new AskJoinPledge(player.getObjectId(), subPledgeName, _pledgeType, pledgeName));
	}
	
	public int getPledgeType()
	{
		return _pledgeType;
	}
}
