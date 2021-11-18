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

import java.util.logging.Level;

import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.enums.ItemLocation;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;

/**
 * @author DS
 */
public class PlayerRefund extends ItemContainer
{
	private final PlayerInstance _owner;
	
	public PlayerRefund(PlayerInstance owner)
	{
		_owner = owner;
	}
	
	@Override
	public String getName()
	{
		return "Refund";
	}
	
	@Override
	public PlayerInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.REFUND;
	}
	
	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		try
		{
			if (getSize() > 12)
			{
				final ItemInstance removedItem = _items.stream().findFirst().get();
				if (_items.remove(removedItem))
				{
					ItemTable.getInstance().destroyItem("ClearRefund", removedItem, getOwner(), null);
					removedItem.updateDatabase(true);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "addItem()", e);
		}
	}
	
	@Override
	public void refreshWeight()
	{
	}
	
	@Override
	public void deleteMe()
	{
		try
		{
			for (ItemInstance item : _items)
			{
				if (item != null)
				{
					ItemTable.getInstance().destroyItem("ClearRefund", item, getOwner(), null);
					item.updateDatabase(true);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		_items.clear();
	}
	
	@Override
	public void restore()
	{
	}
}