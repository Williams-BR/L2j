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
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;

public class CharSelected implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final int _sessionId;
	
	/**
	 * @param player
	 * @param sessionId
	 */
	public CharSelected(PlayerInstance player, int sessionId)
	{
		_player = player;
		_sessionId = sessionId;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.CHARACTER_SELECTED.writeId(packet);
		
		packet.writeS(_player.getName());
		packet.writeD(_player.getObjectId());
		packet.writeS(_player.getTitle());
		packet.writeD(_sessionId);
		packet.writeD(_player.getClanId());
		packet.writeD(0x00); // ??
		packet.writeD(_player.getAppearance().isFemale() ? 1 : 0);
		packet.writeD(_player.getRace().ordinal());
		packet.writeD(_player.getClassId().getId());
		packet.writeD(0x01); // active ??
		packet.writeD(_player.getX());
		packet.writeD(_player.getY());
		packet.writeD(_player.getZ());
		
		packet.writeF(_player.getCurrentHp());
		packet.writeF(_player.getCurrentMp());
		packet.writeD((int) _player.getSp());
		packet.writeQ(_player.getExp());
		packet.writeD(_player.getLevel());
		packet.writeD(_player.getKarma()); // thx evill33t
		packet.writeD(_player.getPkKills());
		packet.writeD(_player.getINT());
		packet.writeD(_player.getSTR());
		packet.writeD(_player.getCON());
		packet.writeD(_player.getMEN());
		packet.writeD(_player.getDEX());
		packet.writeD(_player.getWIT());
		
		packet.writeD(GameTimeTaskManager.getInstance().getGameTime() % (24 * 60)); // "reset" on 24th hour
		packet.writeD(0x00);
		
		packet.writeD(_player.getClassId().getId());
		
		packet.writeD(0x00);
		packet.writeD(0x00);
		packet.writeD(0x00);
		packet.writeD(0x00);
		
		packet.writeB(new byte[64]);
		packet.writeD(0x00);
		return true;
	}
}
