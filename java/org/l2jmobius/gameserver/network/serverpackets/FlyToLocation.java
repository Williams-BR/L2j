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
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author KenM
 */
public class FlyToLocation implements IClientOutgoingPacket
{
	private final int _destX;
	private final int _destY;
	private final int _destZ;
	private final int _chaObjId;
	private final int _orgX;
	private final int _orgY;
	private final int _orgZ;
	private final FlyType _type;
	
	public enum FlyType
	{
		THROW_UP,
		THROW_HORIZONTAL,
		DUMMY, // no effect
		CHARGE;
	}
	
	public FlyToLocation(Creature creature, int destX, int destY, int destZ, FlyType type)
	{
		_chaObjId = creature.getObjectId();
		_orgX = creature.getX();
		_orgY = creature.getY();
		_orgZ = creature.getZ();
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_type = type;
		
		if (creature.isPlayer())
		{
			creature.getActingPlayer().setBlinkActive(true);
		}
	}
	
	public FlyToLocation(Creature creature, ILocational dest, FlyType type)
	{
		this(creature, dest.getX(), dest.getY(), dest.getZ(), type);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.FLY_TO_LOCATION.writeId(packet);
		packet.writeD(_chaObjId);
		packet.writeD(_destX);
		packet.writeD(_destY);
		packet.writeD(_destZ);
		packet.writeD(_orgX);
		packet.writeD(_orgY);
		packet.writeD(_orgZ);
		packet.writeD(_type.ordinal());
		return true;
	}
}
