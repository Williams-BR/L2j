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
package org.l2jmobius.gameserver.model.itemcontainer;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.ItemLocation;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.player.clanwh.OnPlayerClanWHItemAdd;
import org.l2jmobius.gameserver.model.events.impl.creature.player.clanwh.OnPlayerClanWHItemDestroy;
import org.l2jmobius.gameserver.model.events.impl.creature.player.clanwh.OnPlayerClanWHItemTransfer;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;

public class ClanWarehouse extends Warehouse
{
	private final Clan _clan;
	
	public ClanWarehouse(Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public String getName()
	{
		return "ClanWarehouse";
	}
	
	@Override
	public int getOwnerId()
	{
		return _clan.getId();
	}
	
	@Override
	public PlayerInstance getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	public String getLocationId()
	{
		return "0";
	}
	
	public int getLocationId(boolean dummy)
	{
		return 0;
	}
	
	public void setLocationId(PlayerInstance dummy)
	{
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return (_items.size() + slots) <= Config.WAREHOUSE_SLOTS_CLAN;
	}
	
	@Override
	public ItemInstance addItem(String process, int itemId, long count, PlayerInstance actor, Object reference)
	{
		final ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(process, actor, item, this), item.getItem());
		return item;
	}
	
	@Override
	public ItemInstance addItem(String process, ItemInstance item, PlayerInstance actor, Object reference)
	{
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(process, actor, item, this), item.getItem());
		return super.addItem(process, item, actor, reference);
	}
	
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, long count, PlayerInstance actor, Object reference)
	{
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemDestroy(process, actor, item, count, this), item.getItem());
		return super.destroyItem(process, item, count, actor, reference);
	}
	
	@Override
	public ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, PlayerInstance actor, Object reference)
	{
		final ItemInstance item = getItemByObjectId(objectId);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemTransfer(process, actor, item, count, target), item.getItem());
		return super.transferItem(process, objectId, count, target, actor, reference);
	}
}
