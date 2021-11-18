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
import org.l2jmobius.gameserver.model.items.Henna;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author Zoey76
 */
public class HennaItemRemoveInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final Henna _henna;
	
	public HennaItemRemoveInfo(Henna henna, PlayerInstance player)
	{
		_henna = henna;
		_player = player;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.HENNA_UNEQUIP_INFO.writeId(packet);
		packet.writeD(_henna.getDyeId()); // symbol Id
		packet.writeD(_henna.getDyeItemId()); // item id of dye
		packet.writeQ(_henna.getCancelCount()); // total amount of dye require
		packet.writeQ(_henna.getCancelFee()); // total amount of Adena require to remove symbol
		packet.writeD(_henna.isAllowedClass(_player.getClassId()) ? 0x01 : 0x00); // able to remove or not
		packet.writeQ(_player.getAdena());
		packet.writeD(_player.getINT()); // current INT
		packet.writeC(_player.getINT() - _henna.getStatINT()); // equip INT
		packet.writeD(_player.getSTR()); // current STR
		packet.writeC(_player.getSTR() - _henna.getStatSTR()); // equip STR
		packet.writeD(_player.getCON()); // current CON
		packet.writeC(_player.getCON() - _henna.getStatCON()); // equip CON
		packet.writeD(_player.getMEN()); // current MEN
		packet.writeC(_player.getMEN() - _henna.getStatMEN()); // equip MEN
		packet.writeD(_player.getDEX()); // current DEX
		packet.writeC(_player.getDEX() - _henna.getStatDEX()); // equip DEX
		packet.writeD(_player.getWIT()); // current WIT
		packet.writeC(_player.getWIT() - _henna.getStatWIT()); // equip WIT
		return true;
	}
}
