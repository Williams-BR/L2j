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

import java.nio.ByteBuffer;

import org.l2jmobius.gameserver.enums.GeoType;

public class BlockFlat extends ABlock
{
	protected final short _height;
	protected byte _nswe;
	
	/**
	 * Creates FlatBlock.
	 * @param bb : Input byte buffer.
	 * @param type : The type of loaded geodata.
	 */
	public BlockFlat(ByteBuffer bb, GeoType type)
	{
		// Get height and nswe.
		_height = bb.getShort();
		_nswe = GeoStructure.CELL_FLAG_ALL;
		
		// Read dummy data.
		if (type == GeoType.L2OFF)
		{
			bb.getShort();
		}
	}
	
	@Override
	public boolean hasGeoPos()
	{
		return true;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		return _height;
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		return _nswe;
	}
	
	@Override
	public int getIndexNearest(int geoX, int geoY, int worldZ)
	{
		return 0;
	}
	
	@Override
	public int getIndexAbove(int geoX, int geoY, int worldZ)
	{
		// Check height and return index.
		return _height > worldZ ? 0 : -1;
	}
	
	@Override
	public int getIndexBelow(int geoX, int geoY, int worldZ)
	{
		// Check height and return index.
		return _height < worldZ ? 0 : -1;
	}
	
	@Override
	public short getHeight(int index)
	{
		return _height;
	}
	
	@Override
	public byte getNswe(int index)
	{
		return _nswe;
	}
}