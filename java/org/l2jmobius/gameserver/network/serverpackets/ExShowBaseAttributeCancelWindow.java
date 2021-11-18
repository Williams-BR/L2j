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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class ExShowBaseAttributeCancelWindow implements IClientOutgoingPacket
{
	private final Collection<ItemInstance> _items;
	private long _price;
	
	public ExShowBaseAttributeCancelWindow(PlayerInstance player)
	{
		_items = player.getInventory().getElementItems();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SHOW_BASE_ATTRIBUTE_CANCEL_WINDOW.writeId(packet);
		packet.writeD(_items.size());
		for (ItemInstance item : _items)
		{
			packet.writeD(item.getObjectId());
			packet.writeQ(getPrice(item));
		}
		return true;
	}
	
	/**
	 * TODO: Update prices for Top/Mid/Low S80/S84
	 * @param item
	 * @return
	 */
	private long getPrice(ItemInstance item)
	{
		switch (item.getItem().getCrystalType())
		{
			case S:
			{
				if (item.getItem() instanceof Weapon)
				{
					_price = 50000;
				}
				else
				{
					_price = 40000;
				}
				break;
			}
			case S80:
			{
				if (item.getItem() instanceof Weapon)
				{
					_price = 100000;
				}
				else
				{
					_price = 80000;
				}
				break;
			}
			case S84:
			{
				if (item.getItem() instanceof Weapon)
				{
					_price = 200000;
				}
				else
				{
					_price = 160000;
				}
				break;
			}
		}
		return _price;
	}
}