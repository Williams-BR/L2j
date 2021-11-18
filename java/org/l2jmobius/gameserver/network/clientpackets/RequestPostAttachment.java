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

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.ADENA_ID;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.enums.ItemLocation;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author Migi, DS
 */
public class RequestPostAttachment implements IClientIncomingPacket
{
	private int _msgId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_msgId = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		if (!Config.ALLOW_MAIL || !Config.ALLOW_ATTACHMENTS)
		{
			return;
		}
		
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("getattach"))
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level");
			return;
		}
		
		if (!player.isInsideZone(ZoneId.PEACE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_DURING_AN_EXCHANGE);
			return;
		}
		
		if (player.isEnchanting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}
		
		final Message msg = MailManager.getInstance().getMessage(_msgId);
		if (msg == null)
		{
			return;
		}
		
		if (msg.getReceiverId() != player.getObjectId())
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to get not own attachment!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!msg.hasAttachments())
		{
			return;
		}
		
		final ItemContainer attachments = msg.getAttachments();
		if (attachments == null)
		{
			return;
		}
		
		int weight = 0;
		int slots = 0;
		for (ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			// Calculate needed slots
			if (item.getOwnerId() != msg.getSenderId())
			{
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to get wrong item (ownerId != senderId) from attachment!", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getItemLocation() != ItemLocation.MAIL)
			{
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to get wrong item (Location != MAIL) from attachment!", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getLocationSlot() != msg.getId())
			{
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to get items from different attachment!", Config.DEFAULT_PUNISH);
				return;
			}
			
			weight += item.getCount() * item.getItem().getWeight();
			if (!item.isStackable())
			{
				slots += item.getCount();
			}
			else if (player.getInventory().getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		// Weight limit Check
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		final long adena = msg.getReqAdena();
		if ((adena > 0) && !player.reduceAdena("PayMail", adena, null, true))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
			return;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getOwnerId() != msg.getSenderId())
			{
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to get items with owner != sender !", Config.DEFAULT_PUNISH);
				return;
			}
			
			final long count = item.getCount();
			final ItemInstance newItem = attachments.transferItem(attachments.getName(), item.getObjectId(), item.getCount(), player.getInventory(), player, null);
			if (newItem == null)
			{
				return;
			}
			
			if (playerIU != null)
			{
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
			}
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S2_S1);
			sm.addItemName(item.getId());
			sm.addLong(count);
			player.sendPacket(sm);
		}
		
		// Send updated item list to the player
		if (playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendPacket(new ItemList(player, false));
		}
		
		msg.removeAttachments();
		
		// Update current load status on player
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		SystemMessage sm;
		final PlayerInstance sender = World.getInstance().getPlayer(msg.getSenderId());
		if (adena > 0)
		{
			if (sender != null)
			{
				sender.addAdena("PayMail", adena, player, false);
				sm = new SystemMessage(SystemMessageId.S2_HAS_MADE_A_PAYMENT_OF_S1_ADENA_PER_YOUR_PAYMENT_REQUEST_MAIL);
				sm.addLong(adena);
				sm.addString(player.getName());
				sender.sendPacket(sm);
			}
			else
			{
				final ItemInstance paidAdena = ItemTable.getInstance().createItem("PayMail", ADENA_ID, adena, player, null);
				paidAdena.setOwnerId(msg.getSenderId());
				paidAdena.setItemLocation(ItemLocation.INVENTORY);
				paidAdena.updateDatabase(true);
				World.getInstance().removeObject(paidAdena);
			}
		}
		else if (sender != null)
		{
			sm = new SystemMessage(SystemMessageId.S1_ACQUIRED_THE_ATTACHED_ITEM_TO_YOUR_MAIL);
			sm.addString(player.getName());
			sender.sendPacket(sm);
		}
		
		player.sendPacket(new ExChangePostState(true, _msgId, Message.READED));
		player.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_RECEIVED);
	}
}
