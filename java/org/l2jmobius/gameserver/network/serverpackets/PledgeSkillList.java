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

import java.util.List;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author -Wooden-
 */
public class PledgeSkillList implements IClientOutgoingPacket
{
	private final Skill[] _skills;
	private final List<SubPledgeSkill> _subSkills;
	
	public static class SubPledgeSkill
	{
		int _subType;
		int _skillId;
		int _skillLevel;
		
		public SubPledgeSkill(int subType, int skillId, int skillLevel)
		{
			super();
			_subType = subType;
			_skillId = skillId;
			_skillLevel = skillLevel;
		}
	}
	
	public PledgeSkillList(Clan clan)
	{
		_skills = clan.getAllSkills();
		_subSkills = clan.getAllSubSkills();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PLEDGE_SKILL_LIST.writeId(packet);
		packet.writeD(_skills.length);
		packet.writeD(_subSkills.size()); // Squad skill length
		for (Skill sk : _skills)
		{
			packet.writeD(sk.getDisplayId());
			packet.writeD(sk.getDisplayLevel());
		}
		for (SubPledgeSkill sk : _subSkills)
		{
			packet.writeD(sk._subType); // Clan Sub-unit types
			packet.writeD(sk._skillId);
			packet.writeD(sk._skillLevel);
		}
		return true;
	}
}
