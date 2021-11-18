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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.holders.PrimeShopProductHolder;

/**
 * @author Mobius
 */
public class PrimeShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PrimeShopData.class.getName());
	private final Map<Integer, PrimeShopProductHolder> _products = new HashMap<>();
	
	protected PrimeShopData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_products.clear();
		
		if (!Config.ENABLE_PRIME_SHOP)
		{
			return;
		}
		
		parseDatapackFile("data/PrimeShop.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		NamedNodeMap attrs;
		Node att;
		StatSet set = null;
		for (Node a = doc.getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("product".equalsIgnoreCase(b.getNodeName()))
					{
						attrs = b.getAttributes();
						set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						final PrimeShopProductHolder product = new PrimeShopProductHolder(set.getInt("id"), set.getInt("category"), set.getInt("points"), set.getInt("item"), set.getInt("count"));
						_products.put(set.getInt("id"), product);
					}
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _products.size() + " products.");
	}
	
	public Collection<PrimeShopProductHolder> getAllItems()
	{
		return _products.values();
	}
	
	public PrimeShopProductHolder getProduct(int id)
	{
		return _products.get(id);
	}
	
	public static PrimeShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrimeShopData INSTANCE = new PrimeShopData();
	}
}
