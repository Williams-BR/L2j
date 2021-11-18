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
package org.l2jmobius.gameserver.model.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.CastleManorManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager.Territory;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.TowerSpawn;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.ArtefactInstance;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.residences.AbstractResidence;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.type.CastleZone;
import org.l2jmobius.gameserver.model.zone.type.ResidenceTeleportZone;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Castle extends AbstractResidence
{
	protected static final Logger LOGGER = Logger.getLogger(Castle.class.getName());
	
	private final List<DoorInstance> _doors = new ArrayList<>();
	int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true; // true if Castle Lords set the time, or 24h is elapsed after the siege
	private Calendar _siegeTimeRegistrationEndDate; // last siege end date + 1 day
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private long _treasury = 0;
	private boolean _showNpcCrest = false;
	private SiegeZone _zone = null;
	private ResidenceTeleportZone _teleZone;
	private Clan _formerOwner = null;
	private final List<ArtefactInstance> _artefacts = new ArrayList<>(1);
	private final Map<Integer, CastleFunction> _function;
	private int _ticketBuyCount = 0;
	
	/** Castle Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class CastleFunction
	{
		final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		final long _rate;
		long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (_ownerId <= 0)
			{
				return;
			}
			final long currentTime = Chronos.currentTimeMillis();
			if (_endDate > currentTime)
			{
				ThreadPool.schedule(new FunctionTask(cwh), _endDate - currentTime);
			}
			else
			{
				ThreadPool.schedule(new FunctionTask(cwh), 0);
			}
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (_ownerId <= 0)
					{
						return;
					}
					if ((ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						int fee = _fee;
						if (_endDate == -1)
						{
							fee = _tempFee;
						}
						
						setEndTime(Chronos.currentTimeMillis() + _rate);
						dbSave();
						if (_cwh)
						{
							ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CS_function_fee", Inventory.ADENA_ID, fee, null, null);
						}
						ThreadPool.schedule(new FunctionTask(true), _rate);
					}
					else
					{
						removeFunction(_type);
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "", e);
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, _type);
				ps.setInt(3, _lvl);
				ps.setInt(4, _fee);
				ps.setLong(5, _rate);
				ps.setLong(6, _endDate);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	public Castle(int castleId)
	{
		super(castleId);
		load();
		/*
		 * if (getResidenceId() == 7 || castleId == 9) // Goddard and Schuttgart _nbArtifact = 2;
		 */
		_function = new ConcurrentHashMap<>();
		initResidenceZone();
		if (_ownerId != 0)
		{
			loadFunctions();
			loadDoorUpgrade();
		}
	}
	
	/**
	 * Return function with id
	 * @param type
	 * @return
	 */
	public CastleFunction getFunction(int type)
	{
		return _function.get(type);
	}
	
	public synchronized void engrave(Clan clan, WorldObject target)
	{
		if (!_artefacts.contains(target))
		{
			return;
		}
		setOwner(clan);
		final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCESSFULLY_ENGRAVED_THE_HOLY_ARTIFACT);
		msg.addString(clan.getName());
		getSiege().announceToPlayer(msg, true);
	}
	
	// This method add to the treasury
	/**
	 * Add amount to castle instance's treasury (warehouse).
	 * @param amountValue
	 */
	public void addToTreasury(long amountValue)
	{
		// check if owned
		if (_ownerId <= 0)
		{
			return;
		}
		
		long amount = amountValue;
		if (getName().equalsIgnoreCase("Schuttgart") || getName().equalsIgnoreCase("Goddard"))
		{
			final Castle rune = CastleManager.getInstance().getCastle("rune");
			if (rune != null)
			{
				final long runeTax = (long) (amount * rune.getTaxRate());
				if (rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax);
				}
				amount -= runeTax;
			}
		}
		if (!getName().equalsIgnoreCase("aden") && !getName().equalsIgnoreCase("Rune") && !getName().equalsIgnoreCase("Schuttgart") && !getName().equalsIgnoreCase("Goddard")) // If current castle instance is not Aden, Rune, Goddard or Schuttgart.
		{
			final Castle aden = CastleManager.getInstance().getCastle("aden");
			if (aden != null)
			{
				final long adenTax = (long) (amount * aden.getTaxRate()); // Find out what Aden gets from the current castle instance's income
				if (aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned
				}
				
				amount -= adenTax; // Subtract Aden's income from current castle instance's income
			}
		}
		
		addToTreasuryNoTax(amount);
	}
	
	/**
	 * Add amount to castle instance's treasury (warehouse), no tax paying.
	 * @param amountValue
	 * @return
	 */
	public boolean addToTreasuryNoTax(long amountValue)
	{
		if (_ownerId <= 0)
		{
			return false;
		}
		
		long amount = amountValue;
		if (amount < 0)
		{
			amount *= -1;
			if (_treasury < amount)
			{
				return false;
			}
			_treasury -= amount;
		}
		else if ((_treasury + amount) > Inventory.MAX_ADENA)
		{
			_treasury = Inventory.MAX_ADENA;
		}
		else
		{
			_treasury += amount;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?"))
		{
			ps.setLong(1, _treasury);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Move non clan members off castle area and to nearest town.
	 */
	public void banishForeigners()
	{
		getResidenceZone().banishForeigners(_ownerId);
	}
	
	/**
	 * Return true if object is inside the zone
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}
	
	public SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getResidenceId())
				{
					_zone = zone;
					break;
				}
			}
		}
		return _zone;
	}
	
	@Override
	public CastleZone getResidenceZone()
	{
		return (CastleZone) super.getResidenceZone();
	}
	
	public ResidenceTeleportZone getTeleZone()
	{
		if (_teleZone == null)
		{
			for (ResidenceTeleportZone zone : ZoneManager.getInstance().getAllZones(ResidenceTeleportZone.class))
			{
				if (zone.getResidenceId() == getResidenceId())
				{
					_teleZone = zone;
					break;
				}
			}
		}
		return _teleZone;
	}
	
	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}
	
	/**
	 * Get the objects distance to this castle
	 * @param obj
	 * @return
	 */
	public double getDistance(WorldObject obj)
	{
		return getZone().getDistanceToZone(obj);
	}
	
	public void closeDoor(PlayerInstance player, int doorId)
	{
		openCloseDoor(player, doorId, false);
	}
	
	public void openDoor(PlayerInstance player, int doorId)
	{
		openCloseDoor(player, doorId, true);
	}
	
	public void openCloseDoor(PlayerInstance player, int doorId, boolean open)
	{
		if (player.getClanId() != _ownerId)
		{
			return;
		}
		
		final DoorInstance door = getDoor(doorId);
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	// This method is used to begin removing all castle upgrades
	public void removeUpgrade()
	{
		removeDoorUpgrade();
		removeTrapUpgrade();
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}
	
	// This method updates the castle tax rate
	public void setOwner(Clan clan)
	{
		// Remove old owner
		if ((_ownerId > 0) && ((clan == null) || (clan.getId() != _ownerId)))
		{
			final Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
			if (oldOwner != null)
			{
				if (_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if (Config.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getResidenceId());
					}
				}
				try
				{
					final PlayerInstance oldleader = oldOwner.getLeader().getPlayerInstance();
					if ((oldleader != null) && (oldleader.getMountType() == MountType.WYVERN))
					{
						oldleader.dismount();
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Exception in setOwner: " + e.getMessage(), e);
				}
				oldOwner.setCastleId(0); // Unset has castle flag for old owner
				for (PlayerInstance member : oldOwner.getOnlineMembers(0))
				{
					removeResidentialSkills(member);
					member.sendSkillList();
				}
			}
		}
		
		updateOwnerInDB(clan); // Update in database
		setShowNpcCrest(false);
		
		// if clan have fortress, remove it
		if ((clan != null) && (clan.getFortId() > 0))
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}
		
		if (getSiege().isInProgress())
		{
			getSiege().midVictory(); // Mid victory phase of siege
		}
		
		TerritoryWarManager.getInstance().getTerritory(getResidenceId()).setOwnerClan(clan);
		
		if (clan != null)
		{
			for (PlayerInstance member : clan.getOnlineMembers(0))
			{
				giveResidentialSkills(member);
				member.sendSkillList();
			}
		}
	}
	
	public void removeOwner(Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;
			if (Config.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getResidenceId());
			}
			for (PlayerInstance member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList();
			}
			clan.setCastleId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}
		
		updateOwnerInDB(null);
		if (getSiege().isInProgress())
		{
			getSiege().midVictory();
		}
		
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}
	
	public void setTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?"))
		{
			ps.setInt(1, taxPercent);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Respawn all doors on castle grounds.
	 */
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	/**
	 * Respawn all doors on castle grounds
	 * @param isDoorWeak
	 */
	public void spawnDoor(boolean isDoorWeak)
	{
		for (DoorInstance door : _doors)
		{
			if (door.isDead())
			{
				door.doRevive();
				door.setCurrentHp((isDoorWeak) ? (door.getMaxHp() / 2) : (door.getMaxHp()));
			}
			
			if (door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	// This method loads castle
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			PreparedStatement ps2 = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?"))
		{
			ps1.setInt(1, getResidenceId());
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					setName(rs.getString("name"));
					// _OwnerId = rs.getInt("ownerId");
					_siegeDate = Calendar.getInstance();
					_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
					_siegeTimeRegistrationEndDate = Calendar.getInstance();
					_siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
					_isTimeRegistrationOver = rs.getBoolean("regTimeOver");
					_taxPercent = rs.getInt("taxPercent");
					_treasury = rs.getLong("treasury");
					_showNpcCrest = rs.getBoolean("showNpcCrest");
					_ticketBuyCount = rs.getInt("ticketBuyCount");
				}
			}
			_taxRate = _taxPercent / 100.0;
			ps2.setInt(1, getResidenceId());
			try (ResultSet rs = ps2.executeQuery())
			{
				while (rs.next())
				{
					_ownerId = rs.getInt("clan_id");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}
	
	/** Load All Functions */
	private void loadFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_functions WHERE castle_id = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Castle.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove function In List and in DB
	 * @param functionType
	 */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_functions WHERE castle_id=? AND type=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, functionType);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Castle.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	public boolean updateFunctions(PlayerInstance player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
		{
			return false;
		}
		if ((lease > 0) && !player.destroyItemByItemId("Consume", Inventory.ADENA_ID, lease, null, true))
		{
			return false;
		}
		if (addNew)
		{
			_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, 0, false));
		}
		else if ((lvl == 0) && (lease == 0))
		{
			removeFunction(type);
		}
		else
		{
			final int diffLease = lease - _function.get(type).getLease();
			if (diffLease > 0)
			{
				_function.remove(type);
				_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, -1, false));
			}
			else
			{
				_function.get(type).setLease(lease);
				_function.get(type).setLvl(lvl);
				_function.get(type).dbSave();
			}
		}
		return true;
	}
	
	public void activateInstance()
	{
		loadDoor();
	}
	
	// This method loads castle door data from database
	private void loadDoor()
	{
		for (DoorInstance door : DoorData.getInstance().getDoors())
		{
			if ((door.getCastle() != null) && (door.getCastle().getResidenceId() == getResidenceId()))
			{
				_doors.add(door);
			}
		}
	}
	
	// This method loads castle door upgrade data from database
	private void loadDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_doorupgrade WHERE castleId=?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					setDoorUpgrade(rs.getInt("doorId"), rs.getInt("ratio"), false);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadCastleDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	private void removeDoorUpgrade()
	{
		for (DoorInstance door : _doors)
		{
			door.getStat().setUpgradeHpRatio(1);
			door.setCurrentHp(door.getCurrentHp());
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_doorupgrade WHERE castleId=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	public void setDoorUpgrade(int doorId, int ratio, boolean save)
	{
		final DoorInstance door = (getDoors().isEmpty()) ? DoorData.getInstance().getDoor(doorId) : getDoor(doorId);
		if (door == null)
		{
			return;
		}
		
		door.getStat().setUpgradeHpRatio(ratio);
		door.setCurrentHp(door.getMaxHp());
		
		if (save)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_doorupgrade (doorId, ratio, castleId) values (?,?,?)"))
			{
				ps.setInt(1, doorId);
				ps.setInt(2, ratio);
				ps.setInt(3, getResidenceId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception: setDoorUpgrade(int doorId, int ratio, int castleId): " + e.getMessage(), e);
			}
		}
	}
	
	private void updateOwnerInDB(Clan clan)
	{
		if (clan != null)
		{
			_ownerId = clan.getId(); // Update owner id property
		}
		else
		{
			_ownerId = 0; // Remove owner
			CastleManorManager.getInstance().resetManorData(getResidenceId());
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Need to remove has castle flag from clan_data, should be checked from castle table.
			try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = 0 WHERE hasCastle = ?"))
			{
				ps.setInt(1, getResidenceId());
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = ? WHERE clan_id = ?"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, _ownerId);
				ps.execute();
			}
			
			// Announce to clan members
			if (clan != null)
			{
				clan.setCastleId(getResidenceId()); // Set has castle flag for new owner
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
	
	public DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}
		
		for (DoorInstance door : _doors)
		{
			if (door.getId() == doorId)
			{
				return door;
			}
		}
		return null;
	}
	
	public List<DoorInstance> getDoors()
	{
		return _doors;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public Clan getOwner()
	{
		return (_ownerId != 0) ? ClanTable.getInstance().getClan(_ownerId) : null;
	}
	
	public Siege getSiege()
	{
		if (_siege == null)
		{
			_siege = new Siege(this);
		}
		return _siege;
	}
	
	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public boolean isTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}
	
	public void setTimeRegistrationOver(boolean value)
	{
		_isTimeRegistrationOver = value;
	}
	
	public Calendar getTimeRegistrationOverDate()
	{
		if (_siegeTimeRegistrationEndDate == null)
		{
			_siegeTimeRegistrationEndDate = Calendar.getInstance();
		}
		return _siegeTimeRegistrationEndDate;
	}
	
	public int getTaxPercent()
	{
		return _taxPercent;
	}
	
	public double getTaxRate()
	{
		return _taxRate;
	}
	
	public long getTreasury()
	{
		return _treasury;
	}
	
	public boolean getShowNpcCrest()
	{
		return _showNpcCrest;
	}
	
	public void setShowNpcCrest(boolean showNpcCrest)
	{
		if (_showNpcCrest != showNpcCrest)
		{
			_showNpcCrest = showNpcCrest;
			updateShowNpcCrest();
		}
	}
	
	public void updateClansReputation()
	{
		if (_formerOwner != null)
		{
			if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
			{
				final int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.takeReputationScore(Config.LOOSE_CASTLE_POINTS, true);
				final Clan owner = ClanTable.getInstance().getClan(getOwnerId());
				if (owner != null)
				{
					owner.addReputationScore(Math.min(Config.TAKE_CASTLE_POINTS, maxreward), true);
				}
			}
			else
			{
				_formerOwner.addReputationScore(Config.CASTLE_DEFENDED_POINTS, true);
			}
		}
		else
		{
			final Clan owner = ClanTable.getInstance().getClan(getOwnerId());
			if (owner != null)
			{
				owner.addReputationScore(Config.TAKE_CASTLE_POINTS, true);
			}
		}
	}
	
	public void updateShowNpcCrest()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET showNpcCrest = ? WHERE id = ?"))
		{
			ps.setString(1, String.valueOf(_showNpcCrest));
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.info("Error saving showNpcCrest for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	@Override
	public void giveResidentialSkills(PlayerInstance player)
	{
		final Territory territory = TerritoryWarManager.getInstance().getTerritory(getResidenceId());
		if ((territory != null) && territory.getOwnedWardIds().contains(getResidenceId() + 80))
		{
			for (int wardId : territory.getOwnedWardIds())
			{
				final List<SkillLearn> territorySkills = SkillTreeData.getInstance().getAvailableResidentialSkills(wardId);
				for (SkillLearn s : territorySkills)
				{
					final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
					if (sk != null)
					{
						player.addSkill(sk, false);
					}
					else
					{
						LOGGER.warning("Trying to add a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!");
					}
				}
			}
		}
		super.giveResidentialSkills(player);
	}
	
	@Override
	public void removeResidentialSkills(PlayerInstance player)
	{
		if (TerritoryWarManager.getInstance().getTerritory(getResidenceId()) != null)
		{
			for (int wardId : TerritoryWarManager.getInstance().getTerritory(getResidenceId()).getOwnedWardIds())
			{
				final List<SkillLearn> territorySkills = SkillTreeData.getInstance().getAvailableResidentialSkills(wardId);
				for (SkillLearn s : territorySkills)
				{
					final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
					if (sk != null)
					{
						player.removeSkill(sk, false, true);
					}
					else
					{
						LOGGER.warning("Trying to remove a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!");
					}
				}
			}
		}
		super.removeResidentialSkills(player);
	}
	
	/**
	 * Register Artefact to castle
	 * @param artefact
	 */
	public void registerArtefact(ArtefactInstance artefact)
	{
		_artefacts.add(artefact);
	}
	
	public List<ArtefactInstance> getArtefacts()
	{
		return _artefacts;
	}
	
	/**
	 * @return the tickets exchanged for this castle
	 */
	public int getTicketBuyCount()
	{
		return _ticketBuyCount;
	}
	
	/**
	 * Set the exchanged tickets count.<br>
	 * Performs database update.
	 * @param count the ticket count to set
	 */
	public void setTicketBuyCount(int count)
	{
		_ticketBuyCount = count;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET ticketBuyCount = ? WHERE id = ?"))
		{
			ps.setInt(1, _ticketBuyCount);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public int getTrapUpgradeLevel(int towerIndex)
	{
		final TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(getResidenceId()).get(towerIndex);
		return (spawn != null) ? spawn.getUpgradeLevel() : 0;
	}
	
	public void setTrapUpgrade(int towerIndex, int level, boolean save)
	{
		if (save)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_trapupgrade (castleId, towerIndex, level) values (?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, towerIndex);
				ps.setInt(3, level);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception: setTrapUpgradeLevel(int towerIndex, int level, int castleId): " + e.getMessage(), e);
			}
		}
		final TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(getResidenceId()).get(towerIndex);
		if (spawn != null)
		{
			spawn.setUpgradeLevel(level);
		}
	}
	
	private void removeTrapUpgrade()
	{
		for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(getResidenceId()))
		{
			ts.setUpgradeLevel(0);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_trapupgrade WHERE castleId=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	@Override
	protected void initResidenceZone()
	{
		for (CastleZone zone : ZoneManager.getInstance().getAllZones(CastleZone.class))
		{
			if (zone.getResidenceId() == getResidenceId())
			{
				setResidenceZone(zone);
				break;
			}
		}
	}
}
