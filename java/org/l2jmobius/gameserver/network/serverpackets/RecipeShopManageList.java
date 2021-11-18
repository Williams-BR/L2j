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

import java.util.Collection;
import java.util.Iterator;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.ManufactureItem;
import org.l2jmobius.gameserver.model.RecipeList;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class RecipeShopManageList implements IClientOutgoingPacket
{
	private final PlayerInstance _seller;
	private final boolean _isDwarven;
	private Collection<RecipeList> _recipes;
	
	public RecipeShopManageList(PlayerInstance seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;
		if (_isDwarven && _seller.hasDwarvenCraft())
		{
			_recipes = _seller.getDwarvenRecipeBook();
		}
		else
		{
			_recipes = _seller.getCommonRecipeBook();
		}
		
		if (_seller.hasManufactureShop())
		{
			final Iterator<ManufactureItem> it = _seller.getManufactureItems().values().iterator();
			ManufactureItem item;
			while (it.hasNext())
			{
				item = it.next();
				if ((item.isDwarven() != _isDwarven) || !seller.hasRecipeList(item.getRecipeId()))
				{
					it.remove();
				}
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.RECIPE_SHOP_MANAGE_LIST.writeId(packet);
		packet.writeD(_seller.getObjectId());
		packet.writeD((int) _seller.getAdena());
		packet.writeD(_isDwarven ? 0x00 : 0x01);
		
		if (_recipes == null)
		{
			packet.writeD(0);
		}
		else
		{
			packet.writeD(_recipes.size()); // number of items in recipe book
			int count = 0;
			for (RecipeList recipe : _recipes)
			{
				count++;
				packet.writeD(recipe.getId());
				packet.writeD(count);
			}
		}
		
		if (!_seller.hasManufactureShop())
		{
			packet.writeD(0x00);
		}
		else
		{
			packet.writeD(_seller.getManufactureItems().size());
			for (ManufactureItem item : _seller.getManufactureItems().values())
			{
				packet.writeD(item.getRecipeId());
				packet.writeD(0x00);
				packet.writeQ(item.getCost());
			}
		}
		return true;
	}
}
