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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.fishing.Fish;

/**
 * This class holds the Fish information.
 * @author nonom
 */
public class FishData implements IXmlReader
{
	private final Map<Integer, Fish> _fishNormal = new HashMap<>();
	private final Map<Integer, Fish> _fishEasy = new HashMap<>();
	private final Map<Integer, Fish> _fishHard = new HashMap<>();
	
	/**
	 * Instantiates a new fish data.
	 */
	protected FishData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_fishEasy.clear();
		_fishNormal.clear();
		_fishHard.clear();
		parseDatapackFile("data/stats/fishing/fishes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_fishEasy.size() + _fishNormal.size() + _fishHard.size()) + " fishes.");
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
					if ("fish".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final Fish fish = new Fish(set);
						switch (fish.getFishGrade())
						{
							case 0:
							{
								_fishEasy.put(fish.getFishId(), fish);
								break;
							}
							case 1:
							{
								_fishNormal.put(fish.getFishId(), fish);
								break;
							}
							case 2:
							{
								_fishHard.put(fish.getFishId(), fish);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets the fish.
	 * @param level the fish Level
	 * @param group the fish Group
	 * @param grade the fish Grade
	 * @return List of Fish that can be fished
	 */
	public List<Fish> getFish(int level, int group, int grade)
	{
		final List<Fish> result = new ArrayList<>();
		Map<Integer, Fish> fish = null;
		switch (grade)
		{
			case 0:
			{
				fish = _fishEasy;
				break;
			}
			case 1:
			{
				fish = _fishNormal;
				break;
			}
			case 2:
			{
				fish = _fishHard;
				break;
			}
			default:
			{
				LOGGER.warning(getClass().getSimpleName() + ": Unmanaged fish grade!");
				return result;
			}
		}
		
		for (Fish f : fish.values())
		{
			if ((f.getFishLevel() != level) || (f.getFishGroup() != group))
			{
				continue;
			}
			result.add(f);
		}
		
		if (result.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot find any fish for level: " + level + " group: " + group + " and grade: " + grade + "!");
		}
		return result;
	}
	
	/**
	 * Gets the single instance of FishData.
	 * @return single instance of FishData
	 */
	public static FishData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FishData INSTANCE = new FishData();
	}
}
