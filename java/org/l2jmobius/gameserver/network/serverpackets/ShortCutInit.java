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
import org.l2jmobius.gameserver.model.Shortcut;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class ShortCutInit implements IClientOutgoingPacket
{
	private Collection<Shortcut> _shortCuts;
	
	public ShortCutInit(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		
		_shortCuts = player.getAllShortCuts();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SHORT_CUT_INIT.writeId(packet);
		packet.writeD(_shortCuts.size());
		for (Shortcut sc : _shortCuts)
		{
			packet.writeD(sc.getType().ordinal());
			packet.writeD(sc.getSlot() + (sc.getPage() * 12));
			
			switch (sc.getType())
			{
				case ITEM:
				{
					packet.writeD(sc.getId());
					packet.writeD(0x01);
					packet.writeD(sc.getSharedReuseGroup());
					packet.writeD(0x00);
					packet.writeD(0x00);
					packet.writeH(0x00);
					packet.writeH(0x00);
					break;
				}
				case SKILL:
				{
					packet.writeD(sc.getId());
					packet.writeD(sc.getLevel());
					packet.writeC(0x00); // C5
					packet.writeD(0x01); // C6
					break;
				}
				case ACTION:
				case MACRO:
				case RECIPE:
				case BOOKMARK:
				{
					packet.writeD(sc.getId());
					packet.writeD(0x01); // C6
				}
			}
		}
		return true;
	}
}
