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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author ShanSoft
 */
public class ExBuySellList extends AbstractItemPacket
{
	private final Collection<ItemInstance> _sellList;
	private Collection<ItemInstance> _refundList = null;
	private final boolean _done;
	
	public ExBuySellList(PlayerInstance player, boolean done)
	{
		_sellList = player.getInventory().getAvailableItems(false, false, false);
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		_done = done;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_BUY_SELL_LIST.writeId(packet);
		packet.writeD(0x01);
		
		if ((_sellList != null))
		{
			packet.writeH(_sellList.size());
			for (ItemInstance item : _sellList)
			{
				writeItem(packet, item);
				packet.writeQ(Config.MERCHANT_ZERO_SELL_PRICE ? 0 : item.getItem().getReferencePrice() / 2);
			}
		}
		else
		{
			packet.writeH(0x00);
		}
		
		if ((_refundList != null) && !_refundList.isEmpty())
		{
			packet.writeH(_refundList.size());
			int i = 0;
			for (ItemInstance item : _refundList)
			{
				writeItem(packet, item);
				packet.writeD(i++);
				packet.writeQ(Config.MERCHANT_ZERO_SELL_PRICE ? 0 : (item.getItem().getReferencePrice() / 2) * item.getCount());
			}
		}
		else
		{
			packet.writeH(0x00);
		}
		
		packet.writeC(_done ? 0x01 : 0x00);
		return true;
	}
}