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
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.CrystalType;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * @version $Revision: 1.2.2.3.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestCrystallizeItem implements IClientIncomingPacket
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
			LOGGER.fine("RequestCrystalizeItem: activeChar was null");
			return;
		}
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("crystallize"))
		{
			player.sendMessage("You are crystallizing too fast.");
			return;
		}
		
		if (_count <= 0)
		{
			Util.handleIllegalPlayerAction(player, "[RequestCrystallizeItem] count <= 0! ban! oid: " + _objectId + " owner: " + player.getName(), Config.DEFAULT_PUNISH);
			return;
		}
		
		if ((player.getPrivateStoreType() != PrivateStoreType.NONE) || player.isInCrystallize())
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}
		
		final int skillLevel = player.getSkillLevel(CommonSkill.CRYSTALLIZE.getId());
		if (skillLevel <= 0)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CRYSTALLIZE_THIS_ITEM_YOUR_CRYSTALLIZATION_SKILL_LEVEL_IS_TOO_LOW);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if ((player.getRace() != Race.DWARF) && (player.getClassId().getId() != 117) && (player.getClassId().getId() != 55))
			{
				LOGGER.info("Player " + client + " used crystalize with classid: " + player.getClassId().getId());
			}
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		if (inventory != null)
		{
			final ItemInstance item = inventory.getItemByObjectId(_objectId);
			if ((item == null) || item.isHeroItem() || (!Config.ALT_ALLOW_AUGMENT_DESTROY && item.isAugmented()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (_count > item.getCount())
			{
				_count = player.getInventory().getItemByObjectId(_objectId).getCount();
			}
		}
		
		final ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);
		if ((itemToRemove == null) || itemToRemove.isShadowItem() || itemToRemove.isTimeLimitedItem())
		{
			return;
		}
		
		if (!itemToRemove.getItem().isCrystallizable() || (itemToRemove.getItem().getCrystalCount() <= 0) || (itemToRemove.getItem().getCrystalType() == CrystalType.NONE))
		{
			LOGGER.warning(player.getName() + " (" + player.getObjectId() + ") tried to crystallize " + itemToRemove.getItem().getId());
			return;
		}
		
		if (!player.getInventory().canManipulateWithItemId(itemToRemove.getId()))
		{
			player.sendMessage("You cannot use this item.");
			return;
		}
		
		// Check if the char can crystallize items and return if false;
		boolean canCrystallize = true;
		
		switch (itemToRemove.getItem().getCrystalTypePlus())
		{
			case C:
			{
				if (skillLevel <= 1)
				{
					canCrystallize = false;
				}
				break;
			}
			case B:
			{
				if (skillLevel <= 2)
				{
					canCrystallize = false;
				}
				break;
			}
			case A:
			{
				if (skillLevel <= 3)
				{
					canCrystallize = false;
				}
				break;
			}
			case S:
			{
				if (skillLevel <= 4)
				{
					canCrystallize = false;
				}
				break;
			}
		}
		
		if (!canCrystallize)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CRYSTALLIZE_THIS_ITEM_YOUR_CRYSTALLIZATION_SKILL_LEVEL_IS_TOO_LOW);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setInCrystallize(true);
		
		// unequip if needed
		SystemMessage sm;
		if (itemToRemove.isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance item : player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot()))
			{
				iu.addModifiedItem(item);
			}
			player.sendPacket(iu);
			
			if (itemToRemove.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(itemToRemove.getEnchantLevel());
				sm.addItemName(itemToRemove);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				sm.addItemName(itemToRemove);
			}
			player.sendPacket(sm);
		}
		
		// remove from inventory
		final ItemInstance removedItem = player.getInventory().destroyItem("Crystalize", _objectId, _count, player, null);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		player.sendPacket(iu);
		
		// add crystals
		final int crystalId = itemToRemove.getItem().getCrystalItemId();
		final int crystalAmount = itemToRemove.getCrystalCount();
		final ItemInstance createditem = player.getInventory().addItem("Crystalize", crystalId, crystalAmount, player, player);
		sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_CRYSTALLIZED);
		sm.addItemName(removedItem);
		player.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
		sm.addItemName(createditem);
		sm.addLong(crystalAmount);
		player.sendPacket(sm);
		
		player.broadcastUserInfo();
		
		final World world = World.getInstance();
		world.removeObject(removedItem);
		
		player.setInCrystallize(false);
	}
}
