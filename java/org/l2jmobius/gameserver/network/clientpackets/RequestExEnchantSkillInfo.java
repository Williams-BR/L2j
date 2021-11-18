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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;

/**
 * Format (ch) dd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill level
 * @author -Wooden-
 */
public class RequestExEnchantSkillInfo implements IClientIncomingPacket
{
	private int _skillId;
	private int _skillLevel;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_skillId = packet.readD();
		_skillLevel = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		if ((_skillId <= 0) || (_skillLevel <= 0))
		{
			return;
		}
		
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getLevel() < 76)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		if ((skill == null) || (skill.getId() != _skillId))
		{
			return;
		}
		
		if (EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_skillId) == null)
		{
			return;
		}
		
		final int playerskillLevel = player.getSkillLevel(_skillId);
		if ((playerskillLevel == -1) || (playerskillLevel != _skillLevel))
		{
			return;
		}
		
		player.sendPacket(new ExEnchantSkillInfo(_skillId, _skillLevel));
	}
}