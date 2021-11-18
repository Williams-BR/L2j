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
package org.l2jmobius.gameserver.geoengine;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.enums.GeoType;
import org.l2jmobius.gameserver.enums.MoveDirectionType;
import org.l2jmobius.gameserver.geoengine.geodata.ABlock;
import org.l2jmobius.gameserver.geoengine.geodata.BlockComplex;
import org.l2jmobius.gameserver.geoengine.geodata.BlockFlat;
import org.l2jmobius.gameserver.geoengine.geodata.BlockMultilayer;
import org.l2jmobius.gameserver.geoengine.geodata.BlockNull;
import org.l2jmobius.gameserver.geoengine.geodata.GeoStructure;
import org.l2jmobius.gameserver.geoengine.pathfinding.NodeBuffer;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;

public class GeoEngine
{
	protected static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
	
	private final ABlock[][] _blocks;
	private final BlockNull _nullBlock;
	
	// Pre-allocated buffers.
	private final BufferHolder[] _buffers;
	
	public GeoEngine()
	{
		LOGGER.info("GeoEngine: Initializing...");
		
		// Initialize block container.
		_blocks = new ABlock[GeoStructure.GEO_BLOCKS_X][GeoStructure.GEO_BLOCKS_Y];
		
		// Load null block.
		_nullBlock = new BlockNull();
		
		// Initialize multilayer temporarily buffer.
		BlockMultilayer.initialize();
		
		// Load geo files according to geoengine config setup.
		int loaded = 0;
		try
		{
			for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++)
			{
				for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++)
				{
					final Path geoFilePath = Config.GEODATA_PATH.resolve(String.format(Config.GEODATA_TYPE.getFilename(), regionX, regionY));
					if (Files.exists(geoFilePath))
					{
						// Region file is load-able, try to load it.
						if (loadGeoBlocks(regionX, regionY))
						{
							loaded++;
						}
					}
					else
					{
						// Region file is not load-able, load null blocks.
						loadNullBlocks(regionX, regionY);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("GeoEngine: Failed to load geodata! " + e);
			System.exit(1);
		}
		LOGGER.info("GeoEngine: Loaded " + loaded + " geodata files.");
		
		// Release multilayer block temporarily buffer.
		BlockMultilayer.release();
		
		String[] array = Config.PATHFIND_BUFFERS.split(";");
		_buffers = new BufferHolder[array.length];
		
		int count = 0;
		for (int i = 0; i < array.length; i++)
		{
			String buf = array[i];
			String[] args = buf.split("x");
			
			try
			{
				int size = Integer.parseInt(args[1]);
				count += size;
				_buffers[i] = new BufferHolder(Integer.parseInt(args[0]), size);
			}
			catch (Exception e)
			{
				LOGGER.warning("Could not load buffer setting:" + buf + ". " + e);
			}
		}
		LOGGER.info("Loaded " + count + " node buffers.");
		
		// Avoid wrong configs when no files are loaded.
		if ((loaded == 0) && Config.PATHFINDING)
		{
			Config.PATHFINDING = false;
			LOGGER.info("GeoEngine: Forcing PathFinding setting to false.");
		}
	}
	
	/**
	 * Provides optimize selection of the buffer. When all pre-initialized buffer are locked, creates new buffer and log this situation.
	 * @param size : pre-calculated minimal required size
	 * @return NodeBuffer : buffer
	 */
	private NodeBuffer getBuffer(int size)
	{
		NodeBuffer current = null;
		for (BufferHolder holder : _buffers)
		{
			// Find proper size of buffer.
			if (holder._size < size)
			{
				continue;
			}
			
			// Get NodeBuffer.
			current = holder.getBuffer();
			if (current != null)
			{
				return current;
			}
		}
		
		return current;
	}
	
	/**
	 * Loads geodata from a file. When file does not exist, is corrupted or not consistent, loads none geodata.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
	 * @return boolean : True, when geodata file was loaded without problem.
	 */
	private boolean loadGeoBlocks(int regionX, int regionY)
	{
		final String filename = String.format(Config.GEODATA_TYPE.getFilename(), regionX, regionY);
		final String filepath = Config.GEODATA_PATH + File.separator + filename;
		
		// Standard load.
		try (RandomAccessFile raf = new RandomAccessFile(filepath, "r");
			FileChannel fc = raf.getChannel())
		{
			// Initialize file buffer.
			MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			// Load 18B header for L2OFF geodata (1st and 2nd byte...region X and Y).
			if (Config.GEODATA_TYPE == GeoType.L2OFF)
			{
				for (int i = 0; i < 18; i++)
				{
					buffer.get();
				}
			}
			
			// Get block indexes.
			final int blockX = (regionX - World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
			final int blockY = (regionY - World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
			
			// Loop over region blocks.
			for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
			{
				for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
				{
					if (Config.GEODATA_TYPE == GeoType.L2J)
					{
						// Get block type.
						final byte type = buffer.get();
						
						// Load block according to block type.
						switch (type)
						{
							case GeoStructure.TYPE_FLAT_L2J_L2OFF:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockFlat(buffer, Config.GEODATA_TYPE);
								break;
							}
							case GeoStructure.TYPE_COMPLEX_L2J:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockComplex(buffer);
								break;
							}
							case GeoStructure.TYPE_MULTILAYER_L2J:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockMultilayer(buffer, Config.GEODATA_TYPE);
								break;
							}
							default:
							{
								throw new IllegalArgumentException("Unknown block type: " + type);
							}
						}
					}
					else
					{
						// Get block type.
						final short type = buffer.getShort();
						
						// Load block according to block type.
						switch (type)
						{
							case GeoStructure.TYPE_FLAT_L2J_L2OFF:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockFlat(buffer, Config.GEODATA_TYPE);
								break;
							}
							case GeoStructure.TYPE_COMPLEX_L2OFF:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockComplex(buffer);
								break;
							}
							default:
							{
								_blocks[blockX + ix][blockY + iy] = new BlockMultilayer(buffer, Config.GEODATA_TYPE);
								break;
							}
						}
					}
				}
			}
			
			// Check data consistency.
			if (buffer.remaining() > 0)
			{
				LOGGER.warning("Region file " + filename + " can be corrupted, remaining " + buffer.remaining() + " bytes to read.");
			}
			
			// Loading was successful.
			return true;
		}
		catch (Exception e)
		{
			// An error occured while loading, load null blocks.
			LOGGER.warning("Error loading " + filename + " region file. " + e);
			
			// Replace whole region file with null blocks.
			loadNullBlocks(regionX, regionY);
			
			// Loading was not successful.
			return false;
		}
	}
	
	/**
	 * Loads null blocks. Used when no region file is detected or an error occurs during loading.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
	 */
	private void loadNullBlocks(int regionX, int regionY)
	{
		// Get block indexes.
		final int blockX = (regionX - World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
		final int blockY = (regionY - World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
		
		// Load all null blocks.
		for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
		{
			for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
			{
				_blocks[blockX + ix][blockY + iy] = _nullBlock;
			}
		}
	}
	
	/**
	 * Converts world X to geodata X.
	 * @param worldX
	 * @return int : Geo X
	 */
	public static int getGeoX(int worldX)
	{
		return (worldX - World.WORLD_X_MIN) >> 4;
	}
	
	/**
	 * Converts world Y to geodata Y.
	 * @param worldY
	 * @return int : Geo Y
	 */
	public static int getGeoY(int worldY)
	{
		return (worldY - World.WORLD_Y_MIN) >> 4;
	}
	
	/**
	 * Converts geodata X to world X.
	 * @param geoX
	 * @return int : World X
	 */
	public static int getWorldX(int geoX)
	{
		return (geoX << 4) + World.WORLD_X_MIN + 8;
	}
	
	/**
	 * Converts geodata Y to world Y.
	 * @param geoY
	 * @return int : World Y
	 */
	public static int getWorldY(int geoY)
	{
		return (geoY << 4) + World.WORLD_Y_MIN + 8;
	}
	
	/**
	 * Returns block of geodata on given coordinates.
	 * @param geoX : Geodata X
	 * @param geoY : Geodata Y
	 * @return {@link ABlock} : Block of geodata.
	 */
	public ABlock getBlock(int geoX, int geoY)
	{
		final int x = geoX / GeoStructure.BLOCK_CELLS_X;
		if ((x < 0) || (x >= GeoStructure.GEO_BLOCKS_X))
		{
			return null;
		}
		final int y = geoY / GeoStructure.BLOCK_CELLS_Y;
		if ((y < 0) || (y >= GeoStructure.GEO_BLOCKS_Y))
		{
			return null;
		}
		return _blocks[x][y];
	}
	
	/**
	 * Check if geo coordinates has geo.
	 * @param geoX : Geodata X
	 * @param geoY : Geodata Y
	 * @return boolean : True, if given geo coordinates have geodata
	 */
	public boolean hasGeoPos(int geoX, int geoY)
	{
		final ABlock block = getBlock(geoX, geoY);
		return (block != null) && block.hasGeoPos();
	}
	
	/**
	 * Returns the height of cell, which is closest to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, closest to given coordinates.
	 */
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		final ABlock block = getBlock(geoX, geoY);
		if (block == null)
		{
			return (short) worldZ;
		}
		return block.getHeightNearest(geoX, geoY, worldZ);
	}
	
	/**
	 * Returns the NSWE flag byte of cell, which is closes to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell NSWE flag byte coordinate, closest to given coordinates.
	 */
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		final ABlock block = getBlock(geoX, geoY);
		if (block == null)
		{
			return GeoStructure.CELL_FLAG_ALL;
		}
		return block.getNsweNearest(geoX, geoY, worldZ);
	}
	
	/**
	 * Check if world coordinates has geo.
	 * @param worldX : World X
	 * @param worldY : World Y
	 * @return boolean : True, if given world coordinates have geodata
	 */
	public boolean hasGeo(int worldX, int worldY)
	{
		return hasGeoPos(getGeoX(worldX), getGeoY(worldY));
	}
	
	/**
	 * Returns closest Z coordinate according to geodata.
	 * @param loc : The location used as reference.
	 * @return short : nearest Z coordinates according to geodata
	 */
	public short getHeight(Location loc)
	{
		return getHeightNearest(getGeoX(loc.getX()), getGeoY(loc.getY()), loc.getZ());
	}
	
	/**
	 * Returns closest Z coordinate according to geodata.
	 * @param worldX : world x
	 * @param worldY : world y
	 * @param worldZ : world z
	 * @return short : nearest Z coordinates according to geodata
	 */
	public short getHeight(int worldX, int worldY, int worldZ)
	{
		return getHeightNearest(getGeoX(worldX), getGeoY(worldY), worldZ);
	}
	
	/**
	 * Check line of sight from {@link WorldObject} to {@link WorldObject}.<br>
	 * @param object : The origin object.
	 * @param target : The target object.
	 * @return True, when object can see target.
	 */
	public boolean canSeeTarget(WorldObject object, WorldObject target)
	{
		// Can always see doors.
		if (target.isDoor())
		{
			return true;
		}
		
		if (object.getInstanceId() != target.getInstanceId())
		{
			return false;
		}
		
		if (DoorData.getInstance().checkIfDoorsBetween(object.getX(), object.getY(), object.getZ(), target.getX(), target.getY(), target.getZ(), object.getInstanceId(), false))
		{
			return false;
		}
		
		if (FenceData.getInstance().checkIfFenceBetween(object.getX(), object.getY(), object.getZ(), target.getX(), target.getY(), target.getZ(), object.getInstanceId()))
		{
			return false;
		}
		
		// Get object's and target's line of sight height (if relevant).
		// Note: real creature height = collision height * 2
		double oheight = 0;
		if (object instanceof Creature)
		{
			oheight += (((Creature) object).getTemplate().getCollisionHeight() * 2 * Config.PART_OF_CHARACTER_HEIGHT) / 100;
		}
		
		double theight = 0;
		if (target instanceof Creature)
		{
			theight += (((Creature) target).getTemplate().getCollisionHeight() * 2 * Config.PART_OF_CHARACTER_HEIGHT) / 100;
		}
		
		return canSee(object.getX(), object.getY(), object.getZ(), oheight, target.getX(), target.getY(), target.getZ(), theight) && canSee(target.getX(), target.getY(), target.getZ(), theight, object.getX(), object.getY(), object.getZ(), oheight);
	}
	
	/**
	 * Check line of sight from {@link WorldObject} to {@link Location}.<br>
	 * Note: The check uses {@link Location}'s real Z coordinate (e.g. point above ground), not its geodata representation.
	 * @param object : The origin object.
	 * @param position : The target position.
	 * @return True, when object can see position.
	 */
	public boolean canSeeLocation(WorldObject object, Location position)
	{
		// Get object and location coordinates.
		int ox = object.getX();
		int oy = object.getY();
		int oz = object.getZ();
		int tx = position.getX();
		int ty = position.getY();
		int tz = position.getZ();
		
		if (DoorData.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz, object.getInstanceId(), false))
		{
			return false;
		}
		
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz, object.getInstanceId()))
		{
			return false;
		}
		
		// Get object's line of sight height (if relevant).
		// Note: real creature height = collision height * 2
		double oheight = 0;
		if (object instanceof Creature)
		{
			oheight += (((Creature) object).getTemplate().getCollisionHeight() * 2 * Config.PART_OF_CHARACTER_HEIGHT) / 100;
		}
		
		// Perform geodata check.
		return canSee(ox, oy, oz, oheight, tx, ty, tz, 0) && canSee(tx, ty, tz, 0, ox, oy, oz, oheight);
	}
	
	/**
	 * Simple check for origin to target visibility.<br>
	 * @param ox : Origin X coordinate.
	 * @param oy : Origin Y coordinate.
	 * @param oz : Origin Z coordinate.
	 * @param oheight : The height of origin, used as start point.
	 * @param tx : Target X coordinate.
	 * @param ty : Target Y coordinate.
	 * @param tz : Target Z coordinate.
	 * @param theight : The height of target, used as end point.
	 * @return True, when origin can see target.
	 */
	public boolean canSee(int ox, int oy, int oz, double oheight, int tx, int ty, int tz, double theight)
	{
		// Get origin geodata coordinates.
		int gox = getGeoX(ox);
		int goy = getGeoY(oy);
		ABlock block = getBlock(gox, goy);
		if ((block == null) || !block.hasGeoPos())
		{
			return true; // No Geodata found.
		}
		
		// Get target geodata coordinates.
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		
		// Check being on same cell and layer (index).
		// Note: Get index must use origin height increased by cell height, the method returns index to height exclusive self.
		int index = block.getIndexNearest(gox, goy, oz + GeoStructure.CELL_HEIGHT); // getIndexBelow
		if (index < 0)
		{
			return false;
		}
		
		if ((gox == gtx) && (goy == gty))
		{
			return index == block.getIndexNearest(gtx, gty, tz + GeoStructure.CELL_HEIGHT); // getIndexBelow
		}
		
		// Get ground and nswe flag.
		int groundZ = block.getHeight(index);
		int nswe = block.getNswe(index);
		
		// Get delta coordinates, slope of line (XY, XZ) and direction data.
		final int dx = tx - ox;
		final int dy = ty - oy;
		final double dz = (tz + theight) - (oz + oheight);
		final double m = (double) dy / dx;
		final double mz = dz / Math.sqrt((dx * dx) + (dy * dy));
		final MoveDirectionType mdt = MoveDirectionType.getDirection(gtx - gox, gty - goy);
		
		// Get cell grid coordinates.
		int gridX = ox & 0xFFFFFFF0;
		int gridY = oy & 0xFFFFFFF0;
		
		// Run loop.
		byte dir;
		while ((gox != gtx) || (goy != gty))
		{
			// Calculate intersection with cell's X border.
			int checkX = gridX + mdt.getOffsetX();
			int checkY = (int) (oy + (m * (checkX - ox)));
			
			if ((mdt.getStepX() != 0) && (getGeoY(checkY) == goy))
			{
				// Set next cell in X direction.
				gridX += mdt.getStepX();
				gox += mdt.getSignumX();
				dir = mdt.getDirectionX();
			}
			else
			{
				// Calculate intersection with cell's Y border.
				checkY = gridY + mdt.getOffsetY();
				checkX = (int) (ox + ((checkY - oy) / m));
				checkX = CommonUtil.limit(checkX, gridX, gridX + 15);
				
				// Set next cell in Y direction.
				gridY += mdt.getStepY();
				goy += mdt.getSignumY();
				dir = mdt.getDirectionY();
			}
			
			// Get block of the next cell.
			block = getBlock(gox, goy);
			if ((block == null) || !block.hasGeoPos())
			{
				return true; // No Geodata found.
			}
			
			// Get line of sight height (including Z slope).
			double losz = oz + oheight + Config.MAX_OBSTACLE_HEIGHT;
			losz += mz * Math.sqrt(((checkX - ox) * (checkX - ox)) + ((checkY - oy) * (checkY - oy)));
			
			// Check line of sight going though wall (vertical check).
			
			// Get index of particular layer, based on last iterated cell conditions.
			boolean canMove = (nswe & dir) != 0;
			if (canMove)
			{
				// No wall present, get next cell below current cell.
				index = block.getIndexBelow(gox, goy, groundZ + GeoStructure.CELL_IGNORE_HEIGHT);
			}
			else
			{
				// Wall present, get next cell above current cell.
				index = block.getIndexAbove(gox, goy, groundZ - (2 * GeoStructure.CELL_HEIGHT));
			}
			
			// Next cell's does not exist (no geodata with valid condition), return fail.
			if (index < 0)
			{
				return false;
			}
			
			// Get next cell's layer height.
			int z = block.getHeight(index);
			
			// Perform sine of sight check (next cell is above line of sight line), return fail.
			if (z > losz)
			{
				return false;
			}
			
			// Next cell is accessible, update z and NSWE.
			groundZ = z;
			nswe = block.getNswe(index);
		}
		
		// Iteration is completed, no obstacle is found.
		return true;
	}
	
	/**
	 * Check movement from coordinates to coordinates.<br>
	 * Note: The Z coordinates are supposed to be already validated geodata coordinates.
	 * @param ox : Origin X coordinate.
	 * @param oy : Origin Y coordinate.
	 * @param oz : Origin Z coordinate.
	 * @param tx : Target X coordinate.
	 * @param ty : Target Y coordinate.
	 * @param tz : Target Z coordinate.
	 * @param instanceId
	 * @return True, when target coordinates are reachable from origin coordinates.
	 */
	public boolean canMoveToTarget(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz, instanceId, false))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz, instanceId))
		{
			return false;
		}
		
		// Get geodata coordinates.
		int gox = getGeoX(ox);
		int goy = getGeoY(oy);
		ABlock block = getBlock(gox, goy);
		if ((block == null) || !block.hasGeoPos())
		{
			return true; // No Geodata found.
		}
		int goz = getHeightNearest(gox, goy, oz);
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		
		// Check movement within same cell.
		if ((gox == gtx) && (goy == gty))
		{
			return goz == getHeight(tx, ty, tz);
		}
		
		// Get nswe flag.
		int nswe = getNsweNearest(gox, goy, goz);
		
		// Get delta coordinates, slope of line and direction data.
		final int dx = tx - ox;
		final int dy = ty - oy;
		final double m = (double) dy / dx;
		final MoveDirectionType mdt = MoveDirectionType.getDirection(gtx - gox, gty - goy);
		
		// Get cell grid coordinates.
		int gridX = ox & 0xFFFFFFF0;
		int gridY = oy & 0xFFFFFFF0;
		
		// Run loop.
		byte dir;
		int nx = gox;
		int ny = goy;
		while ((gox != gtx) || (goy != gty))
		{
			// Calculate intersection with cell's X border.
			int checkX = gridX + mdt.getOffsetX();
			int checkY = (int) (oy + (m * (checkX - ox)));
			
			if ((mdt.getStepX() != 0) && (getGeoY(checkY) == goy))
			{
				// Set next cell is in X direction.
				gridX += mdt.getStepX();
				nx += mdt.getSignumX();
				dir = mdt.getDirectionX();
			}
			else
			{
				// Calculate intersection with cell's Y border.
				checkY = gridY + mdt.getOffsetY();
				checkX = (int) (ox + ((checkY - oy) / m));
				checkX = CommonUtil.limit(checkX, gridX, gridX + 15);
				
				// Set next cell in Y direction.
				gridY += mdt.getStepY();
				ny += mdt.getSignumY();
				dir = mdt.getDirectionY();
			}
			
			// Check point heading into obstacle, if so return current point.
			if ((nswe & dir) == 0)
			{
				return false;
			}
			
			block = getBlock(nx, ny);
			if ((block == null) || !block.hasGeoPos())
			{
				return true; // No Geodata found.
			}
			
			// Check next point for extensive Z difference, if so return current point.
			final int i = block.getIndexBelow(nx, ny, goz + GeoStructure.CELL_IGNORE_HEIGHT);
			if (i < 0)
			{
				return false;
			}
			
			// Update current point's coordinates and nswe.
			gox = nx;
			goy = ny;
			goz = block.getHeight(i);
			nswe = block.getNswe(i);
		}
		
		// When origin Z is target Z, the move is successful.
		return goz == getHeight(tx, ty, tz);
	}
	
	/**
	 * Check movement from origin to target coordinates. Returns last available point in the checked path.<br>
	 * Target X and Y reachable and Z is on same floor:
	 * <ul>
	 * <li>Location of the target with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y reachable but Z is on another floor:
	 * <ul>
	 * <li>Location of the origin with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y not reachable:
	 * <ul>
	 * <li>Last accessible location in destination to target.</li>
	 * </ul>
	 * @param ox : Origin X coordinate.
	 * @param oy : Origin Y coordinate.
	 * @param oz : Origin Z coordinate.
	 * @param tx : Target X coordinate.
	 * @param ty : Target Y coordinate.
	 * @param tz : Target Z coordinate.
	 * @param instanceId
	 * @return The {@link Location} representing last point of movement (e.g. just before wall).
	 */
	public Location getValidLocation(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz, instanceId, false))
		{
			return new Location(ox, oy, oz);
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz, instanceId))
		{
			return new Location(ox, oy, oz);
		}
		
		// Get geodata coordinates.
		int gox = getGeoX(ox);
		int goy = getGeoY(oy);
		ABlock block = getBlock(gox, goy);
		if ((block == null) || !block.hasGeoPos())
		{
			return new Location(tx, ty, tz); // No Geodata found.
		}
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		final int gtz = getHeightNearest(gtx, gty, tz);
		int goz = getHeightNearest(gox, goy, oz);
		int nswe = getNsweNearest(gox, goy, goz);
		
		// Get delta coordinates, slope of line and direction data.
		final int dx = tx - ox;
		final int dy = ty - oy;
		final double m = (double) dy / dx;
		final MoveDirectionType mdt = MoveDirectionType.getDirection(gtx - gox, gty - goy);
		
		// Get cell grid coordinates.
		int gridX = ox & 0xFFFFFFF0;
		int gridY = oy & 0xFFFFFFF0;
		
		// Run loop.
		byte dir;
		int nx = gox;
		int ny = goy;
		while ((gox != gtx) || (goy != gty))
		{
			// Calculate intersection with cell's X border.
			int checkX = gridX + mdt.getOffsetX();
			int checkY = (int) (oy + (m * (checkX - ox)));
			
			if ((mdt.getStepX() != 0) && (getGeoY(checkY) == goy))
			{
				// Set next cell is in X direction.
				gridX += mdt.getStepX();
				nx += mdt.getSignumX();
				dir = mdt.getDirectionX();
			}
			else
			{
				// Calculate intersection with cell's Y border.
				checkY = gridY + mdt.getOffsetY();
				checkX = (int) (ox + ((checkY - oy) / m));
				checkX = CommonUtil.limit(checkX, gridX, gridX + 15);
				
				// Set next cell in Y direction.
				gridY += mdt.getStepY();
				ny += mdt.getSignumY();
				dir = mdt.getDirectionY();
			}
			
			// Check target cell is out of geodata grid (world coordinates).
			if ((nx < 0) || (nx >= GeoStructure.GEO_CELLS_X) || (ny < 0) || (ny >= GeoStructure.GEO_CELLS_Y))
			{
				return new Location(checkX, checkY, goz);
			}
			
			// Check point heading into obstacle, if so return current (border) point.
			if ((nswe & dir) == 0)
			{
				return new Location(checkX, checkY, goz);
			}
			
			block = getBlock(nx, ny);
			if ((block == null) || !block.hasGeoPos())
			{
				return new Location(tx, ty, tz); // No Geodata found.
			}
			
			// Check next point for extensive Z difference, if so return current (border) point.
			final int i = block.getIndexBelow(nx, ny, goz + GeoStructure.CELL_IGNORE_HEIGHT);
			if (i < 0)
			{
				return new Location(checkX, checkY, goz);
			}
			
			// Update current point's coordinates and nswe.
			gox = nx;
			goy = ny;
			goz = block.getHeight(i);
			nswe = block.getNswe(i);
		}
		
		// Compare Z coordinates:
		// If same, path is okay, return target point and fix its Z geodata coordinate.
		// If not same, path is does not exist, return origin point.
		return goz == gtz ? new Location(tx, ty, gtz) : new Location(ox, oy, oz);
	}
	
	/**
	 * Returns the list of location objects as a result of complete path calculation.
	 * @param ox : origin x
	 * @param oy : origin y
	 * @param oz : origin z
	 * @param tx : target x
	 * @param ty : target y
	 * @param tz : target z
	 * @param instanceId
	 * @return {@code List<Location>} : complete path from nodes
	 */
	public List<Location> findPath(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// Get origin and check existing geo coords.
		int gox = getGeoX(ox);
		int goy = getGeoY(oy);
		if (!hasGeoPos(gox, goy))
		{
			return Collections.emptyList();
		}
		
		int goz = getHeightNearest(gox, goy, oz);
		
		// Get target and check existing geo coords.
		int gtx = getGeoX(tx);
		int gty = getGeoY(ty);
		if (!hasGeoPos(gtx, gty))
		{
			return Collections.emptyList();
		}
		
		int gtz = getHeightNearest(gtx, gty, tz);
		
		// Prepare buffer for pathfinding calculations.
		int dx = Math.abs(gox - gtx);
		int dy = Math.abs(goy - gty);
		int dz = Math.abs(goz - gtz) / 8;
		int total = dx + dy + dz;
		int size = 1000 + (10 * total);
		NodeBuffer buffer = getBuffer(size);
		if (buffer == null)
		{
			return Collections.emptyList();
		}
		
		// Find path.
		List<Location> path = null;
		try
		{
			path = buffer.findPath(gox, goy, goz, gtx, gty, gtz);
			if (path.isEmpty())
			{
				return Collections.emptyList();
			}
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
		finally
		{
			buffer.free();
		}
		
		// Check path.
		if (path.size() < 3)
		{
			return path;
		}
		
		// Get path list iterator.
		ListIterator<Location> point = path.listIterator();
		
		// Get node A (origin).
		int nodeAx = ox;
		int nodeAy = oy;
		int nodeAz = goz;
		
		// Get node B.
		Location nodeB = point.next();
		
		// Iterate thought the path to optimize it.
		while (point.hasNext())
		{
			// Get node C.
			Location nodeC = path.get(point.nextIndex());
			
			// Check movement from node A to node C.
			if (canMoveToTarget(nodeAx, nodeAy, nodeAz, nodeC.getX(), nodeC.getY(), nodeC.getZ(), instanceId))
			{
				// Can move from node A to node C.
				// Remove node B.
				point.remove();
			}
			else
			{
				// Can not move from node A to node C.
				// Set node A (node B is part of path, update A coordinates).
				nodeAx = nodeB.getX();
				nodeAy = nodeB.getY();
				nodeAz = nodeB.getZ();
			}
			
			// Set node B.
			nodeB = point.next();
		}
		
		return path;
	}
	
	/**
	 * NodeBuffer container with specified size and count of separate buffers.
	 */
	private static class BufferHolder
	{
		final int _size;
		final int _count;
		final Set<NodeBuffer> _buffer;
		
		public BufferHolder(int size, int count)
		{
			_size = size;
			_count = count * 4;
			_buffer = ConcurrentHashMap.newKeySet(_count);
			
			for (int i = 0; i < count; i++)
			{
				_buffer.add(new NodeBuffer(size));
			}
		}
		
		public NodeBuffer getBuffer()
		{
			// Get available free NodeBuffer.
			for (NodeBuffer buffer : _buffer)
			{
				if (!buffer.isLocked())
				{
					continue;
				}
				
				return buffer;
			}
			
			// No free NodeBuffer found, try allocate new buffer.
			if (_buffer.size() < _count)
			{
				NodeBuffer buffer = new NodeBuffer(_size);
				buffer.isLocked();
				_buffer.add(buffer);
				
				if (_buffer.size() == _count)
				{
					LOGGER.warning("NodeBuffer holder with " + _size + " size reached max capacity.");
				}
				
				return buffer;
			}
			
			return null;
		}
	}
	
	/**
	 * Returns the instance of the {@link GeoEngine}.
	 * @return {@link GeoEngine} : The instance.
	 */
	public static GeoEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine INSTANCE = new GeoEngine();
	}
}