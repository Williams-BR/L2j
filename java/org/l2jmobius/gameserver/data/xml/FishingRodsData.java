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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.fishing.FishingRod;

/**
 * This class holds the Fishing Rods information.
 * @author nonom
 */
public class FishingRodsData implements IXmlReader
{
	private final Map<Integer, FishingRod> _fishingRods = new HashMap<>();
	
	/**
	 * Instantiates a new fishing rods data.
	 */
	protected FishingRodsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_fishingRods.clear();
		parseDatapackFile("data/stats/fishing/fishingRods.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fishingRods.size() + " fishing rods.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("fishingRod".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final FishingRod fishingRod = new FishingRod(set);
						_fishingRods.put(fishingRod.getFishingRodItemId(), fishingRod);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the fishing rod.
	 * @param itemId the item id
	 * @return A fishing Rod by Item Id
	 */
	public FishingRod getFishingRod(int itemId)
	{
		return _fishingRods.get(itemId);
	}
	
	/**
	 * Gets the single instance of FishingRodsData.
	 * @return single instance of FishingRodsData
	 */
	public static FishingRodsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FishingRodsData INSTANCE = new FishingRodsData();
	}
}
