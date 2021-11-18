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
package org.l2jmobius.gameserver.geoengine.pathfinding;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.geodata.GeoStructure;
import org.l2jmobius.gameserver.model.Location;

public class Node extends Location implements Comparable<Node>
{
	// Node geodata values.
	private int _geoX;
	private int _geoY;
	private byte _nswe;
	private byte _nsweExpand;
	
	// The cost G (movement cost done) and cost H (estimated cost to target).
	private int _costG;
	private int _costH;
	private int _costF;
	
	// Node parent (reverse path construction).
	private Node _parent;
	
	public Node()
	{
		super(0, 0, 0);
	}
	
	@Override
	public void clean()
	{
		super.clean();
		
		_geoX = 0;
		_geoY = 0;
		_nswe = GeoStructure.CELL_FLAG_NONE;
		_nsweExpand = GeoStructure.CELL_FLAG_NONE;
		
		_costG = 0;
		_costH = 0;
		_costF = 0;
		
		_parent = null;
	}
	
	public void setGeo(int gx, int gy, int gz, byte nswe, byte nsweExpand)
	{
		super.setXYZ(GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), gz);
		
		_geoX = gx;
		_geoY = gy;
		_nswe = nswe;
		_nsweExpand = nsweExpand;
	}
	
	public void setCost(Node parent, int weight, int costH)
	{
		_costG = weight;
		if (parent != null)
		{
			_costG += parent._costG;
		}
		_costH = costH;
		_costF = _costG + _costH;
		
		_parent = parent;
	}
	
	public int getGeoX()
	{
		return _geoX;
	}
	
	public int getGeoY()
	{
		return _geoY;
	}
	
	public byte getNSWE()
	{
		return _nswe;
	}
	
	public byte getNsweExpand()
	{
		return _nsweExpand;
	}
	
	public int getCostF()
	{
		return _costF;
	}
	
	public Node getParent()
	{
		return _parent;
	}
	
	@Override
	public int compareTo(Node o)
	{
		return _costF - o._costF;
	}
}