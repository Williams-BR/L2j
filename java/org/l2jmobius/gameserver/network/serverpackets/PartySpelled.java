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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class PartySpelled implements IClientOutgoingPacket
{
	private final List<BuffInfo> _effects = new ArrayList<>();
	private final Creature _creature;
	
	public PartySpelled(Creature creature)
	{
		_creature = creature;
	}
	
	public void addSkill(BuffInfo info)
	{
		_effects.add(info);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PARTY_SPELLED.writeId(packet);
		packet.writeD(_creature.isServitor() ? 2 : _creature.isPet() ? 1 : 0);
		packet.writeD(_creature.getObjectId());
		packet.writeD(_effects.size());
		for (BuffInfo info : _effects)
		{
			if ((info != null) && info.isInUse())
			{
				packet.writeD(info.getSkill().getDisplayId());
				packet.writeH(info.getSkill().getDisplayLevel());
				packet.writeD(info.getTime());
			}
		}
		return true;
	}
}
