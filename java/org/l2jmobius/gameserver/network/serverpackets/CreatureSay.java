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
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.OutgoingPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class CreatureSay implements IClientOutgoingPacket
{
	private final Creature _sender;
	private final ChatType _chatType;
	private String _senderName = null;
	private String _text = null;
	private int _charId = 0;
	private int _messageId = -1;
	private List<String> _parameters;
	
	public CreatureSay(Creature sender, ChatType chatType, String senderName, String text)
	{
		_sender = sender;
		_chatType = chatType;
		_senderName = senderName;
		_text = text;
	}
	
	public CreatureSay(Creature sender, ChatType chatType, NpcStringId npcStringId)
	{
		_sender = sender;
		_chatType = chatType;
		_messageId = npcStringId.getId();
	}
	
	public CreatureSay(ChatType chatType, int charId, SystemMessageId systemMessageId)
	{
		_sender = null;
		_chatType = chatType;
		_charId = charId;
		_messageId = systemMessageId.getId();
	}
	
	/**
	 * String parameter for argument S1,S2,.. in npcstring-e.dat
	 * @param text
	 */
	public void addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		_parameters.add(text);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SAY2.writeId(packet);
		
		packet.writeD(_sender == null ? 0 : _sender.getObjectId());
		packet.writeD(_chatType.getClientId());
		if (_senderName != null)
		{
			packet.writeS(_senderName);
		}
		else
		{
			packet.writeD(_charId);
		}
		packet.writeD(_messageId); // High Five NPCString ID
		if (_text != null)
		{
			packet.writeS(_text);
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				packet.writeS(s);
			}
		}
		return true;
	}
	
	@Override
	public void runImpl(PlayerInstance player)
	{
		if (player != null)
		{
			player.broadcastSnoop(_chatType, _senderName, _text);
		}
	}
}
