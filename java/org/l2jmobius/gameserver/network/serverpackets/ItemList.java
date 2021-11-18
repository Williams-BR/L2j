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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class ItemList extends AbstractItemPacket
{
	private final PlayerInstance _player;
	private final List<ItemInstance> _items = new ArrayList<>();
	private final boolean _showWindow;
	
	public ItemList(PlayerInstance player, boolean showWindow)
	{
		_player = player;
		_showWindow = showWindow;
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (!item.isQuestItem())
			{
				_items.add(item);
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.ITEM_LIST.writeId(packet);
		packet.writeH(_showWindow ? 0x01 : 0x00);
		packet.writeH(_items.size());
		for (ItemInstance item : _items)
		{
			writeItem(packet, item);
		}
		writeInventoryBlock(packet, _player.getInventory());
		return true;
	}
	
	@Override
	public void runImpl(PlayerInstance player)
	{
		player.sendPacket(new ExQuestItemList(_player));
	}
}
