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
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.ExBrExtraUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.util.Util;

public class RequestExEnchantItemAttribute implements IClientIncomingPacket
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
		
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails)
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			player.sendMessage("You cannot add elemental power while trading.");
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		final ItemInstance stone = player.getInventory().getItemByObjectId(player.getActiveEnchantAttrItemId());
		if ((item == null) || (stone == null))
		{
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!item.isElementable())
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
					return;
				}
				break;
			}
			default:
			{
				player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
				return;
			}
		}
		
		final int stoneId = stone.getId();
		byte elementToAdd = Elementals.getItemElement(stoneId);
		// Armors have the opposite element
		if (item.isArmor())
		{
			elementToAdd = Elementals.getOppositeElement(elementToAdd);
		}
		final byte opositeElement = Elementals.getOppositeElement(elementToAdd);
		final Elementals oldElement = item.getElemental(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		if ((item.isWeapon() && (oldElement != null) && (oldElement.getElement() != elementToAdd) && (oldElement.getElement() != -2)) || (item.isArmor() && (item.getElemental(elementToAdd) == null) && (item.getElementals() != null) && (item.getElementals().length >= 3)))
		{
			player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED);
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		if (item.isArmor() && (item.getElementals() != null))
		{
			// cant add opposite element
			for (Elementals elm : item.getElementals())
			{
				if (elm.getElement() == opositeElement)
				{
					player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to add oposite attribute to item!", Config.DEFAULT_PUNISH);
					return;
				}
			}
		}
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		if (!player.destroyItem("AttrEnchant", stone, 1, player, true))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to attribute enchant with a stone he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
			return;
		}
		boolean success = false;
		switch (Elementals.getItemElemental(stoneId).getType())
		{
			case STONE:
			case ROUGH_ORE:
			{
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_STONE;
				break;
			}
			case CRYSTAL:
			{
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_CRYSTAL;
				break;
			}
			case JEWEL:
			{
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_JEWEL;
				break;
			}
			case ENERGY:
			{
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_ENERGY;
				break;
			}
		}
		if (success)
		{
			final byte realElement = item.isArmor() ? opositeElement : elementToAdd;
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.THE_S2_S_ATTRIBUTE_WAS_SUCCESSFULLY_BESTOWED_ON_S1_AND_RESISTANCE_TO_S3_WAS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1);
				}
				sm.addItemName(item);
				sm.addElemental(realElement);
				if (item.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			else
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.THE_S3_S_ATTRIBUTE_WAS_SUCCESSFULLY_BESTOWED_ON_S1_S2_AND_RESISTANCE_TO_S4_WAS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S3_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1_S2);
				}
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				sm.addElemental(realElement);
				if (item.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			player.sendPacket(sm);
			item.setElementAttr(elementToAdd, newPower);
			if (item.isEquipped())
			{
				item.updateElementAttrBonus(player);
			}
			
			// send packets
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
		}
		
		player.sendPacket(new ExAttributeEnchantResult(powerToAdd));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.setActiveEnchantAttrItemId(PlayerInstance.ID_NONE);
	}
	
	public int getLimit(ItemInstance item, int sotneId)
	{
		final ElementalItemHolder elementItem = Elementals.getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return Elementals.WEAPON_VALUES[elementItem.getType().getMaxLevel()];
		}
		return Elementals.ARMOR_VALUES[elementItem.getType().getMaxLevel()];
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, ItemInstance item)
	{
		if (Elementals.getItemElement(stoneId) != Elementals.NONE)
		{
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return Elementals.FIRST_WEAPON_BONUS;
				}
				return Elementals.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return Elementals.ARMOR_BONUS;
			}
		}
		return 0;
	}
}
