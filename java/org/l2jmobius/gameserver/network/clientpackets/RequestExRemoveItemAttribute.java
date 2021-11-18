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
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExBaseAttributeCancelResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

public class RequestExRemoveItemAttribute implements IClientIncomingPacket
{
	private int _objectId;
	private long _price;
	private byte _element;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_element = (byte) packet.readD();
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
		
		final ItemInstance targetItem = player.getInventory().getItemByObjectId(_objectId);
		if (targetItem == null)
		{
			return;
		}
		
		if ((targetItem.getElementals() == null) || (targetItem.getElemental(_element) == null))
		{
			return;
		}
		
		if (player.reduceAdena("RemoveElement", getPrice(targetItem), player, true))
		{
			if (targetItem.isEquipped())
			{
				targetItem.getElemental(_element).removeBonus(player);
			}
			targetItem.clearElementAttr(_element);
			player.sendPacket(new UserInfo(player));
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(targetItem);
			player.sendPacket(iu);
			SystemMessage sm;
			final byte realElement = targetItem.isArmor() ? Elementals.getOppositeElement(_element) : _element;
			if (targetItem.getEnchantLevel() > 0)
			{
				if (targetItem.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S_S3_ATTRIBUTE_WAS_REMOVED_SO_RESISTANCE_TO_S4_WAS_DECREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S_S3_ATTRIBUTE_HAS_BEEN_REMOVED);
				}
				sm.addInt(targetItem.getEnchantLevel());
				sm.addItemName(targetItem);
				sm.addElemental(realElement);
				if (targetItem.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			else
			{
				if (targetItem.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S1_S_S2_ATTRIBUTE_WAS_REMOVED_AND_RESISTANCE_TO_S3_WAS_DECREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S_S2_ATTRIBUTE_HAS_BEEN_REMOVED);
				}
				sm.addItemName(targetItem);
				sm.addElemental(realElement);
				if (targetItem.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			player.sendPacket(sm);
			player.sendPacket(new ExBaseAttributeCancelResult(targetItem.getObjectId(), _element));
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_FUNDS_TO_CANCEL_THIS_ATTRIBUTE);
		}
	}
	
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