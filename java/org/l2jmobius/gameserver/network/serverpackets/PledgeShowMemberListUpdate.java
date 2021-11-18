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

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author -Wooden-
 */
public class PledgeShowMemberListUpdate implements IClientOutgoingPacket
{
	private final int _pledgeType;
	private int _hasSponsor;
	private final String _name;
	private final int _level;
	private final int _classId;
	private final int _objectId;
	private final boolean _isOnline;
	private final int _race;
	private final int _sex;
	
	public PledgeShowMemberListUpdate(PlayerInstance player)
	{
		_pledgeType = player.getPledgeType();
		if (_pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			_hasSponsor = player.getSponsor() != 0 ? 1 : 0;
		}
		else
		{
			_hasSponsor = 0;
		}
		_name = player.getName();
		_level = player.getLevel();
		_classId = player.getClassId().getId();
		_race = player.getRace().ordinal();
		_sex = player.getAppearance().isFemale() ? 1 : 0;
		_objectId = player.getObjectId();
		_isOnline = player.isOnline();
	}
	
	public PledgeShowMemberListUpdate(ClanMember member)
	{
		_name = member.getName();
		_level = member.getLevel();
		_classId = member.getClassId();
		_objectId = member.getObjectId();
		_isOnline = member.isOnline();
		_pledgeType = member.getPledgeType();
		_race = member.getRaceOrdinal();
		_sex = member.getSex() ? 1 : 0;
		if (_pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			_hasSponsor = member.getSponsor() != 0 ? 1 : 0;
		}
		else
		{
			_hasSponsor = 0;
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PLEDGE_SHOW_MEMBER_LIST_UPDATE.writeId(packet);
		packet.writeS(_name);
		packet.writeD(_level);
		packet.writeD(_classId);
		packet.writeD(_sex);
		packet.writeD(_race);
		if (_isOnline)
		{
			packet.writeD(_objectId);
			packet.writeD(_pledgeType);
		}
		else
		{
			// when going offline send as 0
			packet.writeD(0);
			packet.writeD(0);
		}
		packet.writeD(_hasSponsor);
		return true;
	}
}
