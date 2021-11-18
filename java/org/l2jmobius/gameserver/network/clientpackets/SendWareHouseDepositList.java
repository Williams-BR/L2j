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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.util.Util;

/**
 * SendWareHouseDepositList client packet class.
 */
public class SendWareHouseDepositList implements IClientIncomingPacket
{
	private static final int BATCH_LENGTH = 12;
	
	private List<ItemHolder> _items = null;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		final int size = packet.readD();
		if ((size <= 0) || (size > Config.MAX_ITEM_IN_PACKET) || ((size * BATCH_LENGTH) != packet.getReadableBytes()))
		{
			return false;
		}
		
		_items = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			final int objId = packet.readD();
			final long count = packet.readQ();
			if ((objId < 1) || (count < 0))
			{
				_items = null;
				return false;
			}
			_items.add(new ItemHolder(objId, count));
		}
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		if (_items == null)
		{
			return;
		}
		
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("deposit"))
		{
			player.sendMessage("You are depositing items too fast.");
			return;
		}
		
		final ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
		{
			return;
		}
		
		final Npc manager = player.getLastFolkNPC();
		if (((manager == null) || !manager.isWarehouse() || !manager.canInteract(player)) && !player.isGM())
		{
			return;
		}
		
		final boolean isPrivate = warehouse instanceof PlayerWarehouse;
		if (!isPrivate && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		if (player.getActiveEnchantItemId() != PlayerInstance.ID_NONE)
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (player.getKarma() > 0))
		{
			return;
		}
		
		// Freight price from config or normal price per item slot (30)
		final long fee = _items.size() * 30;
		long currentAdena = player.getAdena();
		int slots = 0;
		for (ItemHolder itemHolder : _items)
		{
			final ItemInstance item = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "deposit");
			if (item == null)
			{
				LOGGER.warning("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				return;
			}
			
			// Calculate needed adena and slots
			if (item.getId() == ADENA_ID)
			{
				currentAdena -= itemHolder.getCount();
			}
			if (!item.isStackable())
			{
				slots += itemHolder.getCount();
			}
			else if (warehouse.getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}
		
		// Check if enough adena and charge the fee
		if ((currentAdena < fee) || !player.reduceAdena(warehouse.getName(), fee, manager, false))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}
		
		// get current tradelist if any
		if (player.getActiveTradeList() != null)
		{
			return;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (ItemHolder itemHolder : _items)
		{
			// Check validity of requested item
			final ItemInstance oldItem = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "deposit");
			if (oldItem == null)
			{
				LOGGER.warning("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				return;
			}
			
			if (!oldItem.isDepositable(isPrivate) || !oldItem.isAvailable(player, true, isPrivate))
			{
				continue;
			}
			
			final ItemInstance newItem = player.getInventory().transferItem(warehouse.getName(), itemHolder.getId(), itemHolder.getCount(), warehouse, player, manager);
			if (newItem == null)
			{
				LOGGER.warning("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}
			
			if (playerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
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
		
		// Update current load status on player
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
