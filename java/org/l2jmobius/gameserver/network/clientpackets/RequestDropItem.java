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
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillUseHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.EtcItemType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.util.GMAudit;
import org.l2jmobius.gameserver.util.Util;

/**
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/02 21:25:21 $
 */
public class RequestDropItem implements IClientIncomingPacket
{
	private int _objectId;
	private long _count;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_count = packet.readQ();
		_x = packet.readD();
		_y = packet.readD();
		_z = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if ((player == null) || player.isDead())
		{
			return;
		}
		
		// Flood protect drop to avoid packet lag
		if (!client.getFloodProtectors().getDropItem().tryPerformAction("drop item"))
		{
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		if ((item == null) || (_count == 0) || !player.validateItemManipulation(_objectId, "drop") || (!Config.ALLOW_DISCARDITEM && !player.canOverrideCond(PlayerCondOverride.DROP_ALL_ITEMS)) || (!item.isDropable() && !(player.canOverrideCond(PlayerCondOverride.DROP_ALL_ITEMS) && Config.GM_TRADE_RESTRICTED_ITEMS)) || ((item.getItemType() == EtcItemType.PET_COLLAR) && player.havePetInvItems()) || player.isInsideZone(ZoneId.NO_ITEM_DROP))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		if (item.isQuestItem() && !(player.canOverrideCond(PlayerCondOverride.DROP_ALL_ITEMS) && Config.GM_TRADE_RESTRICTED_ITEMS))
		{
			return;
		}
		
		if (_count > item.getCount())
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && player.isInvul() && !player.isGM())
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		if (_count < 0)
		{
			Util.handleIllegalPlayerAction(player, "[RequestDropItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to drop item with oid " + _objectId + " but has count < 0!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!item.isStackable() && (_count > 1))
		{
			Util.handleIllegalPlayerAction(player, "[RequestDropItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to drop non-stackable item with oid " + _objectId + " but has count > 1!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (Config.JAIL_DISABLE_TRANSACTION && player.isJailed())
		{
			player.sendMessage("You cannot drop items in Jail.");
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}
		
		if (player.isProcessingTransaction() || (player.getPrivateStoreType() != PrivateStoreType.NONE))
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}
		
		if (player.isFishing())
		{
			// You can't mount, dismount, break and drop items while fishing
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
			return;
		}
		
		if (player.isFlying())
		{
			return;
		}
		
		if (player.isCastingNow())
		{
			final SkillUseHolder skill = player.getCurrentSkill();
			if (skill != null)
			{
				// Cannot discard item that the skill is consuming.
				if ((skill.getSkill().getItemConsumeId() == item.getId()) && ((player.getInventory().getInventoryItemCount(item.getId(), -1) - skill.getSkill().getItemConsumeCount()) < _count))
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
					return;
				}
				
				// Do not drop items when casting known skills to avoid exploits.
				if (player.getKnownSkill(skill.getSkillId()) != null)
				{
					player.sendMessage("You cannot drop an item while casting " + skill.getSkill().getName() + ".");
					return;
				}
			}
		}
		
		if (player.isCastingSimultaneouslyNow())
		{
			final Skill skill = player.getLastSimultaneousSkillCast();
			if (skill != null)
			{
				// Cannot discard item that the skill is consuming.
				if ((skill.getItemConsumeId() == item.getId()) && ((player.getInventory().getInventoryItemCount(item.getId(), -1) - skill.getItemConsumeCount()) < _count))
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
					return;
				}
				
				// Do not drop items when casting known skills to avoid exploits.
				if (player.getKnownSkill(skill.getId()) != null)
				{
					player.sendMessage("You cannot drop an item while casting " + skill.getName() + ".");
					return;
				}
			}
		}
		
		if ((Item.TYPE2_QUEST == item.getItem().getType2()) && !player.canOverrideCond(PlayerCondOverride.DROP_ALL_ITEMS))
		{
			player.sendPacket(SystemMessageId.THAT_ITEM_CANNOT_BE_DISCARDED_OR_EXCHANGED);
			return;
		}
		
		if (!player.isInsideRadius2D(_x, _y, 0, 150) || (Math.abs(_z - player.getZ()) > 50))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISCARD_SOMETHING_THAT_FAR_AWAY_FROM_YOU);
			return;
		}
		
		if (!player.getInventory().canManipulateWithItemId(item.getId()))
		{
			player.sendMessage("You cannot use this item.");
			return;
		}
		
		if (item.isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot()))
			{
				itm.unChargeAllShots();
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();
			player.sendPacket(new ItemList(player, true));
		}
		
		final ItemInstance dropedItem = player.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false, false);
		
		// player.broadcastUserInfo();
		if (player.isGM())
		{
			final String target = (player.getTarget() != null ? player.getTarget().getName() : "no-target");
			GMAudit.auditGMAction(player.getName() + " [" + player.getObjectId() + "]", "Drop", target, "(id: " + dropedItem.getId() + " name: " + dropedItem.getItemName() + " objId: " + dropedItem.getObjectId() + " x: " + player.getX() + " y: " + player.getY() + " z: " + player.getZ() + ")");
		}
		
		if ((dropedItem != null) && (dropedItem.getId() == Inventory.ADENA_ID) && (dropedItem.getCount() >= 1000000))
		{
			final String msg = "Character (" + player.getName() + ") has dropped (" + dropedItem.getCount() + ")adena at (" + _x + "," + _y + "," + _z + ")";
			LOGGER.warning(msg);
			AdminData.getInstance().broadcastMessageToGMs(msg);
		}
	}
}
