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
package org.l2jmobius.gameserver.model.items;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.type.ArmorType;
import org.l2jmobius.gameserver.model.skills.Skill;

/**
 * This class is dedicated to the management of armors.
 */
public class Armor extends Item
{
	/**
	 * Skill that activates when armor is enchanted +4.
	 */
	private SkillHolder _enchant4Skill = null;
	private ArmorType _type;
	
	/**
	 * Constructor for Armor.
	 * @param set the StatSet designating the set of couples (key,value) characterizing the armor.
	 */
	public Armor(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = set.getEnum("armor_type", ArmorType.class, ArmorType.NONE);
		
		final int bodyPart = getBodyPart();
		if ((bodyPart == Item.SLOT_NECK) || ((bodyPart & Item.SLOT_L_EAR) != 0) || ((bodyPart & Item.SLOT_L_FINGER) != 0) || ((bodyPart & Item.SLOT_R_BRACELET) != 0) || ((bodyPart & Item.SLOT_L_BRACELET) != 0))
		{
			_type1 = Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = Item.TYPE2_ACCESSORY;
		}
		else
		{
			if ((_type == ArmorType.NONE) && (getBodyPart() == Item.SLOT_L_HAND))
			{
				_type = ArmorType.SHIELD;
			}
			_type1 = Item.TYPE1_SHIELD_ARMOR;
			_type2 = Item.TYPE2_SHIELD_ARMOR;
		}
		
		final String skill = set.getString("enchant4_skill", null);
		if (skill == null)
		{
			return;
		}
		
		final String[] info = skill.split("-");
		if ((info == null) || (info.length != 2))
		{
			return;
		}
		
		int id = 0;
		int level = 0;
		try
		{
			id = Integer.parseInt(info[0]);
			level = Integer.parseInt(info[1]);
		}
		catch (Exception nfe)
		{
			// Incorrect syntax, don't add new skill
			LOGGER.info("> Couldnt parse " + skill + " in armor enchant skills! item " + this);
		}
		if ((id > 0) && (level > 0))
		{
			_enchant4Skill = new SkillHolder(id, level);
		}
	}
	
	/**
	 * @return the type of the armor.
	 */
	@Override
	public ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	@Override
	public boolean isArmor()
	{
		return true;
	}
	
	/**
	 * @return skill that player get when has equipped armor +4 or more
	 */
	@Override
	public Skill getEnchant4Skill()
	{
		return _enchant4Skill == null ? null : _enchant4Skill.getSkill();
	}
}
