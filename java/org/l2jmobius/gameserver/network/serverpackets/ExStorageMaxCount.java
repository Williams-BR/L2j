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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author -Wooden-, KenM
 */
public class ExStorageMaxCount implements IClientOutgoingPacket
{
	private final int _inventory;
	private final int _warehouse;
	private final int _clan;
	private final int _privateSell;
	private final int _privateBuy;
	private final int _receipeD;
	private final int _recipe;
	private final int _inventoryExtraSlots;
	private final int _inventoryQuestItems;
	
	public ExStorageMaxCount(PlayerInstance player)
	{
		_inventory = player.getInventoryLimit();
		_warehouse = player.getWareHouseLimit();
		_privateSell = player.getPrivateSellStoreLimit();
		_privateBuy = player.getPrivateBuyStoreLimit();
		_clan = Config.WAREHOUSE_SLOTS_CLAN;
		_receipeD = player.getDwarfRecipeLimit();
		_recipe = player.getCommonRecipeLimit();
		_inventoryExtraSlots = (int) player.getStat().calcStat(Stat.INV_LIM, 0, null, null);
		_inventoryQuestItems = Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_STORAGE_MAX_COUNT.writeId(packet);
		
		packet.writeD(_inventory);
		packet.writeD(_warehouse);
		packet.writeD(_clan);
		packet.writeD(_privateSell);
		packet.writeD(_privateBuy);
		packet.writeD(_receipeD);
		packet.writeD(_recipe);
		packet.writeD(_inventoryExtraSlots); // Belt inventory slots increase count
		packet.writeD(_inventoryQuestItems);
		return true;
	}
}