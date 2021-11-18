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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Henna;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * This server packet sends the player's henna information.
 * @author Zoey76
 */
public class HennaInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final List<Henna> _hennas = new ArrayList<>();
	
	public HennaInfo(PlayerInstance player)
	{
		_player = player;
		for (Henna henna : _player.getHennaList())
		{
			if (henna != null)
			{
				_hennas.add(henna);
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.HENNA_INFO.writeId(packet);
		packet.writeC(_player.getHennaStatINT()); // equip INT
		packet.writeC(_player.getHennaStatSTR()); // equip STR
		packet.writeC(_player.getHennaStatCON()); // equip CON
		packet.writeC(_player.getHennaStatMEN()); // equip MEN
		packet.writeC(_player.getHennaStatDEX()); // equip DEX
		packet.writeC(_player.getHennaStatWIT()); // equip WIT
		packet.writeD(3); // Slots
		packet.writeD(_hennas.size()); // Size
		for (Henna henna : _hennas)
		{
			packet.writeD(henna.getDyeId());
			packet.writeD(0x01);
		}
		return true;
	}
}
