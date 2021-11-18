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
package org.l2jmobius.gameserver.geoengine.geodata;

public abstract class ABlock
{
	/**
	 * Checks the block for having geodata.
	 * @return boolean : True, when block has geodata (Flat, Complex, Multilayer).
	 */
	public abstract boolean hasGeoPos();
	
	/**
	 * Returns the height of cell, which is closest to given coordinates.<br>
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, nearest to given coordinates.
	 */
	public abstract short getHeightNearest(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns the NSWE flag byte of cell, which is closest to given coordinates.<br>
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell NSWE flag byte, nearest to given coordinates.
	 */
	public abstract byte getNsweNearest(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns index to data of the cell, which is closes layer to given coordinates.<br>
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return {@code int} : Cell index.
	 */
	public abstract int getIndexNearest(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns index to data of the cell, which is first layer above given coordinates.<br>
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return {@code int} : Cell index. -1..when no layer available below given Z coordinate.
	 */
	public abstract int getIndexAbove(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns index to data of the cell, which is first layer below given coordinates.<br>
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return {@code int} : Cell index. -1..when no layer available below given Z coordinate.
	 */
	public abstract int getIndexBelow(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns the height of cell given by cell index.<br>
	 * @param index : Index of the cell.
	 * @return short : Cell geodata Z coordinate, below given coordinates.
	 */
	public abstract short getHeight(int index);
	
	/**
	 * Returns the NSWE flag byte of cell given by cell index.<br>
	 * @param index : Index of the cell.
	 * @return short : Cell geodata Z coordinate, below given coordinates.
	 */
	public abstract byte getNswe(int index);
}