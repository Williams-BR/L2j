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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.model.EnchantSkillGroup.EnchantSkillHolder;
import org.l2jmobius.gameserver.model.EnchantSkillLearn;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author KenM
 */
public class ExEnchantSkillInfoDetail implements IClientOutgoingPacket
{
	private static final int TYPE_NORMAL_ENCHANT = 0;
	private static final int TYPE_SAFE_ENCHANT = 1;
	private static final int TYPE_UNTRAIN_ENCHANT = 2;
	private static final int TYPE_CHANGE_ENCHANT = 3;
	
	private int bookId = 0;
	private int reqCount = 0;
	private int multi = 1;
	private final int _type;
	private final int _skillId;
	private final int _skillLevel;
	private final int _chance;
	private int _sp;
	private final int _adenacount;
	
	public ExEnchantSkillInfoDetail(int type, int skillId, int skillLevel, PlayerInstance ply)
	{
		final EnchantSkillLearn enchantLearn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
		EnchantSkillHolder esd = null;
		// do we have this skill?
		if (enchantLearn != null)
		{
			if (skillLevel > 100)
			{
				esd = enchantLearn.getEnchantSkillHolder(skillLevel);
			}
			else
			{
				esd = enchantLearn.getFirstRouteGroup().getEnchantGroupDetails().get(0);
			}
		}
		
		if (esd == null)
		{
			throw new IllegalArgumentException("Skill " + skillId + " dont have enchant data for level " + skillLevel);
		}
		
		if (type == 0)
		{
			multi = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
		}
		else if (type == 1)
		{
			multi = EnchantSkillGroupsData.SAFE_ENCHANT_COST_MULTIPLIER;
		}
		_chance = esd.getRate(ply);
		_sp = esd.getSpCost();
		if (type == TYPE_UNTRAIN_ENCHANT)
		{
			_sp = (int) (0.8 * _sp);
		}
		_adenacount = esd.getAdenaCost() * multi;
		_type = type;
		_skillId = skillId;
		_skillLevel = skillLevel;
		
		switch (type)
		{
			case TYPE_NORMAL_ENCHANT:
			{
				bookId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK;
				reqCount = (((_skillLevel % 100) > 1) ? 0 : 1);
				break;
			}
			case TYPE_SAFE_ENCHANT:
			{
				bookId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK;
				reqCount = 1;
				break;
			}
			case TYPE_UNTRAIN_ENCHANT:
			{
				bookId = EnchantSkillGroupsData.UNTRAIN_ENCHANT_BOOK;
				reqCount = 1;
				break;
			}
			case TYPE_CHANGE_ENCHANT:
			{
				bookId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK;
				reqCount = 1;
				break;
			}
			default:
			{
				return;
			}
		}
		
		if ((type != TYPE_SAFE_ENCHANT) && !Config.ES_SP_BOOK_NEEDED)
		{
			reqCount = 0;
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_ENCHANT_SKILL_INFO_DETAIL.writeId(packet);
		packet.writeD(_type);
		packet.writeD(_skillId);
		packet.writeD(_skillLevel);
		packet.writeD(_sp * multi); // sp
		packet.writeD(_chance); // exp
		packet.writeD(0x02); // items count?
		packet.writeD(Inventory.ADENA_ID); // Adena
		packet.writeD(_adenacount); // Adena count
		packet.writeD(bookId); // ItemId Required
		packet.writeD(reqCount);
		return true;
	}
}
