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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * @version $Revision: 1.7.2.4.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestDestroyItem implements IClientIncomingPacket
{
	private int _objectId;
	private long _count;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_count = packet.readQ();
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
		
		if (_count <= 0)
		{
			if (_count < 0)
			{
				Util.handleIllegalPlayerAction(player, "[RequestDestroyItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to destroy item with oid " + _objectId + " but has count < 0!", Config.DEFAULT_PUNISH);
			}
			return;
		}
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("destroy"))
		{
			player.sendMessage("You are destroying items too fast.");
			return;
		}
		
		long count = _count;
		if (player.isProcessingTransaction() || (player.getPrivateStoreType() != PrivateStoreType.NONE))
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}
		
		final ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);
		
		// if we can't find the requested item, its actually a cheat
		if (itemToRemove == null)
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		// Cannot discard item that the skill is consuming
		if (player.isCastingNow() && (player.getCurrentSkill() != null) && (player.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		// Cannot discard item that the skill is consuming
		if (player.isCastingSimultaneouslyNow() && (player.getLastSimultaneousSkillCast() != null) && (player.getLastSimultaneousSkillCast().getItemConsumeId() == itemToRemove.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		final int itemId = itemToRemove.getId();
		if (!Config.DESTROY_ALL_ITEMS && ((!player.canOverrideCond(PlayerCondOverride.DESTROY_ALL_ITEMS) && !itemToRemove.isDestroyable()) || CursedWeaponsManager.getInstance().isCursed(itemId)))
		{
			if (itemToRemove.isHeroItem())
			{
				player.sendPacket(SystemMessageId.HERO_WEAPONS_CANNOT_BE_DESTROYED);
			}
			else
			{
				player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			}
			return;
		}
		
		if (!itemToRemove.isStackable() && (count > 1))
		{
			Util.handleIllegalPlayerAction(player, "[RequestDestroyItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to destroy a non-stackable item with oid " + _objectId + " but has count > 1!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!player.getInventory().canManipulateWithItemId(itemToRemove.getId()))
		{
			player.sendMessage("You cannot use this item.");
			return;
		}
		
		if (_count > itemToRemove.getCount())
		{
			count = itemToRemove.getCount();
		}
		
		if (itemToRemove.getItem().isPetItem())
		{
			if (player.hasSummon() && (player.getSummon().getControlObjectId() == _objectId))
			{
				player.getSummon().unSummon(player);
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
			{
				statement.setInt(1, _objectId);
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "could not delete pet objectid: ", e);
			}
		}
		if (itemToRemove.isTimeLimitedItem())
		{
			itemToRemove.endOfLife();
		}
		
		if (itemToRemove.isEquipped())
		{
			if (itemToRemove.getEnchantLevel() > 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(itemToRemove.getEnchantLevel());
				sm.addItemName(itemToRemove);
				player.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				sm.addItemName(itemToRemove);
				player.sendPacket(sm);
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot()))
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
		}
		
		final ItemInstance removedItem = player.getInventory().destroyItem("Destroy", itemToRemove, count, player, null);
		if (removedItem == null)
		{
			return;
		}
		
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			if (removedItem.getCount() == 0)
			{
				iu.addRemovedItem(removedItem);
			}
			else
			{
				iu.addModifiedItem(removedItem);
			}
			player.sendPacket(iu);
		}
		else
		{
			client.sendPacket(new ItemList(player, true));
		}
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
