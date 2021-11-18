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
import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.FriendAddRequest;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendInvite implements IClientIncomingPacket
{
	private String _name;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_name = packet.readS();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance friend = World.getInstance().getPlayer(_name);
		
		// Target is not found in the game.
		if ((friend == null) || !friend.isOnline() || friend.isInvisible())
		{
			player.sendPacket(SystemMessageId.THE_USER_WHO_REQUESTED_TO_BECOME_FRIENDS_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}
		// You cannot add yourself to your own friend list.
		if (friend == player)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
			return;
		}
		// Target is in olympiad.
		if (player.isInOlympiadMode() || friend.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}
		// Target blocked active player.
		if (BlockList.isBlocked(friend, player))
		{
			player.sendMessage("You are in target's block list.");
			return;
		}
		SystemMessage sm;
		// Target is blocked.
		if (BlockList.isBlocked(player, friend))
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_BLOCKED_C1);
			sm.addString(friend.getName());
			player.sendPacket(sm);
			return;
		}
		// Target already in friend list.
		if (player.getFriendList().contains(friend.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_ON_YOUR_FRIENDS_LIST);
			return;
		}
		// Target is busy.
		if (friend.isProcessingRequest())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(_name);
			player.sendPacket(sm);
			return;
		}
		// Friend request sent.
		player.onTransactionRequest(friend);
		friend.sendPacket(new FriendAddRequest(player.getName()));
		sm = new SystemMessage(SystemMessageId.YOU_VE_REQUESTED_C1_TO_BE_ON_YOUR_FRIENDS_LIST);
		sm.addString(_name);
		player.sendPacket(sm);
	}
}