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

import java.util.List;

/**
 * Extractable skill DTO.
 * @author Zoey76
 */
public class ExtractableSkill
{
	private final int _hash;
	private final List<ExtractableProductItem> _product;
	
	/**
	 * Instantiates a new extractable skill.
	 * @param hash the hash
	 * @param products the products
	 */
	public ExtractableSkill(int hash, List<ExtractableProductItem> products)
	{
		_hash = hash;
		_product = products;
	}
	
	/**
	 * Gets the skill hash.
	 * @return the skill hash
	 */
	public int getSkillHash()
	{
		return _hash;
	}
	
	/**
	 * Gets the product items.
	 * @return the product items
	 */
	public List<ExtractableProductItem> getProductItems()
	{
		return _product;
	}
}