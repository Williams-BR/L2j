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
package org.l2jmobius.gameserver.model.itemauction;

import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.model.Augmentation;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;

/**
 * @author Forsaiken
 */
public class AuctionItem
{
	private final int _auctionItemId;
	private final int _auctionLength;
	private final long _auctionInitBid;
	
	private final int _itemId;
	private final long _itemCount;
	private final StatSet _itemExtra;
	
	public AuctionItem(int auctionItemId, int auctionLength, long auctionInitBid, int itemId, long itemCount, StatSet itemExtra)
	{
		_auctionItemId = auctionItemId;
		_auctionLength = auctionLength;
		_auctionInitBid = auctionInitBid;
		_itemId = itemId;
		_itemCount = itemCount;
		_itemExtra = itemExtra;
	}
	
	public boolean checkItemExists()
	{
		return ItemTable.getInstance().getTemplate(_itemId) != null;
	}
	
	public int getAuctionItemId()
	{
		return _auctionItemId;
	}
	
	public int getAuctionLength()
	{
		return _auctionLength;
	}
	
	public long getAuctionInitBid()
	{
		return _auctionInitBid;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public long getItemCount()
	{
		return _itemCount;
	}
	
	public ItemInstance createNewItemInstance()
	{
		final ItemInstance item = ItemTable.getInstance().createItem("ItemAuction", _itemId, _itemCount, null, null);
		item.setEnchantLevel(item.getDefaultEnchantLevel());
		
		final int augmentationId = _itemExtra.getInt("augmentation_id", 0);
		if (augmentationId > 0)
		{
			item.setAugmentation(new Augmentation(augmentationId));
		}
		return item;
	}
}