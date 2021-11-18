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

import java.util.Collection;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.TradeItem;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class PrivateStoreManageListBuy extends AbstractItemPacket
{
	private final int _objId;
	private final long _playerAdena;
	private final Collection<ItemInstance> _itemList;
	private final Collection<TradeItem> _buyList;
	
	public PrivateStoreManageListBuy(PlayerInstance player)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		_itemList = player.getInventory().getUniqueItems(false, true);
		_buyList = player.getBuyList().getItems();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PRIVATE_STORE_BUY_MANAGE_LIST.writeId(packet);
		// section 1
		packet.writeD(_objId);
		packet.writeQ(_playerAdena);
		
		// section2
		packet.writeD(_itemList.size()); // inventory items for potential buy
		for (ItemInstance item : _itemList)
		{
			writeItem(packet, item);
			packet.writeQ(item.getItem().getReferencePrice() * 2);
		}
		
		// section 3
		packet.writeD(_buyList.size()); // count for all items already added for buy
		for (TradeItem item : _buyList)
		{
			writeItem(packet, item);
			packet.writeQ(item.getPrice());
			packet.writeQ(item.getItem().getReferencePrice() * 2);
			packet.writeQ(item.getCount());
		}
		return true;
	}
}
