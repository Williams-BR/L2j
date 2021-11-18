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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class World
{
	private static final Logger LOGGER = Logger.getLogger(World.class.getName());
	
	public static volatile int MAX_CONNECTED_COUNT = 0;
	public static volatile int OFFLINE_TRADE_COUNT = 0;
	
	/** Gracia border Flying objects not allowed to the east of it. */
	public static final int GRACIA_MAX_X = -166168;
	public static final int GRACIA_MAX_Z = 6105;
	public static final int GRACIA_MIN_Z = -895;
	
	/** Bit shift, defines number of regions note, shifting by 15 will result in regions corresponding to map tiles shifting by 11 divides one tile to 16x16 regions. */
	public static final int SHIFT_BY = 11;
	
	public static final int TILE_SIZE = 32768;
	
	/** Map dimensions. */
	public static final int TILE_X_MIN = 11;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_X_MAX = 26;
	public static final int TILE_Y_MAX = 26;
	public static final int TILE_ZERO_COORD_X = 20;
	public static final int TILE_ZERO_COORD_Y = 18;
	public static final int WORLD_X_MIN = (TILE_X_MIN - TILE_ZERO_COORD_X) * TILE_SIZE;
	public static final int WORLD_Y_MIN = (TILE_Y_MIN - TILE_ZERO_COORD_Y) * TILE_SIZE;
	
	public static final int WORLD_X_MAX = ((TILE_X_MAX - TILE_ZERO_COORD_X) + 1) * TILE_SIZE;
	public static final int WORLD_Y_MAX = ((TILE_Y_MAX - TILE_ZERO_COORD_Y) + 1) * TILE_SIZE;
	
	/** Calculated offset used so top left region is 0,0 */
	public static final int OFFSET_X = Math.abs(WORLD_X_MIN >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(WORLD_Y_MIN >> SHIFT_BY);
	
	/** Number of regions. */
	private static final int REGIONS_X = (WORLD_X_MAX >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y;
	
	/** Map containing all the players in game. */
	private static final Map<Integer, PlayerInstance> _allPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Good players in game. */
	private static final Map<Integer, PlayerInstance> _allGoodPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Evil players in game. */
	private static final Map<Integer, PlayerInstance> _allEvilPlayers = new ConcurrentHashMap<>();
	/** Map containing all visible objects. */
	private static final Map<Integer, WorldObject> _allObjects = new ConcurrentHashMap<>();
	/** Map with the pets instances and their owner ID. */
	private static final Map<Integer, PetInstance> _petsInstance = new ConcurrentHashMap<>();
	
	private static final WorldRegion[][] _worldRegions = new WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];
	
	/** Constructor of World. */
	protected World()
	{
		// Initialize regions.
		for (int x = 0; x <= REGIONS_X; x++)
		{
			for (int y = 0; y <= REGIONS_Y; y++)
			{
				_worldRegions[x][y] = new WorldRegion(x, y);
			}
		}
		
		// Set surrounding regions.
		for (int rx = 0; rx <= REGIONS_X; rx++)
		{
			for (int ry = 0; ry <= REGIONS_Y; ry++)
			{
				final List<WorldRegion> surroundingRegions = new ArrayList<>();
				for (int sx = rx - 1; sx <= (rx + 1); sx++)
				{
					for (int sy = ry - 1; sy <= (ry + 1); sy++)
					{
						if (((sx >= 0) && (sx < REGIONS_X) && (sy >= 0) && (sy < REGIONS_Y)))
						{
							surroundingRegions.add(_worldRegions[sx][sy]);
						}
					}
				}
				WorldRegion[] regionArray = new WorldRegion[surroundingRegions.size()];
				regionArray = surroundingRegions.toArray(regionArray);
				_worldRegions[rx][ry].setSurroundingRegions(regionArray);
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
	}
	
	/**
	 * Adds an object to the world.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Withdraw an item from the warehouse, create an item</li>
	 * <li>Spawn a Creature (PC, NPC, Pet)</li>
	 * </ul>
	 * @param object
	 */
	public void addObject(WorldObject object)
	{
		if (_allObjects.putIfAbsent(object.getObjectId(), object) != null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Object " + object + " already exists in the world. Stack Trace: " + CommonUtil.getTraceString(Thread.currentThread().getStackTrace()));
		}
		
		if (object.isPlayer())
		{
			final PlayerInstance newPlayer = (PlayerInstance) object;
			if (newPlayer.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			
			final PlayerInstance existingPlayer = _allPlayers.putIfAbsent(object.getObjectId(), newPlayer);
			if (existingPlayer != null)
			{
				Disconnection.of(existingPlayer).defaultSequence(LeaveWorld.STATIC_PACKET);
				Disconnection.of(newPlayer).defaultSequence(LeaveWorld.STATIC_PACKET);
				LOGGER.warning(getClass().getSimpleName() + ": Duplicate character!? Disconnected both characters (" + newPlayer.getName() + ")");
			}
			else if (Config.FACTION_SYSTEM_ENABLED)
			{
				addFactionPlayerToWorld(newPlayer);
			}
		}
	}
	
	/**
	 * Removes an object from the world.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Delete item from inventory, transfer Item from inventory to warehouse</li>
	 * <li>Crystallize item</li>
	 * <li>Remove NPC/PC/Pet from the world</li>
	 * </ul>
	 * @param object the object to remove
	 */
	public void removeObject(WorldObject object)
	{
		_allObjects.remove(object.getObjectId());
		if (object.isPlayer())
		{
			final PlayerInstance player = (PlayerInstance) object;
			if (player.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			_allPlayers.remove(object.getObjectId());
			
			if (Config.FACTION_SYSTEM_ENABLED)
			{
				if (player.isGood())
				{
					_allGoodPlayers.remove(player.getObjectId());
				}
				else if (player.isEvil())
				{
					_allEvilPlayers.remove(player.getObjectId());
				}
			}
		}
	}
	
	/**
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packets : Action, AttackRequest, RequestJoinParty, RequestJoinPledge...</li>
	 * </ul>
	 * @param objectId Identifier of the WorldObject
	 * @return the WorldObject object that belongs to an ID or null if no object found.
	 */
	public WorldObject findObject(int objectId)
	{
		return _allObjects.get(objectId);
	}
	
	public Collection<WorldObject> getVisibleObjects()
	{
		return _allObjects.values();
	}
	
	/**
	 * Get the count of all visible objects in world.
	 * @return count off all World objects
	 */
	public int getVisibleObjectsCount()
	{
		return _allObjects.size();
	}
	
	public Collection<PlayerInstance> getPlayers()
	{
		return _allPlayers.values();
	}
	
	public Collection<PlayerInstance> getAllGoodPlayers()
	{
		return _allGoodPlayers.values();
	}
	
	public Collection<PlayerInstance> getAllEvilPlayers()
	{
		return _allEvilPlayers.values();
	}
	
	/**
	 * <b>If you have access to player objectId use {@link #getPlayer(int playerObjId)}</b>
	 * @param name Name of the player to get Instance
	 * @return the player instance corresponding to the given name.
	 */
	public PlayerInstance getPlayer(String name)
	{
		return getPlayer(CharNameTable.getInstance().getIdByName(name));
	}
	
	/**
	 * @param objectId of the player to get Instance
	 * @return the player instance corresponding to the given object ID.
	 */
	public PlayerInstance getPlayer(int objectId)
	{
		return _allPlayers.get(objectId);
	}
	
	/**
	 * @param ownerId ID of the owner
	 * @return the pet instance from the given ownerId.
	 */
	public PetInstance getPet(int ownerId)
	{
		return _petsInstance.get(ownerId);
	}
	
	/**
	 * Add the given pet instance from the given ownerId.
	 * @param ownerId ID of the owner
	 * @param pet PetInstance of the pet
	 * @return
	 */
	public PetInstance addPet(int ownerId, PetInstance pet)
	{
		return _petsInstance.put(ownerId, pet);
	}
	
	/**
	 * Remove the given pet instance.
	 * @param ownerId ID of the owner
	 */
	public void removePet(int ownerId)
	{
		_petsInstance.remove(ownerId);
	}
	
	/**
	 * Add a WorldObject in the world. <b><u>Concept</u>:</b> WorldObject (including PlayerInstance) are identified in <b>_visibleObjects</b> of his current WorldRegion and in <b>_knownObjects</b> of other surrounding Creatures<br>
	 * PlayerInstance are identified in <b>_allPlayers</b> of World, in <b>_allPlayers</b> of his current WorldRegion and in <b>_knownPlayer</b> of other surrounding Creatures <b><u> Actions</u>:</b>
	 * <li>Add the WorldObject object in _allPlayers* of World</li>
	 * <li>Add the WorldObject object in _gmList** of GmListTable</li>
	 * <li>Add object in _knownObjects and _knownPlayer* of all surrounding WorldRegion Creatures</li>
	 * <li>If object is a Creature, add all surrounding WorldObject in its _knownObjects and all surrounding PlayerInstance in its _knownPlayer</li><br>
	 * <i>* only if object is a PlayerInstance</i><br>
	 * <i>** only if object is a GM PlayerInstance</i> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T ADD the object in _visibleObjects and _allPlayers* of WorldRegion (need synchronisation)</b></font><br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T ADD the object to _allObjects and _allPlayers* of World (need synchronisation)</b></font> <b><u> Example of use</u>:</b>
	 * <li>Drop an Item</li>
	 * <li>Spawn a Creature</li>
	 * <li>Apply Death Penalty of a PlayerInstance</li><br>
	 * @param object L2object to add in the world
	 * @param newRegion WorldRegion in wich the object will be add (not used)
	 */
	public void addVisibleObject(WorldObject object, WorldRegion newRegion)
	{
		if (!newRegion.isActive())
		{
			return;
		}
		
		forEachVisibleObject(object, WorldObject.class, wo ->
		{
			if (object.isPlayer() && wo.isVisibleFor((PlayerInstance) object))
			{
				wo.sendInfo((PlayerInstance) object);
				if (wo.isCreature())
				{
					final CreatureAI ai = ((Creature) wo).getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer((PlayerInstance) object);
						if (wo.isMonster() && (ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE))
						{
							ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						}
					}
				}
			}
			
			if (wo.isPlayer() && object.isVisibleFor((PlayerInstance) wo))
			{
				object.sendInfo((PlayerInstance) wo);
				if (object.isCreature())
				{
					final CreatureAI ai = ((Creature) object).getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer((PlayerInstance) wo);
						if (object.isMonster() && (ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE))
						{
							ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						}
					}
				}
			}
		});
	}
	
	public static void addFactionPlayerToWorld(PlayerInstance player)
	{
		if (player.isGood())
		{
			_allGoodPlayers.put(player.getObjectId(), player);
		}
		else if (player.isEvil())
		{
			_allEvilPlayers.put(player.getObjectId(), player);
		}
	}
	
	/**
	 * Remove a WorldObject from the world. <b><u>Concept</u>:</b> WorldObject (including PlayerInstance) are identified in <b>_visibleObjects</b> of his current WorldRegion and in <b>_knownObjects</b> of other surrounding Creatures<br>
	 * PlayerInstance are identified in <b>_allPlayers</b> of World, in <b>_allPlayers</b> of his current WorldRegion and in <b>_knownPlayer</b> of other surrounding Creatures <b><u> Actions</u>:</b>
	 * <li>Remove the WorldObject object from _allPlayers* of World</li>
	 * <li>Remove the WorldObject object from _visibleObjects and _allPlayers* of WorldRegion</li>
	 * <li>Remove the WorldObject object from _gmList** of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer* of all surrounding WorldRegion Creatures</li>
	 * <li>If object is a Creature, remove all WorldObject from its _knownObjects and all PlayerInstance from its _knownPlayer</li> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T REMOVE the object from _allObjects of World</b></font> <i>* only if object is a PlayerInstance</i><br>
	 * <i>** only if object is a GM PlayerInstance</i> <b><u> Example of use</u>:</b>
	 * <li>Pickup an Item</li>
	 * <li>Decay a Creature</li><br>
	 * @param object L2object to remove from the world
	 * @param oldRegion WorldRegion in which the object was before removing
	 */
	public void removeVisibleObject(WorldObject object, WorldRegion oldRegion)
	{
		if ((object == null) || (oldRegion == null))
		{
			return;
		}
		
		oldRegion.removeVisibleObject(object);
		
		// Go through all surrounding WorldRegion Creatures
		final WorldRegion[] surroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final List<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			for (int j = 0; j < visibleObjects.size(); j++)
			{
				final WorldObject wo = visibleObjects.get(j);
				if ((wo == null) || (wo == object))
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = (Creature) object;
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = (Creature) wo;
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
	}
	
	public void switchRegion(WorldObject object, WorldRegion newRegion)
	{
		final WorldRegion oldRegion = object.getWorldRegion();
		if ((oldRegion == null) || (oldRegion == newRegion))
		{
			return;
		}
		
		final WorldRegion[] oldSurroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < oldSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = oldSurroundingRegions[i];
			if (newRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final List<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			for (int j = 0; j < visibleObjects.size(); j++)
			{
				final WorldObject wo = visibleObjects.get(j);
				if ((wo == null) || (wo == object))
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = (Creature) object;
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = (Creature) wo;
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
		
		final WorldRegion[] newSurroundingRegions = newRegion.getSurroundingRegions();
		for (int i = 0; i < newSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = newSurroundingRegions[i];
			if (oldRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final List<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			for (int j = 0; j < visibleObjects.size(); j++)
			{
				final WorldObject wo = visibleObjects.get(j);
				if ((wo == null) || (wo == object) || (wo.getInstanceId() != object.getInstanceId()))
				{
					continue;
				}
				
				if (object.isPlayer() && wo.isVisibleFor((PlayerInstance) object))
				{
					wo.sendInfo((PlayerInstance) object);
					if (wo.isCreature())
					{
						final CreatureAI ai = ((Creature) wo).getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer((PlayerInstance) object);
							if (wo.isMonster() && (ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE))
							{
								ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
					}
				}
				
				if (wo.isPlayer() && object.isVisibleFor((PlayerInstance) wo))
				{
					object.sendInfo((PlayerInstance) wo);
					if (object.isCreature())
					{
						final CreatureAI ai = ((Creature) object).getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer((PlayerInstance) wo);
							if (object.isMonster() && (ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE))
							{
								ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
					}
				}
			}
		}
	}
	
	public <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz)
	{
		final List<T> result = new ArrayList<>();
		forEachVisibleObject(object, clazz, result::add);
		return result;
	}
	
	public <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		final List<T> result = new ArrayList<>();
		forEachVisibleObject(object, clazz, o ->
		{
			if (predicate.test(o))
			{
				result.add(o);
			}
		});
		return result;
	}
	
	public <T extends WorldObject> void forEachVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final List<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			for (int j = 0; j < visibleObjects.size(); j++)
			{
				final WorldObject wo = visibleObjects.get(j);
				if ((wo == null) || (wo == object) || !clazz.isInstance(wo))
				{
					continue;
				}
				
				if (wo.getInstanceId() != object.getInstanceId())
				{
					continue;
				}
				
				c.accept(clazz.cast(wo));
			}
		}
	}
	
	public <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range)
	{
		final List<T> result = new ArrayList<>();
		forEachVisibleObjectInRange(object, clazz, range, result::add);
		return result;
	}
	
	public <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		final List<T> result = new ArrayList<>();
		forEachVisibleObjectInRange(object, clazz, range, o ->
		{
			if (predicate.test(o))
			{
				result.add(o);
			}
		});
		return result;
	}
	
	public <T extends WorldObject> void forEachVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final List<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			for (int j = 0; j < visibleObjects.size(); j++)
			{
				final WorldObject wo = visibleObjects.get(j);
				if ((wo == null) || (wo == object) || !clazz.isInstance(wo))
				{
					continue;
				}
				
				if (wo.getInstanceId() != object.getInstanceId())
				{
					continue;
				}
				
				if (wo.calculateDistance3D(object) <= range)
				{
					c.accept(clazz.cast(wo));
				}
			}
		}
	}
	
	/**
	 * Calculate the current WorldRegions of the object according to its position (x,y). <b><u>Example of use</u>:</b>
	 * <li>Set position of a new WorldObject (drop, spawn...)</li>
	 * <li>Update position of a WorldObject after a movement</li><br>
	 * @param object the object
	 * @return
	 */
	public WorldRegion getRegion(WorldObject object)
	{
		try
		{
			return _worldRegions[(object.getX() >> SHIFT_BY) + OFFSET_X][(object.getY() >> SHIFT_BY) + OFFSET_Y];
		}
		catch (ArrayIndexOutOfBoundsException e) // Precaution. Moved at invalid region?
		{
			disposeOutOfBoundsObject(object);
			return null;
		}
	}
	
	public WorldRegion getRegion(int x, int y)
	{
		try
		{
			return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Incorrect world region X: " + ((x >> SHIFT_BY) + OFFSET_X) + " Y: " + ((y >> SHIFT_BY) + OFFSET_Y));
			return null;
		}
	}
	
	/**
	 * Returns the whole 3d array containing the world regions used by ZoneData.java to setup zones inside the world regions
	 * @return
	 */
	public WorldRegion[][] getWorldRegions()
	{
		return _worldRegions;
	}
	
	public synchronized void disposeOutOfBoundsObject(WorldObject object)
	{
		if (object.isPlayer())
		{
			((Creature) object).stopMove(((PlayerInstance) object).getLastServerPosition());
		}
		else if (object.isSummon())
		{
			final Summon summon = (Summon) object;
			summon.unSummon(summon.getOwner());
		}
		else if (_allObjects.remove(object.getObjectId()) != null)
		{
			if (object.isNpc())
			{
				final Npc npc = (Npc) object;
				LOGGER.warning("Deleting npc " + object.getName() + " NPCID[" + npc.getId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				npc.deleteMe();
				
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					LOGGER.warning("Spawn location X:" + spawn.getX() + " Y:" + spawn.getY() + " Z:" + spawn.getZ() + " Heading:" + spawn.getHeading());
				}
			}
			else if (object.isCreature())
			{
				LOGGER.warning("Deleting object " + object.getName() + " OID[" + object.getObjectId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				((Creature) object).deleteMe();
			}
			
			if (object.getWorldRegion() != null)
			{
				object.getWorldRegion().removeVisibleObject(object);
			}
		}
	}
	
	public static World getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final World INSTANCE = new World();
	}
}
