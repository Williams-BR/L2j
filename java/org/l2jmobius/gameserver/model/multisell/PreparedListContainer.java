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
package org.l2jmobius.gameserver.model.multisell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;

public class PreparedListContainer extends ListContainer
{
	private int _npcObjectId = 0;
	
	public PreparedListContainer(ListContainer template, boolean inventoryOnly, PlayerInstance player, Npc npc)
	{
		super(template.getListId());
		setMaintainEnchantment(template.getMaintainEnchantment());
		setApplyTaxes(false);
		double taxRate = 0;
		if (npc != null)
		{
			_npcObjectId = npc.getObjectId();
			if (template.getApplyTaxes() && npc.isInTown() && (npc.getCastle().getOwnerId() > 0))
			{
				setApplyTaxes(true);
				taxRate = npc.getCastle().getTaxRate();
			}
		}
		
		if (inventoryOnly)
		{
			if (player == null)
			{
				return;
			}
			
			final Collection<ItemInstance> items;
			if (getMaintainEnchantment())
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false, false);
			}
			
			_entries = new LinkedList<>();
			for (Entry entry : template.getEntries())
			{
				if (!entry.getIngredients().isEmpty())
				{
					final int ingredientId = entry.getIngredients().get(0).getItemId();
					for (ItemInstance item : items)
					{
						if (!item.isEquipped() && (item.getId() == ingredientId))
						{
							_entries.add(new PreparedEntry(entry, item, getApplyTaxes(), getMaintainEnchantment(), taxRate));
							break;
						}
					}
				}
			}
		}
		else
		{
			_entries = new ArrayList<>(template.getEntries().size());
			for (Entry ent : template.getEntries())
			{
				_entries.add(new PreparedEntry(ent, null, getApplyTaxes(), false, taxRate));
			}
		}
		
		_npcsAllowed = template._npcsAllowed;
	}
	
	public boolean checkNpcObjectId(int npcObjectId)
	{
		return (_npcObjectId == 0) || (_npcObjectId == npcObjectId);
	}
}