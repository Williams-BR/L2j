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
package org.l2jmobius.gameserver.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.ChannelInboundHandler;
import org.l2jmobius.commons.network.ICrypt;
import org.l2jmobius.commons.network.IIncomingPacket;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.LoginServerThread.SessionKey;
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SecondaryAuthData;
import org.l2jmobius.gameserver.model.CharSelectInfoPackage;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.security.SecondaryPasswordAuth;
import org.l2jmobius.gameserver.util.FloodProtectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * Represents a client connected on Game Server.
 * @author KenM
 */
public class GameClient extends ChannelInboundHandler<GameClient>
{
	protected static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());
	protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);
	private final ReentrantLock _playerLock = new ReentrantLock();
	private final Crypt _crypt = new Crypt();
	private InetAddress _addr;
	private Channel _channel;
	private String _accountName;
	private SessionKey _sessionId;
	private PlayerInstance _player;
	private SecondaryPasswordAuth _secondaryAuth;
	private ClientHardwareInfoHolder _hardwareInfo;
	private List<CharSelectInfoPackage> _charSlotMapping = null;
	private volatile boolean _isDetached = false;
	private boolean _isAuthedGG;
	private boolean _protocolOk;
	private int _protocolVersion;
	private int[][] _trace;
	
	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		super.channelActive(ctx);
		
		setConnectionState(ConnectionState.CONNECTED);
		final InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
		_addr = address.getAddress();
		_channel = ctx.channel();
		LOGGER_ACCOUNTING.finer("Client Connected: " + ctx.channel());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		LOGGER_ACCOUNTING.finer("Client Disconnected: " + ctx.channel());
		LoginServerThread.getInstance().sendLogout(getAccountName());
		
		if ((_player == null) || !_player.isInOfflineMode())
		{
			Disconnection.of(this).onDisconnection();
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, IIncomingPacket<GameClient> packet)
	{
		try
		{
			packet.run(this);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception for: " + toString() + " on packet.run: " + packet.getClass().getSimpleName(), e);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
	}
	
	public void closeNow()
	{
		if (_channel != null)
		{
			_channel.close();
		}
	}
	
	public void close(IClientOutgoingPacket packet)
	{
		sendPacket(packet);
		closeNow();
	}
	
	public Channel getChannel()
	{
		return _channel;
	}
	
	public byte[] enableCrypt()
	{
		final byte[] key = BlowFishKeygen.getRandomKey();
		_crypt.setKey(key);
		return key;
	}
	
	/**
	 * For loaded offline traders returns localhost address.
	 * @return cached connection IP address, for checking detached clients.
	 */
	public InetAddress getConnectionAddress()
	{
		return _addr;
	}
	
	public PlayerInstance getPlayer()
	{
		return _player;
	}
	
	public void setPlayer(PlayerInstance player)
	{
		_player = player;
	}
	
	public ReentrantLock getPlayerLock()
	{
		return _playerLock;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _floodProtectors;
	}
	
	public void setGameGuardOk(boolean value)
	{
		_isAuthedGG = value;
	}
	
	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}
	
	public void setAccountName(String activeChar)
	{
		_accountName = activeChar;
		if (SecondaryAuthData.getInstance().isEnabled())
		{
			_secondaryAuth = new SecondaryPasswordAuth(this);
		}
	}
	
	public String getAccountName()
	{
		return _accountName;
	}
	
	public void setSessionId(SessionKey sk)
	{
		_sessionId = sk;
	}
	
	public SessionKey getSessionId()
	{
		return _sessionId;
	}
	
	public void sendPacket(IClientOutgoingPacket packet)
	{
		if (_isDetached || (packet == null))
		{
			return;
		}
		
		// TODO: Set as parameter to packets used?
		if (Config.MULTILANG_ENABLE && (_player != null))
		{
			final String lang = _player.getLang();
			if ((lang != null) && !lang.equals("en"))
			{
				if (packet instanceof SystemMessage)
				{
					((SystemMessage) packet).setLang(lang);
				}
				else if (packet instanceof NpcSay)
				{
					((NpcSay) packet).setLang(lang);
				}
				else if (packet instanceof ExShowScreenMessage)
				{
					((ExShowScreenMessage) packet).setLang(lang);
				}
				else if (packet instanceof NpcInfo)
				{
					((NpcInfo) packet).setLang(lang);
				}
			}
		}
		
		// Write into the channel.
		_channel.writeAndFlush(packet);
		
		// Run packet implementation.
		packet.runImpl(_player);
	}
	
	/**
	 * @param smId
	 */
	public void sendPacket(SystemMessageId smId)
	{
		sendPacket(new SystemMessage(smId));
	}
	
	public boolean isDetached()
	{
		return _isDetached;
	}
	
	public void setDetached(boolean value)
	{
		_isDetached = value;
	}
	
	/**
	 * Method to handle character deletion
	 * @param characterSlot
	 * @return a byte:
	 *         <li>-1: Error: No char was found for such charslot, caught exception, etc...
	 *         <li>0: character is not member of any clan, proceed with deletion
	 *         <li>1: character is member of a clan, but not clan leader
	 *         <li>2: character is clan leader
	 */
	public byte markToDeleteChar(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return -1;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clanId FROM characters WHERE charId=?"))
		{
			statement.setInt(1, objectId);
			byte answer = 0;
			try (ResultSet rs = statement.executeQuery())
			{
				final int clanId = rs.next() ? rs.getInt(1) : 0;
				if (clanId != 0)
				{
					final Clan clan = ClanTable.getInstance().getClan(clanId);
					if (clan == null)
					{
						answer = 0; // jeezes!
					}
					else if (clan.getLeaderId() == objectId)
					{
						answer = 2;
					}
					else
					{
						answer = 1;
					}
				}
				
				// Setting delete time
				if (answer == 0)
				{
					if (Config.DELETE_DAYS == 0)
					{
						deleteCharByObjId(objectId);
					}
					else
					{
						try (PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?"))
						{
							ps2.setLong(1, Chronos.currentTimeMillis() + (Config.DELETE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
							ps2.setInt(2, objectId);
							ps2.execute();
						}
					}
					
					LOGGER_ACCOUNTING.info("Delete, " + objectId + ", " + this);
				}
			}
			return answer;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error updating delete time of character.", e);
			return -1;
		}
	}
	
	public void restore(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?"))
		{
			statement.setInt(1, objectId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring character.", e);
		}
		
		LOGGER_ACCOUNTING.info("Restore, " + objectId + ", " + this);
	}
	
	public static void deleteCharByObjId(int objid)
	{
		if (objid < 0)
		{
			return;
		}
		
		CharNameTable.getInstance().removeName(objid);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_contacts WHERE charId=? OR contactId=?"))
			{
				ps.setInt(1, objid);
				ps.setInt(2, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?"))
			{
				ps.setInt(1, objid);
				ps.setInt(2, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_quests WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM heroes WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_raid_points WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_reco_bonus WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE charId=?"))
			{
				ps.setInt(1, objid);
				ps.execute();
			}
			
			if (Config.ALLOW_WEDDING)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE player1Id = ? OR player2Id = ?"))
				{
					ps.setInt(1, objid);
					ps.setInt(2, objid);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error deleting character.", e);
		}
	}
	
	public PlayerInstance load(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return null;
		}
		
		PlayerInstance player = World.getInstance().getPlayer(objectId);
		if (player != null)
		{
			// exploit prevention, should not happens in normal way
			if (player.isOnlineInt() == 1)
			{
				LOGGER.severe("Attempt of double login: " + player.getName() + "(" + objectId + ") " + _accountName);
			}
			
			if (player.getClient() != null)
			{
				Disconnection.of(player).defaultSequence(LeaveWorld.STATIC_PACKET);
			}
			else
			{
				player.storeMe();
				player.deleteMe();
			}
			
			return null;
		}
		
		player = PlayerInstance.load(objectId);
		if (player != null)
		{
			// prevent some values for each login
			player.setRunning(); // running is default
			player.standUp(); // standing is default
			player.refreshOverloaded();
			player.refreshExpertisePenalty();
		}
		else
		{
			LOGGER.severe("Could not restore in slot: " + characterSlot);
		}
		
		return player;
	}
	
	public void setCharSelection(List<CharSelectInfoPackage> characters)
	{
		_charSlotMapping = characters;
	}
	
	public CharSelectInfoPackage getCharSelection(int charslot)
	{
		if ((_charSlotMapping == null) || (charslot < 0) || (charslot >= _charSlotMapping.size()))
		{
			return null;
		}
		return _charSlotMapping.get(charslot);
	}
	
	public SecondaryPasswordAuth getSecondaryAuth()
	{
		return _secondaryAuth;
	}
	
	/**
	 * @param characterSlot
	 * @return
	 */
	private int getObjectIdForSlot(int characterSlot)
	{
		final CharSelectInfoPackage info = getCharSelection(characterSlot);
		if (info == null)
		{
			LOGGER.warning(toString() + " tried to delete Character in slot " + characterSlot + " but no characters exits at that slot.");
			return -1;
		}
		return info.getObjectId();
	}
	
	/**
	 * Produces the best possible string representation of this client.
	 */
	@Override
	public String toString()
	{
		try
		{
			final InetAddress address = _addr;
			final ConnectionState state = (ConnectionState) getConnectionState();
			switch (state)
			{
				case CONNECTED:
				{
					return "[IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				}
				case AUTHENTICATED:
				{
					return "[Account: " + _accountName + " - IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				}
				case ENTERING:
				case IN_GAME:
				{
					return "[Character: " + (_player == null ? "disconnected" : _player.getName() + "[" + _player.getObjectId() + "]") + " - Account: " + _accountName + " - IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				}
				default:
				{
					throw new IllegalStateException("Missing state on switch");
				}
			}
		}
		catch (NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}
	
	public void setProtocolVersion(int version)
	{
		_protocolVersion = version;
	}
	
	public int getProtocolVersion()
	{
		return _protocolVersion;
	}
	
	public boolean isProtocolOk()
	{
		return _protocolOk;
	}
	
	public void setProtocolOk(boolean value)
	{
		_protocolOk = value;
	}
	
	public void setClientTracert(int[][] tracert)
	{
		_trace = tracert;
	}
	
	public int[][] getTrace()
	{
		return _trace;
	}
	
	public void sendActionFailed()
	{
		sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public ICrypt getCrypt()
	{
		return _crypt;
	}
	
	/**
	 * @return the hardwareInfo
	 */
	public ClientHardwareInfoHolder getHardwareInfo()
	{
		return _hardwareInfo;
	}
	
	/**
	 * @param hardwareInfo
	 */
	public void setHardwareInfo(ClientHardwareInfoHolder hardwareInfo)
	{
		_hardwareInfo = hardwareInfo;
	}
}
