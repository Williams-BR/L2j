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
package org.l2jmobius.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class BlockList
{
	private static final Logger LOGGER = Logger.getLogger(BlockList.class.getName());
	private static final Map<Integer, List<Integer>> OFFLINE_LIST = new ConcurrentHashMap<>();
	
	private final PlayerInstance _owner;
	private List<Integer> _blockList;
	
	public BlockList(PlayerInstance owner)
	{
		_owner = owner;
		_blockList = OFFLINE_LIST.get(owner.getObjectId());
		if (_blockList == null)
		{
			_blockList = loadList(_owner.getObjectId());
		}
	}
	
	private void addToBlockList(int target)
	{
		_blockList.add(target);
		updateInDB(target, true);
	}
	
	private void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		updateInDB(target, false);
	}
	
	public void playerLogout()
	{
		OFFLINE_LIST.put(_owner.getObjectId(), _blockList);
	}
	
	private static List<Integer> loadList(int objId)
	{
		final List<Integer> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1"))
		{
			statement.setInt(1, objId);
			try (ResultSet rset = statement.executeQuery())
			{
				int friendId;
				while (rset.next())
				{
					friendId = rset.getInt("friendId");
					if (friendId == objId)
					{
						continue;
					}
					list.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + objId + " FriendList while loading BlockList: " + e.getMessage(), e);
		}
		return list;
	}
	
	private void updateInDB(int targetId, boolean state)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (state) // add
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId, relation) VALUES (?, ?, 1)"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
			else
			// remove
			{
				try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? AND friendId=? AND relation=1"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not add block player: " + e.getMessage(), e);
		}
	}
	
	public boolean isInBlockList(PlayerInstance target)
	{
		return _blockList.contains(target.getObjectId());
	}
	
	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}
	
	private boolean isBlockAll()
	{
		return _owner.getMessageRefusal();
	}
	
	public static boolean isBlocked(PlayerInstance listOwner, PlayerInstance target)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(target);
	}
	
	public static boolean isBlocked(PlayerInstance listOwner, int targetId)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(targetId);
	}
	
	private void setBlockAll(boolean value)
	{
		_owner.setMessageRefusal(value);
	}
	
	private List<Integer> getBlockList()
	{
		return _blockList;
	}
	
	public static void addToBlockList(PlayerInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		final String charName = CharNameTable.getInstance().getNameById(targetId);
		if (listOwner.getFriendList().contains(targetId))
		{
			listOwner.sendPacket(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_ON_YOUR_FRIENDS_LIST);
			return;
		}
		
		if (listOwner.getBlockList().getBlockList().contains(targetId))
		{
			listOwner.sendMessage("Already in ignore list.");
			return;
		}
		
		listOwner.getBlockList().addToBlockList(targetId);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
		
		final PlayerInstance player = World.getInstance().getPlayer(targetId);
		if (player != null)
		{
			sm = new SystemMessage(SystemMessageId.S1_HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST);
			sm.addString(listOwner.getName());
			player.sendPacket(sm);
		}
	}
	
	public static void removeFromBlockList(PlayerInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		SystemMessage sm;
		
		final String charName = CharNameTable.getInstance().getNameById(targetId);
		if (!listOwner.getBlockList().getBlockList().contains(targetId))
		{
			sm = new SystemMessage(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			listOwner.sendPacket(sm);
			return;
		}
		
		listOwner.getBlockList().removeFromBlockList(targetId);
		
		sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
	}
	
	public static boolean isInBlockList(PlayerInstance listOwner, PlayerInstance target)
	{
		return listOwner.getBlockList().isInBlockList(target);
	}
	
	public boolean isBlockAll(PlayerInstance listOwner)
	{
		return listOwner.getBlockList().isBlockAll();
	}
	
	public static void setBlockAll(PlayerInstance listOwner, boolean newValue)
	{
		listOwner.getBlockList().setBlockAll(newValue);
	}
	
	public static void sendListToOwner(PlayerInstance listOwner)
	{
		int i = 1;
		listOwner.sendPacket(SystemMessageId.IGNORE_LIST);
		for (int playerId : listOwner.getBlockList().getBlockList())
		{
			listOwner.sendMessage((i++) + ". " + CharNameTable.getInstance().getNameById(playerId));
		}
		listOwner.sendPacket(SystemMessageId.EMPTY_3);
	}
	
	/**
	 * @param ownerId object id of owner block list
	 * @param targetId object id of potential blocked player
	 * @return true if blocked
	 */
	public static boolean isInBlockList(int ownerId, int targetId)
	{
		final PlayerInstance player = World.getInstance().getPlayer(ownerId);
		if (player != null)
		{
			return isBlocked(player, targetId);
		}
		if (!OFFLINE_LIST.containsKey(ownerId))
		{
			OFFLINE_LIST.put(ownerId, loadList(ownerId));
		}
		return OFFLINE_LIST.get(ownerId).contains(targetId);
	}
}
