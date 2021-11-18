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
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * ExBrExtraUserInfo server packet implementation.
 * @author Kerberos, Zoey76
 */
public class ExBrExtraUserInfo implements IClientOutgoingPacket
{
	/** Player object ID. */
	private final int _objectId;
	/** Event abnormal visual effects map. */
	private final int _abnormalVisualEffectsEvent;
	/** Lecture mark. */
	private final int _lectureMark;
	
	public ExBrExtraUserInfo(PlayerInstance player)
	{
		_objectId = player.getObjectId();
		_abnormalVisualEffectsEvent = player.getAbnormalVisualEffectEvent();
		_lectureMark = 1; // TODO: Implement.
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_BR_EXTRA_USER_INFO.writeId(packet);
		packet.writeD(_objectId);
		packet.writeD(_abnormalVisualEffectsEvent);
		packet.writeC(_lectureMark);
		return true;
	}
}
