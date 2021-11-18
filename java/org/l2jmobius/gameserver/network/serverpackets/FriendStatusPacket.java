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
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * Support for "Chat with Friends" dialog. <br />
 * Inform player about friend online status change
 * @author JIV
 */
public class FriendStatusPacket implements IClientOutgoingPacket
{
	private final boolean _online;
	private final int _objid;
	private final String _name;
	
	public FriendStatusPacket(int objId)
	{
		_objid = objId;
		_name = CharNameTable.getInstance().getNameById(objId);
		_online = World.getInstance().getPlayer(objId) != null;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.L2_FRIEND_STATUS.writeId(packet);
		packet.writeD(_online ? 1 : 0);
		packet.writeS(_name);
		packet.writeD(_objid);
		return true;
	}
}
