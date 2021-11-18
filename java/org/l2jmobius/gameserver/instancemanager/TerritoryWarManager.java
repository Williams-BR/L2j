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
package org.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.PropertiesParser;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.SiegeClan;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.TerritoryWard;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.Siegable;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.Util;

public class TerritoryWarManager implements Siegable
{
	private static final Logger LOGGER = Logger.getLogger(TerritoryWarManager.class.getName());
	
	// SQL
	private static final String DELETE = "DELETE FROM territory_registrations WHERE castleId = ? and registeredId = ?";
	private static final String INSERT = "INSERT INTO territory_registrations (castleId, registeredId) values (?, ?)";
	public static final Map<Integer, Integer> TERRITORY_ITEM_IDS = new HashMap<>();
	static
	{
		TERRITORY_ITEM_IDS.put(81, 13757);
		TERRITORY_ITEM_IDS.put(82, 13758);
		TERRITORY_ITEM_IDS.put(83, 13759);
		TERRITORY_ITEM_IDS.put(84, 13760);
		TERRITORY_ITEM_IDS.put(85, 13761);
		TERRITORY_ITEM_IDS.put(86, 13762);
		TERRITORY_ITEM_IDS.put(87, 13763);
		TERRITORY_ITEM_IDS.put(88, 13764);
		TERRITORY_ITEM_IDS.put(89, 13765);
	}
	
	public static String qn = "TerritoryWarSuperClass";
	public static String GLOBAL_VARIABLE = "nextTWStartDate";
	public static int DEFENDERMAXCLANS; // Max number of clans
	public static int DEFENDERMAXPLAYERS; // Max number of individual player
	public static int CLANMINLEVEL;
	public static int PLAYERMINLEVEL;
	public static int MINTWBADGEFORNOBLESS;
	public static int MINTWBADGEFORSTRIDERS;
	public static int MINTWBADGEFORBIGSTRIDER;
	public static Long WARLENGTH;
	public static boolean PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS;
	public static boolean RETURN_WARDS_WHEN_TW_STARTS;
	
	// Territory War settings
	private final Map<Integer, List<Clan>> _registeredClans = new ConcurrentHashMap<>();
	private final Map<Integer, List<Integer>> _registeredMercenaries = new ConcurrentHashMap<>();
	private final Map<Integer, Territory> _territoryList = new ConcurrentHashMap<>();
	protected Collection<Integer> _disguisedPlayers = ConcurrentHashMap.newKeySet();
	private final Collection<TerritoryWard> _territoryWards = ConcurrentHashMap.newKeySet();
	private final Map<Clan, SiegeFlagInstance> _clanFlags = new ConcurrentHashMap<>();
	private final Map<Integer, Integer[]> _participantPoints = new ConcurrentHashMap<>();
	protected Calendar _startTWDate = Calendar.getInstance();
	protected boolean _isRegistrationOver = true;
	protected boolean _isTWChannelOpen = false;
	boolean _isTWInProgress = false;
	protected ScheduledFuture<?> _scheduledStartTWTask = null;
	protected ScheduledFuture<?> _scheduledEndTWTask = null;
	protected ScheduledFuture<?> _scheduledRewardOnlineTask = null;
	
	protected TerritoryWarManager()
	{
		load();
	}
	
	public int getRegisteredTerritoryId(PlayerInstance player)
	{
		if ((player == null) || !_isTWChannelOpen || (player.getLevel() < PLAYERMINLEVEL))
		{
			return 0;
		}
		if (player.getClan() != null)
		{
			if (player.getClan().getCastleId() > 0)
			{
				return player.getClan().getCastleId() + 80;
			}
			for (int cId : _registeredClans.keySet())
			{
				if (_registeredClans.get(cId).contains(player.getClan()))
				{
					return cId + 80;
				}
			}
		}
		for (int cId : _registeredMercenaries.keySet())
		{
			if (_registeredMercenaries.get(cId).contains(player.getObjectId()))
			{
				return cId + 80;
			}
		}
		return 0;
	}
	
	public boolean isAllyField(PlayerInstance player, int fieldId)
	{
		if ((player == null) || (player.getSiegeSide() == 0))
		{
			return false;
		}
		else if ((player.getSiegeSide() - 80) == fieldId)
		{
			return true;
		}
		else if ((fieldId > 100) && _territoryList.containsKey((player.getSiegeSide() - 80)) && (_territoryList.get((player.getSiegeSide() - 80)).getFortId() == fieldId))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @param castleId
	 * @param clan The Clan of the player
	 * @return true if the clan is registered
	 */
	public boolean checkIsRegistered(int castleId, Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		if (clan.getCastleId() > 0)
		{
			return (castleId == -1 ? true : (clan.getCastleId() == castleId));
		}
		
		if (castleId == -1)
		{
			for (int cId : _registeredClans.keySet())
			{
				if (_registeredClans.get(cId).contains(clan))
				{
					return true;
				}
			}
			return false;
		}
		return _registeredClans.get(castleId).contains(clan);
	}
	
	/**
	 * @param castleId
	 * @param objId
	 * @return true if the player is registered
	 */
	public boolean checkIsRegistered(int castleId, int objId)
	{
		if (castleId == -1)
		{
			for (int cId : _registeredMercenaries.keySet())
			{
				if (_registeredMercenaries.get(cId).contains(objId))
				{
					return true;
				}
			}
			return false;
		}
		return _registeredMercenaries.get(castleId).contains(objId);
	}
	
	public Territory getTerritory(int castleId)
	{
		return _territoryList.get(castleId);
	}
	
	public List<Territory> getAllTerritories()
	{
		final List<Territory> ret = new LinkedList<>();
		for (Territory t : _territoryList.values())
		{
			if (t.getOwnerClan() != null)
			{
				ret.add(t);
			}
		}
		return ret;
	}
	
	public List<Clan> getRegisteredClans(int castleId)
	{
		return _registeredClans.get(castleId);
	}
	
	public void addDisguisedPlayer(int playerObjId)
	{
		_disguisedPlayers.add(playerObjId);
	}
	
	public boolean isDisguised(int playerObjId)
	{
		return _disguisedPlayers.contains(playerObjId);
	}
	
	public List<Integer> getRegisteredMercenaries(int castleId)
	{
		return _registeredMercenaries.get(castleId);
	}
	
	public long getTWStartTimeInMillis()
	{
		return _startTWDate.getTimeInMillis();
	}
	
	public Calendar getTWStart()
	{
		return _startTWDate;
	}
	
	public void setTWStartTimeInMillis(long time)
	{
		_startTWDate.setTimeInMillis(time);
		if (_isTWInProgress)
		{
			if (_scheduledEndTWTask != null)
			{
				_scheduledEndTWTask.cancel(false);
			}
			_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), 1000);
		}
		else
		{
			if (_scheduledStartTWTask != null)
			{
				_scheduledStartTWTask.cancel(false);
			}
			_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), 1000);
		}
	}
	
	public boolean isTWChannelOpen()
	{
		return _isTWChannelOpen;
	}
	
	public void registerClan(int castleId, Clan clan)
	{
		if ((clan == null) || ((_registeredClans.get(castleId) != null) && _registeredClans.get(castleId).contains(clan)))
		{
			return;
		}
		
		_registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
		_registeredClans.get(castleId).add(clan);
		changeRegistration(castleId, clan.getId(), false);
	}
	
	public void registerMerc(int castleId, PlayerInstance player)
	{
		if ((player == null) || (player.getLevel() < PLAYERMINLEVEL) || ((_registeredMercenaries.get(castleId) != null) && _registeredMercenaries.get(castleId).contains(player.getObjectId())))
		{
			return;
		}
		
		_registeredMercenaries.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
		_registeredMercenaries.get(castleId).add(player.getObjectId());
		changeRegistration(castleId, player.getObjectId(), false);
	}
	
	public void removeClan(int castleId, Clan clan)
	{
		if ((clan != null) && (_registeredClans.get(castleId) != null) && _registeredClans.get(castleId).contains(clan))
		{
			_registeredClans.get(castleId).remove(clan);
			changeRegistration(castleId, clan.getId(), true);
		}
	}
	
	public void removeMerc(int castleId, PlayerInstance player)
	{
		if ((player != null) && (_registeredMercenaries.get(castleId) != null) && _registeredMercenaries.get(castleId).contains(player.getObjectId()))
		{
			_registeredMercenaries.get(castleId).remove(_registeredMercenaries.get(castleId).indexOf(player.getObjectId()));
			changeRegistration(castleId, player.getObjectId(), true);
		}
	}
	
	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	public boolean isTWInProgress()
	{
		return _isTWInProgress;
	}
	
	public void territoryCatapultDestroyed(int castleId)
	{
		if (_territoryList.get(castleId) != null)
		{
			_territoryList.get(castleId).changeNPCsSpawn(2, false);
		}
		for (DoorInstance door : CastleManager.getInstance().getCastleById(castleId).getDoors())
		{
			door.openMe();
		}
	}
	
	public Npc addTerritoryWard(int territoryId, int newOwnerId, int oldOwnerId, boolean broadcastMessage)
	{
		Npc ret = null;
		final Territory terNew = _territoryList.get(newOwnerId);
		if (terNew != null)
		{
			final TerritoryNPCSpawn ward = terNew.getFreeWardSpawnPlace();
			if (ward != null)
			{
				ward._npcId = territoryId;
				ret = spawnNPC(36491 + territoryId, ward.getLocation());
				ward.setNPC(ret);
				if (!_isTWInProgress && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
				{
					ret.decayMe();
				}
				if ((terNew.getOwnerClan() != null) && terNew.getOwnedWardIds().contains(newOwnerId + 80))
				{
					for (int wardId : terNew.getOwnedWardIds())
					{
						final List<SkillLearn> residentialSkills = SkillTreeData.getInstance().getAvailableResidentialSkills(wardId);
						for (SkillLearn s : residentialSkills)
						{
							final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
							if (sk != null)
							{
								for (PlayerInstance member : terNew.getOwnerClan().getOnlineMembers(0))
								{
									if (!member.isInOlympiadMode())
									{
										member.addSkill(sk, false);
									}
								}
							}
						}
					}
				}
			}
			if (_territoryList.containsKey(oldOwnerId))
			{
				final Territory terOld = _territoryList.get(oldOwnerId);
				terOld.removeWard(territoryId);
				updateTerritoryData(terOld);
				updateTerritoryData(terNew);
				if (broadcastMessage)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEEDED_IN_CAPTURING_S2_S_TERRITORY_WARD);
					sm.addString(terNew.getOwnerClan().getName());
					sm.addCastleId(territoryId);
					announceToParticipants(sm, 135000, 13500);
				}
				if (terOld.getOwnerClan() != null)
				{
					final List<SkillLearn> territorySkills = SkillTreeData.getInstance().getAvailableResidentialSkills(territoryId);
					for (SkillLearn s : territorySkills)
					{
						final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
						if (sk != null)
						{
							for (PlayerInstance member : terOld.getOwnerClan().getOnlineMembers(0))
							{
								member.removeSkill(sk, false);
							}
						}
					}
					
					if (!terOld.getOwnedWardIds().isEmpty() && !terOld.getOwnedWardIds().contains(oldOwnerId + 80))
					{
						for (int wardId : terOld.getOwnedWardIds())
						{
							final List<SkillLearn> wardSkills = SkillTreeData.getInstance().getAvailableResidentialSkills(wardId);
							for (SkillLearn s : wardSkills)
							{
								final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
								if (sk != null)
								{
									for (PlayerInstance member : terOld.getOwnerClan().getOnlineMembers(0))
									{
										member.removeSkill(sk, false);
									}
								}
							}
						}
					}
				}
			}
		}
		else
		{
			LOGGER.warning(getClass().getSimpleName() + ": Missing territory for new Ward owner: " + newOwnerId + ";" + territoryId);
		}
		return ret;
	}
	
	public SiegeFlagInstance getHQForClan(Clan clan)
	{
		if (clan.getCastleId() > 0)
		{
			return _territoryList.get(clan.getCastleId()).getHQ();
		}
		return null;
	}
	
	public SiegeFlagInstance getHQForTerritory(int territoryId)
	{
		return _territoryList.get(territoryId - 80).getHQ();
	}
	
	public void setHQForClan(Clan clan, SiegeFlagInstance hq)
	{
		if (clan.getCastleId() > 0)
		{
			_territoryList.get(clan.getCastleId()).setHQ(hq);
		}
	}
	
	public void addClanFlag(Clan clan, SiegeFlagInstance flag)
	{
		_clanFlags.put(clan, flag);
	}
	
	public boolean isClanHasFlag(Clan clan)
	{
		return _clanFlags.containsKey(clan);
	}
	
	public SiegeFlagInstance getFlagForClan(Clan clan)
	{
		return _clanFlags.get(clan);
	}
	
	public void removeClanFlag(Clan clan)
	{
		_clanFlags.remove(clan);
	}
	
	public Collection<TerritoryWard> getAllTerritoryWards()
	{
		return _territoryWards;
	}
	
	public TerritoryWard getTerritoryWardForOwner(int castleId)
	{
		for (TerritoryWard twWard : _territoryWards)
		{
			if (twWard.getTerritoryId() == castleId)
			{
				return twWard;
			}
		}
		return null;
	}
	
	public TerritoryWard getTerritoryWard(int territoryId)
	{
		for (TerritoryWard twWard : _territoryWards)
		{
			if (twWard.getTerritoryId() == territoryId)
			{
				return twWard;
			}
		}
		return null;
	}
	
	public TerritoryWard getTerritoryWard(PlayerInstance player)
	{
		for (TerritoryWard twWard : _territoryWards)
		{
			if (twWard.playerId == player.getObjectId())
			{
				return twWard;
			}
		}
		return null;
	}
	
	public void dropCombatFlag(PlayerInstance player, boolean isKilled, boolean isSpawnBack)
	{
		for (TerritoryWard twWard : _territoryWards)
		{
			if (twWard.playerId == player.getObjectId())
			{
				twWard.dropIt();
				if (_isTWInProgress)
				{
					if (isKilled)
					{
						twWard.spawnMe();
					}
					else if (isSpawnBack)
					{
						twWard.spawnBack();
					}
					else
					{
						for (TerritoryNPCSpawn wardSpawn : _territoryList.get(twWard.getOwnerCastleId()).getOwnedWard())
						{
							if (wardSpawn.getId() == twWard.getTerritoryId())
							{
								wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn());
								twWard.unSpawnMe();
								twWard.setNpc(wardSpawn.getNpc());
							}
						}
					}
				}
				if (isKilled)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CHARACTER_THAT_ACQUIRED_S1_S_WARD_HAS_BEEN_KILLED);
					sm.addString(twWard.getNpc().getName().replace(" Ward", ""));
					announceToParticipants(sm, 0, 0);
				}
			}
		}
	}
	
	public void giveTWQuestPoint(PlayerInstance player)
	{
		_participantPoints.putIfAbsent(player.getObjectId(), new Integer[]
		{
			player.getSiegeSide(),
			0,
			0,
			0,
			0,
			0,
			0
		});
		_participantPoints.get(player.getObjectId())[2]++;
	}
	
	public void giveTWPoint(PlayerInstance killer, int victimSide, int type)
	{
		if (victimSide == 0)
		{
			return;
		}
		if ((killer.getParty() != null) && (type < 5))
		{
			for (PlayerInstance pl : killer.getParty().getMembers())
			{
				if ((pl.getSiegeSide() == victimSide) || (pl.getSiegeSide() == 0) || !Util.checkIfInRange(2000, killer, pl, false))
				{
					continue;
				}
				
				_participantPoints.putIfAbsent(pl.getObjectId(), new Integer[]
				{
					pl.getSiegeSide(),
					0,
					0,
					0,
					0,
					0,
					0
				});
				_participantPoints.get(pl.getObjectId())[type]++;
			}
		}
		else
		{
			if ((killer.getSiegeSide() == victimSide) || (killer.getSiegeSide() == 0))
			{
				return;
			}
			
			_participantPoints.putIfAbsent(killer.getObjectId(), new Integer[]
			{
				killer.getSiegeSide(),
				0,
				0,
				0,
				0,
				0,
				0
			});
			_participantPoints.get(killer.getObjectId())[type]++;
		}
	}
	
	public int[] calcReward(PlayerInstance player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			final int[] reward = new int[2];
			final Integer[] temp = _participantPoints.get(player.getObjectId());
			reward[0] = temp[0];
			reward[1] = 0;
			// badges for being online. if char was not online at least 10 mins
			// than he cant get rewards(also this will handle that player already get his/her rewards)
			if (temp[6] < 10)
			{
				return reward;
			}
			reward[1] += (temp[6] > 70 ? 7 : (int) (temp[6] * 0.1));
			// badges for player Quests
			reward[1] += temp[2] * 7;
			// badges for player Kills
			if (temp[1] < 50)
			{
				reward[1] += temp[1] * 0.1;
			}
			else if (temp[1] < 120)
			{
				reward[1] += (5 + ((temp[1] - 50) / 14));
			}
			else
			{
				reward[1] += 10;
			}
			// badges for territory npcs
			reward[1] += temp[3];
			// badges for territory catapults
			reward[1] += temp[4] * 2;
			// badges for territory Wards
			reward[1] += (temp[5] > 0 ? 5 : 0);
			// badges for territory quest done
			reward[1] += Math.min(_territoryList.get(temp[0] - 80).getQuestDone()[0], 10);
			reward[1] += _territoryList.get(temp[0] - 80).getQuestDone()[1];
			reward[1] += _territoryList.get(temp[0] - 80).getOwnedWardIds().size();
			return reward;
		}
		return new int[]
		{
			0,
			0
		};
	}
	
	public void debugReward(PlayerInstance player)
	{
		player.sendMessage("Registred TerrId: " + player.getSiegeSide());
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			final Integer[] temp = _participantPoints.get(player.getObjectId());
			player.sendMessage("TerrId: " + temp[0]);
			player.sendMessage("PcKill: " + temp[1]);
			player.sendMessage("PcQuests: " + temp[2]);
			player.sendMessage("npcKill: " + temp[3]);
			player.sendMessage("CatatKill: " + temp[4]);
			player.sendMessage("WardKill: " + temp[5]);
			player.sendMessage("onlineTime: " + temp[6]);
		}
		else
		{
			player.sendMessage("No points for you!");
		}
		if (_territoryList.containsKey(player.getSiegeSide() - 80))
		{
			player.sendMessage("Your Territory's jobs:");
			player.sendMessage("npcKill: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[0]);
			player.sendMessage("WardCaptured: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[1]);
		}
	}
	
	public void resetReward(PlayerInstance player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			_participantPoints.get(player.getObjectId())[6] = 0;
		}
	}
	
	public Npc spawnNPC(int npcId, Location loc)
	{
		try
		{
			final Spawn spawnDat = new Spawn(npcId);
			spawnDat.setAmount(1);
			spawnDat.setXYZ(loc);
			spawnDat.setHeading(loc.getHeading());
			spawnDat.stopRespawn();
			return spawnDat.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		return null;
	}
	
	private void changeRegistration(int castleId, int objId, boolean delete)
	{
		final String query = delete ? DELETE : INSERT;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setInt(1, castleId);
			ps.setInt(2, objId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: Territory War registration: " + e.getMessage(), e);
		}
	}
	
	private void updateTerritoryData(Territory ter)
	{
		final StringBuilder wardList = new StringBuilder();
		for (int i : ter.getOwnedWardIds())
		{
			wardList.append(i + ";");
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE territories SET ownedWardIds=? WHERE territoryId=?"))
		{
			ps.setString(1, wardList.toString());
			ps.setInt(2, ter.getTerritoryId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: Territory Data update: " + e.getMessage(), e);
		}
	}
	
	private final void load()
	{
		final PropertiesParser territoryWarSettings = new PropertiesParser(Config.TW_CONFIG_FILE);
		
		// Siege setting
		DEFENDERMAXCLANS = territoryWarSettings.getInt("DefenderMaxClans", 500);
		DEFENDERMAXPLAYERS = territoryWarSettings.getInt("DefenderMaxPlayers", 500);
		CLANMINLEVEL = territoryWarSettings.getInt("ClanMinLevel", 0);
		PLAYERMINLEVEL = territoryWarSettings.getInt("PlayerMinLevel", 40);
		WARLENGTH = territoryWarSettings.getLong("WarLength", 120) * 60000;
		PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE = territoryWarSettings.getBoolean("PlayerWithWardCanBeKilledInPeaceZone", false);
		SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS = territoryWarSettings.getBoolean("SpawnWardsWhenTWIsNotInProgress", false);
		RETURN_WARDS_WHEN_TW_STARTS = territoryWarSettings.getBoolean("ReturnWardsWhenTWStarts", false);
		MINTWBADGEFORNOBLESS = territoryWarSettings.getInt("MinTerritoryBadgeForNobless", 100);
		MINTWBADGEFORSTRIDERS = territoryWarSettings.getInt("MinTerritoryBadgeForStriders", 50);
		MINTWBADGEFORBIGSTRIDER = territoryWarSettings.getInt("MinTerritoryBadgeForBigStrider", 80);
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM territory_spawnlist"))
		{
			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int npcId = rs.getInt("npcId");
				final Location loc = new Location(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"));
				final int spawnType = rs.getInt("spawnType");
				if (!_territoryList.containsKey(castleId))
				{
					_territoryList.put(castleId, new Territory(castleId));
				}
				switch (spawnType)
				{
					case 0: // town npcs
					case 1: // fortress npcs
					case 2: // castle npcs
					{
						_territoryList.get(castleId).getSpawnList().add(new TerritoryNPCSpawn(castleId, loc, npcId, spawnType, null));
						break;
					}
					case 3: // ward spawns
					{
						_territoryList.get(castleId).addWardSpawnPlace(loc);
						break;
					}
					default:
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unknown npc type for " + rs.getInt("id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": SpawnList error: " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM territories"))
		{
			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int fortId = rs.getInt("fortId");
				final String ownedWardIds = rs.getString("OwnedWardIds");
				final Territory t = _territoryList.get(castleId);
				if (t != null)
				{
					t._fortId = fortId;
					if (CastleManager.getInstance().getCastleById(castleId).getOwnerId() > 0)
					{
						t.setOwnerClan(ClanTable.getInstance().getClan(CastleManager.getInstance().getCastleById(castleId).getOwnerId()));
						t.changeNPCsSpawn(0, true);
					}
					
					if (!ownedWardIds.isEmpty())
					{
						for (String wardId : ownedWardIds.split(";"))
						{
							if (Integer.parseInt(wardId) > 0)
							{
								addTerritoryWard(Integer.parseInt(wardId), castleId, 0, false);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": territory list error(): " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM territory_registrations"))
		{
			while (rs.next())
			{
				final int castleId = rs.getInt("castleId");
				final int registeredId = rs.getInt("registeredId");
				if (ClanTable.getInstance().getClan(registeredId) != null)
				{
					if (_registeredClans.get(castleId) == null)
					{
						_registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
					}
					_registeredClans.get(castleId).add(ClanTable.getInstance().getClan(registeredId));
				}
				else
				{
					if (_registeredMercenaries.get(castleId) == null)
					{
						_registeredMercenaries.put(castleId, new CopyOnWriteArrayList<>());
					}
					_registeredMercenaries.get(castleId).add(registeredId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": registration list error: " + e.getMessage(), e);
		}
	}
	
	protected void startTerritoryWar()
	{
		if (_territoryList == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": TerritoryList is NULL!");
			return;
		}
		final List<Territory> activeTerritoryList = new LinkedList<>();
		for (Territory t : _territoryList.values())
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			if (castle != null)
			{
				if (castle.getOwnerId() > 0)
				{
					activeTerritoryList.add(t);
				}
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
			}
		}
		
		if (activeTerritoryList.size() < 2)
		{
			return;
		}
		
		_isTWInProgress = true;
		updatePlayerTWStateFlags(false);
		
		// teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
		for (Territory t : activeTerritoryList)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			final Fort fort = FortManager.getInstance().getFortById(t.getFortId());
			// spawnControlTower(t.getCastleId()); // Spawn control tower
			if (castle != null)
			{
				t.changeNPCsSpawn(2, true);
				castle.spawnDoor(); // Spawn door
				castle.getZone().setSiegeInstance(this);
				castle.getZone().setActive(true);
				castle.getZone().updateZoneStatusForCharactersInside();
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
			}
			if (fort != null)
			{
				t.changeNPCsSpawn(1, true);
				fort.resetDoors(); // Spawn door
				fort.getZone().setSiegeInstance(this);
				fort.getZone().setActive(true);
				fort.getZone().updateZoneStatusForCharactersInside();
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Fort missing! FortId: " + t.getFortId());
			}
			for (TerritoryNPCSpawn ward : t.getOwnedWard())
			{
				if ((ward.getNpc() != null) && (t.getOwnerClan() != null))
				{
					if (!ward.getNpc().isSpawned())
					{
						ward.setNPC(ward.getNpc().getSpawn().doSpawn());
					}
					_territoryWards.add(new TerritoryWard(ward.getId(), ward.getLocation().getX(), ward.getLocation().getY(), ward.getLocation().getZ(), 0, ward.getId() + 13479, t.getCastleId(), ward.getNpc()));
				}
			}
			t.getQuestDone()[0] = 0; // killed npc
			t.getQuestDone()[1] = 0; // captured wards
		}
		_participantPoints.clear();
		
		if (RETURN_WARDS_WHEN_TW_STARTS)
		{
			for (TerritoryWard ward : _territoryWards)
			{
				if (ward.getOwnerCastleId() != (ward.getTerritoryId() - 80))
				{
					ward.unSpawnMe();
					ward.setNpc(addTerritoryWard(ward.getTerritoryId(), ward.getTerritoryId() - 80, ward.getOwnerCastleId(), false));
					ward.setOwnerCastleId(ward.getTerritoryId() - 80);
				}
			}
		}
		
		Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_BEGUN));
	}
	
	protected void endTerritoryWar()
	{
		_isTWInProgress = false;
		if (_territoryList == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": TerritoryList is NULL!");
			return;
		}
		final List<Territory> activeTerritoryList = new LinkedList<>();
		for (Territory t : _territoryList.values())
		{
			final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			if (castle != null)
			{
				if (castle.getOwnerId() > 0)
				{
					activeTerritoryList.add(t);
				}
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
			}
		}
		
		updatePlayerTWStateFlags(true);
		
		for (TerritoryWard twWard : _territoryWards)
		{
			twWard.unSpawnMe();
		}
		_territoryWards.clear();
		
		// teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
		if (activeTerritoryList.size() > 1)
		{
			for (Territory t : activeTerritoryList)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
				final Fort fort = FortManager.getInstance().getFortById(t.getFortId());
				if (castle != null)
				{
					castle.spawnDoor();
					t.changeNPCsSpawn(2, false);
					castle.getZone().setActive(false);
					castle.getZone().updateZoneStatusForCharactersInside();
					castle.getZone().setSiegeInstance(null);
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
				}
				
				if (fort != null)
				{
					t.changeNPCsSpawn(1, false);
					fort.getZone().setActive(false);
					fort.getZone().updateZoneStatusForCharactersInside();
					fort.getZone().setSiegeInstance(null);
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Fort missing! FortId: " + t.getFortId());
				}
				
				if (t.getHQ() != null)
				{
					t.getHQ().deleteMe();
				}
				
				for (TerritoryNPCSpawn ward : t.getOwnedWard())
				{
					if (ward.getNpc() != null)
					{
						if (!ward.getNpc().isSpawned() && SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
						{
							ward.setNPC(ward.getNpc().getSpawn().doSpawn());
						}
						else if (ward.getNpc().isSpawned() && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS)
						{
							ward.getNpc().decayMe();
						}
					}
				}
			}
		}
		
		for (SiegeFlagInstance flag : _clanFlags.values())
		{
			flag.deleteMe();
		}
		_clanFlags.clear();
		
		for (Integer castleId : _registeredClans.keySet())
		{
			for (Clan clan : _registeredClans.get(castleId))
			{
				changeRegistration(castleId, clan.getId(), true);
			}
		}
		
		for (Integer castleId : _registeredMercenaries.keySet())
		{
			for (Integer pl_objId : _registeredMercenaries.get(castleId))
			{
				changeRegistration(castleId, pl_objId, true);
			}
		}
		
		// Change next TW date.
		setNextTWDate();
		Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_ENDED));
	}
	
	public void setNextTWDate()
	{
		final Calendar cal = Calendar.getInstance();
		final long nextSiegeDate = GlobalVariablesManager.getInstance().getLong(GLOBAL_VARIABLE, 0);
		if (nextSiegeDate > Chronos.currentTimeMillis())
		{
			cal.setTimeInMillis(nextSiegeDate);
		}
		else
		{
			// Let's check if territory war date was in the past.
			if (cal.before(Calendar.getInstance()))
			{
				cal.setTimeInMillis(Chronos.currentTimeMillis());
			}
			
			final boolean hasOwnedCastle = CastleManager.getInstance().hasOwnedCastle();
			cal.set(Calendar.DAY_OF_WEEK, hasOwnedCastle ? Calendar.SATURDAY : Calendar.SUNDAY);
			cal.set(Calendar.HOUR_OF_DAY, hasOwnedCastle ? 20 : 22);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			if (cal.before(Calendar.getInstance()))
			{
				cal.add(Calendar.WEEK_OF_YEAR, 2);
			}
			GlobalVariablesManager.getInstance().set(GLOBAL_VARIABLE, cal.getTimeInMillis());
		}
		setTWStartTimeInMillis(cal.getTimeInMillis());
	}
	
	protected void updatePlayerTWStateFlags(boolean clear)
	{
		final Quest twQuest = QuestManager.getInstance().getQuest(qn);
		if (twQuest != null)
		{
			twQuest.setOnEnterWorld(_isTWInProgress);
		}
		
		for (int castleId : _registeredClans.keySet())
		{
			for (Clan clan : _registeredClans.get(castleId))
			{
				for (PlayerInstance player : clan.getOnlineMembers(0))
				{
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
						{
							player.setSiegeSide(0);
						}
					}
					else
					{
						if ((player.getLevel() < PLAYERMINLEVEL) || (player.getClassId().level() < 2))
						{
							continue;
						}
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + castleId);
					}
					player.broadcastUserInfo();
				}
			}
		}
		for (int castleId : _registeredMercenaries.keySet())
		{
			for (int objId : _registeredMercenaries.get(castleId))
			{
				final PlayerInstance player = World.getInstance().getPlayer(objId);
				if (player == null)
				{
					continue;
				}
				if (clear)
				{
					player.setSiegeState((byte) 0);
					if (!_isTWChannelOpen)
					{
						player.setSiegeSide(0);
					}
				}
				else
				{
					if (_isTWInProgress)
					{
						player.setSiegeState((byte) 1);
					}
					player.setSiegeSide(80 + castleId);
				}
				player.broadcastUserInfo();
			}
		}
		for (Territory terr : _territoryList.values())
		{
			if (terr.getOwnerClan() != null)
			{
				for (PlayerInstance player : terr.getOwnerClan().getOnlineMembers(0))
				{
					if (player == null)
					{
						continue;
					}
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
						{
							player.setSiegeSide(0);
						}
					}
					else
					{
						if ((player.getLevel() < PLAYERMINLEVEL) || (player.getClassId().level() < 2))
						{
							continue;
						}
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + terr.getCastleId());
					}
					player.broadcastUserInfo();
				}
			}
		}
	}
	
	protected class RewardOnlineParticipants implements Runnable
	{
		@Override
		public void run()
		{
			if (_isTWInProgress)
			{
				for (PlayerInstance player : World.getInstance().getPlayers())
				{
					if ((player != null) && (player.getSiegeSide() > 0))
					{
						giveTWPoint(player, 1000, 6);
					}
				}
			}
			else
			{
				_scheduledRewardOnlineTask.cancel(false);
			}
		}
	}
	
	protected class ScheduleStartTWTask implements Runnable
	{
		private final Logger LOGGER = Logger.getLogger(ScheduleStartTWTask.class.getName());
		
		@Override
		public void run()
		{
			_scheduledStartTWTask.cancel(false);
			try
			{
				final long timeRemaining = _startTWDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				if (timeRemaining > 7200000)
				{
					_isRegistrationOver = false;
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining - 7200000); // Prepare task for 2h before TW start to end registration
				}
				else if ((timeRemaining <= 7200000) && (timeRemaining > 1200000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_REQUEST_PERIOD_HAS_ENDED);
					Broadcast.toAllOnlinePlayers(sm);
					_isRegistrationOver = true;
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining - 1200000); // Prepare task for 20 mins left before TW start.
				}
				else if ((timeRemaining <= 1200000) && (timeRemaining > 600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_BEGIN_IN_20_MINUTES_TERRITORY_RELATED_FUNCTIONS_I_E_BATTLEFIELD_CHANNEL_DISGUISE_SCROLLS_TRANSFORMATIONS_ETC_CAN_NOW_BE_USED);
					Broadcast.toAllOnlinePlayers(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining - 600000); // Prepare task for 10 mins left before TW start.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_BEGINS_IN_10_MINUTES);
					Broadcast.toAllOnlinePlayers(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining - 300000); // Prepare task for 5 mins left before TW start.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 60000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_BEGINS_IN_5_MINUTES);
					Broadcast.toAllOnlinePlayers(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining - 60000); // Prepare task for 1 min left before TW start.
				}
				else if ((timeRemaining <= 60000) && (timeRemaining > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_BEGINS_IN_1_MINUTE);
					Broadcast.toAllOnlinePlayers(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPool.schedule(new ScheduleStartTWTask(), timeRemaining); // Prepare task for TW start.
				}
				else if ((timeRemaining + WARLENGTH) > 0)
				{
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					startTerritoryWar();
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), 1000); // Prepare task for TW end.
					_scheduledRewardOnlineTask = ThreadPool.scheduleAtFixedRate(new RewardOnlineParticipants(), 60000, 60000);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private class ScheduleEndTWTask implements Runnable
	{
		private final Logger LOGGER = Logger.getLogger(ScheduleEndTWTask.class.getName());
		
		protected ScheduleEndTWTask()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				_scheduledEndTWTask.cancel(false);
				final long timeRemaining = (_startTWDate.getTimeInMillis() + WARLENGTH) - Calendar.getInstance().getTimeInMillis();
				if (timeRemaining > 3600000)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_HOUR_S);
					sm.addInt(2);
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), timeRemaining - 3600000); // Prepare task for 1 hr left.
				}
				else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
					sm.addInt((int) (timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
					sm.addInt((int) (timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
					sm.addInt((int) (timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_TO_THE_END_OF_TERRITORY_WAR);
					sm.addInt((int) (timeRemaining / 1000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPool.schedule(new ScheduleEndTWTask(), timeRemaining); // Prepare task for second count down
				}
				else
				{
					endTerritoryWar();
					// _scheduledStartTWTask = ThreadPoolManager.schedule(new ScheduleStartTWTask(), 1000);
					ThreadPool.schedule(new closeTerritoryChannelTask(), 600000);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private class closeTerritoryChannelTask implements Runnable
	{
		public closeTerritoryChannelTask()
		{
		}
		
		@Override
		public void run()
		{
			_isTWChannelOpen = false;
			_disguisedPlayers.clear();
			updatePlayerTWStateFlags(true);
		}
	}
	
	public void announceToParticipants(IClientOutgoingPacket sm, int exp, int sp)
	{
		// broadcast to clan members
		for (Territory ter : _territoryList.values())
		{
			if (ter.getOwnerClan() != null)
			{
				for (PlayerInstance member : ter.getOwnerClan().getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						member.addExpAndSp(exp, sp);
					}
				}
			}
		}
		for (List<Clan> list : _registeredClans.values())
		{
			for (Clan c : list)
			{
				for (PlayerInstance member : c.getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						member.addExpAndSp(exp, sp);
					}
				}
			}
		}
		// broadcast to mercenaries
		for (List<Integer> list : _registeredMercenaries.values())
		{
			for (int objId : list)
			{
				final PlayerInstance player = World.getInstance().getPlayer(objId);
				if ((player != null) && ((player.getClan() == null) || !checkIsRegistered(-1, player.getClan())))
				{
					player.sendPacket(sm);
					if ((exp > 0) || (sp > 0))
					{
						player.addExpAndSp(exp, sp);
					}
				}
			}
		}
	}
	
	public static class TerritoryNPCSpawn implements IIdentifiable
	{
		private final Location _location;
		protected int _npcId;
		private final int _castleId;
		private final int _type;
		private Npc _npc;
		
		public TerritoryNPCSpawn(int castleId, Location loc, int npcId, int type, Npc npc)
		{
			_castleId = castleId;
			_location = loc;
			_npcId = npcId;
			_type = type;
			_npc = npc;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		/**
		 * Gets the NPC ID.
		 * @return the NPC ID
		 */
		@Override
		public int getId()
		{
			return _npcId;
		}
		
		public int getType()
		{
			return _type;
		}
		
		public void setNPC(Npc npc)
		{
			if (_npc != null)
			{
				_npc.deleteMe();
			}
			_npc = npc;
		}
		
		public Npc getNpc()
		{
			return _npc;
		}
		
		public Location getLocation()
		{
			return _location;
		}
	}
	
	public class Territory
	{
		private final Logger LOGGER = Logger.getLogger(Territory.class.getName());
		
		private final int _territoryId;
		private final int _castleId; // territory Castle
		protected int _fortId; // territory Fortress
		private Clan _ownerClan;
		private final List<TerritoryNPCSpawn> _spawnList = new LinkedList<>();
		private final TerritoryNPCSpawn[] _territoryWardSpawnPlaces;
		private boolean _isInProgress = false;
		private SiegeFlagInstance _territoryHQ = null;
		private final int[] _questDone;
		
		public Territory(int castleId)
		{
			_castleId = castleId;
			_territoryId = castleId + 80;
			_territoryWardSpawnPlaces = new TerritoryNPCSpawn[9];
			_questDone = new int[2];
		}
		
		protected void addWardSpawnPlace(Location loc)
		{
			for (int i = 0; i < _territoryWardSpawnPlaces.length; i++)
			{
				if (_territoryWardSpawnPlaces[i] == null)
				{
					_territoryWardSpawnPlaces[i] = new TerritoryNPCSpawn(_castleId, loc, 0, 4, null);
					return;
				}
			}
		}
		
		protected TerritoryNPCSpawn getFreeWardSpawnPlace()
		{
			for (TerritoryNPCSpawn _territoryWardSpawnPlace : _territoryWardSpawnPlaces)
			{
				if ((_territoryWardSpawnPlace != null) && (_territoryWardSpawnPlace.getNpc() == null))
				{
					return _territoryWardSpawnPlace;
				}
			}
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": no free Ward spawn found for territory: " + _territoryId);
			for (int i = 0; i < _territoryWardSpawnPlaces.length; i++)
			{
				if (_territoryWardSpawnPlaces[i] == null)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": territory ward spawn place " + i + " is null!");
				}
				else if (_territoryWardSpawnPlaces[i].getNpc() != null)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": territory ward spawn place " + i + " has npc name: " + _territoryWardSpawnPlaces[i].getNpc().getName());
				}
				else
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": territory ward spawn place " + i + " is empty!");
				}
			}
			return null;
		}
		
		public List<TerritoryNPCSpawn> getSpawnList()
		{
			return _spawnList;
		}
		
		protected void changeNPCsSpawn(int type, boolean isSpawn)
		{
			if ((type < 0) || (type > 3))
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": wrong type(" + type + ") for NPCs spawn change!");
				return;
			}
			for (TerritoryNPCSpawn twSpawn : _spawnList)
			{
				if (twSpawn.getType() != type)
				{
					continue;
				}
				if (isSpawn)
				{
					twSpawn.setNPC(spawnNPC(twSpawn.getId(), twSpawn.getLocation()));
				}
				else
				{
					final Npc npc = twSpawn.getNpc();
					if ((npc != null) && !npc.isDead())
					{
						npc.deleteMe();
					}
					twSpawn.setNPC(null);
				}
			}
		}
		
		protected void removeWard(int wardId)
		{
			for (TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
			{
				if (wardSpawn.getId() == wardId)
				{
					wardSpawn.getNpc().deleteMe();
					wardSpawn.setNPC(null);
					wardSpawn._npcId = 0;
					return;
				}
			}
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Can't delete wardId: " + wardId + " for territory: " + _territoryId);
		}
		
		public int getTerritoryId()
		{
			return _territoryId;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		public int getFortId()
		{
			return _fortId;
		}
		
		public Clan getOwnerClan()
		{
			return _ownerClan;
		}
		
		public void setOwnerClan(Clan newOwner)
		{
			_ownerClan = newOwner;
		}
		
		public void setHQ(SiegeFlagInstance hq)
		{
			_territoryHQ = hq;
		}
		
		public SiegeFlagInstance getHQ()
		{
			return _territoryHQ;
		}
		
		public TerritoryNPCSpawn[] getOwnedWard()
		{
			return _territoryWardSpawnPlaces;
		}
		
		public int[] getQuestDone()
		{
			return _questDone;
		}
		
		public List<Integer> getOwnedWardIds()
		{
			final List<Integer> ret = new LinkedList<>();
			for (TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
			{
				if (wardSpawn.getId() > 0)
				{
					ret.add(wardSpawn.getId());
				}
			}
			return ret;
		}
		
		public boolean isInProgress()
		{
			return _isInProgress;
		}
		
		public void setInProgress(boolean value)
		{
			_isInProgress = value;
		}
	}
	
	@Override
	public void startSiege()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void endSiege()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<SiegeClan> getAttackerClans()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<PlayerInstance> getAttackersInZone()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	@Override
	public int getFameFrequency()
	{
		return Config.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}
	
	@Override
	public int getFameAmount()
	{
		return Config.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}
	
	@Override
	public void updateSiege()
	{
	}
	
	public static TerritoryWarManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TerritoryWarManager INSTANCE = new TerritoryWarManager();
	}
}
