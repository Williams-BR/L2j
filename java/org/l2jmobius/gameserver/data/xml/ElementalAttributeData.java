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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.enums.ElementalItemType;
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.holders.ElementalItemHolder;

/**
 * @author Mobius
 */
public class ElementalAttributeData implements IXmlReader
{
	private static final Map<Integer, ElementalItemHolder> ELEMENTAL_ITEMS = new HashMap<>();
	
	protected ElementalAttributeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		ELEMENTAL_ITEMS.clear();
		parseDatapackFile("data/ElementalAttributeData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + ELEMENTAL_ITEMS.size() + " elemental attribute items.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "item", itemNode ->
		{
			final StatSet set = new StatSet(parseAttributes(itemNode));
			
			final int id = set.getInt("id");
			if (ItemTable.getInstance().getTemplate(id) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + id + ".");
				return;
			}
			
			int elementalId = Elementals.NONE;
			switch (set.getString("elemental"))
			{
				case "FIRE":
				{
					elementalId = Elementals.FIRE;
					break;
				}
				case "WATER":
				{
					elementalId = Elementals.WATER;
					break;
				}
				case "WIND":
				{
					elementalId = Elementals.WIND;
					break;
				}
				case "EARTH":
				{
					elementalId = Elementals.EARTH;
					break;
				}
				case "HOLY":
				{
					elementalId = Elementals.HOLY;
					break;
				}
				case "DARK":
				{
					elementalId = Elementals.DARK;
					break;
				}
			}
			
			ELEMENTAL_ITEMS.put(id, new ElementalItemHolder(id, elementalId, set.getEnum("type", ElementalItemType.class)));
		}));
	}
	
	public ElementalItemHolder getElementalItem(int itemId)
	{
		return ELEMENTAL_ITEMS.getOrDefault(itemId, null);
	}
	
	public static ElementalAttributeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ElementalAttributeData INSTANCE = new ElementalAttributeData();
	}
	
}
