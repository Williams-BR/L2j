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
import org.l2jmobius.gameserver.data.AugmentationData;
import org.l2jmobius.gameserver.model.Augmentation;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

/**
 * Format:(ch) dddd
 * @author -Wooden-
 */
public class RequestRefine extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemStoneItemObjId;
	private long _gemStoneCount;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_targetItemObjId = packet.readD();
		_refinerItemObjId = packet.readD();
		_gemStoneItemObjId = packet.readD();
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
		final ItemInstance gemStoneItem = player.getInventory().getItemByObjectId(_gemStoneItemObjId);
		if (gemStoneItem == null)
		{
			return;
		}
		
		if (!isValid(player, targetItem, refinerItem, gemStoneItem))
		{
			player.sendPacket(new ExVariationResult(0, 0, 0));
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final LifeStone ls = getLifeStone(refinerItem.getId());
		if (ls == null)
		{
			return;
		}
		
		final int lifeStoneLevel = ls.getLevel();
		final int lifeStoneGrade = ls.getGrade();
		if (_gemStoneCount != getGemStoneCount(targetItem.getItem().getCrystalType(), lifeStoneGrade))
		{
			player.sendPacket(new ExVariationResult(0, 0, 0));
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		// unequip item
		if (targetItem.isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : player.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot()))
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();
		}
		
		// consume the life stone
		if (!player.destroyItem("RequestRefine", refinerItem, 1, null, false))
		{
			return;
		}
		
		// consume the gemstones
		if (!player.destroyItem("RequestRefine", gemStoneItem, _gemStoneCount, null, false))
		{
			return;
		}
		
		final Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade, targetItem.getItem().getBodyPart(), refinerItem.getId(), targetItem);
		targetItem.setAugmentation(aug);
		
		final int stat12 = 0x0000FFFF & aug.getAugmentationId();
		final int stat34 = aug.getAugmentationId() >> 16;
		player.sendPacket(new ExVariationResult(stat12, stat34, 1));
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
