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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelAskStart;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) Sd
 * @author -Wooden-
 */
public class RequestDuelStart implements IClientIncomingPacket
{
	private String _player;
	private int _partyDuel;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_player = packet.readS();
		_partyDuel = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		final PlayerInstance targetChar = World.getInstance().getPlayer(_player);
		if (player == null)
		{
			return;
		}
		if (targetChar == null)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		if (player == targetChar)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		
		// Check if duel is possible
		if (!player.canDuel())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
			return;
		}
		else if (!targetChar.canDuel())
		{
			player.sendPacket(targetChar.getNoDuelReason());
			return;
		}
		// Players may not be too far apart
		else if (!player.isInsideRadius2D(targetChar, 250))
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.C1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_C1_IS_TOO_FAR_AWAY);
			msg.addString(targetChar.getName());
			player.sendPacket(msg);
			return;
		}
		
		// Duel is a party duel
		if (_partyDuel == 1)
		{
			// Player must be in a party & the party leader
			if (!player.isInParty() || !player.getParty().isLeader(player))
			{
				player.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}
			// Target must be in a party
			else if (!targetChar.isInParty())
			{
				player.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}
			// Target may not be of the same party
			else if (player.getParty().containsPlayer(targetChar))
			{
				player.sendMessage("This player is a member of your own party.");
				return;
			}
			
			// Check if every player is ready for a duel
			for (PlayerInstance temp : player.getParty().getMembers())
			{
				if (!temp.canDuel())
				{
					player.sendMessage("Not all the members of your party are ready for a duel.");
					return;
				}
			}
			PlayerInstance partyLeader = null; // snatch party leader of targetChar's party
			for (PlayerInstance temp : targetChar.getParty().getMembers())
			{
				if (partyLeader == null)
				{
					partyLeader = temp;
				}
				if (!temp.canDuel())
				{
					player.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					return;
				}
			}
			
			// Send request to targetChar's party leader
			if (partyLeader != null)
			{
				if (!partyLeader.isProcessingRequest())
				{
					player.onTransactionRequest(partyLeader);
					partyLeader.sendPacket(new ExDuelAskStart(player.getName(), _partyDuel));
					SystemMessage msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
					msg.addString(partyLeader.getName());
					player.sendPacket(msg);
					
					msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
					msg.addString(player.getName());
					targetChar.sendPacket(msg);
				}
				else
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
					msg.addString(partyLeader.getName());
					player.sendPacket(msg);
				}
			}
		}
		else
		// 1vs1 duel
		{
			if (!targetChar.isProcessingRequest())
			{
				player.onTransactionRequest(targetChar);
				targetChar.sendPacket(new ExDuelAskStart(player.getName(), _partyDuel));
				SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
				msg.addString(targetChar.getName());
				player.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.C1_HAS_CHALLENGED_YOU_TO_A_DUEL);
				msg.addString(player.getName());
				targetChar.sendPacket(msg);
			}
			else
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
				msg.addString(targetChar.getName());
				player.sendPacket(msg);
			}
		}
	}
}
