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
import java.nio.ByteOrder;
import java.util.Arrays;

import org.l2jmobius.gameserver.enums.GeoType;

public class BlockMultilayer extends ABlock
{
	private static final int MAX_LAYERS = Byte.MAX_VALUE;
	
	private static ByteBuffer _temp;
	
	/**
	 * Initializes the temporary buffer.
	 */
	public static final void initialize()
	{
		// Initialize temporary buffer and sorting mechanism.
		_temp = ByteBuffer.allocate(GeoStructure.BLOCK_CELLS * MAX_LAYERS * 3);
		_temp.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Releases temporary buffer.
	 */
	public static final void release()
	{
		_temp = null;
	}
	
	protected byte[] _buffer;
	
	/**
	 * Implicit constructor for children class.
	 */
	protected BlockMultilayer()
	{
		// Buffer is initialized in children class.
		_buffer = null;
	}
	
	/**
	 * Creates MultilayerBlock.
	 * @param bb : Input byte buffer.
	 * @param type : The type of loaded geodata.
	 */
	public BlockMultilayer(ByteBuffer bb, GeoType type)
	{
		// Move buffer pointer to end of MultilayerBlock.
		for (int cell = 0; cell < GeoStructure.BLOCK_CELLS; cell++)
		{
			// Get layer count for this cell.
			final byte layers = type != GeoType.L2OFF ? bb.get() : (byte) bb.getShort();
			
			if ((layers <= 0) || (layers > MAX_LAYERS))
			{
				throw new RuntimeException("Invalid layer count for MultilayerBlock");
			}
			
			// Add layers count.
			_temp.put(layers);
			
			// Loop over layers.
			for (byte layer = 0; layer < layers; layer++)
			{
				// Get data.
				final short data = bb.getShort();
				
				// Add nswe and height.
				_temp.put((byte) (data & 0x000F));
				_temp.putShort((short) ((short) (data & 0xFFF0) >> 1));
			}
		}
		
		// Initialize buffer.
		_buffer = Arrays.copyOf(_temp.array(), _temp.position());
		
		// Clear temp buffer.
		_temp.clear();
	}
	
	@Override
	public boolean hasGeoPos()
	{
		return true;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		// Get cell index.
		final int index = getIndexNearest(geoX, geoY, worldZ);
		
		// Get height.
		return (short) ((_buffer[index + 1] & 0x00FF) | (_buffer[index + 2] << 8));
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// Get cell index.
		final int index = getIndexNearest(geoX, geoY, worldZ);
		
		// Get nswe.
		return _buffer[index];
	}
	
	@Override
	public int getIndexNearest(int geoX, int geoY, int worldZ)
	{
		// Move index to the cell given by coordinates.
		int index = 0;
		for (int i = 0; i < (((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y) + (geoY % GeoStructure.BLOCK_CELLS_Y)); i++)
		{
			// Move index by amount of layers for this cell.
			index += (_buffer[index] * 3) + 1;
		}
		
		// Get layers count and shift to last layer data (first from bottom).
		byte layers = _buffer[index++];
		
		// Loop though all cell layers, find closest layer to given worldZ.
		int limit = Integer.MAX_VALUE;
		while (layers-- > 0)
		{
			// Get layer height.
			final int height = (_buffer[index + 1] & 0x00FF) | (_buffer[index + 2] << 8);
			
			// Get Z distance and compare with limit.
			// Note: When 2 layers have same distance to worldZ (worldZ is in the middle of them):
			// > Returns bottom layer.
			// >= Returns upper layer.
			final int distance = Math.abs(height - worldZ);
			if (distance > limit)
			{
				break;
			}
			
			// Update limit and move to next layer.
			limit = distance;
			index += 3;
		}
		
		// Return layer index.
		return index - 3;
	}
	
	@Override
	public int getIndexAbove(int geoX, int geoY, int worldZ)
	{
		// Move index to the cell given by coordinates.
		int index = 0;
		for (int i = 0; i < (((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y) + (geoY % GeoStructure.BLOCK_CELLS_Y)); i++)
		{
			// Move index by amount of layers for this cell.
			index += (_buffer[index] * 3) + 1;
		}
		
		// Get layers count and shift to last layer data (first from bottom).
		byte layers = _buffer[index++];
		index += (layers - 1) * 3;
		
		// Loop though all layers, find first layer above worldZ.
		while (layers-- > 0)
		{
			// Get layer height.
			final int height = (_buffer[index + 1] & 0x00FF) | (_buffer[index + 2] << 8);
			
			// Layer height is higher than worldZ, return layer index.
			if (height > worldZ)
			{
				return index;
			}
			
			// Move index to next layer.
			index -= 3;
		}
		
		// No layer found.
		return -1;
	}
	
	@Override
	public int getIndexBelow(int geoX, int geoY, int worldZ)
	{
		// Move index to the cell given by coordinates.
		int index = 0;
		for (int i = 0; i < (((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y) + (geoY % GeoStructure.BLOCK_CELLS_Y)); i++)
		{
			// Move index by amount of layers for this cell.
			index += (_buffer[index] * 3) + 1;
		}
		
		// Get layers count and shift to first layer data (first from top).
		byte layers = _buffer[index++];
		
		// Loop though all layers, find first layer below worldZ.
		while (layers-- > 0)
		{
			// Get layer height.
			final int height = (_buffer[index + 1] & 0x00FF) | (_buffer[index + 2] << 8);
			
			// Layer height is lower than worldZ, return layer index.
			if (height < worldZ)
			{
				return index;
			}
			
			// Move index to next layer.
			index += 3;
		}
		
		// No layer found.
		return -1;
	}
	
	@Override
	public short getHeight(int index)
	{
		// Get height.
		return (short) ((_buffer[index + 1] & 0x00FF) | (_buffer[index + 2] << 8));
	}
	
	@Override
	public byte getNswe(int index)
	{
		// Get nswe.
		return _buffer[index];
	}
}