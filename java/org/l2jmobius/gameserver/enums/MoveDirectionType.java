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
package org.l2jmobius.gameserver.enums;

import org.l2jmobius.gameserver.geoengine.geodata.GeoStructure;

/**
 * Container of movement constants used for various geodata and movement checks.
 */
public enum MoveDirectionType
{
	N(0, -1),
	S(0, 1),
	W(-1, 0),
	E(1, 0),
	NW(-1, -1),
	SW(-1, 1),
	NE(1, -1),
	SE(1, 1);
	
	// Step and signum.
	private final int _stepX;
	private final int _stepY;
	private final int _signumX;
	private final int _signumY;
	
	// Cell offset.
	private final int _offsetX;
	private final int _offsetY;
	
	// Direction flags.
	private final byte _directionX;
	private final byte _directionY;
	private final String _symbolX;
	private final String _symbolY;
	
	private MoveDirectionType(int signumX, int signumY)
	{
		// Get step (world -16, 0, 16) and signum (geodata -1, 0, 1) coordinates.
		_stepX = signumX * GeoStructure.CELL_SIZE;
		_stepY = signumY * GeoStructure.CELL_SIZE;
		_signumX = signumX;
		_signumY = signumY;
		
		// Get border offsets in a direction of iteration.
		_offsetX = signumX >= 0 ? GeoStructure.CELL_SIZE - 1 : 0;
		_offsetY = signumY >= 0 ? GeoStructure.CELL_SIZE - 1 : 0;
		
		// Get direction NSWE flag and symbol.
		_directionX = signumX < 0 ? GeoStructure.CELL_FLAG_W : signumX == 0 ? 0 : GeoStructure.CELL_FLAG_E;
		_directionY = signumY < 0 ? GeoStructure.CELL_FLAG_N : signumY == 0 ? 0 : GeoStructure.CELL_FLAG_S;
		_symbolX = signumX < 0 ? "W" : signumX == 0 ? "-" : "E";
		_symbolY = signumY < 0 ? "N" : signumY == 0 ? "-" : "S";
	}
	
	public int getStepX()
	{
		return _stepX;
	}
	
	public int getStepY()
	{
		return _stepY;
	}
	
	public int getSignumX()
	{
		return _signumX;
	}
	
	public int getSignumY()
	{
		return _signumY;
	}
	
	public int getOffsetX()
	{
		return _offsetX;
	}
	
	public int getOffsetY()
	{
		return _offsetY;
	}
	
	public byte getDirectionX()
	{
		return _directionX;
	}
	
	public byte getDirectionY()
	{
		return _directionY;
	}
	
	public String getSymbolX()
	{
		return _symbolX;
	}
	
	public String getSymbolY()
	{
		return _symbolY;
	}
	
	/**
	 * @param gdx : Geodata X delta coordinate.
	 * @param gdy : Geodata Y delta coordinate.
	 * @return {@link MoveDirectionType} based on given geodata dx and dy delta coordinates.
	 */
	public static MoveDirectionType getDirection(int gdx, int gdy)
	{
		if (gdx == 0)
		{
			return (gdy < 0) ? MoveDirectionType.N : MoveDirectionType.S;
		}
		
		if (gdy == 0)
		{
			return (gdx < 0) ? MoveDirectionType.W : MoveDirectionType.E;
		}
		
		if (gdx > 0)
		{
			return (gdy < 0) ? MoveDirectionType.NE : MoveDirectionType.SE;
		}
		
		return (gdy < 0) ? MoveDirectionType.NW : MoveDirectionType.SW;
	}
}