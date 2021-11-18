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

import java.util.Collection;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class GMViewSkillInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final Collection<Skill> _skills;
	
	public GMViewSkillInfo(PlayerInstance player)
	{
		_player = player;
		_skills = _player.getAllSkills();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.GM_VIEW_SKILL_INFO.writeId(packet);
		packet.writeS(_player.getName());
		packet.writeD(_skills.size());
		
		final boolean isDisabled = (_player.getClan() != null) ? (_player.getClan().getReputationScore() < 0) : false;
		for (Skill skill : _skills)
		{
			packet.writeD(skill.isPassive() ? 1 : 0);
			packet.writeD(skill.getDisplayLevel());
			packet.writeD(skill.getDisplayId());
			packet.writeC(isDisabled && skill.isClanSkill() ? 1 : 0);
			packet.writeC(SkillData.getInstance().isEnchantable(skill.getDisplayId()) ? 1 : 0);
		}
		return true;
	}
}