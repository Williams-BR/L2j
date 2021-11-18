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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.instancemanager.SellBuffsManager;
import org.l2jmobius.gameserver.model.TradeItem;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class PrivateStoreListSell extends AbstractItemPacket
{
	private final PlayerInstance _player;
	private final PlayerInstance _seller;
	
	public PrivateStoreListSell(PlayerInstance player, PlayerInstance seller)
	{
		_player = player;
		_seller = seller;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		if (_seller.isSellingBuffs())
		{
			SellBuffsManager.getInstance().sendBuffMenu(_player, _seller, 0);
		}
		else
		{
			OutgoingPackets.PRIVATE_STORE_SELL_LIST.writeId(packet);
			
			packet.writeD(_seller.getObjectId());
			packet.writeD(_seller.getSellList().isPackaged() ? 1 : 0);
			packet.writeQ(_player.getAdena());
			packet.writeD(_seller.getSellList().getItems().size());
			for (TradeItem item : _seller.getSellList().getItems())
			{
				writeItem(packet, item);
				packet.writeQ(item.getPrice());
				packet.writeQ(item.getItem().getReferencePrice() * 2);
			}
		}
		return true;
	}
}
