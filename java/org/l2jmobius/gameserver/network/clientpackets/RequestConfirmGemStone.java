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
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;

/**
 * Format:(ch) dddd
 * @author -Wooden-
 */
public class RequestConfirmGemStone extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private long _gemStoneCount;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_targetItemObjId = packet.readD();
		_refinerItemObjId = packet.readD();
		_gemstoneItemObjId = packet.readD();
		_gemStoneCount = packet.readQ();
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
		final ItemInstance targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			return;
		}
		final ItemInstance refinerItem = player.getInventory().getItemByObjectId(_refinerItemObjId);
		if (refinerItem == null)
		{
			return;
		}
		final ItemInstance gemStoneItem = player.getInventory().getItemByObjectId(_gemstoneItemObjId);
		if (gemStoneItem == null)
		{
			return;
		}
		
		// Make sure the item is a gemstone
		if (!isValid(player, targetItem, refinerItem, gemStoneItem))
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		
		// Check for gemstone count
		final LifeStone ls = getLifeStone(refinerItem.getId());
		if (ls == null)
		{
			return;
		}
		
		if (_gemStoneCount != getGemStoneCount(targetItem.getItem().getCrystalType(), ls.getGrade()))
		{
			player.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
			return;
		}
		
		player.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, _gemStoneCount, gemStoneItem.getId()));
	}
}
