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
import org.l2jmobius.gameserver.instancemanager.ItemAuctionManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.itemauction.ItemAuction;
import org.l2jmobius.gameserver.model.itemauction.ItemAuctionInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAuctionInfoPacket;

/**
 * @author Forsaiken
 */
public class RequestInfoItemAuction implements IClientIncomingPacket
{
	private int _instanceId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_instanceId = packet.readD();
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
		
		if (!client.getFloodProtectors().getItemAuction().tryPerformAction("RequestInfoItemAuction"))
		{
			return;
		}
		
		final ItemAuctionInstance instance = ItemAuctionManager.getInstance().getManagerInstance(_instanceId);
		if (instance == null)
		{
			return;
		}
		
		final ItemAuction auction = instance.getCurrentAuction();
		if (auction == null)
		{
			return;
		}
		
		player.updateLastItemAuctionRequest();
		player.sendPacket(new ExItemAuctionInfoPacket(true, auction, instance.getNextAuction()));
	}
}