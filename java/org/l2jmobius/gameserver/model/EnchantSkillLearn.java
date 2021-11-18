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
package org.l2jmobius.gameserver.model;

import java.util.Set;
import java.util.TreeMap;

import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.model.EnchantSkillGroup.EnchantSkillHolder;

public class EnchantSkillLearn
{
	private final int _id;
	private final int _baseLevel;
	private final TreeMap<Integer, Integer> _enchantRoutes = new TreeMap<>();
	
	public EnchantSkillLearn(int id, int baseLevel)
	{
		_id = id;
		_baseLevel = baseLevel;
	}
	
	public void addNewEnchantRoute(int route, int group)
	{
		_enchantRoutes.put(route, group);
	}
	
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return Returns the minLevel.
	 */
	public int getBaseLevel()
	{
		return _baseLevel;
	}
	
	public static int getEnchantRoute(int level)
	{
		return (int) Math.floor(level / 100);
	}
	
	public static int getEnchantIndex(int level)
	{
		return (level % 100) - 1;
	}
	
	public static int getEnchantType(int level)
	{
		return ((level - 1) / 100) - 1;
	}
	
	public EnchantSkillGroup getFirstRouteGroup()
	{
		return EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.firstEntry().getValue());
	}
	
	public Set<Integer> getAllRoutes()
	{
		return _enchantRoutes.keySet();
	}
	
	public int getMinSkillLevel(int level)
	{
		return (level % 100) == 1 ? _baseLevel : level - 1;
	}
	
	public boolean isMaxEnchant(int level)
	{
		final int enchantType = getEnchantRoute(level);
		if ((enchantType < 1) || !_enchantRoutes.containsKey(enchantType))
		{
			return false;
		}
		final int index = getEnchantIndex(level);
		return (index + 1) >= EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(enchantType)).getEnchantGroupDetails().size();
	}
	
	public EnchantSkillHolder getEnchantSkillHolder(int level)
	{
		final int enchantType = getEnchantRoute(level);
		if ((enchantType < 1) || !_enchantRoutes.containsKey(enchantType))
		{
			return null;
		}
		final int index = getEnchantIndex(level);
		final EnchantSkillGroup group = EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(enchantType));
		if (index < 0)
		{
			return group.getEnchantGroupDetails().get(0);
		}
		else if (index >= group.getEnchantGroupDetails().size())
		{
			return group.getEnchantGroupDetails().get(EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(enchantType)).getEnchantGroupDetails().size() - 1);
		}
		return group.getEnchantGroupDetails().get(index);
	}
}