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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;
import org.l2jmobius.gameserver.util.Util;

/**
 * Format(ch) d
 * @author -Wooden-
 */
public class RequestConfirmCancelItem implements IClientIncomingPacket
{
	private int _objectId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
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
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		if (item.getOwnerId() != player.getObjectId())
		{
			Util.handleIllegalPlayerAction(client.getPlayer(), "Warning!! Character " + client.getPlayer().getName() + " of account " + client.getPlayer().getAccountName() + " tryied to destroy augment on item that doesn't own.", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!item.isAugmented())
		{
			player.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}
		
		if (item.isPvp() && !Config.ALT_ALLOW_AUGMENT_PVP_ITEMS)
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		
		int price = 0;
		switch (item.getItem().getCrystalType())
		{
			case C:
			{
				if (item.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (item.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			}
			case B:
			{
				if (item.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			}
			case A:
			{
				if (item.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (item.getCrystalCount() < 2824)
				{
					price = 390000;
				}
				else
				{
					price = 420000;
				}
				break;
			}
			case S:
			{
				price = 480000;
				break;
			}
			case S80:
			case S84:
			{
				price = 920000;
				break;
			}
			// TODO: S84 TOP price 3.2M
			// any other item type is not augmentable
			default:
			{
				return;
			}
		}
		
		player.sendPacket(new ExPutItemResultForVariationCancel(item, price));
	}
}
