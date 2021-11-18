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
import org.l2jmobius.gameserver.model.partymatching.PartyMatchRoom;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * Mode:
 * <ul>
 * <li>0 - add</li>
 * <li>1 - modify</li>
 * <li>2 - quit</li>
 * </ul>
 * @author Gnacik
 */
public class ExManagePartyRoomMember implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final PartyMatchRoom _room;
	private final int _mode;
	
	public ExManagePartyRoomMember(PlayerInstance player, PartyMatchRoom room, int mode)
	{
		_player = player;
		_room = room;
		_mode = mode;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_MANAGE_PARTY_ROOM_MEMBER.writeId(packet);
		packet.writeD(_mode);
		packet.writeD(_player.getObjectId());
		packet.writeS(_player.getName());
		packet.writeD(_player.getActiveClass());
		packet.writeD(_player.getLevel());
		packet.writeD(_room.getLocation());
		if (_room.getOwner().equals(_player))
		{
			packet.writeD(1);
		}
		else
		{
			if ((_room.getOwner().isInParty() && _player.isInParty()) && (_room.getOwner().getParty().getLeaderObjectId() == _player.getParty().getLeaderObjectId()))
			{
				packet.writeD(0x02);
			}
			else
			{
				packet.writeD(0x00);
			}
		}
		return true;
	}
}