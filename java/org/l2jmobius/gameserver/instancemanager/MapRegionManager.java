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

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.MapRegion;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.model.zone.type.RespawnZone;

/**
 * Map Region Manager.
 * @author Nyaran
 */
public class MapRegionManager implements IXmlReader
{
	private static final Map<String, MapRegion> REGIONS = new ConcurrentHashMap<>();
	private static final String DEFAULT_RESPAWN = "talking_island_town";
	
	protected MapRegionManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		REGIONS.clear();
		parseDatapackDirectory("data/mapregion", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + REGIONS.size() + " map regions.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		NamedNodeMap attrs;
		String name;
		String town;
		int locId;
		int castle;
		int bbs;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("region".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						name = attrs.getNamedItem("name").getNodeValue();
						town = attrs.getNamedItem("town").getNodeValue();
						locId = parseInteger(attrs, "locId");
						castle = parseInteger(attrs, "castle");
						bbs = parseInteger(attrs, "bbs");
						
						final MapRegion region = new MapRegion(name, town, locId, castle, bbs);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
							{
								final int spawnX = parseInteger(attrs, "X");
								final int spawnY = parseInteger(attrs, "Y");
								final int spawnZ = parseInteger(attrs, "Z");
								if (parseBoolean(attrs, "isOther", false))
								{
									region.addOtherSpawn(spawnX, spawnY, spawnZ);
								}
								else if (parseBoolean(attrs, "isChaotic", false))
								{
									region.addChaoticSpawn(spawnX, spawnY, spawnZ);
								}
								else if (parseBoolean(attrs, "isBanish", false))
								{
									region.addBanishSpawn(spawnX, spawnY, spawnZ);
								}
								else
								{
									region.addSpawn(spawnX, spawnY, spawnZ);
								}
							}
							else if ("map".equalsIgnoreCase(c.getNodeName()))
							{
								region.addMap(parseInteger(attrs, "X"), parseInteger(attrs, "Y"));
							}
							else if ("banned".equalsIgnoreCase(c.getNodeName()))
							{
								region.addBannedRace(attrs.getNamedItem("race").getNodeValue(), attrs.getNamedItem("point").getNodeValue());
							}
						}
						REGIONS.put(name, region);
					}
				}
			}
		}
	}
	
	/**
	 * @param locX
	 * @param locY
	 * @return
	 */
	public MapRegion getMapRegion(int locX, int locY)
	{
		for (MapRegion region : REGIONS.values())
		{
			if (region.isZoneInRegion(getMapRegionX(locX), getMapRegionY(locY)))
			{
				return region;
			}
		}
		return null;
	}
	
	/**
	 * @param locX
	 * @param locY
	 * @return
	 */
	public int getMapRegionLocId(int locX, int locY)
	{
		final MapRegion region = getMapRegion(locX, locY);
		if (region != null)
		{
			return region.getLocId();
		}
		return 0;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	public MapRegion getMapRegion(WorldObject obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	/**
	 * @param obj
	 * @return
	 */
	public int getMapRegionLocId(WorldObject obj)
	{
		return getMapRegionLocId(obj.getX(), obj.getY());
	}
	
	/**
	 * @param posX
	 * @return
	 */
	public int getMapRegionX(int posX)
	{
		return (posX >> 15) + 9 + 11; // + centerTileX;
	}
	
	/**
	 * @param posY
	 * @return
	 */
	public int getMapRegionY(int posY)
	{
		return (posY >> 15) + 10 + 8; // + centerTileX;
	}
	
	/**
	 * Get town name by character position
	 * @param creature
	 * @return
	 */
	public String getClosestTownName(Creature creature)
	{
		final MapRegion region = getMapRegion(creature);
		return region == null ? "Aden Castle Town" : region.getTown();
	}
	
	/**
	 * @param creature
	 * @return
	 */
	public int getAreaCastle(Creature creature)
	{
		final MapRegion region = getMapRegion(creature);
		return region == null ? 0 : region.getCastle();
	}
	
	/**
	 * @param creature
	 * @param teleportWhere
	 * @return
	 */
	public Location getTeleToLocation(Creature creature, TeleportWhereType teleportWhere)
	{
		Location loc;
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;
			if ((player.getClan() != null) && !player.isFlyingMounted() && !player.isFlying()) // flying players in gracia cant use teleports to aden continent
			{
				// If teleport to clan hall
				if (teleportWhere == TeleportWhereType.CLANHALL)
				{
					clanhall = ClanHallManager.getInstance().getAbstractHallByOwner(player.getClan());
					if (clanhall != null)
					{
						final ClanHallZone zone = clanhall.getZone();
						if ((zone != null) && !player.isFlyingMounted())
						{
							if (player.getKarma() > 0)
							{
								return zone.getChaoticSpawnLoc();
							}
							return zone.getSpawnLoc();
						}
					}
				}
				
				// If teleport to castle
				if (teleportWhere == TeleportWhereType.CASTLE)
				{
					castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (castle == null)
					{
						castle = CastleManager.getInstance().getCastle(player);
						if (!((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().getDefenderClan(player.getClan()) != null)))
						{
							castle = null;
						}
					}
					
					if ((castle != null) && (castle.getResidenceId() > 0))
					{
						if (player.getKarma() > 0)
						{
							return castle.getResidenceZone().getChaoticSpawnLoc();
						}
						return castle.getResidenceZone().getSpawnLoc();
					}
				}
				
				// If teleport to fortress
				if (teleportWhere == TeleportWhereType.FORTRESS)
				{
					fort = FortManager.getInstance().getFortByOwner(player.getClan());
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (fort == null)
					{
						fort = FortManager.getInstance().getFort(player);
						if (!((fort != null) && fort.getSiege().isInProgress() && (fort.getOwnerClan() == player.getClan())))
						{
							fort = null;
						}
					}
					
					if ((fort != null) && (fort.getResidenceId() > 0))
					{
						if (player.getKarma() > 0)
						{
							return fort.getResidenceZone().getChaoticSpawnLoc();
						}
						return fort.getResidenceZone().getSpawnLoc();
					}
				}
				
				// If teleport to SiegeHQ
				if (teleportWhere == TeleportWhereType.SIEGEFLAG)
				{
					castle = CastleManager.getInstance().getCastle(player);
					fort = FortManager.getInstance().getFort(player);
					clanhall = ClanHallManager.getInstance().getNearbyAbstractHall(creature.getX(), creature.getY(), 10000);
					final SiegeFlagInstance twFlag = TerritoryWarManager.getInstance().getHQForClan(player.getClan());
					if (twFlag != null)
					{
						return twFlag.getLocation();
					}
					else if (castle != null)
					{
						if (castle.getSiege().isInProgress())
						{
							// Check if player's clan is attacker
							final Collection<Npc> flags = castle.getSiege().getFlag(player.getClan());
							if ((flags != null) && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								return flags.stream().findFirst().get().getLocation();
							}
						}
					}
					else if (fort != null)
					{
						if (fort.getSiege().isInProgress())
						{
							// Check if player's clan is attacker
							final Set<Npc> flags = fort.getSiege().getFlag(player.getClan());
							if ((flags != null) && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								return flags.stream().findFirst().get().getLocation();
							}
						}
					}
					else if ((clanhall != null) && clanhall.isSiegableHall())
					{
						final SiegableHall sHall = (SiegableHall) clanhall;
						final Collection<Npc> flags = sHall.getSiege().getFlag(player.getClan());
						if ((flags != null) && !flags.isEmpty())
						{
							return flags.stream().findFirst().get().getLocation();
						}
					}
				}
			}
			
			// Karma player land out of city
			if (player.getKarma() > 0)
			{
				try
				{
					final RespawnZone zone = ZoneManager.getInstance().getZone(player, RespawnZone.class);
					if (zone != null)
					{
						return getRestartRegion(creature, zone.getRespawnPoint((PlayerInstance) creature)).getChaoticSpawnLoc();
					}
					// Opposing race check.
					if (getMapRegion(creature).getBannedRace().containsKey(creature.getRace()))
					{
						return REGIONS.get(getMapRegion(creature).getBannedRace().get(creature.getRace())).getChaoticSpawnLoc();
					}
					return getMapRegion(creature).getChaoticSpawnLoc();
				}
				catch (Exception e)
				{
					if (player.isFlyingMounted())
					{
						return REGIONS.get("union_base_of_kserth").getChaoticSpawnLoc();
					}
					return REGIONS.get(DEFAULT_RESPAWN).getChaoticSpawnLoc();
				}
			}
			
			// Checking if needed to be respawned in "far" town from the castle; and if player's clan is participating
			castle = CastleManager.getInstance().getCastle(player);
			if ((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().checkIsDefender(player.getClan()) || castle.getSiege().checkIsAttacker(player.getClan())) && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN))
			{
				return castle.getResidenceZone().getOtherSpawnLoc();
			}
			
			// Checking if in an instance
			if (player.getInstanceId() > 0)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(player.getInstanceId());
				if (inst != null)
				{
					loc = inst.getExitLoc();
					if (loc != null)
					{
						return loc;
					}
				}
			}
		}
		
		if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_RESPAWN_AT_BASE)
		{
			if (creature.getActingPlayer().isGood())
			{
				return Config.FACTION_GOOD_BASE_LOCATION;
			}
			if (creature.getActingPlayer().isEvil())
			{
				return Config.FACTION_EVIL_BASE_LOCATION;
			}
		}
		
		// Get the nearest town
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(creature, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegion(creature, zone.getRespawnPoint((PlayerInstance) creature)).getSpawnLoc();
			}
			// Opposing race check.
			if (getMapRegion(creature).getBannedRace().containsKey(creature.getRace()))
			{
				return REGIONS.get(getMapRegion(creature).getBannedRace().get(creature.getRace())).getChaoticSpawnLoc();
			}
			return getMapRegion(creature).getSpawnLoc();
		}
		catch (Exception e)
		{
			// Port to the default respawn if no closest town found.
			return REGIONS.get(DEFAULT_RESPAWN).getSpawnLoc();
		}
	}
	
	/**
	 * @param creature
	 * @param point
	 * @return
	 */
	public MapRegion getRestartRegion(Creature creature, String point)
	{
		try
		{
			final PlayerInstance player = (PlayerInstance) creature;
			final MapRegion region = REGIONS.get(point);
			if (region.getBannedRace().containsKey(player.getRace()))
			{
				getRestartRegion(player, region.getBannedRace().get(player.getRace()));
			}
			return region;
		}
		catch (Exception e)
		{
			return REGIONS.get(DEFAULT_RESPAWN);
		}
	}
	
	/**
	 * @param regionName the map region name.
	 * @return if exists the map region identified by that name, null otherwise.
	 */
	public MapRegion getMapRegionByName(String regionName)
	{
		return REGIONS.get(regionName);
	}
	
	/**
	 * Gets the single instance of {@code MapRegionManager}.
	 * @return single instance of {@code MapRegionManager}
	 */
	public static MapRegionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MapRegionManager INSTANCE = new MapRegionManager();
	}
}
